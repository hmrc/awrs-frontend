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

import org.mockito.Mockito.when
import play.twirl.api.HtmlFormat
import views.ViewTestFixture
import views.html.reenrolment.awrs_deenrolment_confirmation

class DeEnrolmentConfirmationViewTest extends ViewTestFixture {

  val testPostCodePageUrl = "/testUrl"
  val testBtaRedirectUrl  = "/testBtaLink"
  when(mockAppConfig.businessTaxAccountPage).thenReturn(testBtaRedirectUrl)

  val view: awrs_deenrolment_confirmation =
    app.injector.instanceOf[views.html.reenrolment.awrs_deenrolment_confirmation]

  override val htmlContent: HtmlFormat.Appendable =
    view.apply(testPostCodePageUrl)(fakeRequest, applicationConfig = mockAppConfig, messages = messages)

  "deenrolment_confirmation page" should {

    "render the correct content" in {
      heading mustBe messages("awrs.de_enrollment_confirmation")
      bodyText mustBe (messages("awrs.de_enrollment_confirmation.p1"))
    }

    "contain a button to go to PostCode page" in {
      val link = document.getElementById("continue")
      link.attr("href") mustBe testPostCodePageUrl
    }

    "contain a button to access the Business Tax Account page" in {
      val link = document.getElementById("bta-redirect-link")
      link.attr("href") mustBe testBtaRedirectUrl
    }
  }

}
