/*
 * Copyright 2026 HM Revenue & Customs
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

import forms.ApplicationDeclarationForm.applicationDeclarationForm
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat

class ApplicationDeclarationViewTest extends ViewTestFixture {

  val templateApplicationDeclaration: views.html.awrs_application_declaration = app.injector.instanceOf[views.html.awrs_application_declaration]

  val htmlContent: HtmlFormat.Appendable = templateApplicationDeclaration.apply(applicationDeclarationForm.form, isEnrolledApplicant = true)(fakeRequest, messages, mockAppConfig)

  "the application declaration page" must {

    "display the correct header" in {
      heading mustBe messages("awrs.application_declaration.heading")
    }

    "display the correct declaration text" in {
      bodyText mustBe Messages("awrs.application_declaration.lede")
    }

    "display the name and job title/role input fields" in {
      document.getElementById("declarationName").attr("name") mustBe "declarationName"
      document.getElementById("declarationRole").attr("name") mustBe "declarationRole"
    }

    "display the information text" in {
      document.getElementsByClass("govuk-warning-text__text").text() mustBe Messages("Warning I want to amend the application for . I confirm that the information provided in this application is accurate and complete. I understand that knowingly including any false information in this application is a criminal offence.")
    }

    "display the continue button" in {
      buttonText mustBe Messages("awrs.application_declaration.confirmation_and_send_application")
    }

    "display the back link" in {
      back_link mustBe Messages("awrs.generic.back")
      back_link_href mustBe controllers.routes.IndexController.showIndex.url
    }
  }

}
