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
import views.html.reenrolment.awrs_successful_enrolment

class SuccessfulEnrolmentViewTest extends ViewTestFixture {

  val view: awrs_successful_enrolment =
    app.injector.instanceOf[views.html.reenrolment.awrs_successful_enrolment]
  override val htmlContent: HtmlFormat.Appendable = view.apply()(fakeRequest, messages, mockAppConfig)

  "successful_enrolment page" should {

    "render the correct content" in {
      heading mustBe messages("awrs.reenrolment.successful_enrolment.title")
      bodyText mustBe (
        messages("awrs.reenrolment.successful_enrolment.p1") + " " +
        messages("awrs.reenrolment.successful_enrolment.p2")
        )
    }

    "contain a button to access the Business Tax Account page" in {

      val link = document.getElementById("bta-redirect-button")
      link.attr("role") mustBe "button"
    }
  }
}