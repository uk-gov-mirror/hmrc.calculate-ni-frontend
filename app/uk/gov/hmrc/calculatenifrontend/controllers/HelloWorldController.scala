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
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.calculatenifrontend.config.AppConfig
import uk.gov.hmrc.calculatenifrontend.views.html.HelloWorldPage
import scalatags.Text.all._
import scala.concurrent.Future
import play.twirl.api.Html

@Singleton
class HelloWorldController @Inject()(
  appConfig: AppConfig,
  mcc: MessagesControllerComponents,
  helloWorldPage: HelloWorldPage)
    extends FrontendController(mcc) {

  implicit val config: AppConfig = appConfig

  val calc: Action[AnyContent] = Action { implicit request =>
    val htmlRaw = Html("""
<html data-geolocscriptallow="true" lang="en"><head><meta charset="utf-8"><link rel="icon" href="/calculate-ni/favicon.ico"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="theme-color" content="#000000"><meta name="description" content="Web site created using create-react-app"><link rel="apple-touch-icon" href="/calculate-ni/logo192.png"><link rel="manifest" href="/calculate-ni/manifest.json"><title>React App</title><link href="/calculate-ni/static/css/main.dec0bd8b.chunk.css" rel="stylesheet"><script src="moz-extension://4a843693-d819-4a3b-97ec-f8a4d9835abb/assets/prompt.js"></script></head><body><noscript>You need to enable JavaScript to run this app.</noscript>

<div id="root">
  <div class="App">
    <header>
      <img src="/calculate-ni/static/media/logo.80694881.png" class="App-logo" alt="logo">
    </header>
    <form novalidate="">
      <fieldset class="details">
         <legend class="float-left">Details</legend>
         <button type="button" class="toggle">Open details</button>
      </fieldset>
      <div class="form-group table-wrapper">
        <div>
          <div class="container">
            <div class="form-group half">
              <label class="form-label">Tax year:</label>
              <div class="select tax-year"><select><option value="50">April 5th 2018 - April 5th 2019</option><option value="49">April 5th 2017 - April 5th 2018</option><option value="48">April 5th 2016 - April 5th 2017</option><option value="47">April 5th 2015 - April 5th 2016</option><option value="46">April 5th 2014 - April 5th 2015</option><option value="45">April 5th 2013 - April 5th 2014</option><option value="44">April 5th 2012 - April 5th 2013</option><option value="43">April 5th 2011 - April 5th 2012</option><option value="42">April 5th 2010 - April 5th 2011</option><option value="41">April 5th 2009 - April 5th 2010</option><option value="40">April 5th 2008 - April 5th 2009</option><option value="39">April 5th 2007 - April 5th 2008</option><option value="38">April 5th 2006 - April 5th 2007</option><option value="37">April 5th 2005 - April 5th 2006</option><option value="36">April 5th 2004 - April 5th 2005</option><option value="35">April 5th 2003 - April 5th 2004</option><option value="34">April 5th 2002 - April 5th 2003</option><option value="33">April 5th 2001 - April 5th 2002</option><option value="32">April 5th 2000 - April 5th 2001</option><option value="31">April 5th 1999 - April 5th 2000</option><option value="30">April 5th 1998 - April 5th 1999</option><option value="29">April 5th 1997 - April 5th 1998</option><option value="28">April 5th 1996 - April 5th 1997</option><option value="27">April 5th 1995 - April 5th 1996</option><option value="26">April 5th 1994 - April 5th 1995</option><option value="25">April 5th 1993 - April 5th 1994</option><option value="24">April 5th 1992 - April 5th 1993</option><option value="23">April 5th 1991 - April 5th 1992</option><option value="22">April 5th 1990 - April 5th 1991</option><option value="21">October 2nd 1989 - April 5th 1990</option><option value="20">April 5th 1989 - October 2nd 1989</option><option value="19">April 5th 1988 - April 5th 1989</option><option value="18">April 5th 1987 - April 5th 1988</option><option value="17">April 5th 1986 - April 5th 1987</option><option value="16">October 2nd 1985 - April 5th 1986</option><option value="15">April 5th 1985 - October 2nd 1985</option><option value="14">October 2nd 1984 - April 5th 1985</option><option value="13">April 5th 1984 - October 2nd 1984</option><option value="12">August 2nd 1983 - April 5th 1984</option><option value="11">April 5th 1983 - August 2nd 1983</option><option value="10">August 2nd 1982 - April 5th 1983</option><option value="9">April 5th 1982 - August 2nd 1982</option><option value="8">April 5th 1981 - April 5th 1982</option><option value="7">April 5th 1980 - April 5th 1981</option><option value="6">April 5th 1979 - April 5th 1980</option><option value="5">October 2nd 1978 - April 5th 1979</option><option value="4">April 5th 1978 - October 2nd 1978</option><option value="3">April 5th 1977 - April 5th 1978</option><option value="2">April 5th 1976 - April 5th 1977</option><option value="1">April 5th 1975 - April 5th 1976</option></select>

</div></div>
<div class="form-group half"><button type="button" class="button govuk-button govuk-button--secondary nomar">Save and print</button></div></div><table class="contribution-details"><thead>
<tr class="clear">
  <th class="lg" colspan="4"><span>Contribution payment details</span></th>
  <th class="border" colspan="2"><span>Net contributions</span></th>
</tr>
<tr>
<th>Period</th>
<th>Category</th>
<th>Gross Pay</th>
<th>EE</th>
<th>ER</th>
</tr>
</thead>

<tbody>
<tr class="" id="ki4kbuj6">
<td>
  <select name="period">
    <option value="Wk">Weekly</option>
    <option value="Mnth">Monthly</option>
    <option value="4Wk">Four week</option>
  </select>
</td>
<td>
  <select name="category">
    <option value="A">A</option>
    <option value="B">B</option>
    <option value="C">C</option>
    <option value="J">J</option>
    <option value="M">M</option>
    <option value="H">H</option>
    <option value="Z">Z</option>
  </select>
</td>
<td class="">
  <input class="gross-pay" name="ki4kbuj6-gross" type="text" id="ki4kbuj6-gross" value="0">
</td>
<td>£0.00</td>
<td>£0.00</td>
</tr></tbody></table><div class="container"><div class="container"><div class="form-group subsection"><button class="button govuk-button govuk-button--secondary">Repeat row</button></div><div class="form-group subsection"><button class="button govuk-button govuk-button--secondary">Clear table</button></div></div><div class="container"><div class="form-group subsection"><button class="button">Calculate</button></div></div></div></div></div><div class="subsection totals"><h2 class="section-heading">Totals</h2><div class="spaced-table-wrapper"><table class="totals-table spaced-table"><thead><tr><th>Gross pay</th><th>Net contributions</th><th>Employee contributions</th><th>Employer contributions</th></tr></thead><tbody><tr><td class="readonly"><span>£0.00</span></td><td class="readonly"><span>£0.00</span></td><td class="readonly"><span>£0.00</span></td><td class="readonly"><span>£0.00</span></td></tr><tr><th class="right error-line-label"><span>NI Paid</span></th><td class="input-cell"><div class="form-group "><input type="text" inputmode="decimal" name="niPaidNet" id="niPaidNet" class="govuk-input " value="0"></div></td><td class="input-cell"><div class="form-group "><input type="text" inputmode="decimal" name="NiPaidEmployee" id="NiPaidEmployee" class="govuk-input " value="0"></div></td><td class="readonly"><span>£0.00</span></td></tr><tr><th class="right">Underpayment</th><td class="readonly"><span>£0.00</span></td><td class="readonly"><span>£0.00</span></td><td class="readonly"><span>£0.00</span></td></tr><tr><th class="right">Overpayment</th><td class="readonly"><span>£0.00</span></td><td class="readonly"><span>£0.00</span></td><td class="readonly"><span>£0.00</span></td></tr></tbody></table></div></div></form></div></div>
<div style="pointer-events: none !important; display: none !important; position: absolute; top: 0px !important; left: 0px !important; z-index: 2147483647 !important; box-sizing: content-box !important;"></div></body></html>""")

    val options = List(
      option(value:="50")("April 5th 2018 - April 5th 2019")
    )

    val htmlP = html(
      head(
        title := ""
      )(
        link(src:="/calculate-ni/static/css/main.dec0bd8b.chunk.css", tpe:= "stylesheet")
      ),
      body(
        div(id:= "root")(
          div(id:="App")(
            header(
              img(cls:="App-logo", alt:="logo", src:="/calculate-ni/static/media/logo.80694881.png")
            ),
            form(
              fieldset(cls:= "details")(
                legend(cls:="float-left")("Details"),
                button(tpe:= "button", cls:="toggle")("Open Details")
              ),
              div(cls:="form-group table-wrapper")(
                div(
                  div(cls:="container")(
                    div(cls:="form-group half")(
                      label(cls:= "form-label")("Tax year:"),
                      div(cls:="select tax-year")(
                        select(
                          options: _*
                        )
                      )
                    ),
                    div(cls:="form-group half")(
                      button(tpe:= "button", cls:="button govuk-button govuk-button--secondary nomar")("Save and print")
                    )
                  ),
                  table(cls:="contribution-details")(
                    thead(
                      tr(
                        th(cls:="lg", colspan:=2)(span("Contribution payment details")), 
                        th(cls:="border",colspan:=2)(span("Net contributions"))
                      ),
                      tr(
                        th("Period"),
                        th("Category"),
                        th("Gross Pay"),
                        th("EE"),
                        th("ER")
                      )
                    )
                  )
                )
              )
            )
          )
        )
      )
    )

    Ok(htmlP
      // html(
      //   head(
      //     title := "Test"
      //   ),
      //   body(
      //     h1("This is a Triumph"),
      //     div(
      //       "Test"
      //     )
      //   )
      // )
    )
  }


}
