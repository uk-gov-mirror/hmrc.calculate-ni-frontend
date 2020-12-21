package eoi
package calc

import org.scalatest._
import cats.implicits._
import java.time.LocalDate

/**
  * These are based off of examples from https://www.gov.uk/government/publications/payroll-technical-specifications-national-insurance
  */
class ClassOneSpec extends FlatSpec with Matchers {

  lazy val C1 = eoi.calc.default

  implicit def yearToDate(year: Int) = LocalDate.of(year, 10, 1)
  implicit class RichMap(in: Map[String, (BigDecimal, BigDecimal, BigDecimal)]) {
    def totals: (BigDecimal, BigDecimal) = in.values.foldLeft((BigDecimal(0), BigDecimal(0))){
      case ((eeAcc, erAcc), (_, ee, er)) => (eeAcc + ee, erAcc + er)
    }
  }

  "2017-2018 examples" should "be the same for example 1" in {
    val result = C1.calculateClassOne(2017, 118.53, 'A', Period.Week)
    result.totals should be ((0,0))
  }

  it should "be the same for example 2" in {
    val result = C1.calculateClassOne(2017, 1000, 'A', Period.Week)
    result.totals should be (85.14,98.25)
  }

  it should "be the same for example 3" in {
    // The example doesn't specify the NI category
    // I don't know if deferred is the form that produces a rate of 5.85% or not...
    val result = C1.calculateClassOne(2017, 508, 'J', Period.Week, 2)
    result.totals should be ((11.35,26.77))
  }

  it should "be the same for example 4" in {
    val result = C1.calculateClassOne(2017, 934, 'M', Period.Week)
    result.totals should be ((86.44,9.38))
  }

  it should "be the same for example 5" in {
    val result = C1.calculateClassOne(2017, 37643, 'A', Period.Week, 18)    
    result.totals should be ((1971.44,4804.75))
  }

  it should "be the same for example 6" in {
    val catA = C1.calculateClassOne(2017, 16967, 'A', Period.Week, 24)
    val catC = C1.calculateClassOne(2017, 5000, 'C', Period.Week, 24)
    catA.totals should be ((1131.84, 1301.62))
    catC.totals should be ((0, 690))
    (catA |+| catC).totals should be ((1131.84, 1991.62))
  }

  it should "be the same for example 7" in {
    val result = C1.calculateClassOne(250, 2017, 'M', Period.Week, 1)    
    result.totals should be ((10.56,0))
  }

  "2018-2019 examples" should "be the same for example 1" in {
    val result = C1.calculateClassOne(2018, 118.53, 'A', Period.Week)
    result.totals should be ((0,0))
  }

  it should "be the same for example 2" in {
    val result = C1.calculateClassOne(2018, 1000, 'A', Period.Week)    
    result.totals should be ((89.76,115.64))
  }

  it should "be the same for example 3" in {
    // The example doesn't specify the NI category
    // I don't know if deferred is the form that produces a rate of 5.85% or not...
    val result = C1.calculateClassOne(2018, 508, 'J', Period.Week, 2)    
    result.totals should be ((10.76,25.39))
  }

  it should "be the same for example 4" in {
    val result = C1.calculateClassOne(2018, 1004, 'M', Period.Week, 1)    
    result.totals should be ((88.44,5.80))
  }

  it should "be the same for example 5" in {
    val result = C1.calculateClassOne(2018, 37643, 'A', Period.Week, 18)    
    result.totals should be ((2007.44,4792.32))
  }

  it should "be the same for example 6" in {
    val result = C1.calculateClassOne(2018, 52000, 'M', Period.Week, 52)    
    result.totals should be ((4664.12, 779.70))
  }

  it should "be the same for example 7" in {
    val result = C1.calculateClassOne(2018, 250, 'M', Period.Week, 1)    
    result.totals should be ((10.56,0))
  }

  // https://assets.publishing.service.gov.uk/government/uploads/system/uploads/attachment_data/file/773423/NI_Guidance__Software_Developers_2019_to_2020__Jan19.pdf
  "2019-2020 examples" should "be the same for example 1" in {
    val result = C1.calculateClassOne(2019, 118.53, 'A', Period.Week)
    result.totals should be ((0,0))
  }

  it should "be the same for example 2" in {
    val result = C1.calculateClassOne(2019, 1000, 'A', Period.Week)    
    result should be ((96.28,115.09))
  }

  it should "be the same for example 3" in {
    // The example doesn't specify the NI category
    // I don't know if deferred is the form that produces a rate of 5.85% or not...
    val result = C1.calculateClassOne(2019, 508, 'J', Period.Week, 2)    
    result should be ((10.3,24.29))
  }

  it should "be the same for example 4" in {
    val result = C1.calculateClassOne(1004, 2019, 'M', Period.Week, 1)    
    result.totals should be ((96.36,5.80))
  }

  it should "be the same for example 5" in {
    val result = C1.calculateClassOne(2019, 37643, 'A', Period.Week, 18)    
    result.totals should be ((2125.10,4782.39))
  }

  it should "be the same for example 6" in {
    val result = C1.calculateClassOne(2019, 52000, 'M', Period.Week, 52)    
    result.totals should be ((5004.16, 276.00))
  }

  it should "be the same for example 7" in {
    val result = C1.calculateClassOne(250, 2019, 'M', Period.Week, 1)    
    result.totals should be ((10.08,0))
  }

  "2020-2021 examples" should "be the same for example 1" in {
    val result = C1.calculateClassOne(2020, 120.53, 'A', Period.Week)
    result.totals should be ((0,0))
  }

  it should "be the same for example 2" in {
    val result = C1.calculateClassOne(1000, 2020, 'A', Period.Week)    
    result.totals should be ((94.24,114.67))
  }

  it should "be the same for example 3" in {
    // The example doesn't specify the NI category
    // I don't know if deferred is the form that produces a rate of 5.85% or not...
    val result = C1.calculateClassOne(2020, 508, 'J', Period.Week, 2)    
    result.totals should be ((8.31,23.46))
  }

  it should "be the same for example 4" in {
    val result = C1.calculateClassOne(2020, 1004, 'M', Period.Week, 1)    
    result.totals should be ((93.48,5.80))
  }

  it should "be the same for example 5" in {
    val result = C1.calculateClassOne(2020, 37643, 'A', Period.Week, 18)    
    result.totals should be ((2088.98,4774.94))
  }

  it should "be the same for example 6" in {
    val result = C1.calculateClassOne(2020, 52000, 'M', Period.Week, 52)    
    result.totals should be ((4900, 276.00))
  }

  it should "be the same for example 7" in {
    val result = C1.calculateClassOne(2020, 250, 'M', Period.Week, 1)    
    result.totals should be ((8.04,0))
  }


}
