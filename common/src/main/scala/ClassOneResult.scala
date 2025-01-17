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

import spire.math.Interval
import cats.syntax.applicative._
import cats.syntax.traverse._
import cats.instances.list._
import cats.syntax.apply._
import spire.implicits._
import spire.math.interval.{Bound, Closed, EmptyBound, Open, Unbound, ValueBound}

import java.time.LocalDate

case class ClassOneRowInput(
  rowId: String,
  money: BigDecimal,
  category: Char,
  period: Period.Period,
  periodQty: BigDecimal = 1
)

case class ClassOneRowOutput(
                              on: LocalDate,
                              config: Map[String, RateDefinition],
                              rowId: String,
                              money: BigDecimal,
                              category: Char,
                              period: Period.Period,
                              periodQty: BigDecimal,
                              precededAmount: BigDecimal = Zero,
                              proRata: Boolean = false
                            ) {

  case class ClassOneRowOutputBand(
                                    bandId: String,
                                    definition: RateDefinition
                                  ) {

    def moneyInterval: Explained[Interval[BigDecimal]] = {
      val id = s"$rowId.$bandId.band"

      if (proRata)
        moneyIntervalProRata(id)
      else {
        import Period._
        val baseInterval = period match {
          case Year => definition.year.pure[Explained]
          case Month => definition.month.fold {
            (definition.year / 12).gives(s"$id: year / 12 = ${definition.year} / 12")
          }(_.pure[Explained])
          case FourWeek => definition.fourWeek.fold {
            (definition.year / 13).gives(s"$id: year / 13 = ${definition.year} / 13")
          }(_.pure[Explained])
          case Week => definition.week.fold {
            (definition.year / 52).gives(s"$id: year / 52 = ${definition.year} / 52")
          }(_.pure[Explained])
        }

        for {
          a <- baseInterval
          b <- if (periodQty == 1) a.pure[Explained] else {
            (a * periodQty) gives s"$id: $a * $periodQty"
          }
          rounded = b.mapBounds(_.roundNi)
          c <- if (rounded == b) {
            b.pure[Explained]
          } else {
            rounded gives s"$id: ⌈$b⌉"
          }
        } yield c
      }
    }

    private def moneyIntervalProRata(id: String): Explained[Interval[BigDecimal]] = {
      def sequence(x: Bound[Explained[BigDecimal]]): Explained[Bound[BigDecimal]] = x match {
        case EmptyBound() => (EmptyBound(): Bound[BigDecimal]).pure[Explained]
        case Unbound() => (Unbound(): Bound[BigDecimal]).pure[Explained]
        case Open(explained) => explained.map(Open(_))
        case Closed(explained) => explained.map(Closed(_))
      }

      period match {
        case Period.Year =>
          definition.year.pure[Explained]

        case Period.Week =>
          val weekInterval: Explained[Interval[BigDecimal]] = definition.week.fold(
            (definition.year / 52).gives(s"$id: year / 52 = ${definition.year} / 52")
          )(_.pure[Explained])

          weekInterval.flatMap{ w =>
            def proRataBound(yearBound: Bound[BigDecimal], weekBound: Bound[BigDecimal], boundDescription: String): Explained[Bound[BigDecimal]] = {
              val result = yearBound.map { y =>
                val w: BigDecimal = weekBound match {
                  case ValueBound(w) => w
                  case _ => sys.error(s"Weekly $boundDescription bound value not found but annual $boundDescription bound value exists")
                }

                if ((y / 52).isWhole) {
                  val result = w * periodQty
                  val description = s"$id: $boundDescription yearBound / 52 = $y / 52 ∈ ℤ ⇒ $boundDescription bound = $periodQty * $boundDescription weekBound = $result"
                  result.gives(description)
                }
                else {
                  val result = (y * periodQty / 52).roundUpWhole
                  val description = s"$id: $boundDescription yearBound / 52 = $y / 52 ∉ ℤ ⇒ $boundDescription bound = ⌈ $periodQty * $boundDescription yearBound / 52 ⌉ = $result"
                  result.gives(description)
                }
              }

              sequence(result)
            }

            for {
              lowerBound <- proRataBound(definition.year.lowerBound, w.lowerBound, "lower")
              upperBound <- proRataBound(definition.year.upperBound, w.upperBound, "upper")
            } yield Interval.fromBounds(lowerBound, upperBound)

          }

        case other => sys.error(s"Period '$other' not handled in pro-rata calculations")
      }
    }

    def amountInBand: Explained[BigDecimal] = moneyInterval.flatMap(m =>
      if(precededAmount == Zero)
      money.inBand(m) gives s"$rowId.$bandId.amountInBand: |[0, $money] ∩ $m|"
      else {
        val intersection = Interval(precededAmount, money + precededAmount).intersect(m)
          val intersectionSize = for{
            upper <- intersection.upperValue
            lower <- intersection.lowerValue
          } yield upper - lower

          intersectionSize.getOrElse(Zero) gives s"$rowId.$bandId.amountInBand: |[$precededAmount, $money + $precededAmount,  ∞) ∩ $m|"
      }
    )

    def employerRate: BigDecimal = definition.employer.getOrElse(category, Zero)
    def employeeRate: BigDecimal = definition.employee.getOrElse(category, Zero)
    def employerContributions: Explained[BigDecimal] = if (employerRate != 0) {
      amountInBand.flatMap{
        case Zero => Zero.pure[Explained]

        case amt if period == Period.FourWeek && on.getYear <= 1999 =>
          ((amt / 4 * employerRate).roundNi * 4) gives
            s"$rowId.$bandId.employer (pre-2000 rule):" ++
              s" ⌊amt / 4 * employerRate⌋ * 4 = ⌊$amt / 4 * $employerRate⌋ * 4"

        case amt => (amt * employerRate).roundNi gives
          s"$rowId.$bandId.employer: ⌊$amt * $employerRate⌋ = ⌊${amt * employerRate}⌋"
      }
    } else Zero.pure[Explained]

    def employeeContributions: Explained[BigDecimal] = if (employeeRate != 0) {
      amountInBand.flatMap{
        case Zero => Zero.pure[Explained]
        case amt if period == Period.FourWeek && on.getYear <= 1999 =>
          ((amt / 4 * employeeRate).roundNi * 4) gives
            s"$rowId.$bandId.employee (pre-2000 rule):" ++
              s" ⌊amt / 4 * employeeRate⌋ * 4 = ⌊$amt / 4 * $employeeRate⌋ * 4"

        case amt => (amt * employeeRate).roundNi gives
          s"$rowId.$bandId.employee: ⌊$amt * $employeeRate⌋ = ⌊${amt * employeeRate}⌋"
      }
    } else Zero.pure[Explained]

    def totalContributions: Explained[BigDecimal] = (
      employeeContributions,
      employerContributions
      ).tupled.flatMap {case (ee,er) =>
      (ee + er) gives s"$rowId.$bandId.total: employee + employer = $ee + $er"
    }
  }


  lazy val bands: List[ClassOneRowOutputBand] = config.toList.sortBy(_._2.year.lowerValue.getOrElse(Zero)).collect {
    case (bandId, bandDefinition) if bandDefinition.trigger.interval(period, periodQty).contains(money + precededAmount) =>
      ClassOneRowOutputBand(bandId, bandDefinition)
  }

  lazy val displaySummaryBands = {
    config.toList.filterNot(_._2.hideOnSummary).sortBy(_._2.year.lowerValue.getOrElse(Zero)).map{
      case (bandId, bandDefinition) => ClassOneRowOutputBand(bandId, bandDefinition)
    }
  }

  def employeeContributions: Explained[BigDecimal] = {
    bands.map(b => b.employeeContributions.map(b.bandId -> _): Explained[(String, BigDecimal)])
      .sequence
      .flatMap{ e =>
        val (ids, amts) = e.filter(_._2 != 0).unzip
        amts.sum gives
          s"$rowId.employee: ${ids.mkString(" + ")} = ${amts.mkString(" + ")}"
      }
  }

  def employerContributions: Explained[BigDecimal] = {
    bands.map(b => b.employerContributions.map(b.bandId -> _): Explained[(String, BigDecimal)])
      .sequence
      .flatMap{ e =>
        val (ids, amts) = e.filter(_._2 != 0).unzip
        amts.sum gives
          s"$rowId.employer: ${ids.mkString(" + ")} = ${amts.mkString(" + ")}"
      }
  }

  def totalContributions: Explained[BigDecimal] = (
    employeeContributions,
    employerContributions
    ).tupled.flatMap {case (ee,er) =>
    (ee + er) gives s"$rowId.total: employee + employer = $ee + er"
  }

}


