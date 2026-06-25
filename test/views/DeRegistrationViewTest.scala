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

import forms.DeRegistrationForm.deRegistrationForm
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import views.html.awrs_de_registration


class DeRegistrationViewTest extends ViewTestFixture {

  val templateDeRegistration: awrs_de_registration = app.injector.instanceOf[awrs_de_registration]

  val htmlContent: HtmlFormat.Appendable = templateDeRegistration.apply(deRegistrationForm)(fakeRequest, messages, mockAppConfig)

  "the de-registration page" must {

    "display the correct header" in {
      heading mustBe messages("awrs.de_registration.heading")
    }

    "display the correct message" in {
      bodyText mustBe messages("awrs.de_registration.lede")
    }

    "display date fields" in {
      val dayField = document.getElementById("proposedEndDate.day")
      val monthField = document.getElementById("proposedEndDate.month")
      val yearField = document.getElementById("proposedEndDate.year")

      dayField.attr("type") mustBe "text"
      dayField.attr("maxlength") mustBe "2"
      monthField.attr("type") mustBe "text"
      monthField.attr("maxlength") mustBe "2"
      yearField.attr("type") mustBe "text"
      yearField.attr("maxlength") mustBe "4"
    }

    "display error if date fields are empty" in {
      val emptyForm = deRegistrationForm.bind(Map("proposedEndDate.day" -> "", "proposedEndDate.month" -> "", "proposedEndDate.year" -> ""))
      val doc = Jsoup.parse(templateDeRegistration.apply(emptyForm)(fakeRequest, messages, mockAppConfig).body)
      val errorBody = doc.select(".govuk-error-summary__body")
      errorBody.isEmpty mustBe false
      val errorLinkText = doc.select(".govuk-error-summary__list li a").text()
      errorLinkText mustBe Messages("awrs.de_registration.error.date_empty")
    }

    "display error if date fields are invalid" in {
      val invalidForm = deRegistrationForm.bind(Map("proposedEndDate.day" -> "32", "proposedEndDate.month" -> "13", "proposedEndDate.year" -> "2020"))
      val doc = Jsoup.parse(templateDeRegistration.apply(invalidForm)(fakeRequest, messages, mockAppConfig).body)
      val errorBody = doc.select(".govuk-error-summary__body")
      errorBody.isEmpty mustBe false
      val errorLinkText = doc.select(".govuk-error-summary__list li a").text()
      errorLinkText mustBe Messages("awrs.de_registration.error.date_valid")
    }

    "display error if date fields are too early" in {
      val invalidForm = deRegistrationForm.bind(Map("proposedEndDate.day" -> "1", "proposedEndDate.month" -> "1", "proposedEndDate.year" -> "1900"))
      val doc = Jsoup.parse(templateDeRegistration.apply(invalidForm)(fakeRequest, messages, mockAppConfig).body)
      val errorBody = doc.select(".govuk-error-summary__body")
      errorBody.isEmpty mustBe false
      val errorLinkText = doc.select(".govuk-error-summary__list li a").text()
      errorLinkText mustBe Messages("awrs.de_registration.error.proposedDate_toEarly")
    }

    "display a continue button" in {
      buttonText mustBe Messages("awrs.generic.continue")
    }

    "display the back link" in {
      back_link mustBe Messages("awrs.generic.back")
      back_link_href mustBe controllers.routes.DeRegistrationController.showReason.url
    }
  }

}
