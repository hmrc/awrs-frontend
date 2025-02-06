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
import views.html.awrs_successful_enrolment

class SuccessfulEnrolmentTest extends ViewTestFixture {

  val view: awrs_successful_enrolment =
    app.injector.instanceOf[views.html.awrs_successful_enrolment]
  override val htmlContent: HtmlFormat.Appendable = view.apply()(fakeRequest, messages, mockAppConfig)
  "successful_enrolment page" should {
    "render the correct content" in {
      heading mustBe "You have added your AWRS to your business tax account"
      bodyText mustBe "You can now view and manage your AWRS registration in your business tax account. More detailed information can be found in the AWRS guidance (opens in a new tab)"
    }
  }
}