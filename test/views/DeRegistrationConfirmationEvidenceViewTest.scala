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

import models.TupleDate
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import views.html.awrs_de_registration_confirmation_evidence


class DeRegistrationConfirmationEvidenceViewTest extends ViewTestFixture {

  val templateDeRegistrationConfirmationEvidence:awrs_de_registration_confirmation_evidence = app.injector.instanceOf[awrs_de_registration_confirmation_evidence]

  val htmlContent: HtmlFormat.Appendable = templateDeRegistrationConfirmationEvidence.apply(proposedEndDate = TupleDate("1","1","2020"))(fakeRequest, messages, mockAppConfig)

  "the de-registration confirmation evidence page" must {

    "display the correct header" in {
      heading mustBe Messages("awrs.de_registration.confirmation_page.description")
    }

    "display the correct message" in {
      bodyText mustBe Messages("awrs.de_registration.confirmation_page.information_what_next_2", "01 January 2020")
    }

    "not have a backlink" in {
      back_link.isEmpty mustBe true
    }
  }
}
