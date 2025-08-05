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

package views.reenrolment

import controllers.reenrolment.routes
import forms.DeEnrolmentConfirmationForm
import play.twirl.api.HtmlFormat
import views.ViewTestFixture
import views.html.reenrolment.awrs_deenrolment_confirmation

class DeEnrolmentConfirmationViewTest extends ViewTestFixture {

  val view: awrs_deenrolment_confirmation =
    app.injector.instanceOf[views.html.reenrolment.awrs_deenrolment_confirmation]


  override val htmlContent: HtmlFormat.Appendable = view.apply(DeEnrolmentConfirmationForm.deEnrolmentConfirmationForm)(request = fakeRequest,messages = messages, applicationConfig = mockAppConfig)
  "deenrolment_confirmation page" should {
    "Display all fields correctly" in {
      heading mustBe messages("awrs.generic.de_enrolment_confirmation")
      document.getElementById("confirmDeEnrollment-hint").text() mustBe messages("awrs.generic.error.de_enrolment_confirmation_hint")
      document.getElementById("confirmDeEnrollment").attr("value")  mustBe "Yes"
      document.getElementById("confirmDeEnrollment-2").attr("value")  mustBe "No"
      document.getElementById("back").attr("href")  mustBe routes.RegisteredUrnController.showArwsUrnPage.url
    }
  }
}