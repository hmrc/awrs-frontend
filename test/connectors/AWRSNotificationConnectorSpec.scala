/*
 * Copyright 2020 HM Revenue & Customs
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

package connectors

import models._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status.{BAD_REQUEST => _, INTERNAL_SERVER_ERROR => _, NOT_FOUND => _, OK => _, SERVICE_UNAVAILABLE => _}
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import utils.TestConstants._
import utils.{AwrsUnitTestTraits, TestUtil}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class AWRSNotificationConnectorSpec extends AwrsUnitTestTraits {
  val MockAuditConnector: AuditConnector = mock[AuditConnector]
  val dummyAppName = "awrs-frontend"
  val mockWSHttp: DefaultHttpClient = mock[DefaultHttpClient]

  override def beforeEach: Unit = {
    reset(mockWSHttp)
    reset(MockAuditConnector)

    when(mockAppConfig.servicesConfig)
      .thenReturn(mockServicesConfig)
    when(mockServicesConfig.baseUrl(ArgumentMatchers.eq("awrs")))
      .thenReturn("testURL")
    when(mockAccountUtils.authLink(ArgumentMatchers.any()))
      .thenReturn("org/UNUSED")
    when(mockAccountUtils.getAwrsRefNo(ArgumentMatchers.any()))
      .thenReturn("0123456")
  }

  val testAWRSNotificationConnector = new AWRSNotificationConnector(mockWSHttp, mockAppConfig, mockAuditable, mockAccountUtils)


  // these values doesn't really matter since the call itself is mocked
  implicit val request = FakeRequest()
  def awrsRef: String = {
    when(mockAccountUtils.getAwrsRefNo(ArgumentMatchers.any()))
      .thenReturn("0123456")

    "0123456"
  }

  lazy val notificationCacheURI = (awrsRef: String) => s"/$awrsRef"

  lazy val notificationViewedStatusURI = (awrsRef: String) => s"${testAWRSNotificationConnector.markAsViewedURI}/$awrsRef"

  lazy val confirmationEmailURI = testAWRSNotificationConnector.confirmationEmailURI

  lazy val cancellationEmailURI = testAWRSNotificationConnector.cancellationEmailURI

  lazy val withdranEmailURI = testAWRSNotificationConnector.withdrawnEmailURI


  "fetchNotificationCache" must {

    lazy val mindedToReject: Option[StatusNotification] = TestUtil.testStatusNotificationMindedToReject
    lazy val mindedToRejectJson = StatusNotification.writer.writes(mindedToReject.get)

    def mockFetchResponse(responseStatus: Int, responseData: JsValue): Unit =
      when(mockWSHttp.GET[HttpResponse](ArgumentMatchers.endsWith(notificationCacheURI(awrsRef)))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse.apply(responseStatus, responseData.toString())))

    def mockDeleteResponse(responseStatus: Int): Unit =
      when(mockWSHttp.DELETE[HttpResponse](ArgumentMatchers.endsWith(notificationCacheURI(awrsRef)), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse.apply(responseStatus, "")))

    def testFetchCall(implicit  hc: HeaderCarrier) = testAWRSNotificationConnector.fetchNotificationCache(TestUtil.defaultAuthRetrieval)

    def testDeleteCall(implicit  hc: HeaderCarrier) = testAWRSNotificationConnector.deleteFromNotificationCache(TestUtil.defaultAuthRetrieval)

    val revokeJSon = """{
                       "registrationNumber": "XXAW000001234560",
                       "contactNumber": "123456789012",
                       "contactType": "REVR",
                       "status":"08",
                       "storageDatetime":"2017-04-01T0013:07:11"
                       }"""
    val revokeOldJSon = """{
                       "registrationNumber": "XXAW000001234560",
                       "contactNumber": "123456789012",
                       "contactType": "REVR",
                       "status":"08"
                       }"""

    "parse old StatusNotification model correctly with new field(storageDatetime) with the new model" in {
      val statusNotification = Json.parse(revokeOldJSon).as[StatusNotification]
      statusNotification.storageDatetime mustBe None
    }

    "parse StatusNotification model correctly" in {
      val statusNotification = Json.parse(revokeJSon).as[StatusNotification]
      statusNotification.storageDatetime mustBe Some("2017-04-01T0013:07:11")
    }


    "return status as OK | NO_CONTENT, for successful fetch" in {
      mockFetchResponse(OK | NO_CONTENT, mindedToRejectJson)
      val result = testFetchCall
      await(result) mustBe mindedToReject
    }

    "return status as BAD_REQUEST, for unsuccessful fetch" in {
      mockFetchResponse(BAD_REQUEST, mindedToRejectJson)
      val result = testFetchCall
      the[BadRequestException] thrownBy await(result)
    }

    "return status as None, for not NOT_FOUND from fetch" in {
      mockFetchResponse(NOT_FOUND, mindedToRejectJson)
      val result = testFetchCall
      await(result) mustBe None
    }

    "return status as SERVICE_UNAVAILABLE, for unsuccessful fetch" in {
      mockFetchResponse(SERVICE_UNAVAILABLE, mindedToRejectJson)
      val result = testFetchCall
      the[ServiceUnavailableException] thrownBy await(result)
    }

    "return status as INTERNAL_SERVER_ERROR, for unsuccessful fetch" in {
      mockFetchResponse(INTERNAL_SERVER_ERROR, mindedToRejectJson)
      val result = testFetchCall
      the[InternalServerException] thrownBy await(result)
    }

    "return status as unexpected status, for unsuccessful fetch" in {
      val otherStatus = 999
      mockFetchResponse(otherStatus, mindedToRejectJson)
      val result = testFetchCall
      the[InternalServerException] thrownBy await(result)
    }

    "return true for successful delete (status OK | NO_CONTENT)" in {
      mockDeleteResponse(OK | NO_CONTENT)
      val result = testDeleteCall
      await(result) mustBe true
    }

    "return false for unsuccessful delete (status 500)" in {
      mockDeleteResponse(INTERNAL_SERVER_ERROR)
      val result = testDeleteCall
      await(result) mustBe false
    }

    "return false for unsuccessful delete (status 404)" in {
      // N.B. although we perform this test here to test that we handle this situation.
      // However in the current implementation of the awrs notification service it is impossible for this to be the case,
      // because mongo db will return ok on delete even if the entry id not found
      mockDeleteResponse(NOT_FOUND)
      val result = testDeleteCall
      await(result) mustBe false
    }

  }

  // The status tests are not conducted for the following tests because they share the same handler as the one
  // used by fetchNotificationCache
  // The main benefit of the following tests are to ensure the URL is correct
  "getNotificationViewedStatus" must {

    def mockGetViewedStatusResponse(responseStatus: Int, responseData: Option[JsValue]): Unit =
      when(mockWSHttp.GET[HttpResponse](ArgumentMatchers.endsWith(notificationViewedStatusURI(awrsRef)))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse.apply(responseStatus, responseData.getOrElse(Json.obj()), Map.empty[String, Seq[String]])))

    def testGetViewedStatusCall(implicit  hc: HeaderCarrier) = testAWRSNotificationConnector.getNotificationViewedStatus(TestUtil.defaultAuthRetrieval)

    "return status as OK or NO_CONTENT, for successful getNotificationViewedStatus" in {
      val viewed = true
      mockGetViewedStatusResponse(OK | NO_CONTENT, Json.parse(s"""{"viewed": $viewed}"""))
      val result = testGetViewedStatusCall
      await(result).get.viewed mustBe viewed
    }

    "return the value None, for not found status coming from getNotificationViewedStatus" in {
      mockGetViewedStatusResponse(NOT_FOUND, None)
      val result = testGetViewedStatusCall
      await(result) mustBe None
    }

  }

  "markNotificationViewedStatusAsViewed" must {

    def mockMarkViewedStatusResponse(haveResponse: Boolean): Unit =
      when(mockWSHttp.PUT[Unit, HttpResponse](ArgumentMatchers.endsWith(notificationViewedStatusURI(awrsRef)), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(haveResponse match {
          case true => Future.successful(HttpResponse.apply(OK | NO_CONTENT, ""))
          case false => Future.successful(HttpResponse.apply(NOT_FOUND, ""))
        })

    def testMarkViewedStatusCall(implicit  hc: HeaderCarrier) = testAWRSNotificationConnector.markNotificationViewedStatusAsViewed(TestUtil.defaultAuthRetrieval)

    "return the value Some(true), for successful markNotificationViewedStatusAsViewed" in {
      // so long as a response is given then ok is returned
      val marked = true
      mockMarkViewedStatusResponse(haveResponse = true)
      val result = testMarkViewedStatusCall
      await(result).get mustBe marked
    }

    "return the value None, for not found status coming from markNotificationViewedStatusAsViewed" in {
      mockMarkViewedStatusResponse(haveResponse = false)
      val result = testMarkViewedStatusCall
      await(result) mustBe None
    }
  }

  "sendConfirmationEmail" must {
    val testEmailRequest = EmailRequest(ApiTypes.API4, "test business", testUtr, "example@example.com", isNewBusiness = true)

    def sendConfirmationEmailResponse(haveResponse: Boolean): Unit =
      when(mockWSHttp.POST[Unit, HttpResponse](ArgumentMatchers.endsWith(confirmationEmailURI), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(haveResponse match {
          case true => Future.successful(HttpResponse.apply(OK | NO_CONTENT, ""))
          case false => Future.successful(HttpResponse.apply(NOT_FOUND, ""))
        })

    "return true for a successful request" in {
      sendConfirmationEmailResponse(true)
      val result = testAWRSNotificationConnector.sendConfirmationEmail(testEmailRequest)
      await(result) mustBe true
    }

    "return false for a successful request" in {
      sendConfirmationEmailResponse(false)
      val result = testAWRSNotificationConnector.sendConfirmationEmail(testEmailRequest)
      await(result) mustBe false
    }
  }

  "sendCancellationEmail" must {
    val testEmailRequest = EmailRequest(ApiTypes.API10, "test business", testUtr, "example@example.com", isNewBusiness = false)

    def sendCancellationEmailResponse(haveResponse: Boolean): Unit =
      when(mockWSHttp.POST[Unit, HttpResponse](ArgumentMatchers.endsWith(cancellationEmailURI), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(haveResponse match {
          case true => Future.successful(HttpResponse.apply(OK | NO_CONTENT, ""))
          case false => Future.successful(HttpResponse.apply(NOT_FOUND, ""))
        })

    "return true for a successful request" in {
      sendCancellationEmailResponse(true)
      val result = testAWRSNotificationConnector.sendCancellationEmail(testEmailRequest)
      await(result) mustBe true
    }

    "return false for a successful request" in {
      sendCancellationEmailResponse(false)
      val result = testAWRSNotificationConnector.sendCancellationEmail(testEmailRequest)
      await(result) mustBe false
    }
  }

  "sendWithdrawnEmail" must {
    val testEmailRequest = EmailRequest(ApiTypes.API8, "test business", testUtr, "example@example.com", isNewBusiness = false)

    def sendWithdrawnEmailResponse(haveResponse: Boolean): Unit =
      when(mockWSHttp.POST[Unit, HttpResponse](ArgumentMatchers.endsWith(withdranEmailURI), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(haveResponse match {
          case true => Future.successful(HttpResponse.apply(OK | NO_CONTENT, ""))
          case false => Future.successful(HttpResponse.apply(NOT_FOUND, ""))
        })

    "return true for a successful request" in {
      sendWithdrawnEmailResponse(true)
      val result = testAWRSNotificationConnector.sendWithdrawnEmail(testEmailRequest)
      await(result) mustBe true
    }

    "return false for a successful request" in {
      sendWithdrawnEmailResponse(false)
      val result = testAWRSNotificationConnector.sendWithdrawnEmail(testEmailRequest)
      await(result) mustBe false
    }
  }
}
