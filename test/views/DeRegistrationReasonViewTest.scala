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

import forms.DeRegistrationReasonForm
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import utils.AwrsFieldConfig
import views.html.awrs_de_registration_reason


class DeRegistrationReasonViewTest extends ViewTestFixture with AwrsFieldConfig{

  val templateDeRegistrationReason: awrs_de_registration_reason = app.injector.instanceOf[awrs_de_registration_reason]

  val mockAwrsFieldConfig: AwrsFieldConfig = mock[AwrsFieldConfig]

  val htmlContent: HtmlFormat.Appendable = templateDeRegistrationReason.apply(DeRegistrationReasonForm.deRegistrationReasonValidationForm)(fakeRequest, messages, mockAwrsFieldConfig, mockAppConfig)

  "the deregistration reason page" must {

    "display the corrent header" in {
      heading mustBe messages("awrs.de_registration_reason.page_heading")
    }

    "display the correct radio button options" in {
      val radioOptions = document.select("label.govuk-label")
      radioOptions.size() mustBe 8

      radioOptions.get(0).text() mustBe Messages("awrs.de_registration.reason.cases_to_be_registerable_for_the_scheme")
      radioOptions.get(1).text() mustBe Messages("awrs.de_registration.reason.ceases_to_trade_as_an_alcohol_wholesaler")
      radioOptions.get(2).text() mustBe Messages("awrs.de_registration.reason.joining_a_group_to_register_for_awrs")
      radioOptions.get(3).text() mustBe Messages("awrs.de_registration.reason.joining_a_partnership_to_register_for_awrs")
      radioOptions.get(4).text() mustBe Messages("awrs.de_registration.reason.group_disbanded")
      radioOptions.get(5).text() mustBe Messages("awrs.de_registration.reason.partnership_disbanded")
      radioOptions.get(6).text() mustBe Messages("awrs.de_registration.reason.other")
      radioOptions.get(7).text() mustBe Messages("awrs.de_registration_reason.other_input_label")
    }

    "render the option message field if 'Others' is clicked" in {
      val formWithOtherSelected = DeRegistrationReasonForm.deRegistrationReasonValidationForm.bind(Map("deRegistrationReason" -> "Others", "deRegistrationReason-other" -> "a reason"))
      val doc = Jsoup.parse(templateDeRegistrationReason.apply(formWithOtherSelected)(fakeRequest, messages, mockAwrsFieldConfig, mockAppConfig).body)
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
      val emptyForm = DeRegistrationReasonForm.deRegistrationReasonValidationForm.bind(Map("deRegistrationReason" -> ""))
      val doc = Jsoup.parse(templateDeRegistrationReason.apply(emptyForm)(fakeRequest, messages, mockAwrsFieldConfig, mockAppConfig).body)
      val errorBody = doc.select(".govuk-error-summary__body")
      errorBody.isEmpty mustBe false
      val errorLinkText = doc.select(".govuk-error-summary__list li a").text()
      errorLinkText mustBe "Select the reason you are cancelling your registration"
    }
    "should show error messages if 'Others' is selected but no message is entered" in {
      val emptyForm = DeRegistrationReasonForm.deRegistrationReasonValidationForm.bind(Map("deRegistrationReason" -> "Others", "deRegistrationReason-other" -> ""))
      val doc = Jsoup.parse(templateDeRegistrationReason.apply(emptyForm)(fakeRequest, messages, mockAwrsFieldConfig, mockAppConfig).body)
      val otherField = doc.getElementsByClass("govuk-radios__conditional")
      otherField.isEmpty mustBe false
      val errorBody = doc.select(".govuk-error-summary__body")
      errorBody.isEmpty mustBe false
      val errorLinkText = doc.select(".govuk-error-summary__list li a").text()
      errorLinkText mustBe "Enter the other reasons why you are cancelling your registration"
    }
  }
}
