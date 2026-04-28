/*
 * Copyright 2026 HM Revenue & Customs
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

import forms.WithdrawalReasonForm.withdrawalReasonForm
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import views.html.awrs_withdrawal_reasons

class WithdrawalReasonsViewTest extends ViewTestFixture {

  val templateWithdrawalReasons: awrs_withdrawal_reasons = app.injector.instanceOf[views.html.awrs_withdrawal_reasons]

  val htmlContent: HtmlFormat.Appendable = templateWithdrawalReasons.apply(withdrawalReasonForm.form)(fakeRequest, messages, mockAppConfig)

  "the withdrawal reasons page" must {

    "display the correct header" in {
      heading mustBe Messages("awrs.withdrawal.reasons_page.heading")
    }

    "display the correct radio button options" in {
      val radioOptions = document.select("label.govuk-label")
      radioOptions.size() mustBe 6

      radioOptions.get(0).text() mustBe Messages("awrs.withdrawal.reason.applied_in_error")
      radioOptions.get(1).text() mustBe Messages("awrs.withdrawal.reason.no_longer_trading")
      radioOptions.get(2).text() mustBe Messages("awrs.withdrawal.reason.duplicate_application")
      radioOptions.get(3).text() mustBe Messages("awrs.withdrawal.reason.joined_awrs_group")
      radioOptions.get(4).text() mustBe Messages("awrs.withdrawal.reason.other")
      radioOptions.get(5).text() mustBe Messages("awrs.withdrawal.reason.other_message")
    }

    "render the option message field if 'Others' is clicked" in {
      val formWithOtherSelected = withdrawalReasonForm.form.bind(Map("reason" -> "Others", "otherReasonMessage" -> "a reason"))
      val doc = Jsoup.parse(templateWithdrawalReasons.apply(formWithOtherSelected)(fakeRequest, messages, mockAppConfig).body)
      val otherField = doc.getElementsByClass("govuk-radios__conditional")
      otherField.isEmpty mustBe false
    }

    "display a continue button" in {
      buttonText mustBe Messages("awrs.generic.continue")
    }

    "display the back link" in {
      back_link mustBe Messages("awrs.generic.back")
      back_link_href mustBe controllers.routes.IndexController.showIndex.url
    }

    "should show error messages if no radio button is selected" in {
      val emptyForm = withdrawalReasonForm.form.bind(Map("reason" -> ""))
      val doc = Jsoup.parse(templateWithdrawalReasons.apply(emptyForm)(fakeRequest, messages, mockAppConfig).body)
      val errorBody = doc.select(".govuk-error-summary__body")
      errorBody.isEmpty mustBe false
      val errorLinkText = doc.select(".govuk-error-summary__list li a").text()
      errorLinkText mustBe "Select the reason you are withdrawing your registration"
    }

    "should show error messages if 'Others' is selected but no message is entered" in {
      val emptyForm = withdrawalReasonForm.form.bind(Map("reason" -> "Others", "reasonOther" -> ""))
      val doc = Jsoup.parse(templateWithdrawalReasons.apply(emptyForm)(fakeRequest, messages, mockAppConfig).body)
      val otherField = doc.getElementsByClass("govuk-radios__conditional")
      otherField.isEmpty mustBe false
      val errorBody = doc.select(".govuk-error-summary__body")
      errorBody.isEmpty mustBe false
      val errorLinkText = doc.select(".govuk-error-summary__list li a").text()
      errorLinkText mustBe "Enter the other reasons why you are withdrawing your registration"
    }
  }
}
