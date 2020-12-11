/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package acceptance.pages

import acceptance.config.AcceptanceTestConfiguration
import org.openqa.selenium.By

object NiCalcPage extends BasePage {
  val url = AcceptanceTestConfiguration.url("calculate-ni")
  val title = "React App"

  val contributionsTablePath = "/html/body/div/div/form/div[1]/div/table/tbody/"
  val netContributionsFieldId = "niPaidNet"
  val employeeContributionsFieldId = "NiPaidEmployee"
  val calculateButtonPath = "/html/body/div/div/form/div[1]/div/div[2]/div[2]/div/button"

  val grossPayPath = "/html/body/div/div/form/div[2]/div/table/tbody/tr[1]/td[1]/span"

  val netContributionsPath = "/html/body/div/div/form/div[2]/div/table/tbody/tr[1]/td[2]/span"
  val netContUnderPayPath = "/html/body/div/div/form/div[2]/div/table/tbody/tr[3]/td[1]/span"
  val netContOverPayPath = "/html/body/div/div/form/div[2]/div/table/tbody/tr[4]/td[1]/span"

  val employeeContributionsPath = "/html/body/div/div/form/div[2]/div/table/tbody/tr[1]/td[3]/span"
  val employeeContributionsUnderpayPath = "/html/body/div/div/form/div[2]/div/table/tbody/tr[3]/td[2]/span"
  val employeeContributionsOverpayPath = "/html/body/div/div/form/div[2]/div/table/tbody/tr[4]/td[2]/span"

  val employerContributionsPath = "/html/body/div/div/form/div[2]/div/table/tbody/tr[1]/td[4]/span"
  val employerContributionsNiPath = "/html/body/div/div/form/div[2]/div/table/tbody/tr[2]/td[3]/span"
  val employerContributionsUnderpayPath = "/html/body/div/div/form/div[2]/div/table/tbody/tr[3]/td[3]/span"
  val employerContributionsOverpayPath = "/html/body/div/div/form/div[2]/div/table/tbody/tr[4]/td[3]/span"

  def inputContributionDetails(period: String = "Weekly",
                               category: String = "A",
                               grossPay: Int
                              ): Unit = {

    val categoryDropdown = driver.findElement(By.xpath(contributionsTablePath + "tr[1]/td[2]/select"))
    val periodDropDown = driver.findElement(By.xpath(contributionsTablePath + "tr/td[1]/select"))
    val grossPayField = driver.findElement(By.xpath(contributionsTablePath + "tr/td[3]/input"))

    categoryDropdown.sendKeys(category)
    periodDropDown.sendKeys(period)
    grossPayField.clear()
    grossPayField.sendKeys(grossPay.toString)
  }

  def inputContributionDetails(numberOfRows: Int, grossPay: List[Int], category: List[String]): Unit = {

  }

  def inputTotals(netContributions: Int, empContributions: Int): Unit = {
    val netContField = driver.findElement(By.id(netContributionsFieldId))
    val empContField = driver.findElement(By.id(employeeContributionsFieldId))

    netContField.clear()
    netContField.sendKeys(netContributions.toString)
    empContField.clear()
    empContField.sendKeys(empContributions.toString)
  }

  def setTaxYear(taxYear: String): Unit = {
  }

  def calculate: Unit = {
    driver.findElement(By.xpath(calculateButtonPath)).click()
  }

  def getGrossPay: String = {
    driver.findElement(By.xpath(grossPayPath)).getText
  }

  def getNetContributions: List[String] = {
    List[String](
      driver.findElement(By.xpath(netContributionsPath)).getText,
      driver.findElement(By.xpath(netContUnderPayPath)).getText,
      driver.findElement(By.xpath(netContOverPayPath)).getText
    )
  }

  def getEmployeeContributions: List[String] = {
    List[String](
      driver.findElement(By.xpath(employeeContributionsPath)).getText,
      driver.findElement(By.xpath(employeeContributionsUnderpayPath)).getText,
      driver.findElement(By.xpath(employeeContributionsOverpayPath)).getText
    )
  }

  def getEmployerContributions: List[String] = {
    List[String](
      driver.findElement(By.xpath(employerContributionsPath)).getText,
      driver.findElement(By.xpath(employerContributionsNiPath)).getText,
      driver.findElement(By.xpath(employerContributionsUnderpayPath)).getText,
      driver.findElement(By.xpath(employerContributionsOverpayPath)).getText
    )
  }

}
