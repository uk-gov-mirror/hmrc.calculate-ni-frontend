package eoi.frontend

import scalatags.generic._
import cats.implicits._

trait AbstractCalculator[Builder, FragT, Output <: FragT] {

  val topLevelId: String
  val bundle: Bundle[Builder, Output, FragT]
  import bundle.all._
  def render(data: Map[String, String]): Tag =
    div(id:= topLevelId)(innerDiv{
      val dataFormatted = data.map {
        case (k,v) => (k.split("_").toList, v)
      }
      Components(dataFormatted, action = data.get("action").fold(List.empty[String])(_.split("_").toList))
    }:_*)


  def innerDiv(components: Components): List[Tag]

  trait Field[A] {
    def render: Tag
    def getValue: Either[String, A]

    def andThen[B](f: A => Either[String, B]): Field[B] = {
      val that = this;
      new Field[B] {
        def render(): Tag = that.render()
        def getValue: Either[String, B] = that.getValue flatMap f
      }
    }

    def map[B](f: A => B): Field[B] = andThen(x => Right(f(x)))
    def disallowing(f: PartialFunction[A, String]): Field[A] = {
      andThen{ x => 
        f.lift(x) match {
          case None => Right(x)
          case Some(err) => Left(err)
        }
      }
    }
  }

  implicit def autoRender[A](in: Field[A]): Tag = in.render()
  implicit def optBigDecimalToTag(in: Option[BigDecimal]) =
    raw(in.fold("-")(_.toDouble.formatted("£%,.2f")))

  case class Components(
    dataPreTableLogic: Map[List[String], String],
    path: List[String] = Nil,
    action: List[String] = Nil,
  ) {


    val (tableData, nonTableData) = dataPreTableLogic.partition{
      case (("row"::_), _) => true
      case _ => false
    }

    val tableRows: List[Map[List[String], String]] = {
      object IntP {
        def unapply(in: String): Option[Int] =
          Either.catchOnly[NumberFormatException](in.toInt).toOption
      }

      val baseRows: List[Map[List[String], String]] = dataPreTableLogic.foldLeft(
        Map.empty[Int,Map[List[String],String]]
      ){
        case (existing, (atPath::IntP(i)::xs,v)) =>
          val newKey = xs
          existing.get(i) match {
            case Some(entry) => existing + (i -> (entry + (newKey -> v)))
            case None => existing + (i -> Map(newKey -> v))
          }
        case (existing,_) => existing
      }.toList.sortBy(_._1).map(_._2)

      action match {
        case  "clear-table" :: Nil => Nil
        case ("row"::IntP(index)::"remove"::Nil) => baseRows.take(index) ++ baseRows.drop(index + 1)
        case "add-row" :: Nil => baseRows :+ Map.empty[List[String], String]
        case "duplicate-row" :: "last" :: Nil => baseRows ++ baseRows.lastOption.toList
        case "duplicate-row" :: IntP(index) :: Nil => baseRows :+ baseRows(index)
        case _ => baseRows
      }
    }

    val nonEmptyTableRows: List[Map[List[String], String]] =
      if (tableRows.isEmpty) (Map.empty[List[String], String] :: Nil) else tableRows

    lazy val tableComponents: List[Components] = nonEmptyTableRows.zipWithIndex.map{ case (d,i) => 
      Components(
        d,
        path ++ ("row" :: i.toString() :: Nil),
        action
      )
    }

    val data = tableRows.zipWithIndex.flatMap {case (m,i) =>
      m.map{case (k,v) => ("row" :: i.toString :: k, v)}
    }.toMap ++ nonTableData

    case class SelectField[A](fieldName: String*)(options: (String,A)*) extends Field[A] {
      val fullPath = path ++ fieldName.toList      
      def render(): Tag = {
        val o = options.toList.map { case (k,v) =>
          if (data.get(fieldName.toList) == Some(k))
            option(value:=k, selected)(v.toString)
          else
            option(value:=k)(v.toString)
        }
        select(name:=fullPath.mkString("_"))(o:_*)
      }

      def getValue: Either[String, A] = {
        val textValue = data.get(fieldName.toList)
        textValue match {
          case None => Right(options.head._2)
          case Some(entry) =>
            options.collectFirst{ case (`entry`,v) => v }
              .fold(entry.asLeft : Either[String, A])(_.asRight)
        }
      }
    }

    object SelectField {
      def fromIterable[A](fieldName: String)(options: Iterable[A]): SelectField[A] =
        SelectField(fieldName) (
          options.toList.zipWithIndex.map{case (x,i) => i.toString -> x} :_*
        )
    }

    case class TextField(
      fieldName: String
    ) extends Field[String] {
      val fullPath = path :+ fieldName
      def render(): Tag = {
        input(name:= fullPath.mkString("_"), value:= data.get(fieldName :: Nil).getOrElse(""))
      }

      def getValue: Either[String, String] = Right(data.getOrElse(fieldName :: Nil, ""))
    }

    def NumericField(
      fieldName: String
    ): Field[BigDecimal] = TextField(fieldName).andThen( 
      {
        case "" => Right(BigDecimal(0))
        case entry: String => Either.catchOnly[NumberFormatException]{
          BigDecimal(entry)
        }.leftMap(_.getLocalizedMessage())
      }
    )

    def atPath(addPath: String*) = {
      val pathL = addPath.toList
      Components(
        data.collect{ case ((pathL::xs), v) => xs -> v },
        path ++ addPath,
        action
      )
    }

    def actionButton(actionName: String, label: String, secondary: Boolean = false): Tag =
      button(
        name:="action",
        cls:= s"button govuk-button ${if (secondary) "govuk-button--secondary" else ""} nomar",
        value:=(path :+ actionName).mkString("_")
      )(label)
    
  }



}


        
