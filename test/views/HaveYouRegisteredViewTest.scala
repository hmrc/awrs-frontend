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

package views

import config.ApplicationConfig
import forms.HaveYouRegisteredForm.haveYouRegisteredForm
import play.twirl.api.HtmlFormat
import views.html.awrs_have_you_registered
import views.view_application.helpers.LinearViewMode

class HaveYouRegisteredViewTest extends ViewTestFixture {

  val template: awrs_have_you_registered = app.injector.instanceOf[views.html.awrs_have_you_registered]

  implicit val appConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

  override val htmlContent: HtmlFormat.Appendable = template.apply(haveYouRegisteredForm.form)(fakeRequest, LinearViewMode, messages, appConfig)


  "Have you registered template" must {
    "Display the correct h1 header" in {
      heading mustBe "Have you registered with the Alcohol Wholesaler Registration Scheme (AWRS)?"
    }

    "Display radio buttons" in {
      val yesRadio = document.select("input[type=radio][value=true]")
      val noRadio = document.select("input[type=radio][value=false]")
      val radioInputs = document.select("input[name=hasUserRegistered]")

      yesRadio.size() mustBe 1
      noRadio.size() mustBe 1
      radioInputs.size() mustBe 2
    }

    "display save and continue button" in {
      val button = document.select("button#save-and-continue").text()
      button mustBe "Continue"
    }

    "Redirect to the correct page" in {
      val formAction = document.select("form").attr("action")
      formAction mustBe controllers.routes.HaveYouRegisteredController.saveAndContinue.url
    }

    "contain a backlink pointing to the correct controller" in {
      val backlink = document.getElementById("back")
      backlink.attr("href") mustBe "http://localhost:9730/business-account/add-tax/other/alcohol"
    }
  }
}
