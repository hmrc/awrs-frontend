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

import play.twirl.api.HtmlFormat
import views.html.urn_kickout

class URN_KickOutTest extends ViewTestFixture {

  val view: urn_kickout = app.injector.instanceOf[views.html.urn_kickout]
  override val htmlContent: HtmlFormat.Appendable = view.apply()(fakeRequest, messages, mockAppConfig)

  "urn kickout page" should {
    "render the correct content" in {
      heading mustBe "The Unique Reference Number (URN) you entered is not recognised"
      bodyText mustBe "Check that you've entered it correctly and try again If you can't find your URN, you'll need to register for AWRS. Register for AWRS"
      sign_in_btn mustBe "Register for AWRS"
      sign_in_href mustBe "#"
    }
  }
}
