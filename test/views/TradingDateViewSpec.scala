/*
 * Copyright 2024 HM Revenue & Customs
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

package views

import forms.TradingDateForm.tradingDateForm
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import views.html.awrs_trading_date
import views.view_application.helpers.LinearViewMode


class TradingDateViewSpec extends ViewTestFixture {

  val view: awrs_trading_date = app.injector.instanceOf[views.html.awrs_trading_date]

  override val htmlContent: HtmlFormat.Appendable = view.apply(tradingDateForm(past = false, Some(true)), Some(""), pastStart = false)(
    fakeRequest,
    viewApplicationType = LinearViewMode,
    messages,
    mockAppConfig)

  "The 'What date will you start trading wholesale alcohol?' view" when {

    "rendered correctly on load" must {
      "style none of the inputs as errors" in {
        document.getElementById("tradingDate.day").className() must not include "govuk-input--error"
        document.getElementById("tradingDate.month").className() must not include "govuk-input--error"
        document.getElementById("tradingDate.year").className() must not include "govuk-input--error"
      }
    }
    "Correctly return and style errors" when {
      "all fields are empty" in {
        val emptyForm = tradingDateForm(past = false, newBusiness = Some(true)).bind(Map("tradingDate.day" -> "", "tradingDate.month" -> "", "tradingDate.year" -> ""))
        val doc       = Jsoup.parse(view.apply(emptyForm, Some("Ltd"), pastStart=false)(fakeRequest, LinearViewMode, messages, mockAppConfig).body)
        val errorBody = doc.select(".govuk-error-summary__body")
        val errorLink     = doc.select(".govuk-error-summary__list li a").attr("href")
        val errorLinkText = doc.select(".govuk-error-summary__list li a").text()

        errorLink mustBe("#tradingDate.day")
        errorLinkText mustBe Messages("awrs.business_details.error.proposedDate_emptyP")
        errorBody.isEmpty mustBe false
        doc.getElementById("tradingDate.day").className() must include("govuk-input--error")
        doc.getElementById("tradingDate.month").className() must include("govuk-input--error")
        doc.getElementById("tradingDate.year").className() must include("govuk-input--error")
      }
      "month and year is missing" in {
        val invalidForm = tradingDateForm(past = false, newBusiness = Some(true)).bind(Map("tradingDate.day" -> "11", "tradingDate.month" -> "", "tradingDate.year" -> ""))
        val doc       = Jsoup.parse(view.apply(invalidForm, Some("Ltd"), pastStart=false)(fakeRequest, LinearViewMode, messages, mockAppConfig).body)
        val errorBody = doc.select(".govuk-error-summary__body")
        val errorLink     = doc.select(".govuk-error-summary__list li a").attr("href")
        val errorLinkText = doc.select(".govuk-error-summary__list li a").text()

        errorBody.isEmpty mustBe false
        errorLinkText mustBe Messages("awrs.generic.error.date.twoFieldsMissing", "month", "year")
        errorLink mustBe("#tradingDate.month")
        doc.getElementById("tradingDate.day").className() must not include("govuk-input--error")
        doc.getElementById("tradingDate.month").className() must include("govuk-input--error")
        doc.getElementById("tradingDate.year").className() must include("govuk-input--error")
      }
      "year is missing" in {
        val invalidForm = tradingDateForm(past = false, newBusiness = Some(true)).bind(Map("tradingDate.day" -> "11", "tradingDate.month" -> "11", "tradingDate.year" -> ""))
        val doc       = Jsoup.parse(view.apply(invalidForm, Some("Ltd"), pastStart=false)(fakeRequest, LinearViewMode, messages, mockAppConfig).body)
        val errorBody = doc.select(".govuk-error-summary__body")
        val errorLink     = doc.select(".govuk-error-summary__list li a").attr("href")
        val errorLinkText = doc.select(".govuk-error-summary__list li a").text()

        errorBody.isEmpty mustBe false
        errorLinkText mustBe Messages("awrs.generic.error.date.fieldMissing", "year")
        errorLink mustBe("#tradingDate.year")
        doc.getElementById("tradingDate.day").className() must not include("govuk-input--error")
        doc.getElementById("tradingDate.month").className() must not include("govuk-input--error")
        doc.getElementById("tradingDate.year").className() must include("govuk-input--error")
      }
    }
  }
}
