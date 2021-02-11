/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eoi

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import com.github.tototoshi.csv._
import org.scalatest._
import cats.syntax.either._
import java.time.LocalDate
import java.io._
import cats.data.Chain
import cats.syntax.group._
import cats.instances.tuple._
import cats.instances.bigDecimal._

class ClassOneSpec extends FunSpec with Matchers {

  val config: Configuration = eoi.ConfigLoader.default

  val files = {
    val dir = new File("calc/src/test/resources/testing-tables")
    dir.listFiles().filter(_.getName().endsWith(".csv"))
  }

  val reportDir = {
    val d = new File("target/test-reports")
    if (!d.exists) d.mkdirs
    d
  }

  def parsePeriod(in: String): Period.Period = in match {
    case "M" => Period.Month
    case "W" => Period.Week
    case "4W" => Period.FourWeek
    case "Y" => Period.Year
  }

  describe("Access Application compatibility") {

    val reportFile = {
      val f = new File(reportDir, "class-one.txt")
      if (f.exists) f.delete
      f
    }

    val writer = new BufferedWriter(new FileWriter(reportFile))

    def writeln(in: String = ""): Unit = {
      writer.write(in)
      writer.write(System.lineSeparator())
    }

    val testOut = files.toList.flatMap { file =>
      val reader = CSVReader.open(file)
        val lines = reader.all.zipWithIndex.drop(1).filterNot(_._1.mkString.startsWith("#"))

      val fileResults = lines.foldLeft(List.empty[TestResult[(BigDecimal, BigDecimal)]]){
          case (acc,(line, indexMinus)) =>
          line.map(_.trim) match { 
            case (Int(year)::periodS::Int(periodNumber)::categoryS::Money(grossPay)::Money(expectedEmployee)::Money(expectedEmployer)::xs) =>

              val statusString = s"${file.getName}:${indexMinus + 1}"

              val comments = xs.mkString(",")
              val cosr = comments.contains("COSR")
              val category = categoryS(0)
              val res = config.calculateClassOne(
                LocalDate.of(year, 10, 1),
                ClassOneRowInput(
                  "row1", 
                  grossPay,
                  category,
                  parsePeriod(periodS),
                  periodNumber
                ) :: Nil
              )

              val employee = res.employeeContributions.value
              val employer = res.employerContributions.value              
              if (employee != expectedEmployee || employer != expectedEmployer) {
                var msg: Chain[String] = Chain.empty
                def writeln(in: String = "") = msg = msg :+ in
                val director = comments.contains("director")
                writeln(statusString)
                writeln(statusString.map{_ => '='})
                writeln()                
                writeln(s"  year: $year")
                writeln(s"  period: $periodS")
                if (periodNumber > 1) writeln(s"  periodNumber: $periodNumber")
                writeln(s"  category: $categoryS ")
                writeln(s"  grossPay: $grossPay ")
                writeln()

                val (eeError) = expectedEmployee - employee
                val (erError) = expectedEmployer - employer                

                if (eeError != Zero) {
                  writeln(s"  Employee expected: $expectedEmployee, actual: $employee ($eeError)")
                  writeln()
                  writeln(res.employeeContributions.explain.map("  " + _).mkString("\n"))
                  writeln()
                }

                if (erError != Zero) {
                  writeln(s"  Employer expected: $expectedEmployer, actual: $employer ($erError)")
                  writeln()                  
                  writeln(res.employerContributions.explain.map("  " + _).mkString("\n"))
                  writeln()
                }

                TestFail(year, category, (expectedEmployee, expectedEmployer), (employee, employer), msg.toList) ::acc 
              } else {
                TestPass(year, category, (expectedEmployee, expectedEmployer)) ::acc                 
              }

            case _ => acc
          }
      }

      reader.close()
      fileResults
//      describe(file.getName()) { }
    }

    implicit class RichTestRecord(in: TestResult[(BigDecimal, BigDecimal)]) {
      def failCount: Int = in match {
        case r: TestFail[(BigDecimal, BigDecimal)] =>
          if (r.expected._1 == r.actual._1 || r.expected._2 == r.actual._2)
            1
          else
            2
        case _ => 0 
      }
    }


    implicit class RichTestRecordList(in: List[TestResult[(BigDecimal, BigDecimal)]]) {
      def status: String = {
        val potentialScore = in.size * 2
        val fail = in.map(_.failCount).sum
        val pass = potentialScore - fail
        val percentage = BigDecimal(pass) / BigDecimal(potentialScore) * 100
        f"$pass/$potentialScore ($percentage%.2f%%)"
      }
    }

    val passRecords: List[TestPass[(BigDecimal, BigDecimal)]] = testOut.collect{
      case x: TestPass[(BigDecimal, BigDecimal)] => x
    }
    val failRecords = testOut.collect{
      case x: TestFail[(BigDecimal, BigDecimal)] => x
    }

    val total = testOut.size * 2

    val overall = s"Overall: ${testOut.status}"
    writeln(overall)
    println(overall)

    writeln()
    writeln("Error breakdown by year:")
    testOut.groupBy(_.year).toList.sortBy(_._1).foreach { case (year, records) =>
      writeln(s"$year: ${records.status}")
    }

    writeln()
    writeln("Error breakdown by category:")
    testOut.groupBy(_.category)
      .toList
      .sortBy(x => x._1)
      .foreach { case (category, records) =>
        writeln(s"$category: ${records.status}")
      }

    val errorBands = List(0.02,0.05,0.1,0.25,0.50,1,2,4,8)

    writeln()
    writeln("Error breakdown by margin:")
    failRecords.flatMap{x =>
      val dev = x.expected |-| x.actual
      List(dev._1.abs, dev._2.abs)
    }.filter(_ != Zero).map { e =>
      errorBands.find(_ >= e) match {
        case Some(a) => s"≤$a"
        case None => s">${errorBands.last}"
      }
    }.groupBy(identity)
      .mapValues(_.size)
      .toList
      .sortBy(x => (x._1.tail, -x._1.head))
      .foreach { case (amt, qty) =>
        writeln(f"$amt:$qty ${(BigDecimal(qty)/total) * 100}%.2f%%")
      }

    writeln()
    val badness = failRecords.map{x =>
      val dev = x.expected |-| x.actual
      dev._1.abs + dev._2.abs
    }.sum
    writeln(f"total badness: $badness%,.2f" )


    writeln()
    writeln()
    failRecords.sortBy{x =>
      val dev = x.expected |-| x.actual
      -dev._1.abs.max(dev._2.abs)
    }.foreach { r =>
      r.explanation.foreach(writeln)
    }

    writer.close()
  }  
}
