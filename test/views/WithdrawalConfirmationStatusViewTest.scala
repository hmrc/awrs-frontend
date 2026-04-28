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

import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import views.html.awrs_withdrawal_confirmation_status
import utils.SessionUtil._

import java.time.{LocalDate, LocalTime, OffsetDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter

class WithdrawalConfirmationStatusViewTest extends ViewTestFixture {

  val templateConfirmationStatus: awrs_withdrawal_confirmation_status = app.injector.instanceOf[views.html.awrs_withdrawal_confirmation_status]

  val refinedProcessingDateTime: String =
    fakeRequest.getProcessingDate.getOrElse(OffsetDateTime.of(LocalDate.of(2020,1,1), LocalTime.of(1,1,1), ZoneOffset.ofTotalSeconds(0)).format(DateTimeFormatter.ISO_DATE_TIME))

  val htmlContent: HtmlFormat.Appendable = templateConfirmationStatus.apply(processingDate = refinedProcessingDateTime, printFriendly = true)(fakeRequest, messages, mockAppConfig)

  "the withdrawal confirmation status page" must {

    "display the correct header" in {
      heading mustBe Messages("awrs.withdrawal.confirmation_page.description")
    }

    "display the correct message" in {
      bodyText mustBe Messages("awrs.withdrawal.confirmation_page.information_what_next_1")
    }

    "confirm there is no back link" in {
      back_link.isEmpty mustBe true
    }

    "display the confirm button with link" in {
      val confirmButton = document.getElementById("confirmation-page-finish")

      confirmButton.text() mustBe Messages("awrs.confirmation.finish")
      confirmButton.attr("href") mustBe controllers.routes.ApplicationController.logout.toString
    }
  }
}