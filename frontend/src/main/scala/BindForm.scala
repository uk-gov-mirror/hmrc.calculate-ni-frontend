package eoi.frontend

import org.scalajs.dom
import scala.scalajs.js.annotation._
import org.querki.jquery._

object BindForm {
  implicit val bundle = scalatags.JsDom

  def main(args: Array[String]): Unit = {
    val calculator = new RowCalc()
    bindForm(calculator)
  }

  def bindForm[Builder, FragT, Output <: FragT](calculator: AbstractCalculator[Builder, FragT, Output]): Unit = {
    def element = $(s"#${calculator.topLevelId}")

    val event: scalajs.js.ThisFunction0[dom.Element, Boolean] = { e =>
      val existingData = $(s"form:has(#${calculator.topLevelId})").serialize()
      val elems: List[String] = existingData.split("&").toList
      val pairs: Map[String, String] = elems.map(_.split("=").toList).collect {
        case (k::v::Nil) => (k,scalajs.js.URIUtils.decodeURI(v))
      }.toMap
      val data = pairs + ("action" -> e.getAttribute("value"))
      println(data)
      val newContent = calculator.innerDiv(data).map(_.toString).mkString("")
      element.html(newContent)
      bindForm(calculator)
      false
    }
    
    element.find("button").click(event)
  }

}
