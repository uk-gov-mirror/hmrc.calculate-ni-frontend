package eoi
package importer

import com.github.tototoshi.csv._
import java.time.LocalDate
import spire.math.Interval
import scala.collection.immutable.ListMap
import math.Ordering
import cats.implicits._
import java.util.Locale
import java.text.NumberFormat
import java.io._

object Importer {

  def slidingOpenRight[A](in: List[A]): List[List[A]] =
    in.sliding(2).toList ++ List(List(in.last))

  private val percentFormat = {
    val r = NumberFormat.getPercentInstance(Locale.UK)
    r.setMaximumFractionDigits(2)
    r
  }

  private val penceFormat = {
    val r = NumberFormat.getCurrencyInstance(Locale.UK)
    r.setMinimumFractionDigits(2)
    r.setMaximumFractionDigits(2)
    r
  }

  private val poundsFormat = {
    val r = NumberFormat.getCurrencyInstance(Locale.UK)
    r.setMinimumFractionDigits(0)
    r.setMaximumFractionDigits(0)
    r
  }

  implicit class RichString(in: String) {

    def repeat(times: Int): String = {
      @annotation.tailrec
      def inner(rem: Int = times - 1, acc: String = in): String = rem match {
        case 0 => acc
        case _ => inner(rem - 1, in ++ acc)
      }
      inner()
    }

    def indent(cols: Int): String = in.lines.map("  ".repeat(cols) ++ _).mkString("\n")

    def formatPercent: String = percentFormat.format(BigDecimal(in.replace("%","")))

    def formatMoney: String = {
      val bd = BigDecimal(in.replace("£",""))
      // val formatter = if (bd.isWhole) poundsFormat else penceFormat
      // formatter.format(bd)
      in.toString
    }
    
    def formatDate: String = {
      val fst = in.takeWhile(_ != ' ')
      val (y::m::d::_) = fst.split("/").toList
      s"$y-$m-$d"
    }

  }

  def csvFile[A](
    file: String,
    conversion: List[Map[String, String]] => A = (_: List[Map[String, String]]).head
  ): ListMap[Interval[LocalDate], A] = {

    val reader = CSVReader.open("calc/src/test/resources/testing-tables/exported-old/" ++ file ++ ".csv")
    val lines = reader.all
    val (headers::data) = lines.map(_.map(_.trim))

    // because the rows have a start point (some week in some year)
    // but not an end point, we need to pair them with the subsequent
    // line before we can determine the interval
    val datedUngrouped: List[(LocalDate, Map[String, String])] = (data map { case line => 
      val lineMap = (headers zip line).toMap.filter(_._2.nonEmpty)
      val year = lineMap("Tax Yr").toInt
      val dateInterval: LocalDate = lineMap.get("Start Wk") orElse lineMap.get("Start Week") match {
        case None | Some("1") => TaxYear(year).start
        case Some(startWeek) => TaxYear(year).start.plusWeeks(startWeek.toInt - 1)
      }
      dateInterval -> lineMap
    })

    val dated = datedUngrouped.groupBy(_._1).mapValues(x => conversion(x.map(_._2))).toList.sortBy(_._1)

    val ret = slidingOpenRight(dated) map {
      case (start,record) :: (end,_) :: Nil =>
        Interval.closed(start, end.minusDays(1)) -> record
      case (start,record) :: Nil =>
        Interval.closed(start, start.plusYears(1).minusDays(1)) -> record
      case _ => sys.error("slidingOpenRight should only ever have 1 or 2 subentries per element")
    }
    reader.close
    ListMap(ret:_*)
  }

  /** possibly shortened back into a year again. */ 
  def formatPeriod(on: Interval[LocalDate]): String = TaxYear.unapply(on) match {
    case Some(taxYear) => taxYear.toString
    case None => s"""\"${on.toString.replace(" ", "")}\""""
  }

