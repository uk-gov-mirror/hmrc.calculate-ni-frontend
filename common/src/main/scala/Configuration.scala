package eoi

import java.time.LocalDate
import spire.implicits._
import spire.math.Interval
import spire.math.interval._
import scala.collection.immutable.ListMap

case class RateDefinition(
  year: Interval[BigDecimal],
  month: Option[Interval[BigDecimal]],
  week: Option[Interval[BigDecimal]],
  fourWeek: Option[Interval[BigDecimal]],    
  employee: Map[Char, BigDecimal] = Map.empty,
  employer: Map[Char, BigDecimal] = Map.empty,
  contractedOutStandardRate: Option[Boolean] = None,
  trigger: Bands = Bands.all
)

case class ClassTwo(
  weeklyRate: BigDecimal,
  smallEarningsException: BigDecimal
)

case class ClassFour(
  lowerLimit: BigDecimal, 
  upperLimit: BigDecimal,
  mainRate: BigDecimal,
  upperRate: BigDecimal
)



/** Class 1 NICs are earnings related contributions paid by employed
  * earners and their employers. Liability starts at age 16 and ends
  * at Sate Pension age for earners; employers continue to pay beyond
  * State Pension age. Up to April 2016 the contributions were paid at
  * either the contracted-out rate or the not contracted-out rate.
  * The contracted-out rate, abolished in April 2016, was payable
  * payable [sic] only where the employee was a member of a contracted-out
  * occupational scheme in place of State Second Pension (formerly
  * SERPS). Class 1 NICs are collected by HMRC along with income tax
  * under the Pay As You Earn (PAYE) scheme. 
  * 
  * Class 1A NICs are paid only by employers on the value of most
  * taxable benefits-in-kind provided to employees, such as private
  * use of company cars and fuel, private medical insurance,
  * accommodation and loan benefits.  They do not give any benefit
  * rights.
  * 
  * Class 1B NICs were introduced on 6 April 1999.  Like Class 1A they
  * are also paid only by employers and cover PAYE Settlement
  * Agreements (PSA) under which employers agree to meet the income
  * tax liability arising on a restricted range of benefits.  Class 1B
  * is payable on the value of the items included in the PSA that
  * would otherwise attract a Class 1 or Class 1A liability and the
  * value of the income tax met by the employer.  They do not give any
  * benefit rights.
  * 
  * Class 2 contributions are a flat rate weekly liability payable by
  * all self-employed people over 16 (up to State Pension age) with
  * profits above the Small Profits Threshold. Self-employed people
  * with profits below the Small Profits Threshold may pay Class 2
  * contributions voluntary. Voluntary payments of Class 2 NICs are
  * typically collected through self-assessment but can usually be
  * paid up to six years after the tax year.  Class 4 NICs may also
  * have to be paid by the self-employed if their profits for the year
  * are over the lower profits limit (see below).
  * 
  * Class 3 NICs may be paid voluntarily by people aged 16 and over
  * (but below State Pension age) to help them qualify for State
  * Pension if their contribution record would not otherwise be
  * sufficient.  Contributions are flat rate and can be paid up to six
  * years after the year in which they are due.
  * 
  * Class 4 NICS are paid by the self-employed whose profits are above
  * the lower profits limit.  They are profit related and do not count
  * for any benefits themselves.
  * 
  * Source: https://assets.publishing.service.gov.uk/government/uploads/system/uploads/attachment_data/file/882271/Table-a4.pdf
  * */
