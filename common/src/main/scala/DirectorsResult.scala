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

package main.scala

import cats.{Eq, Order}
import cats.syntax.order._
import eoi.{Period, RateDefinition}
import cats.instances.char._
import spire.math.Interval
import eoi._

import java.time.LocalDate
import scala.math.BigDecimal.RoundingMode

case class DirectorsRowInput(
                              category: Char,
                              grossPay: BigDecimal,
                              rowId: String)

case class DirectorsResult(
                            from: LocalDate,
                            to: LocalDate,
                            config: Map[String, RateDefinition],
                            rowsInput: List[DirectorsRowInput],
                            appropriatePersonalPensionScheme: Option[Boolean],
                            netPaid: BigDecimal = Zero,
                            employeePaid: BigDecimal = Zero
                          ) extends ClassOneResultLike {

  import DirectorsResult._

  private def taxWeekNumber(date: LocalDate): Int = {
    val taxYearStartDate: LocalDate = {
      val startYear =
        if (date >= LocalDate.of(date.getYear, 4, 6))
          date.getYear
        else
          date.getYear - 1

      LocalDate.of(startYear, 4, 6)
    }

    Interval(taxYearStartDate, date)
      .numberOfWeeks(RoundingMode.DOWN)
      .getOrElse(
        sys.error(s"Could not work  number of weeks between $taxYearStartDate and $date")
      )
  }


  val (period, periodQuantity, proRata) = {
    val proRataWeeks = (52 - taxWeekNumber(from)).max(1)
    if(proRataWeeks == 52) (Period.Year, 1, false)
    else                   (Period.Week, proRataWeeks, true)
  }

  lazy val rowsOutput: List[ClassOneRowOutput] = {
    val categoryOrder =
      if(from.getYear >= 2016) categoryOrder2016Onwards
      else categoryOrderPre2016(appropriatePersonalPensionScheme.exists(identity))

    val sortedRows = rowsInput.sortWithOrder(categoryOrder)(_.category)
    sortedRows.foldLeft(List.empty[ClassOneRowOutput] -> Zero){ case ((acc, precededAmount), directorsRowInput) =>
      val rowOutput =
        ClassOneRowOutput(
          from,
          config,
          directorsRowInput.rowId,
          directorsRowInput.grossPay,
          directorsRowInput.category,
          period,
          periodQuantity,
          precededAmount,
          proRata = proRata
        )
      (rowOutput :: acc) -> (precededAmount + directorsRowInput.grossPay)
    }._1.reverse
  }

  def grossPay: Explained[BigDecimal] = rowsInput.map{_.grossPay}.sum gives
    s"grossPay: ${rowsInput.map(_.grossPay).mkString(" + ")}"


}

object DirectorsResult {

  implicit class ListOps[A](private val l: List[A]){

    def sortWithOrder[B](order: List[B])(f: A => B)(implicit eq: Eq[B]): List[A] = {

      def loop(toSort: List[(A, B)], order: List[B], acc: List[A]): List[A]  =
        if(toSort.isEmpty) acc
        else order match {
            case Nil => acc
            case head :: tail =>
              val (toTake, toSortStill) = toSort.partition(_._2 === head)
              loop(toSortStill, tail, acc ::: toTake.map(_._1))
        }


      val mappedValues = l.map(f)

      mappedValues.distinct.diff(order) match {
        case Nil => loop(l.zip(mappedValues), order, Nil)
        case unknownValues => throw new IllegalArgumentException(s"Could not sort on unknown values: ${unknownValues.mkString(", ")}")
      }

    }

  }

  val categoryOrder2016Onwards: List[Char] =
    List('B', 'M', 'A', 'Z', 'J', 'C', 'H')

  def categoryOrderPre2016(app: Boolean): List[Char] =
    if(app)
      List('G','E', 'B', 'F', 'I', 'D', 'M', 'A', 'S', 'K', 'L', 'Z', 'J', 'C')
    else
      List('G','E', 'B', 'M', 'A', 'F', 'I', 'D', 'Z', 'J', 'S', 'K', 'L', 'C')


  implicit val localDateOrder: Order[LocalDate] = Order.from(_ compareTo _)

}