trait ClassOneResultLike {

  def rowsOutput: List[ClassOneRowOutput]

  def grossPay: Explained[BigDecimal]

  val netPaid: BigDecimal

  val employeePaid: BigDecimal

  def employerPaid: Explained[BigDecimal] =
    (netPaid - employeePaid) gives s"employerPaid: $netPaid - $employeePaid"


  def employeeContributions: Explained[BigDecimal] = {
    rowsOutput.map(b => b.employeeContributions.map(b.rowId -> _): Explained[(String, BigDecimal)])
      .sequence
      .flatMap{ e =>
        val (ids, amts) = e.filter(_._2 != 0).unzip
        amts.sum gives
          s"employee: ${ids.mkString(" + ")} = ${amts.mkString(" + ")}"
      }
  }

  def employerContributions: Explained[BigDecimal] = {
    rowsOutput.map(b => b.employerContributions.map(b.rowId -> _): Explained[(String, BigDecimal)])
      .sequence
      .flatMap{ e =>
        val (ids, amts) = e.filter(_._2 != 0).unzip
        amts.sum gives
          s"employer: ${ids.mkString(" + ")} = ${amts.mkString(" + ")}"
      }
  }

  def totalContributions: Explained[BigDecimal] = (
    employeeContributions,
    employerContributions
    ).tupled.flatMap {case (ee,er) =>
    (ee + er) gives s"total: employee + employer = $ee + $er"
  }


