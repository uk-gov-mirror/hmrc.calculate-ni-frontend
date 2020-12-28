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

import uk.gov.hmrc.play.bootstrap.frontend.controller._
import play.api.mvc._
import scalatags.Text.all._

abstract class HybridController(
  override val controllerComponents: MessagesControllerComponents
) extends FrontendController(controllerComponents) {

  implicit val renderer = scalatags.Text

  def page(t: String)(content: Tag): Tag = html(
    lang:= "en",
    attr("data-geolocscriptallow") := "true"
  )(
    head(
      meta(charset:= "utf-8"),
      tag("title")(t),
      link(rel:= "icon", href := "/calculate-ni/favicon.ico"),
      link(rel:= "icon", href := "/calculate-ni/favicon.meta"),
      tag("ico")(name:= "viewport", attr("content"):="width=device-width,initial-scale=1"),
      meta( name:="theme-color", attr("content"):="#000000"),
      meta(name:="description", attr("content"):="Web site created using create-react-app"),
      link(rel:="apple-touch-icon", href:="/calculate-ni/logo192.png"),
      link(rel:="manifest", href:="/calculate-ni/manifest.json"),
      link(href:="/calculate-ni/static/css/main.dec0bd8b.chunk.css", rel:="stylesheet"),
      script( src:="moz-extension://4a843693-d819-4a3b-97ec-f8a4d9835abb/assets/prompt.js")
    ),
    body(
      div(id:="root")(
        div(cls:="App")(
          header(
            img(src:="/calculate-ni/static/media/logo.80694881.png", cls:="App-logo", alt:="logo")
          ),
          content
        )
      ),
      raw(
        scalajs.html.scripts("frontend",
          _root_.controllers.routes.Assets.versioned(_).toString,
          name => getClass.getResource(s"/public/$name") != null).body
      )
    )
  )
}
