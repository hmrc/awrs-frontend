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

package views.reenrolment

import forms.reenrolment.RegisteredUrnForm.awrsEnrolmentUrnForm
import play.twirl.api.HtmlFormat
import views.ViewTestFixture
import views.html.reenrolment.awrs_registered_urn

class RegisteredUrnViewTest extends ViewTestFixture  {

  val template: awrs_registered_urn = app.injector.instanceOf[views.html.reenrolment.awrs_registered_urn]

  override val htmlContent: HtmlFormat.Appendable = template.apply(awrsEnrolmentUrnForm.form)(fakeRequest, messages, mockAppConfig)

  "Awrs Urn Template" must {

      "Display all fields correctly" in {

        heading mustBe messages("awrs.reenrolment.registered_urn.title")
        document.select("label").text() mustBe messages("awrs.reenrolment.registered_urn.label")
        document.select("input").attr("id") mustBe "awrsUrn"
      }

    "contain a backlink pointing to the correct controller (haveYouRegistered)" in {

      val backlink = document.getElementById("back")
      backlink.attr("href") mustBe controllers.routes.HaveYouRegisteredController.showHaveYouRegisteredPage.url
    }
  }
}
