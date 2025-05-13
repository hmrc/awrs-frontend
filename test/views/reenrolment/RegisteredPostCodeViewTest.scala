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

import forms.reenrolment.RegisteredPostcodeForm.awrsRegisteredPostcodeForm
import play.twirl.api.HtmlFormat
import views.ViewTestFixture
import views.html.reenrolment.awrs_registered_postcode

class RegisteredPostCodeViewTest extends ViewTestFixture {

  val view: awrs_registered_postcode = app.injector.instanceOf[views.html.reenrolment.awrs_registered_postcode]
  override val htmlContent: HtmlFormat.Appendable = view.apply(awrsRegisteredPostcodeForm.form)(fakeRequest, messages, mockAppConfig)

  "awrs registered postcode page" must {
    "render the correct content" in {

      heading mustBe messages("awrs.reenrolment.registered_postcode.title")
      buttonText mustBe messages("awrs.generic.continue")
      input_field_label mustBe messages("awrs.reenrolment.registered_postcode.heading")
      input_field must not be empty
      back_link mustBe messages("awrs.generic.back")
      back_link_href mustBe controllers.reenrolment.routes.RegisteredUrnController.showArwsUrnPage.url
    }
  }
}
