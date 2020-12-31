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

  def innerDiv(data: Map[String,String]): List[TypedTag[Builder,Output,FragT]] = {

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
          
        )
      )
    )
  }

  val topLevelId: String = "interest-late-paid"

}
