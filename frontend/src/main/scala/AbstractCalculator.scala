package eoi.frontend

import scalatags.generic._

trait AbstractCalculator[Builder, FragT, Output <: FragT] {
  val topLevelId: String
  val bundle: Bundle[Builder, Output, FragT]
  import bundle.all._
  def render(data: Map[String, String]): Tag =
    div(id:= topLevelId)(innerDiv(data):_*)

  def innerDiv(data: Map[String, String]): List[Tag]

  def actionButton(actionName: String, label: String): Tag = 
    button(name:="action", value:=actionName)(label)
}
        
