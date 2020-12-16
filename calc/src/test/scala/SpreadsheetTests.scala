package eoi
package calc

import org.scalatest._
import com.typesafe.config.ConfigValueFactory
import com.github.tototoshi.csv._

class SpreadsheetTests extends FunSpec with Matchers {

  lazy val config = eoi.calc.default

  val files = {
    val dir = new java.io.File("calc/src/test/resources/testing-tables")
    dir.listFiles().filter(_.getName().endsWith(".csv"))
  }

  def parsePeriod(in: String): Period.Period = in match {
    case "M" => Period.Month
    case "W" => Period.Week
    case "4W" => Period.FourWeek
    case "Y" => Period.Year
  }

  object BD {
    def unapply(in: String): Option[BigDecimal] = {
      import cats.implicits._
      Either.catchOnly[NumberFormatException](BigDecimal(in)).toOption
    }
  }

  describe("Access Application compatibility") {
    files.foreach { file =>
      describe(file.getName()) {
        val reader = CSVReader.open(file)
        val lines = reader.all.zipWithIndex.drop(1).filterNot(_._1.mkString.startsWith("#"))
        lines.map { case (line, indexMinus) =>
          val (yearS::periodS::periodNumberS::categoryS::BD(grossPay)::BD(expectedEmployee)::BD(expectedEmployer)::_) =
            line.map(_.trim)

          val startDay = {
            taxPeriodReader.from(ConfigValueFactory.fromAnyRef(yearS, "")) match {
              case Left(e) => throw new IllegalStateException(
                s"Unable to parse tax year/period on line ${indexMinus + 1}: $e"
              )
              case Right(r) => r
            }
          }

          val result = config.calculateClassOne(
            startDay.lowerValue.get.plusDays(1),
            grossPay,
            categoryS(0),
            parsePeriod(periodS),
            periodNumberS.toInt
          )

          val (employee, employer) = result.foldLeft((Zero, Zero)){
            case ((ee_acc, er_acc), (_, (_, ee, er))) => (ee_acc + ee, er_acc + er)
          }

          if(yearS == "2011") {
            // the access app adds a rogue 1.79 into the calculation for 2011...
            ignore(s"Line ${indexMinus + 1} employee's NI") {}
            ignore(s"Line ${indexMinus + 1} employer's NI") {}            
          } else {
            val prefix = s"${file.getName}:${indexMinus + 1} ($yearS, $categoryS, $grossPay over $periodNumberS $periodS)"
            it(s"$prefix employee's NI") {
              employee should be (expectedEmployee +- 0.01)
            }
            it(s"$prefix employer's NI") {
              employer should be (expectedEmployer +- 0.01)
            }
          }
        }
        reader.close()
      }
    }
  }

}
