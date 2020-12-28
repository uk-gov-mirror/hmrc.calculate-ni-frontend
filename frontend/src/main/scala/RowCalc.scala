package eoi.frontend

import scalatags.generic._

case class RowCalc[Builder, FragT, Output <: FragT]()(implicit val bundle: Bundle[Builder, Output, FragT]) extends AbstractCalculator[Builder, FragT, Output] {
  import bundle.all._

  val topLevelId = "rowcalc"

  def innerDiv(data: Map[String, String]): List[Tag] = {

    def textField(fieldName: String): Tag =
      input(name:= fieldName, value:= data.get(fieldName).getOrElse(""))

    def row(rowNum: Int): Tag = tr(
      th(rowNum),
      td(textField(s"row_${rowNum}_input")),
      td(actionButton("addrow","add"), actionButton(s"deleteRow_${rowNum}", "remove"))
    )

    val existingRows: Int = (1 :: data.keys.toList.map(_.split("_").toList).collect {
      case "row" :: x :: "input" :: Nil => x.toInt
    }).max

    val numRows: Int = if (data.get("action") == Some("addrow")) existingRows + 1 else existingRows

    val rows: List[Tag] = (1 to numRows).map(row).toList
    println(rows.size)
    List(
      table(
        rows :_*
      ),
      actionButton("calculate", "submit")
    )
  }

}
