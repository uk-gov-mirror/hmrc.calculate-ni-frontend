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

import cats.syntax.option._
import java.time.LocalDate
import java.time.LocalDate.{of => date}
import org.scalatest._
import spire.math.Interval


class UnpaidInterestSpec extends FunSpec with Matchers {

  describe("An Interval[LocalDate]") {
    it("should give the correct number of days") {
      assert(Interval.openUpper(date(2012,1,1), date(2012,1,2)).numberOfDays === 1.some)
      assert(Interval.closed(date(2012,1,1), date(2012,1,2)).numberOfDays === 2.some)
    }
  }

  val rates: Map[Interval[LocalDate], BigDecimal] = List(
    date(2009,1,6).some -> "0.045",
    date(2009,1,27).some -> "0.035",
    date(2009,3,24).some -> "0.025",
    date(2009,9,29).some -> "0.03",
    date(2016,8,23).some -> "0.0275",
    none -> ""
  ).sliding(2).map {
    case (Some(start), rate) :: (Some(end), _) :: Nil =>
      Interval.openUpper(start, end) -> rate
    case (Some(start), rate) :: (None, _) :: Nil =>
      Interval.atOrAbove(start) -> rate
  }.toMap.mapValues(BigDecimal.apply)

  def calcInterest(amt: BigDecimal, on: LocalDate): Explained[BigDecimal] = {
    import cats.implicits._
    val debtInterval = Interval.open(on, LocalDate.now)
    val amounts: List[Explained[BigDecimal]] =
      rates.toList.flatMap { case (i, annualizedRate) =>
        if (i intersects debtInterval) {
          val intersection = i.intersect(debtInterval)
          intersection.numberOfDays match {
            case Some(days) =>
              val daysAmnesty = (intersection.upperValue.get.getYear - intersection.lowerValue.get.getYear) + 2

              val r = ((days - daysAmnesty) * annualizedRate / 365) gives
              f"(daysIn($intersection) - daysAmnesty) * rate / 365 = ($days - $daysAmnesty) * $annualizedRate / 365"
              r :: Nil
            case _ => Nil
          }

        } else Nil
      }
    amounts.sequence.flatMap { amts =>
      (amts.sum * amt).roundNi gives s"⌊Σ(${amts.map(_.roundNi).mkString(",")})⌋ * $amt"
    }
  }

  describe("An interest calculation") {

    val scenarios = List(
      (500, 2015, 66.54), // 1 day and 6 days respectively for each band
      (500, 2016, 52.40), // 1391 days expected, 1397, out by 6
      (500, 2017, 38.69), // 1027 expected, 1032 computed, out by 5 days
      (500, 2018, 24.98) // 663 days expected, 667 computed, out by 4 days
    )

    scenarios.zipWithIndex.foreach { case ((amt, year, expected), i) =>

      val taxYear = date(year + 1, 4, 19)

      it(s"should align with a test case $i") {
        val s = calcInterest(500, taxYear)
        assert(s.value === expected, "\n  " + s.explain.mkString("\n  "))
      }
    }


  }
}
