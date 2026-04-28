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

import forms.WithdrawalConfirmationForm.withdrawalConfirmation
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import views.html.awrs_withdrawal_confirmation

class WithdrawalConfirmationViewTest extends ViewTestFixture {

  val templateWithdrawalConfirmation: awrs_withdrawal_confirmation = app.injector.instanceOf[views.html.awrs_withdrawal_confirmation]

  val htmlContent: HtmlFormat.Appendable = templateWithdrawalConfirmation.apply(withdrawalConfirmation)(fakeRequest, messages, mockAppConfig)


  "the withdrawal confirmation page" must {

    "display the correct header" in {
      heading mustBe messages("awrs.withdrawal.confirm_page.heading")
    }

    "display the correct radio button options" in {
      val radioOptions = document.select("label.govuk-label")
      radioOptions.size() mustBe 2

      radioOptions.get(0).text() mustBe Messages("Yes")
      radioOptions.get(1).text() mustBe Messages("No")
    }

    "display the continue button" in {
      buttonText mustBe Messages("awrs.generic.continue")
    }

    "display the back link" in {
      back_link mustBe Messages("awrs.generic.back")
      back_link_href mustBe controllers.routes.WithdrawalController.showWithdrawalReasons.url
    }

    "display error messages if a radio button is not selected" in {
      val emptyForm = withdrawalConfirmation.bind(Map("confirmWithdrawal" -> ""))
      val doc = Jsoup.parse(templateWithdrawalConfirmation.apply(emptyForm)(fakeRequest, messages, mockAppConfig).body)
      val errorBody = doc.select(".govuk-error-summary__body")
      errorBody.isEmpty mustBe false
      val errorLinkText = doc.select(".govuk-error-summary__list li a").text()
      errorLinkText mustBe "Select yes if you want to withdraw your registration"
    }

    "display text when yes radio button is selected" in {
      val yesSelectedForm = withdrawalConfirmation.bind(Map("confirmWithdrawal" -> "true"))
      val doc = Jsoup.parse(templateWithdrawalConfirmation.apply(yesSelectedForm)(fakeRequest, messages, mockAppConfig).body)
      val yesText = doc.getElementsByClass("govuk-warning-text__text").text()
      yesText mustBe "Warning You must wait 24 hours after withdrawing your registration before you can start a new registration."
    }
  }
}
