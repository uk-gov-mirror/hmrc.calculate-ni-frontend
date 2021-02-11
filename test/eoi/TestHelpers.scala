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

import cats.syntax.either._
import java.time.LocalDate

object Int {
  def unapply(in: String): Option[Int] = {
    Either.catchOnly[NumberFormatException](in.toInt).toOption
  }
}

object Money {
  def unapply(in: String): Option[BigDecimal] = {
    Either.catchOnly[NumberFormatException](BigDecimal(in)).toOption
  }
}

object PeriodParse {
  def unapply(in: String): Option[Period.Period] = Some(in).collect {
    case "M" => Period.Month
    case "W" => Period.Week
    case "4W" => Period.FourWeek
    case "Y" => Period.Year
  }
}

object Date{
  def unapply(in: String): Option[LocalDate] = {

    def gb = in.trim.split("/").toList match {
      case (Int(d)::Int(m)::Int(y)::Nil) => Some(LocalDate.of(y,m,d))
      case _ => None
    }

    def iso =
      Either.catchOnly[java.time.format.DateTimeParseException](
        LocalDate.parse(in.trim)
      ).toOption

    gb orElse iso
  }
}

sealed trait TestResult[A] {
  def year: Int
  def category: Char
  def expected: A
  def actual: A
  def pass: Boolean
} 

case class TestFail[A](
  year: Int, 
  category: Char,
  expected: A,
  actual: A,
  explanation: List[String]
) extends TestResult[A] {
  def pass = false
}

case class TestPass[A](
  year: Int, 
  category: Char,
  expected: A
) extends TestResult[A] {
  def actual = expected
  def pass = true
}

trait TestHelpers {

}

object TestHelpers extends TestHelpers