  object underpayment {

    def employee: Explained[BigDecimal] =
      employeeContributions.flatMap(c =>
        (c - employeePaid).max(Zero) gives
          s"underpayment.employee: max(0, employeeContributions - employeePaid) = max(0, $c - $employeePaid)"
      )

    def employer: Explained[BigDecimal] = (
      employerContributions,
      employerPaid
      ).tupled.flatMap{ case (c, p) => (c - p).max(Zero) gives
      s"underpayment.employer: max(0, employerContributions - employerPaid) = max(0, $c - $p)"
    }

    def total: Explained[BigDecimal] = (employee, employer).tupled.flatMap { case (ee,er) => (ee + er) gives
      s"underpayment.total: employee + employer = $ee + $er"
    }

    override def toString = (employee.value, employer.value).toString
  }

  object overpayment {
    def employee: Explained[BigDecimal] =
      employeeContributions.flatMap(c =>
        (employeePaid - c).max(Zero) gives
          s"overpayment.employee: max(0, employeePaid - employeeContributions) = max(0, $employeePaid - $c)"
      )

    def employer: Explained[BigDecimal] = (
      employerContributions,
      employerPaid
      ).tupled.flatMap{ case (c, p) => (p - c).max(Zero) gives
      s"overpayment.employer: max(0, $employerPaid - $employerContributions) = max(0, $p - $c)"
    }

    def total: Explained[BigDecimal] = (employee, employer).tupled.flatMap { case (ee,er) => (ee + er) gives
      s"overpayment.total: employee + employer = $ee + $er"
    }

    override def toString = (employee.value, employer.value).toString
  }

}

case class ClassOneResult(
  on: LocalDate,
  config: Map[String, RateDefinition],
  rowsInput: List[ClassOneRowInput],
  netPaid: BigDecimal = Zero,
  employeePaid: BigDecimal = Zero
) extends ClassOneResultLike {

  lazy val rowsOutput: List[ClassOneRowOutput] = rowsInput.map {
    case ClassOneRowInput(id, money, category, period, periodQty) =>
      ClassOneRowOutput(on, config, id, money, category, period, periodQty)
  }

  def grossPay: Explained[BigDecimal] = rowsInput.map{_.money}.sum gives
    s"grossPay: ${rowsInput.map(_.money).mkString(" + ")}"

}
