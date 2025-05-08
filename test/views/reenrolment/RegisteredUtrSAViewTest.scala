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

import forms.reenrolment.RegisteredUtrForm.awrsEnrolmentUtrForm
import play.twirl.api.HtmlFormat
import views.ViewTestFixture
import views.html.reenrolment.awrs_registered_utr

class RegisteredUtrSAViewTest extends ViewTestFixture  {

  val template: awrs_registered_utr = app.injector.instanceOf[views.html.reenrolment.awrs_registered_utr]
  // indicating logged-in user has IR-SA enrolment
  val isSA = true
  override val htmlContent: HtmlFormat.Appendable = template.apply(awrsEnrolmentUtrForm.form, isSA)(fakeRequest, messages, mockAppConfig)

  "Awrs Utr Template" must {

    "Display all fields correctly for IR-AS Tax Enrolment" in {
      heading mustBe "Enter your Self Assessment Tax Unique Taxpayer Reference (UTR)"

      document.select("label").text() mustBe "Your Unique Taxpayer Reference (UTR) is 10 or 13 digits long. You can find it in your Personal Tax Account, the HMRC app, or on tax returns and other letters about Corporation Tax. It might be called ‘reference’, ‘UTR’, or ‘official use’."

      document.select("input").attr("id") mustBe "utr"
    }
  }
}
