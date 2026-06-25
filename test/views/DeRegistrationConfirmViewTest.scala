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

import forms.DeRegistrationConfirmationForm.deRegistrationConfirmationForm
import models.TupleDate
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import views.html.awrs_de_registration_confirm


class DeRegistrationConfirmViewTest extends ViewTestFixture {

  val templateDeRegistrationConfirm: awrs_de_registration_confirm = app.injector.instanceOf[awrs_de_registration_confirm]

  val proposedEndDate: TupleDate = TupleDate("1","1","2020")

  val htmlContent: HtmlFormat.Appendable = templateDeRegistrationConfirm.apply(deRegistrationConfirmationForm, proposedEndDate)(fakeRequest, messages, mockAppConfig)

  "the de-registration confirmation page" must {

    "display the correct header" in {
      heading mustBe Messages("awrs.de_registration.confirmation_heading")
    }

    "display the correct radio button options" in {
      val radioOptions = document.select("label.govuk-label")
      radioOptions.size() mustBe 2

      radioOptions.get(0).text() mustBe Messages("Yes")
      radioOptions.get(1).text() mustBe Messages("No")
    }

    "display error messages if a radio button is not selected" in {
      val emptyForm = deRegistrationConfirmationForm.bind(Map("confirmWithdrawal" -> ""))
      val doc = Jsoup.parse(templateDeRegistrationConfirm.apply(emptyForm, proposedEndDate)(fakeRequest, messages, mockAppConfig).body)
      val errorBody = doc.select(".govuk-error-summary__body")
      errorBody.isEmpty mustBe false
      val errorLinkText = doc.select(".govuk-error-summary__list li a").text()
      errorLinkText mustBe "Select yes if you want to cancel your registration"
    }

    "display test when yes radio button is selected" in {
      val yesSelectedForm = deRegistrationConfirmationForm.bind(Map("confirmWithdrawal" -> "true"))
      val doc = Jsoup.parse(templateDeRegistrationConfirm.apply(yesSelectedForm, proposedEndDate)(fakeRequest, messages, mockAppConfig).body)
      val yesText = doc.getElementsByClass("govuk-warning-text__text").text()
      yesText mustBe "Warning Unless you are joining a group or partnership, you must not trade alcohol wholesale after the 01 January 2020. You must sell or dispose of any existing stock by this date. If you continue to sell alcohol wholesale without an AWRS number, you could be liable to criminal prosecution, financial penalties and/or seizure of alcohol stock (whether or not duty paid). If you cancel your group registration all group members will be no longer approved."
    }

    "display a continue button" in {
      buttonText mustBe Messages("awrs.generic.continue")
    }

    "display the back link" in {
      back_link mustBe Messages("awrs.generic.back")
      back_link_href mustBe controllers.routes.DeRegistrationController.showDate.url
    }
  }

}
