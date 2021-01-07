/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.calculatenifrontend.controllers

import javax.inject.{Inject, Singleton}

import play.api.mvc._
import uk.gov.hmrc.calculatenifrontend.config.AppConfig

@Singleton
class HybridClassOneController @Inject()(
  val appConfig: AppConfig,
  mcc: MessagesControllerComponents
) extends HybridController(mcc) with ConfigController {

  import renderer.all._

  val classOne: Action[AnyContent] = Action { r =>

    val request = r.body
      .asFormUrlEncoded
      .getOrElse(Map.empty)
      .collect { case (k,v::_) => (k,v) }

    Ok(page("Class One"){
      form(
        method := "POST",
        id := "mainform"
      )(
        eoi.frontend.RowCalc(conf).render(request)
        
      )
    })
  }

  val latePaidInterest: Action[AnyContent] = Action { r =>

    val request = r.body
      .asFormUrlEncoded
      .getOrElse(Map.empty)
      .collect { case (k,v::_) => (k,v) }

    Ok(page("Late Paid Interest"){
      form(
        method := "POST",
        id := "mainform"
      )(
        eoi.frontend.InterestLatePaid(conf).render(request)
      )
    })
  }

  val configJson: Action[AnyContent] = Action {
    val jsonString: String = eoi.EoiJsonEncoding.toJson(conf).toString
    Ok(jsonString).as("application/json")
  }

  val configHocon: Action[AnyContent] = Action {
    Ok(hoconText).as("application/hocon")
  }

}
