/*
 * Copyright 2025 HM Revenue & Customs
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
import views.html.reenrolment.awrs_view_enrolments

class ViewEnrolmentViewTest extends ViewTestFixture {

  val testBtaRedirectUrl = "/testBtaLink"
  when(mockAppConfig.businessTaxAccountPage).thenReturn(testBtaRedirectUrl)

  val view: awrs_view_enrolments =
    app.injector.instanceOf[views.html.reenrolment.awrs_view_enrolments]


  override val htmlContent: HtmlFormat.Appendable = view.apply()(fakeRequest, messages, mockAppConfig)
  "view enrolment page" should {

    "render correct back link" in {
      val backLink = document.getElementById("back")
      backLink.attr("href") mustBe "/alcohol-wholesale-scheme/reenrolment/deenrolment-confirmation"
    }
    "render the correct content" in {
      heading mustBe messages("awrs.view_enrolments")
      bodyText mustBe (
        messages("awrs.view_enrolments.p1"))
    }


    "contain a button to access the Business Tax Account page" in {
      val link = document.getElementById("bta-redirect-link")
      link.attr("href") mustBe testBtaRedirectUrl
    }
  }
}