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

package acceptance.specs

import acceptance.pages.NiCalcPage
import acceptance.helpers.Util
import views.html.helper.input
import org.openqa.selenium.{By, WebDriver}

class NICalcSpec extends BaseAcceptanceSpec {
  feature("Calculator Page") {
    scenario(
      "The user visits the calculator"
    ) {
      Given("the user visits the page")
      go to NiCalcPage

      When("they input their contribution payment details")
      NiCalcPage.inputContributionDetails("Four Week", "M", 12345)

      And("they input the total NI Paid")
      NiCalcPage.inputTotals(123,123)

      And("they submit the calculation")
      NiCalcPage.calculate
      Thread.sleep(5000)

      Then("")
      eventually {

        NiCalcPage.getGrossPay should be("£12,345.00")

        val netContributionsList = NiCalcPage.getNetContributions

        netContributionsList(0) should be("£1,743.59")
        netContributionsList(1) should be("£1,620.59")
        netContributionsList(2) should be("£0.00")

        val employeeContributionsList = NiCalcPage.getEmployeeContributions

        employeeContributionsList(0) should be("£517.74")
        employeeContributionsList(1) should be("£394.74")
        employeeContributionsList(2) should be("£0.00")

        val employerContributionsList = NiCalcPage.getEmployerContributions

        employerContributionsList(0) should be("£1,225.85")
        employerContributionsList(1) should be("£0.00")
        employerContributionsList(2) should be("£1,225.85")
        employerContributionsList(3) should be("£0.00")

      }
    }
  }
}