package eoi
package frontend

import org.scalajs.dom
import scala.scalajs.js.annotation._
import org.querki.jquery._
import dom.ext.Ajax
import scalajs.concurrent.JSExecutionContext.Implicits.runNow

object BindForm {
  implicit val bundle = scalatags.JsDom

  def doBindings(config: Configuration): Unit = {
    val classOne = new RowCalc(config)
    bindFormButtons(classOne)

    val latePaid = new InterestLatePaid(config)
    bindFormButtons(latePaid)
    
  }

  def main(args: Array[String]): Unit = {
    Ajax.get("config.json").map{ a =>
      EoiJsonEncoding.fromJson(a.responseText) match {
        case Right(conf) => doBindings(conf)
        case Left(err) =>
          dom.window.alert("unable to parse configuration - see log for details.")
          println(err)
      }
    }

  }

  def bindFormButtons[Builder, FragT, Output <: FragT](calculator: AbstractCalculator[Builder, FragT, Output]): Unit = {
    def element = $(s"#${calculator.topLevelId}")

    def updateContent(action: String): Unit = {
      val existingData = $(s"form:has(#${calculator.topLevelId})").serialize()
      val elems: List[String] = existingData.split("&").toList
      val pairs: Map[String, String] = elems.map(_.split("=").toList).collect {
        case (k::v::Nil) => (k,scalajs.js.URIUtils.decodeURIComponent(v.replace("+", " ")))
      }.toMap
      val data = pairs + ("action" -> action)
      val newContent = calculator.innerDiv{
        val dataFormatted = data.map {
          case (k,v) => (k.split("_").toList, v)
        }
        calculator.Components(dataFormatted, action = data.get("action").fold(List.empty[String])(_.split("_").toList))
      }.map(_.toString).mkString("")
      element.html(newContent)
    }

    // rebuild the form when the user clicks on a button
    element.find("button").click{ e: dom.Element =>
      updateContent(e.getAttribute("value"))
      bindFormButtons(calculator)
      false
    }

    // rebuild the form when the user picks an option from a dropdown
    // we can set this to "select, input", but this creates an odd
    // experience as the user loses focus when editing a field and
    // switching to another
    element.find("select").change{ e: dom.Element =>
      updateContent("updated")
      bindFormButtons(calculator)
    }

  }

}
