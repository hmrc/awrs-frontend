/*
 * Copyright 2023 HM Revenue & Customs
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

import forms.AwrsEnrollmentUrnForm.awrsEnrolmentUrnForm
import play.twirl.api.HtmlFormat
import views.html.awrs_urn

class AwrsUrnViewTest extends ViewTestFixture  {

  val template: awrs_urn = app.injector.instanceOf[views.html.awrs_urn]

  override val htmlContent: HtmlFormat.Appendable = template.apply(awrsEnrolmentUrnForm.form)(fakeRequest, messages, mockAppConfig)

  "Awrs Urn Template" must {

      "Display all fields correctly" in {
        heading mustBe "What is your Alcohol Wholesaler Registration Scheme (AWRS) Unique Reference Number (URN)?"

        document.select("label").text() mustBe "Your URN is 4 letters and 11 numbers, like XXAW00000123456. You can find it printed on the wholesaler or producer’s invoices. If you can’t find it, contact the wholesaler or producer for the URN."

        document.select("input").attr("id") mustBe "awrsUrn"
      }

    "contain a backlink pointing to the correct controller (haveYouRegistered)" in {
      val backlink = document.getElementById("back")
      backlink.attr("href") mustBe controllers.routes.HaveYouRegisteredController.showHaveYouRegisteredPage.url
    }


  }


}
