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

  def innerDiv(dataBase: Map[String, String]): List[Tag] = {

    val data = dataBase.get("action") match {
      case Some("clear-table") => dataBase.filterNot(_._1.startsWith("row"))
      case Some(remove) =>
        remove.split("_").toList match {
          case ("row"::indexS::"remove"::Nil) =>
            val index = indexS.toInt
            dataBase.toList.flatMap { case (k,v) =>
              k.split("_").toList match {
                case ("row"::rowIndex::xs) => rowIndex.toInt match {
                  case `index` => Nil
                  case low if low < index => List((k,v))
                  case high if high > index => List((s"row_${high-1}_${xs.mkString("_")}", v))
                }
                case _ => List((k,v))
              }
            }.toMap
          case _ => dataBase
        }
      case _ => dataBase
    }

    val c = controls(data)
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

    val existingRows: Int = (1 :: data.keys.toList.map(_.split("_").toList).collect {
      case "row" :: x :: _ => x.toInt
    }).max

    val numRows: Int = if (data.get("action") == Some("add-row")) existingRows + 1 else existingRows

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

      def row(rowNum: Int): (Tag, Option[(BigDecimal, BigDecimal, BigDecimal)]) = {
        val categorySelector =
          SelectField(s"row_${rowNum}_category")("ABCJHMNZX".toList.map {
            x => (x.toString, x.toString)
          }:_*)

        val periodSelector = SelectField(s"row_${rowNum}_period")(
          "Wk" -> "Wk",
          "Mnth" -> "Mnth",
          "4Wk" -> "4Wk"
        )

        val amountField = TextField[BigDecimal](
          s"row_${rowNum}_grosspay",
          {
            case "" => BigDecimal(0).some
            case entry: String => Either.catchOnly[NumberFormatException]{
              BigDecimal(entry)
            }.toOption}
        )

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

        (tr(cls:={if(rowNum == 1)"active" else ""})(
          td(rowNum),
          td(periodSelector),
          td(categorySelector),
          td("£", amountField),
          td(results.map(_._1)),
          td(results.map(_._2)),
          td(actionButton(s"row_${rowNum}_remove", "Remove", true))
        ), results)
      }

      val computed = (1 to numRows).map(row).toList
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

    def textField(fieldName: String): Tag =
      input(name:= fieldName, value:= data.get(fieldName).getOrElse(""))

    def row(rowNum: Int): Tag = tr(
      th(rowNum),
      td(textField(s"row_${rowNum}_input")),
      td(actionButton("addrow","add"), actionButton(s"deleteRow_${rowNum}", "remove"))
    )

    val oddButtons: Tag =
      div(cls:="container")(
        div(cls:="container")(
          div(cls:="form-group subsection")(actionButton("add-row", "Repeat Row", true)),
          div(cls:="form-group subsection")(actionButton("clear-table", "Clear Table", true))
        ),
        div(cls:="container")(
          div(cls:="form-group subsection")(actionButton("calculate", "Calculate"))
        )
      )


    val totalsSummary: Tag = {

      val netContributionsField = TextField(
        "net-contributions",
        {
          case "" => BigDecimal(0).some
          case entry: String => Either.catchOnly[NumberFormatException]{
            BigDecimal(entry)
          }.toOption}
      )

      val employeeContributionsField = TextField(
        "employee-contributions",
        {
          case "" => BigDecimal(0).some
          case entry: String => Either.catchOnly[NumberFormatException]{
            BigDecimal(entry)
          }.toOption}
      )

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
