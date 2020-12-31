package eoi.frontend

import scalatags.generic._
import cats.implicits._

trait AbstractCalculator[Builder, FragT, Output <: FragT] {
  val topLevelId: String
  val bundle: Bundle[Builder, Output, FragT]
  import bundle.all._
  def render(data: Map[String, String]): Tag =
    div(id:= topLevelId)(innerDiv(data):_*)

  def innerDiv(data: Map[String, String]): List[Tag]

  def actionButton(actionName: String, label: String, secondary: Boolean = false): Tag = 
    button(name:="action", cls:= s"button govuk-button ${if (secondary) "govuk-button--secondary" else ""} nomar", value:=actionName)(label)

  case class controls(dataIn: Map[String, String]) {

    trait Field[A] {
      def render: Tag
      def getValue: Either[String, A]
    }

    implicit def autoRender[A](in: Field[A]): Tag = in.render()

    case class SelectField[A](fieldName: String)(options: (String,A)*) extends Field[A] {
      def render(): Tag = {
        val o = options.toList.map { case (k,v) =>
          if (dataIn.get(fieldName) == Some(k))
            option(value:=k, selected)(v.toString)
          else
            option(value:=k)(v.toString)
        }
        select(name:=fieldName)(o:_*)
      }

      def getValue: Either[String, A] = {
        val textValue = dataIn.get(fieldName)
        textValue match {
          case None => Right(options.head._2)
          case Some(entry) =>
            options.collectFirst{ case (`entry`,v) => v }
              .fold(entry.asLeft : Either[String, A])(_.asRight)
        }
      }
    }

    case class TextField[A](
      fieldName: String,
      transform: String => Option[A] = {x: String => Some(x)}
    ) extends Field[A] {
      def render(): Tag =
        input(name:= fieldName, value:= dataIn.get(fieldName).getOrElse(""))

      def getValue: Either[String, A] = {
        val plainText = dataIn.getOrElse(fieldName, "")
        Either.fromOption(
          transform(plainText),
          plainText
        )
      }
    }
  }

  implicit def optBigDecimalToTag(in: Option[BigDecimal]) =
    raw(in.fold("-")(_.toDouble.formatted("£%,.2f")))

}
        
