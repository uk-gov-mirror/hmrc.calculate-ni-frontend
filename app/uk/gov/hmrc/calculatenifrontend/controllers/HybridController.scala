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

package uk.gov.hmrc.calculatenifrontend.controllers

import javax.inject.{Inject, Singleton}

import play.api.mvc._
import uk.gov.hmrc.calculatenifrontend.config.AppConfig

@Singleton
class HybridClassOneController @Inject()(
  appConfig: AppConfig,
  mcc: MessagesControllerComponents
) extends HybridController(mcc) {

  import renderer.all._

  implicit val config: AppConfig = appConfig

  val helloWorld: Action[AnyContent] = Action { r =>

    val request = r.body
      .asFormUrlEncoded
      .getOrElse(Map.empty)
      .collect { case (k,v::_) => (k,v) }

    Ok(page("hybrid page"){
      form(
        method := "POST",
        id := "mainform"
      )(
        eoi.frontend.RowCalc().render(request)
        
      )
    })
  }

}
