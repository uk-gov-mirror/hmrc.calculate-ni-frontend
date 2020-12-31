package eoi
package frontend

import scalatags.generic._
import cats.implicits._

case class InterestLatePaid[Builder, FragT, Output <: FragT](
  config: Configuration
)(
  implicit val bundle: Bundle[Builder, Output, FragT]
) extends AbstractCalculator[Builder, FragT, Output] {

  import bundle.all._

  def innerDiv(dataBase: Map[String,String]): List[TypedTag[Builder,Output,FragT]] = {

    // duplication w RowCalc - refactor candidate
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

    List(
      h2("Number of bank holidays"),
      TextField("number-of-bank-holidays"),
      table(cls:="contribution-details table-wrapper")(
        thead(
          tr(cls:="clear")(
            th(cls:="lg", colspan:="2")(span("Tax Year"))
          ),
          tr(
            "Refund|Year|Date|Refund|Payable".split("\\|").map(th.apply(_)):_*
          )
        ),
        tbody(
          tr(

          )
        )
      )
    )
  }

  val topLevelId: String = "interest-late-paid"

}
