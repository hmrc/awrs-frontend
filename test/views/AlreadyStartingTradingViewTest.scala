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

import forms.AlreadyStartingTradingForm.alreadyStartedTradingForm
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import views.view_application.helpers.LinearViewMode

class AlreadyStartingTradingViewTest extends ViewTestFixture {

  val templateAlreadyStartingTrading: views.html.awrs_already_starting_trading = app.injector.instanceOf[views.html.awrs_already_starting_trading]

  val htmlContent: HtmlFormat.Appendable = templateAlreadyStartingTrading.apply(alreadyStartedTradingForm, businessType = Some("businessPartner"))(fakeRequest, viewApplicationType = LinearViewMode, messages, mockAppConfig)

  "the already starting trading page" must {

    "display the correct header" in {
      heading mustBe messages("awrs.generic.have_you_already")
    }

    "display the correct radio button options" in {
      val radioOptions = document.select("label.govuk-label")
      radioOptions.size() mustBe 2

      radioOptions.get(0).text() mustBe Messages("Yes")
      radioOptions.get(1).text() mustBe Messages("No")
    }

    "display the continue button" in {
      buttonText mustBe Messages("awrs.generic.continue")
    }

    "display the back link" in {
      back_link mustBe Messages("awrs.generic.back")
      back_link_href mustBe controllers.routes.TradingLegislationDateController.showBusinessDetails(true).url
    }
  }
}
