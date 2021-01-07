package eoi
package frontend

import scalatags.generic._
import cats.implicits._
import java.time.LocalDate

case class InterestLatePaid[Builder, FragT, Output <: FragT](
  config: Configuration
)(
  implicit val bundle: Bundle[Builder, Output, FragT]
) extends AbstractCalculator[Builder, FragT, Output] {

  import bundle.all._

  def innerDiv(c: Components): List[Tag] = {
    import c._

    val rows: List[(Option[BigDecimal], Option[BigDecimal], Tag)] = tableComponents.map { case c =>
      import c._

      val forField = SelectField.fromIterable("for")(2002 to LocalDate.now.getYear())
      val yearField = SelectField.fromIterable("year")(2002 to LocalDate.now.getYear())
      val refundAmountField = NumericField("refundamt")

      val payable = (
         forField.getValue.toOption,
        yearField.getValue.toOption
      ).mapN(BigDecimal(_) + _)

      (
        payable,
        refundAmountField.getValue.toOption,
        tr(
          td(forField),
          td(yearField),
          td(""),
          td(refundAmountField),
          td(payable)
        )
      )
      
    }

    val totalInterest: Option[BigDecimal] = rows.map(_._1).combineAll
    val totalRefundAmount: Option[BigDecimal] = rows.map(_._2).combineAll
    val total: Option[BigDecimal] = (
      totalInterest,
      totalRefundAmount
    ).mapN(_ + _)

    val numberOfHolsField = NumericField("number-of-bank-holidays")

    List(
      h2("Number of bank holidays"),
      numberOfHolsField,
      table(cls:="contribution-details table-wrapper")(
        thead(
          tr(cls:="clear")(
            th(cls:="lg")(span("Refund")),
            th(cls:="lg", colspan:="2")(span("Amount Paid")),
            th(cls:="lg", colspan:="2")(span("Amount for interest"))                       
          ),
          tr(
            "Refund|Year|Date|Refund|Payable".split("\\|").map(th.apply(_)):_*
          )
        ),
        tbody(
          rows.map(_._3):_*
        )
      ),
      div(cls:="container")(
        div(cls:="container")(
          div(cls:="form-group subsection")(actionButton("add-row", "Repeat Row", true)),
          div(cls:="form-group subsection")(actionButton("clear-table", "Clear Table", true))
        ),
        div(cls:="container")(
          div(cls:="form-group subsection")(actionButton("calculate", "Calculate"))
        )
      ),
      div(cls:="container")(
        table(cls:="contribution-details table-wrapper")(
          tr(td(label("Total amount for refund"))),
          tr(td(cls:="readonly")(div(totalRefundAmount))),
          tr(td(label("Total interest payable"))),
          tr(td(cls:="readonly")(div(totalInterest))),
          tr(td(label("Total amount for refund and interest payable"))),
          tr(td(cls:="readonly")(div(total)))
        )
      )
    )
  }

  val topLevelId: String = "interest-late-paid"

}