case class Configuration(
  categoryNames: Map[Char, String] = Map(
   'A' -> "Regular",
   'B' -> "Married women and widows",
   'C' -> "Pension age",
   'J' -> "Deferred",
   'H' -> "Apprentice under 25",
   'M' -> "Under 21",
   'Z' -> "Deferred and under 21",
   'X' -> "Exempt"
  ),
  classOne: Map[Interval[LocalDate], Map[String, RateDefinition]],
  classOneAB: Map[Interval[LocalDate], BigDecimal], 
  classTwo: Map[Interval[LocalDate], ClassTwo],
  classThree: Map[Interval[LocalDate], BigDecimal],
  classFour: Map[Interval[LocalDate], ClassFour],
  interestUnpaid: Map[LocalDate, BigDecimal],
  interestLatePaidRefunds: Map[LocalDate, BigDecimal]
) {

  private def dateBands[A](in: Map[LocalDate, A]): List[(Interval[LocalDate], A)] = {
    val pairs: List[(LocalDate, Option[A])] = in.mapValues(Some(_)).toList.sortBy(_._1.toEpochDay)
      (pairs :+ (LocalDate.now, None : Option[A])).sliding(2).map {
        case (from, amt) :: (to, _) :: Nil => Interval.closed(from, to.minusDays(1)) -> amt.get
      }.toList
  }

  private def fromBound[A](in: Bound[A]): Option[A] = in match {
    case Open(a) => Some(a)
    case Closed(a) => Some(a)
    case _ => None
  }


  private def intervalSizeDays(in: Interval[LocalDate]): Option[BigDecimal] = for {
    l <- fromBound(in.lowerBound)
    h <- fromBound(in.upperBound)
  } yield BigDecimal(h.toEpochDay - l.toEpochDay)


  /** Gives the amount as a pro-rata percentage of the interval the
    * 'from' date falls within. Returns 'None' if there is no interval
    * the from date is within, or the interval is unbounded 
    */
  def proRataRatio(from: LocalDate, to: LocalDate): Option[BigDecimal] = {

    for {
      taxYear <- classOne.keys.find(_.contains(from))
      total <- intervalSizeDays(taxYear)
      partial <- intervalSizeDays(Interval.closed(from, to))
    } yield (partial / total)
  }

  lazy val interestUnpaidBands = dateBands(interestUnpaid)
  lazy val interestLatePaidRefundsBands = dateBands(interestLatePaidRefunds)

  def calculateInterestUnpaid(
    dueDate: LocalDate,
    debt: BigDecimal,
    remissionPeriods: List[Interval[LocalDate]] = Nil,
    endDate: LocalDate = LocalDate.now    
  ): BigDecimal = {
    val totalInterval = Interval.closed(dueDate,endDate)

    val intervalRates: List[BigDecimal] = interestUnpaidBands.map { case (dateRange, annualisedRate) =>
      val remissionDays = remissionPeriods.map{ x => 
        intervalSizeDays(x.intersect(dateRange)).getOrElse(BigDecimal(0))
      }.sum
      val totalDays = intervalSizeDays(totalInterval).getOrElse(BigDecimal("365.25")) - remissionDays
      val daysOverlap: BigDecimal = intervalSizeDays(totalInterval.intersect(dateRange)).getOrElse(BigDecimal(0))
      val rateForInterval = annualisedRate * (totalDays / BigDecimal("365.25"))
      rateForInterval * (daysOverlap / totalDays)
    }

    debt * intervalRates.product
  }

  def calculateInterestLatePaidRefunds(
    taxYear: Interval[LocalDate], // this appears to not affect the calculation
    refundAmount: BigDecimal,
    dueDate: LocalDate,
    noOfBankHolidays: Int = 0, // this appears to nominally INCREASE the amount owed
    endDate: LocalDate = LocalDate.now        
  ): BigDecimal = {
    val totalInterval = Interval.closed(dueDate,endDate)

    val intervalRates: List[BigDecimal] = interestLatePaidRefundsBands.map { case (dateRange, annualisedRate) =>
      val totalDays = intervalSizeDays(totalInterval).getOrElse(BigDecimal("365.25")) + noOfBankHolidays
      val daysOverlap: BigDecimal = intervalSizeDays(totalInterval.intersect(dateRange)).getOrElse(BigDecimal(0))
      val rateForInterval = annualisedRate * (totalDays / BigDecimal("365.25"))
      rateForInterval * (daysOverlap / totalDays)
    }

    refundAmount * intervalRates.product
  }


  def calculateClassOneAAndB(
    on: LocalDate,
    amount: BigDecimal
  ): Option[BigDecimal] = classOneAB.at(on).map(amount * _)

  def calculateClassThree(
    on: LocalDate,
    numberOfWeeks: Int
  ): Option[BigDecimal] = classThree.at(on).map(_ * numberOfWeeks)


  def calculateClassFour(
    on: LocalDate,
    amount: BigDecimal
  ): Option[(BigDecimal,BigDecimal)] = 
    classFour.at(on).map{ f =>
      val lowerBand = Interval.closed(f.lowerLimit, f.upperLimit)
      val upperBand = Interval.above(f.upperLimit)
      (
        amount.inBand(lowerBand) * f.mainRate,
        amount.inBand(upperBand) * f.upperRate
      )
    }

  def calculateClassTwo(
    on: LocalDate,
    amount: BigDecimal
  ): Option[BigDecimal] = {
    val year: Option[ClassTwo] = classTwo.at(on)
    year.map { x =>
      if (amount < x.smallEarningsException) 0
      else x.weeklyRate * 52
    }
  }

  def calculateClassOne(
    on: LocalDate,
    amount: BigDecimal,
    cat: Char,
    period: Period.Period,
    qty: Int = 1, 
    contractedOutStandardRate: Boolean = false
  ): Map[String,(BigDecimal, BigDecimal, BigDecimal)] = {
    val defs = classOne.at(on).getOrElse(Map.empty)
    val bands = defs.collect { case (k,d) if d.contractedOutStandardRate.fold(true)(_ == contractedOutStandardRate) && d.trigger.interval(period, qty).contains(amount) => 
      val interval = period match {
        case Period.Year => d.year
        case Period.Month => (d.month.getOrElse((d.year / 12)) * qty).mapBounds(_.readDecimal)
        case Period.Week => (d.week.getOrElse((d.year / 52)) * qty).mapBounds(_.readDecimal)
        case Period.FourWeek => (d.fourWeek.getOrElse((d.year / 13)) * qty).mapBounds(_.readDecimal)
      }
      val amountInBand = amount.inBand(interval)
      val employeeRate = d.employee.getOrElse(cat, BigDecimal(0))
      val employerRate = d.employer.getOrElse(cat, BigDecimal(0))
      (k,(
        amountInBand,
        {
          val amount = period match {
            case Period.FourWeek if on.getYear() <= 1999 => ((amountInBand / 4 * employeeRate).roundNi) * 4
            case _ => (amountInBand * employeeRate).roundNi
          }
          amount
        },
        {
          val amount = period match {
            case Period.FourWeek if on.getYear() <= 1999 => ((amountInBand / 4 * employerRate).roundNi) * 4
            case _ => (amountInBand * employerRate).roundNi
          }
          amount
        }
      ))
    }

    val (totalEE, totalER) = bands.values.foldLeft((BigDecimal(0), BigDecimal(0))){
      case ((accEE, accER), (_, ee, er)) => (accEE + ee, accER + er)
    }

    if (totalEE < 0) {
      // small grey area where contribution is less than rebate, the rebate is given to the employer
      bands + ("rebateTransfer" -> (0, -totalEE, totalEE))
    } else {
      bands
    }
  }
}
