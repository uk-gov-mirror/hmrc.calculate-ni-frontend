package eoi
package frontend

import scalatags.generic._
import cats.implicits._

case class RowCalc[Builder, FragT, Output <: FragT](
  config: Configuration
)(
  implicit val bundle: Bundle[Builder, Output, FragT]
) extends AbstractCalculator[Builder, FragT, Output] {
  import bundle.all._

  val topLevelId = "rowcalc"

  def innerDiv(c: Components): List[Tag] = {

    import c._

    val taxYearSelector = {
      val taxYears = config.classOne
        .keys.toList
        .sortBy(_.toString).reverse
        .zipWithIndex
        .map { case (x,i) => (i.toString,x) }

      SelectField("tax-year")(taxYears:_*)
    }

    val taxYear = taxYearSelector.getValue.getOrElse(
      throw new IllegalStateException("no tax year found")
    )

    def taxYearSelectorBlock: Tag = {
        
      div(cls:= "container")(
        div(cls:="form-group half")(
          label(cls:="form-label")("Tax year:"),
          div(cls:="select tax-year")(
            taxYearSelector
          )
        ),
        div(cls:="form-group half")(
          button(tpe:="button", cls:="button govuk-button govuk-button--secondary nomar")("Save and print")
        )
      )
    }

    val (contributionTable: Tag, totals: Option[(BigDecimal, BigDecimal, BigDecimal)]) = {

      def row(c: Components, index: Int): (Tag, Option[(BigDecimal, BigDecimal, BigDecimal)]) = {
        val categorySelector =
          c.SelectField("category")("ABCJHMNZX".toList.map {
            x => (x.toString, x.toString)
          }:_*)

        val periodSelector = c.SelectField("period")(
          "Wk" -> "Wk",
          "Mnth" -> "Mnth",
          "4Wk" -> "4Wk"
        )

        val amountField = c.NumericField(s"grosspay")

        val results = for {
          start <- taxYear.lowerValue
          amount <- amountField.getValue.toOption
          cat <- categorySelector.getValue.map(_.head).toOption
          period <- periodSelector.getValue.toOption
        } yield {
          val calc = config.calculateClassOne(
            start,
            amount,
            cat,
            Period(period)
          )

          val (ee,er) = calc.foldLeft((BigDecimal(0), BigDecimal(0))) {
            case ((eeAcc, erAcc), (_, (_, eeInc, erInc))) => (eeAcc + eeInc, erAcc + erInc)
          }

          (ee,er,amount)
        }

        val cat = categorySelector.getValue

        (tr(
          td(index + 1),
          td(periodSelector),
          td(categorySelector),
          td("£", amountField),
          td(results.map(_._1)),
          td(results.map(_._2)),
          td(c.actionButton(s"remove", "Remove", true))
        ), results)
      }

      val computed = tableComponents.zipWithIndex.map{case (c,i) => row(c,i)}
      val rowTags = computed.map(_._1)

      val tag = table(cls:="contribution-details table-wrapper")(
        thead(
          tr(cls:="clear")(
            th(cls:="lg", colspan:="4")(span("Contribution payment details")),
            th(cls:="border", colspan:="3")(span("Net contributions"))
          ),
          tr(
            th("Num"),
            th("Period"),
            th("Category"),
            th("Gross Pay"),
            th("EE"),
            th("ER"),
            th("")
          )
        ),
        tbody(
          rowTags :_*
        )
      )

      (tag, computed.map(_._2).combineAll)
    }

    val oddButtons: Tag =
      div(cls:="container")(
        div(cls:="container")(
          div(cls:="form-group subsection")(actionButton("duplicate-row_last", "Repeat Row", true)),
          div(cls:="form-group subsection")(actionButton("clear-table", "Clear Table", true))
        ),
        div(cls:="container")(
          div(cls:="form-group subsection")(actionButton("calculate", "Calculate"))
        )
      )

    val totalsSummary: Tag = {

      val netContributionsField = NumericField("net-contributions")
      val employeeContributionsField = NumericField("employee-contributions")

      val employerPaid = (
        netContributionsField.getValue.toOption,
        employeeContributionsField.getValue.toOption
      ).mapN(_ - _)

      val netUnderpayment = (
        totals.map(x => (x._1 + x._2)),
        netContributionsField.getValue.toOption
      ).mapN(_ - _)

      val eeUnderpayment = (
        totals.map(_._1),
        employeeContributionsField.getValue.toOption
      ).mapN(_ - _)

      val erUnderpayment = (
        totals.map(_._2),
        employerPaid
      ).mapN(_ - _)

      div(cls:="subsection totals")(
        h2(cls:="section-heading")("Totals"),
        div(cls:="spaced-table-wrapper")(
          table(cls:="totals-table spaced-table")(
            thead(
              tr(
                th("Gross pay"), th("Net contributions"), th("Employee contributions"), th("Employer contributions")
              )
            ),
            tbody(
              tr(
                td(cls:="readonly")(span(totals.map(_._3))),
                td(cls:="readonly")(span(totals.map(x => x._1 + x._2))),
                td(cls:="readonly")(span(totals.map(_._1))),
                td(cls:="readonly")(span(totals.map(_._2)))                
              ),
              tr(
                td(cls:="right error-line-label")(span("NI Paid")),
                td(cls:="input-cell")(netContributionsField),
                td(cls:="input-cell")(employeeContributionsField),
                td(cls:="readonly")(span(employerPaid)),
              ),
              tr(
                td(cls:="right error-line-label")(span("Underpayment")),
                td(cls:="readonly")(span(netUnderpayment.positive)),
                td(cls:="readonly")(span(eeUnderpayment.positive)),
                td(cls:="readonly")(span(erUnderpayment.positive))                
              ), 
              tr(
                td(cls:="right error-line-label")(span("Overpayment")),
                td(cls:="readonly")(span(netUnderpayment.negative)),
                td(cls:="readonly")(span(eeUnderpayment.negative)),
                td(cls:="readonly")(span(erUnderpayment.negative))                
              )
            )
          )
        )
      )
    }

    List(
      taxYearSelectorBlock, 
      contributionTable,
      oddButtons,
      totalsSummary
    )
  }

}
