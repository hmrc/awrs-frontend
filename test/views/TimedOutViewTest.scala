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
import views.html.timed_out

class TimedOutViewTest extends ViewTestFixture {

  val view: timed_out = app.injector.instanceOf[views.html.timed_out]
  override val htmlContent: HtmlFormat.Appendable = view.apply()(fakeRequest, messages, mockAppConfig)

  "TimedOutView" should {
    "render the correct content" in {
      heading mustBe "For your security, we signed you out"
      bodyText mustBe "We saved your answers. Sign in using your Government Gateway to return to your application summary page."
      sign_in_btn mustBe "Sign in"
      sign_in_href mustBe "/alcohol-wholesale-scheme/landing-page"
    }
  }
}