  def main(args: Array[String]): Unit = {

    /** Some of the tables have multiple rows per year and need to be subgrouped by category */
    val subCatGrouping = (_: List[Map[String, String]]).collect {
      case l if l("Cat").length == 1 => l("Cat").head -> l
    }.toMap

    val c1RatesPre1999: Map[Interval[LocalDate], Map[Char, Map[String, String]]] =
      csvFile("CLASS 1 RATE TABLE", subCatGrouping)
        .filterKeys(_ < TaxYear(1999).asInterval)
    val c1RatesPost1999: ListMap[Interval[LocalDate], Map[Char, Map[String, String]]] =
      csvFile("tblClass1_1999", subCatGrouping)

    val c1BandsPre1999 = csvFile("BAND TABLE")
    val c1BandsPost1999 = csvFile("tblBandLimits1999")
    val class2 = csvFile("CLASS 2 RATE TABLE")
    val class3 = csvFile("CLASS 3 RATE TABLE")
    val class4 = csvFile("CLASS 4 RATE TABLE")

    // if we want to group them - year { class-one {} class-two {} }
    // val allTimePeriods = List(
    //   c1RatesPre1999.keys,
    //   c1RatesPost1999.keys,      
    //   class2.keys,
    //   class3.keys,
    //   class4.keys
    // ).flatten.sorted.distinct

    // allTimePeriods.map{ on =>
    //    ....
    // }

    // if we want to group them - class-one { year { .. } } class-two { year { .. } } 

    val c2 = {
      val entries = class2.map { case (on, c) =>

        val lel = class3(on)("LEL")
        val hrpDate = c.get("HRP Date").map { d =>
          s"hrp-date: ${d.formatDate}"
        }.getOrElse("")

        val finalDate = c.get("Fnl Dte For Pen").map { d =>
          s"final-date: ${d.formatDate}"
        }.getOrElse("")

        val defaultRate = c("Men Wk Rte")
        def rateOpt(lookup: String, name: String): Option[(String, String)] =
          c.get(lookup).filter(_ != defaultRate).map(name -> _)

        val rates = List(
          ("default" -> defaultRate).some,
          rateOpt("Women Wk Rte", "women"),
          rateOpt("Shre Fish Wk Rte", "fishermen"),
          rateOpt("VDW Wk Rte", "volunteer")
        ).flatten.map{ case (k,v) => s"$k: $v" }.mkString("\n").indent(2)

        s"""|${formatPeriod(on)} {
            |  weekly-rate {
            |$rates
            |  }
            |  small-earnings-exception: ${c("SEE Limit")}
            |  lel: $lel
            |  $finalDate 
            |  $hrpDate
            |  no-of-weeks: ${c("No of Wks")}
            |  qualifying-rate: ${c("EF Qual Year")}
            |}""".stripMargin.indent(1)
      }
      s"""|class-two {
          |${entries.mkString("\n")}
          |}""".stripMargin
    }

    val c3 = {
      val entries = class3.map { case (on, c) =>
        val finalDate = c.get("Fnl Dte For Pen").map { d =>
          s"final-date: ${d.formatDate}"
        }.getOrElse("")

        s"""|${formatPeriod(on)} {
            |  week-rate: ${c("Wk Rte")}
            |  $finalDate 
            |  no-of-weeks: ${c("No Of Weeks")}
            |  lel: ${c("LEL")}
            |  qualifying-rate: ${c("EF Qual Year")}
            |}""".stripMargin.indent(1)
      }
      s"""|class-three {
          |${entries.mkString("\n")}
          |}""".stripMargin

    }

    val c4 = {
      val entries = class4.map { case (on, c) =>

        s"""|${formatPeriod(on)} {
            |  lower-limit: ${c("ANN LEL").formatMoney}
            |  upper-limit: ${c("ANN UEL").formatMoney}
            |  main-rate: ${c("PRCNT Rate").formatPercent}
            |  upper-rate: ${c.getOrElse("RateAboveUEL", "0").formatPercent}
            |}""".stripMargin.indent(1)
      }

      s"""|class-four {
          |${entries.mkString("\n")}
          |}""".stripMargin
    }

    val c1 = {

      def formatRatesP(name: String, data: Iterable[(Char, String)]) =
        data.groupBy(_._2)
          .mapValues(_.map(_._1).toList.sorted.mkString)
          .toList match {
            case Nil => ""
            case (rate, cats) :: Nil =>
              s"$name.$cats = ${rate.formatPercent}"
            case many =>
              val i = many.map {
                case (rate, cats) => s"$cats = ${rate.formatPercent}"
              }.mkString("\n").indent(1)
              s"""|$name {
                  |$i
                  |}""".stripMargin
          }

      val oldEntries = for {
        (on, rates) <- c1RatesPre1999
        bands <- c1BandsPre1999.get(on).toList
      } yield {

        // TODO: Via the rates enquiry table view the new structure starts
        // in 1986, but in the data in the access table it starts in 1985....
        val pre1985 = on.lowerValue.get.getYear < 1985

        def makeBand(bounds: (Int, Int), rateEE: String, rateER: String, trigger: (Int, Int) = (0, Int.MaxValue)): String = {

          def makeIntervals(b: (Int, Int)) = (b match {
            case (0, u) => List(
              s"[0,${bands("Wk Band " + u)})",
              s"[0,${bands("Mnth Band " + u)})",
              s"[0,${bands("Ann Band " + u)})"
            )
            case (l, Int.MaxValue) => List(
              s"[${bands("Wk Band " + l)},∞)",
              s"[${bands("Mnth Band " + l)},∞)",
              s"[${bands("Ann Band " + l)},∞)"
            )
            case (l, u) => List(
              s"[${bands("Wk Band " + l)},${bands("Wk Band " + u)})",
              s"[${bands("Mnth Band " + l)},${bands("Mnth Band " + u)})",
              s"[${bands("Ann Band " + l)},${bands("Ann Band " + u)})"
            )
          })

          val intervals = makeIntervals(bounds)
            .zip(List("week", "month", "year"))
            .map { case (definition, label) => s"""$label: "$definition"""" }
            .mkString("\n")

          val triggers = trigger match {
            case (0, Int.MaxValue) => ""
            case t =>
              makeIntervals(t)
                .zip(List("trigger.week", "trigger.month", "trigger.year"))
                .map { case (definition, label) => s"""$label: "$definition"""" }
                .mkString("\n")
          }



          val ratesEE = formatRatesP("employee", rates.mapValues(_.getOrElse(rateEE, "0")))
          val ratesER = formatRatesP("employer", rates.mapValues(_.getOrElse(rateER, "0")))          

          s"""|{
              |${intervals.indent(1)}
              |${ratesEE.indent(1)}
              |${ratesER.indent(1)}
              |${triggers.indent(1)}
              |}""".stripMargin
        }

        val formattedBands = if (pre1985) {
          List(
            "Up to LEL" -> makeBand((0,1), "Up to LEL1 (employee)", "Up to LEL1 (employer)"),
            "LEL to UEL" -> makeBand((1, 5), "Employee Rte 1", "Employer Rte 1"),
            "Above LEL" -> makeBand((5, Int.MaxValue), "", "Above UEL (employer)")
          )
        } else {
          List(
            "up to LEL 1" -> makeBand((0,1), "Up to LEL1 (employee)", "Up to LEL1 (employer)", (1,2)),
            "up to LEL 2" -> makeBand((0,2), "Up to LEL2 (employee)", "Up to LEL2 (employer)", (2,3)),
            "up to LEL 3" -> makeBand((0,3), "Up to LEL3 (employee)", "Up to LEL3 (employer)", (3,4)),
            "up to LEL 4" -> makeBand((0,4), "Up to LEL4 (employee)", "Up to LEL4 (employer)", (4,Int.MaxValue)),
            "Band 1" -> makeBand((1, 2), "Employee Rte 1", "Employer Rte 1", (1,2)),
            "Band 2" -> makeBand((2, 3), "Employee Rte 2", "Employer Rte 2", (2,3)),
            "Band 3" -> makeBand((3, 4), "Employee Rte 3", "Employer Rte 3", (3,4)),
            "Band 4" -> makeBand((4, 5), "Employee Rte 4", "Employer Rte 4", (4, Int.MaxValue)),
            "Above UEL" -> makeBand((5, Int.MaxValue), "", "Above UEL (employer)")
          )
        }

        val bandBlock = formattedBands.map { case (k,v) => s""""$k" $v""" }.mkString("\n").indent(1)
        s"""|${formatPeriod(on)} {     
            |$bandBlock
            |}""".stripMargin.indent(1)   
      }

      val newEntries = for {
        (on, rates) <- c1RatesPost1999
        bands <- c1BandsPost1999.get(on).toList
      } yield {

        val TaxYear(year) = on

           val (employeeKeys, employerKeys) = rates
             .values.flatMap(_.keys).toList
             .distinct.foldLeft((List.empty[String], List.empty[String])) {
               case ((accEE, accER), n) if n.startsWith("EE") => ((n.drop(3) :: accEE), accER)
               case ((accEE, accER), n) if n.startsWith("ER") => (accEE, (n.drop(3) :: accER))
               case ((accEE, accER), _) => (accEE, accER)
             }

           val allKeys = (employeeKeys ++ employerKeys).distinct

           val ratesUnstructured = allKeys.map { key =>
             val employee = rates.mapValues(_.get("EE_" + key)).collect{case (k,Some(v)) => (k,v)}
             val employer = rates.mapValues(_.get("ER_" + key)).collect{case (k,Some(v)) => (k,v)}            
             key -> (employee, employer)
           }

           def upTo(name: String): String =
             s"""|year: "[0,${bands("Ann" + name)})"
                 |month: "[0,${bands("Mnth" + name)})"
                 |week: "[0,${bands("Wk" + name)})"
                 |""".stripMargin.indent(1)

           def above(name: String): String = 
             s"""|year: "[${bands("Ann" + name)},∞)"
                 |month: "[${bands("Mnth" + name)},∞)"
                 |week: "[${bands("Wk" + name)},∞)"
                 |""".stripMargin.indent(1)
        
           def between(from: String, to: String): String =
             s"""|year: "[${bands("Ann" + from)},${bands("Ann" + to)})"
                 |month: "[${bands("Mnth" + from)},${bands("Mnth" + to)})"
                 |week: "[${bands("Wk" + from)},${bands("Wk" + to)})"
                 |""".stripMargin.indent(1)

           def betweenSafe(from: String, to: String): Option[String] =
             (
               bands.get("Ann" + from),
               bands.get("Ann" + to),
               bands.get("Mnth" + from),
               bands.get("Mnth" + to),
               bands.get("Wk" + from),
               bands.get("Wk" + to)
             ).mapN{ case (fYear, tYear, fMonth, tMonth, fWeek, tWeek) => 
              s"""|year: "[$fYear,$tYear)"
                  |month: "[$fMonth,$tMonth)"
                  |week: "[$fWeek,$tWeek)"""".stripMargin.indent(1)
             }

           val zeroRates: String = {
             val allCats = rates.keys.toList.distinct.map(_ -> "0%")
             formatRatesP("employee", allCats) + "\n" + formatRatesP("employer", allCats)
           }.indent(1)

           def singleRates(name: String, key: String): String = {
             val rateMap = rates.mapValues(x => x.get(key) orElse x.get(key.replace("_", " "))).collect{ case (k,Some(v)) => (k,v)}
             formatRatesP(name, rateMap)
           }.indent(1)

           def employeeRatesOnly(name: String): String = singleRates("employee", "EE_" + name)
           def employerRatesOnly(name: String): String = singleRates("employer", "ER_" + name)
           def bothRates(name: String): String =
             employeeRatesOnly(name) + "\n" + employerRatesOnly(name)

        def sameBands(a: String, b: String): Boolean = List(
          "Ann", "Mnth", "Wk"
        ).forall { x => 
          bands.get(x + a) == bands.get(x + b)
        }

           val bandsFormatted = List(
             s"""|"Up to LEL" { 
                 |${upTo("LEL")}
                 |${zeroRates}
                 |}""".some,
             s"""|"LEL to PT" {
                 |${between("LEL", "PT")}
                 |${zeroRates}
                 |}""".some.filter(_ => year >= 2011),
             s"""|"LEL to ST" {
                 |${between("LEL", "ST")}
                 |${zeroRates}
                 |}""".some.filter(_ => year >= 2011),
             s"""|"LEL to EE_ET" {
                 |${between("LEL", "EE_ET")}
                 |${zeroRates}
                 |}""".some.filter(_ => year < 2011),
             s"""|"LEL to ER_ET" {
                 |${between("LEL", "ER_ET")}
                 |${zeroRates}
                 |}""".some.filter(_ => year < 2011),
             s"""|"PT to UEL" {
                 |${between("PT", "UEL")}
                 |${employeeRatesOnly("Rate")}
                 |}""".some.filter(_ => year >= 2011),
             s"""|"ST to UEL" {
                 |${between("ST", "UEL")}
                 |${employerRatesOnly("Rate")}
                 |}""".some.filter(_ => year >= 2011),
             s"""|"EE_ET to UEL" {
                 |${between("EE_ET", "UEL")}
                 |${employeeRatesOnly("Rate")}
                 |}""".some.filter(_ => year < 2011),
             s"""|"ER_ET to UEL" {
                 |${between("ER_ET", "UEL")}
                 |${employerRatesOnly("Rate")}
                 |}""".some.filter(_ => year < 2011),
             s"""|"Above UEL" {
                 |${above("UEL")}
                 |${bothRates("AboveUEL")}
                 |}""".some,
             s"""|"Above UST" {
                 |${above("UST")}
                 |${bothRates("AboveUST")}
                 |}""".some.filter(_ => !sameBands("UST", "UEL")), 
             s"""|"Employees Rebate" {
                 |${between("LEL", "EE_ET")}
                 |${employeeRatesOnly("NIC Rebate")}
                 |}""".some,
             s"""|"Employers Rebate" {
                 |${between("LEL", "ER_ET")}
                 |${employerRatesOnly("NIC Rebate")}
                 |}""".some
               
           ).flatten.map(_.stripMargin).mkString("\n").indent(1)

           val bandsFormattedP = allKeys.map { key =>

             val employee = formatRatesP("employee", rates.mapValues(_.get("EE_" + key)).collect{case (k,Some(v)) => (k,v)})
             val employer = formatRatesP("employer", rates.mapValues(_.get("ER_" + key)).collect{case (k,Some(v)) => (k,v)})            

             s"""|$key {
                 |${employee.indent(1)}
                 |${employer.indent(1)}
                 |}""".stripMargin
           }

        s"""|${formatPeriod(on)} {     
            |${bandsFormatted}
            |}""".stripMargin.indent(1)   
      }

      s"""|class-one {
          |${oldEntries.mkString("\n")}
          |${newEntries.mkString("\n")}
          |}""".stripMargin
    }

      // val c1old = (
      //   c1RatesPre1999.get(on),
      //   c1BandsPre1999.collectFirst{case (k,v) if k isSupersetOf on => v}
      // ).mapN{ case (rates, bands) =>

      //     // up to 5 'bands' - Wk, Mnth and Ann values, e.g. - 'Ann Band 4'
      //     // 'bands' 1 and 5 always present
      //     // 'bands' 2-4 present when year > 1984
      //     // these are actually bounds, not bands
      //     val bounds: List[(Int, String, String, String)] = (1 to 5).map { i =>
      //       (
      //         Some(i),
      //         bands.get(s"Wk Band $i"),
      //         bands.get(s"Mnth Band $i"),
      //         bands.get(s"Ann Band $i"),              
      //       ).tupled
      //     }.toList.flatten

      //     val bandNames = bounds.size match {
      //       case 2 => Vector("Up to LEL", "LEL to UEL", "Above UEL")
      //       case 5 => Vector("Up to LEL", "Band One", "Band Two", "Band Three", "Band Four", "Above UEL")
      //       case _ => sys.error(s"I don't know how to label the bands for $on")
      //     }

      //     def bandAmounts(fromWk: String, fromMnth: String, fromYr: String, toWk: String, toMnth: String, toYr: String): String =
      //       s"""|year: "[$fromYr,$toYr)"
      //           |month: "[$fromMnth,$toMnth)"
      //           |week: "[$fromWk,$toWk)" """.stripMargin.indent(1)

      //     val rateColNames = Vector(
      //       ("Up to LEL1 (employee)", "Up to LEL1 (employer)"),
      //       ("Employee Rte 1","Employer Rte 1"),
      //       ("Employee Rte 2","Employer Rte 2"),
      //       ("Employee Rte 3","Employer Rte 3"),
      //       ("Employee Rte 4","Employer Rte 4"),
      //       ("Above UEL (employee)", "Above UEL (employer)")
      //     )

      //     def bandRates(i: Int) = {
      //       val (ee, er) = rateColNames(i)
      //       val lookup: List[(Char, (String, String))] = rates.toList.map{ case (cat, colMat) =>
      //         cat -> (colMat.getOrElse(ee, "0"), colMat.getOrElse(er, "0"))
      //       }

      //       def formatRates(name: String)(
      //         func: PartialFunction[(Char, (String, String)),(String, Char)]
      //       ) = lookup.collect(func)
      //         .groupBy(_._1)
      //         .mapValues(_.map(_._2).sorted.mkString)
      //         .toList match {
      //           case Nil => ""
      //           case (rate, cats) :: Nil =>
      //             s"$name.$cats = ${rate.formatPercent}"
      //           case many =>
      //             val i = many.map {
      //               case (rate, cats) => s"$cats = ${rate.formatPercent}"
      //             }.mkString("\n").indent(1)
      //             s"""|$name {
      //                 |$i
      //                 |}""".stripMargin
      //         }


      //       val employeeRates = formatRates("employee"){
      //         case (cat, (ee, _)) if BigDecimal(ee) != Zero => (ee, cat)
      //       }

      //       val employerRates = formatRates("employer"){
      //         case (cat, (_, er)) if BigDecimal(er) != Zero => (er, cat)
      //       }

      //       (employeeRates +"\n" + employerRates).indent(1)
      //     }

      //     // val firstBand: String = {
      //     //   val (i, toWk, toMnth, toYr) = bounds.head
      //     //   s"""|below $i {
      //     //       |${bandAmounts("£0","£0","£0",toWk, toMnth, toYr)}
      //     //       |${bandRates(i)}
      //     //       |}""".stripMargin
      //     // }

      //     val lastBand: String = {
      //       val (j, fromWk, fromMnth, fromYr) = bounds.last
      //       s"""|${bandNames.last} {
      //           |${bandAmounts(fromWk, fromMnth, fromYr, "∞", "∞", "∞")}
      //           |${bandRates(j)}
      //           |}""".stripMargin
      //     }

      //     val midBands: List[String] = bounds.sliding(2).map {
      //       case (i, fromWk, fromMnth, fromYr) :: (j, toWk, toMnth, toYr) :: Nil =>
      //         s"""|${bandNames(i)} {
      //             |${bandAmounts(fromWk, fromMnth, fromYr, toWk, toMnth, toYr)}
      //             |${bandRates(i)}
      //             |}""".stripMargin
      //       case _ => sys.error("impossible")
      //     }.toList

      //     val formattedBands = (midBands :+ lastBand).mkString("\n").indent(1)

      //     s"""|class-one {
      //         |$formattedBands
      //         |}""".stripMargin.indent(1)
      // }.getOrElse("")

      // val c1new = (
      //   c1RatesPost1999.get(on),
      //   c1BandsPost1999.get(on)
      // ).mapN{ case (rates, bands) =>

      //     def formatRatesP(name: String, data: Iterable[(Char, String)]) =
      //       data.groupBy(_._2)
      //         .mapValues(_.map(_._1).toList.sorted.mkString)
      //         .toList match {
      //           case Nil => ""
      //           case (rate, cats) :: Nil =>
      //             s"$name.$cats = ${rate}"
      //           case many =>
      //             val i = many.map {
      //               case (rate, cats) => s"$cats = ${rate}"
      //             }.mkString("\n").indent(1)
      //             s"""|$name {
      //                 |$i
      //                 |}""".stripMargin
      //         }

      //     val (employeeKeys, employerKeys) = rates
      //       .values.flatMap(_.keys).toList
      //       .distinct.foldLeft((List.empty[String], List.empty[String])) {
      //         case ((accEE, accER), n) if n.startsWith("EE") => ((n.drop(3) :: accEE), accER)
      //         case ((accEE, accER), n) if n.startsWith("ER") => (accEE, (n.drop(3) :: accER))
      //         case ((accEE, accER), _) => (accEE, accER)
      //       }

      //     val allKeys = (employeeKeys ++ employerKeys).distinct

      //     val ratesUnstructured = allKeys.map { key =>
      //       val employee = rates.mapValues(_.get("EE_" + key)).collect{case (k,Some(v)) => (k,v)}
      //       val employer = rates.mapValues(_.get("ER_" + key)).collect{case (k,Some(v)) => (k,v)}            
      //       key -> (employee, employer)
      //     }

      //     def upTo(name: String): String =
      //       s"""|year: [0,${bands("Ann" + name)})
      //           |month: [0,${bands("Mnth" + name)})
      //           |week: [0,${bands("Wk" + name)})
      //           |""".stripMargin.indent(1)

      //     def above(name: String): String = 
      //       s"""|year: [${bands("Ann" + name)},∞)
      //           |month: [${bands("Mnth" + name)},∞)
      //           |week: [${bands("Wk" + name)},∞)
      //           |""".stripMargin.indent(1)
          
      //     def between(from: String, to: String): String =
      //       s"""|year: [${bands("Ann" + from)},${bands("Ann" + to)})
      //           |month: [${bands("Mnth" + from)},${bands("Mnth" + to)})
      //           |week: [${bands("Wk" + from)},${bands("Wk" + to)})
      //           |""".stripMargin.indent(1)

      //     def betweenSafe(from: String, to: String): Option[String] =
      //       (
      //         bands.get("Ann" + from),
      //         bands.get("Ann" + to),
      //         bands.get("Mnth" + from),
      //         bands.get("Mnth" + to),
      //         bands.get("Wk" + from),
      //         bands.get("Wk" + to)
      //       ).mapN{ case (fYear, tYear, fMonth, tMonth, fWeek, tWeek) => 
      //        s"""|year: [$fYear,$tYear)
      //            |month: [$fMonth,$tMonth)
      //            |week: [$fWeek,$tWeek)""".stripMargin.indent(1)
      //       }

      //     val zeroRates: String = {
      //       val allCats = rates.keys.toList.distinct.map(_ -> "0%")
      //       formatRatesP("employee", allCats) + "\n" + formatRatesP("employer", allCats)
      //     }.indent(1)

      //     def singleRates(name: String, key: String): String = {
      //       val rateMap = rates.mapValues(_.get(key)).collect{ case (k,Some(v)) => (k,v)}
      //       formatRatesP(name, rateMap)
      //     }.indent(1)

      //     def employeeRatesOnly(name: String): String = singleRates("employee", "EE_" + name)
      //     def employerRatesOnly(name: String): String = singleRates("employer", "ER_" + name)
      //     def bothRates(name: String): String =
      //       employeeRatesOnly(name) + "\n" + employerRatesOnly(name)

      //     val bandsFormatted = List(
      //       s"""|Up to LEL { 
      //           |${upTo("LEL")}
      //           |${zeroRates}
      //           |}""".some,
      //       s"""|LEL to Primary threshold {
      //           |${between("LEL", "PT")}
      //           |${zeroRates}
      //           |}""".some,
      //       s"""|LEL to Secondary threshold {
      //           |${between("LEL", "ST")}
      //           |${zeroRates}
      //           |}""".some,
      //       s"""|Primary threshold to UEL {
      //           |${between("PT", "UEL")}
      //           |${employeeRatesOnly("Rate")}
      //           |}""".some,
      //       s"""|Secondary threshold to UEL {
      //           |${between("ST", "UEL")}
      //           |${employerRatesOnly("Rate")}
      //           |}""".some,
      //       s"""|Above UEL {
      //           |${above("UEL")}
      //           |${bothRates("AboveUEL")}
      //           |}""".some,
      //       s"""|Above UST {
      //           |${above("UST")}
      //           |${bothRates("AboveUST")}
      //           |}""".some,
      //     ).flatten.map(_.stripMargin).mkString("\n").indent(1)

      //     val bandsFormattedP = allKeys.map { key =>

      //       val employee = formatRatesP("employee", rates.mapValues(_.get("EE_" + key)).collect{case (k,Some(v)) => (k,v)})
      //       val employer = formatRatesP("employer", rates.mapValues(_.get("ER_" + key)).collect{case (k,Some(v)) => (k,v)})            

      //       s"""|$key {
      //           |${employee.indent(1)}
      //           |${employer.indent(1)}
      //           |}""".stripMargin

      //     }.mkString("\n").indent(1)

      //     s"""|class-one {
      //         |$bandsFormatted
      //         |}""".stripMargin.indent(1)
      // }.getOrElse("")

    val contents = s"""|
                       |$c1
                       |$c2
                       |$c3
                       |$c4
                       |""".stripMargin

    println(contents)


    val file = new File("national-insurance-new.conf")
    val writer = new BufferedWriter(new FileWriter(file))
    writer.write(contents)
    writer.close()

    import sys.process._
    "mv --backup=numbered national-insurance-new.conf national-insurance.conf".!!


      // if (class1.isDefinedAt(on)) println("  class1")
      // if (class2.isDefinedAt(on)) println("  class2")
      // if (class3.isDefinedAt(on)) println("  class3")
      // if (class4.isDefinedAt(on)) println("  class4")                  
      // if (bandTable.isDefinedAt(on)) println("  bandTable")
      // if (limits1999.isDefinedAt(on)) println("  limits1999")                  
  }
}
