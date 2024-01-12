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

package controllers

import builders.SessionBuilder
import connectors.AWRSNotificationConnector
import connectors.mock.MockAuthConnector
import forms.{AWRSEnums, ReapplicationForm}
import models.{ReapplicationConfirmation, StatusContactType, StatusNotification}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.mocks.{MockKeyStoreService, MockSave4LaterService}
import services.{DeEnrolService, KeyStoreService}
import uk.gov.hmrc.http.HttpResponse
import utils.{AwrsUnitTestTraits, TestUtil}
import views.html.{awrs_application_too_soon_error, awrs_reapplication_confirmation}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ReapplicationControllerTest extends AwrsUnitTestTraits
  with MockAuthConnector with MockKeyStoreService with MockSave4LaterService{

  override val mockDeEnrolService: DeEnrolService = mock[DeEnrolService]
  val mockKeyStoreService: KeyStoreService = mock[KeyStoreService]
  val mockAWRSNotificationConnector: AWRSNotificationConnector = mock[AWRSNotificationConnector]
  val template: awrs_application_too_soon_error = app.injector.instanceOf[views.html.awrs_application_too_soon_error]
  val templateConfirm: awrs_reapplication_confirmation = app.injector.instanceOf[views.html.awrs_reapplication_confirmation]

  lazy val testReapplicationController: ReapplicationController = new ReapplicationController(
    mockMCC, mockAWRSNotificationConnector, mockDeEnrolService, mockKeyStoreService, testSave4LaterService,
    mockAuthConnector, mockAuditable, mockAccountUtils, mockAppConfig, template, templateConfirm)

  "Reapplication Controller" must {
    "submit confirmation and redirect to root home page when yes selected" in {
      submitAuthorisedUser(testRequest(ReapplicationConfirmation(AWRSEnums.BooleanRadioEnum.YesString))) {
        result =>
          status(result) mustBe 303
          redirectLocation(result).get must include("/alcohol-wholesale-scheme")
      }
    }

    "submit confirmation and redirect to root home page when no selected" in {
      submitAuthorisedUser(testRequest(ReapplicationConfirmation(AWRSEnums.BooleanRadioEnum.NoString))) {
        result =>
          status(result) mustBe 303
          redirectLocation(result).get must be("/alcohol-wholesale-scheme")
      }
    }

    "show recent withdrawal error page if the user has been Rejected within 24 hours" in {
      showWithException(testStatusNotification(StatusContactType.Rejected)) { result =>
        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("application-error-header").text() must be(Messages("awrs.generic.wait_info",Messages("awrs.generic.wait_info_de-registration")))
      }
    }

    "show recent re-registration error page if the user has been Revoked within 24 hours" in {
      showWithException(testStatusNotification(StatusContactType.Revoked)) { result =>
        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("application-error-header").text() must be(Messages("awrs.generic.wait_info",Messages("awrs.generic.wait_info_de-registration")))
      }
    }

    "redirect to re-application confirm page if more than 24 hours ago" in {
      showWithException(testStatusNotification(StatusContactType.Revoked, storageDatetime = LocalDateTime.now().minusHours(25))) { result =>
        status(result) mustBe 200
        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("withdrawal-confirmation-title").text() must be(Messages("awrs.reapplication.confirm_page.heading"))
      }
    }

    "redirect to re-application confirm page if a date is not found" in {
      showWithException(testStatusNotification(StatusContactType.Revoked, storageDatetime = None)) { result =>
        status(result) mustBe 200
        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("withdrawal-confirmation-title").text() must be(Messages("awrs.reapplication.confirm_page.heading"))
      }
    }

    "redirect to re-application confirm page if a notification is not found" in {
      showWithException(None) { result =>
        status(result) mustBe 200
        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("withdrawal-confirmation-title").text() must be(Messages("awrs.reapplication.confirm_page.heading"))
      }
    }
  }

  def testRequest(reapplication: ReapplicationConfirmation): FakeRequest[AnyContentAsFormUrlEncoded] =
    TestUtil.populateFakeRequest[ReapplicationConfirmation](FakeRequest(), ReapplicationForm.reapplicationForm, reapplication)

  def testStatusNotification(contactType: StatusContactType = StatusContactType.Rejected,
                             storageDatetime: Option[LocalDateTime] = Some(LocalDateTime.now())): StatusNotification = {
    storageDatetime match {
      case Some(storageDatetime) => {
        val fmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        val storageDatetimeString = storageDatetime.format(fmt)
        StatusNotification(Some("reg"), Some("contact"), Some(contactType), None, Some(storageDatetimeString))
      }
      case _ => StatusNotification(Some("reg"), Some("contact"), Some(contactType), None, None)
    }

  }

  private def showWithException(statusNotification: Option[StatusNotification])(test: Future[Result] => Any): Unit = {
    when(mockAWRSNotificationConnector.fetchNotificationCache(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future(statusNotification))
    setAuthMocks()
    val result = testReapplicationController.show().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any): Unit = {
    resetAuthConnector()
    setupMockSave4LaterService()
    setupMockApiSave4LaterService()
    setAuthMocks()
    when(mockDeEnrolService.deEnrolAWRS(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(true))
    when(mockKeyStoreService.removeAll(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse.apply(OK, "")))
    val result = testReapplicationController.submit().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, "LTD").withMethod("POST"))
    test(result)
  }
}
