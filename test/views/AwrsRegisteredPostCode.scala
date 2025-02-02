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

import forms.AwrsRegisteredPostcodeForm.awrsRegisteredPostcodeForm
import play.twirl.api.HtmlFormat
import views.html.awrs_registered_postcode

class AwrsRegisteredPostCode extends ViewTestFixture {

  val view: awrs_registered_postcode = app.injector.instanceOf[views.html.awrs_registered_postcode]
  override val htmlContent: HtmlFormat.Appendable = view.apply(awrsRegisteredPostcodeForm.form)(fakeRequest, messages, mockAppConfig)

  "awrs registered postcode page" should {
    "render the correct content" in {
      heading mustBe "Enter the postcode you registered with Alcohol Wholesaler Registration Scheme (AWRS)"
      buttonText mustBe "Continue"
    }
  }
}
