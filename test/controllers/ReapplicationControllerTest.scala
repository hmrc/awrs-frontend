/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import audit.TestAudit
import builders.SessionBuilder
import connectors.mock.{MockAWRSNotificationConnector, MockAuthConnector}
import connectors.{AWRSNotificationConnector, AuthenticatorConnector, TaxEnrolmentsConnector}
import forms.{AWRSEnums, ReapplicationForm}
import models.{ReapplicationConfirmation, StatusContactType, StatusNotification}
import org.joda.time.LocalDateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.mocks.{MockKeyStoreService, MockSave4LaterService}
import services.{DeEnrolService, KeyStoreService}
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HttpResponse
import utils.{AwrsUnitTestTraits, TestUtil}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ReapplicationControllerTest extends AwrsUnitTestTraits
  with MockAuthConnector with MockKeyStoreService with MockSave4LaterService{

  val mockAudit: Audit = mock[Audit]
  val mockAuthenticatorConnector: AuthenticatorConnector = mock[AuthenticatorConnector]
  val mockTaxEnrolmentsConnector: TaxEnrolmentsConnector = mock[TaxEnrolmentsConnector]
  val mockDeEnrolService: DeEnrolService = mock[DeEnrolService]
  val mockKeyStoreService: KeyStoreService = mock[KeyStoreService]
  val mockAWRSNotificationConnector: AWRSNotificationConnector = mock[AWRSNotificationConnector]

  object TestReapplicationController extends ReapplicationController {
    override val authConnector: AuthConnector = mockAuthConnector
    override val audit: Audit = new TestAudit
    override val keyStoreService: KeyStoreService = mockKeyStoreService
    override val deEnrolService = mockDeEnrolService
    override val save4LaterService = TestSave4LaterService
    override val awrsNotificationConnector = mockAWRSNotificationConnector
  }

  "Reapplication Controller" should {
    "submit confirmation and redirect to root home page when yes selected" in {
      submitAuthorisedUser(testRequest(ReapplicationConfirmation(AWRSEnums.BooleanRadioEnum.YesString))) {
        result =>
          status(result) shouldBe 303
          redirectLocation(result).get should include("/alcohol-wholesale-scheme")
      }
    }

    "submit confirmation and redirect to root home page when no selected" in {
      submitAuthorisedUser(testRequest(ReapplicationConfirmation(AWRSEnums.BooleanRadioEnum.NoString))) {
        result =>
          status(result) shouldBe 303
          redirectLocation(result).get should be("/alcohol-wholesale-scheme")
      }
    }

    "show recent withdrawal error page if the user has been Rejected within 24 hours" in {
      showWithException(testStatusNotification(StatusContactType.Rejected)) { result =>
        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("application-error-header").text() should be(Messages("awrs.generic.wait_info",Messages("awrs.generic.wait_info_de-registration")))
      }
    }

    "show recent re-registration error page if the user has been Revoked within 24 hours" in {
      showWithException(testStatusNotification(StatusContactType.Revoked)) { result =>
        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("application-error-header").text() should be(Messages("awrs.generic.wait_info",Messages("awrs.generic.wait_info_de-registration")))
      }
    }

    "redirect to re-application confirm page if more than 24 hours ago" in {
      showWithException(testStatusNotification(StatusContactType.Revoked, storageDatetime = LocalDateTime.now().minusHours(25))) { result =>
        status(result) shouldBe 200
        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("withdrawal-confirmation-title").text() should be(Messages("awrs.reapplication.confirm_page.heading"))
      }
    }

    "redirect to re-application confirm page if a date is not found" in {
      showWithException(testStatusNotification(StatusContactType.Revoked, storageDatetime = None)) { result =>
        status(result) shouldBe 200
        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("withdrawal-confirmation-title").text() should be(Messages("awrs.reapplication.confirm_page.heading"))
      }
    }

    "redirect to re-application confirm page if a notification is not found" in {
      showWithException(None) { result =>
        status(result) shouldBe 200
        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("withdrawal-confirmation-title").text() should be(Messages("awrs.reapplication.confirm_page.heading"))
      }
    }
  }

  def testRequest(reapplication: ReapplicationConfirmation) =
    TestUtil.populateFakeRequest[ReapplicationConfirmation](FakeRequest(), ReapplicationForm.reapplicationForm, reapplication)

  def testStatusNotification(contactType: StatusContactType = StatusContactType.Rejected,
                             storageDatetime: Option[LocalDateTime] = Some(LocalDateTime.now())) = {
    storageDatetime match {
      case Some(storageDatetime) => {
        val fmt: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss")
        val storageDatetimeString = fmt.print(storageDatetime)
        StatusNotification(Some("reg"), Some("contact"), Some(contactType), None, Some(storageDatetimeString))
      }
      case _ => StatusNotification(Some("reg"), Some("contact"), Some(contactType), None, None)
    }

  }

  private def showWithException(statusNotification: Option[StatusNotification] = None)(test: Future[Result] => Any) {
    when(mockAWRSNotificationConnector.fetchNotificationCache(Matchers.any(), Matchers.any())).thenReturn(Future(statusNotification))
    val result = TestReapplicationController.show().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setUser(hasAwrs = true)
    setupMockSave4LaterService()
    setupMockApiSave4LaterService()
    when(mockDeEnrolService.refreshProfile(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
    when(mockDeEnrolService.deEnrolAWRS(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(true))
    when(mockKeyStoreService.removeAll(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
    val result = TestReapplicationController.submit.apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, "LTD"))
    test(result)
  }
}
