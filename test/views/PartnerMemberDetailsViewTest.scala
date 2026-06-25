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

import forms.PartnershipDetailsForm.partnershipDetailsForm
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import views.html.awrs_partner_member_details
import views.view_application.helpers.LinearViewMode

class PartnerMemberDetailsViewTest extends ViewTestFixture {

  val templatePartnerMemberDetails: awrs_partner_member_details = app.injector.instanceOf[views.html.awrs_partner_member_details]

  val htmlContent: HtmlFormat.Appendable = templatePartnerMemberDetails.apply(partnershipDetailsForm(mockAppConfig).form, 1, isNewRecord = true)(fakeRequest, viewApplicationType = LinearViewMode, messages, mockAppConfig)

  "the partner member details page" must {

    "display the correct header" in {
      heading mustBe Messages("Tell us about your nominated partner or member")
    }

    "display the correct sub header" in {
      document.getElementById("business_partners-subtext").text() mustBe Messages("awrs.business-partner.topText")
    }

    "display the correct radio button options" in {
      val radioOptions = document.getElementsByClass("govuk-radios__item")
      radioOptions.size() mustBe 9

      radioOptions.get(0).text() mustBe Messages("Person")
      radioOptions.get(1).text() mustBe Messages("Company")
      radioOptions.get(2).text() mustBe Messages("Sole trader self-employed")
    }

    "display the continue button" in {
      document.getElementById("save-and-continue").text() mustBe Messages("awrs.generic.save_continue")
    }

    "display the back link" in {
      back_link mustBe Messages("awrs.generic.back")
      back_link_href mustBe "/alcohol-wholesale-scheme/back?section=partnerDetails"
    }
  }
}
