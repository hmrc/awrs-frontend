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

import play.twirl.api.HtmlFormat
import views.ViewTestFixture
import views.html.reenrolment.awrs_reenrolment_kickout

class KickOutViewTest extends ViewTestFixture {

  val view: awrs_reenrolment_kickout = app.injector.instanceOf[views.html.reenrolment.awrs_reenrolment_kickout]
  override val htmlContent: HtmlFormat.Appendable = view.apply()(fakeRequest, messages, mockAppConfig)

  "The kickout page" should {
    "render the content correctly" in {

      heading mustBe messages("awrs.reenrolment.kickout.title")
      bodyText mustBe messages("awrs.reenrolment.kickout.p1")
    }
  }
}
