/*
 * Copyright 2019 HM Revenue & Customs
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

import akka.actor.ActorSystem
import audit.TestAudit
import builders.AuthBuilder
import com.typesafe.config.Config
import metrics.AwrsMetrics
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.{Configuration, Play}
import play.api.Mode.Mode
import play.api.http.Status.{BAD_REQUEST => _, INTERNAL_SERVER_ERROR => _, NOT_FOUND => _, OK => _, SERVICE_UNAVAILABLE => _}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.ws.{WSDelete, WSGet, WSPost, WSPut}
import uk.gov.hmrc.play.http._
import utils.TestConstants._
import utils.{AccountUtils, AwrsUnitTestTraits, TestUtil}

import scala.concurrent.Future
import uk.gov.hmrc.http._


class AWRSNotificationConnectorSpec extends AwrsUnitTestTraits {
  val MockAuditConnector = mock[AuditConnector]

  val dummyAppName = "awrs-frontend"

  class MockHttp extends HttpGet with WSGet with HttpPost with WSPost with HttpPut with WSPut with HttpAuditing with HttpDelete with WSDelete {
    override val hooks = Seq(AuditingHook)

    override def auditConnector: AuditConnector = MockAuditConnector

    override def appName = dummyAppName

    override protected def actorSystem: ActorSystem = Play.current.actorSystem

    override protected def configuration: Option[Config] = Option(Play.current.configuration.underlying)
  }

  val mockWSHttp = mock[MockHttp]

  override def beforeEach = {
    reset(mockWSHttp)
    reset(MockAuditConnector)
  }

  object TestAWRSNotificationConnector extends AWRSNotificationConnector {
    override val httpGet: MockHttp = mockWSHttp
    override val httpDelete: MockHttp = mockWSHttp
    override val httpPut: MockHttp = mockWSHttp
    override val httpPost: MockHttp = mockWSHttp
    override val appName = dummyAppName
   override val metrics = mock[AwrsMetrics]
    override val audit: Audit = new TestAudit

    override protected def mode: Mode = Play.current.mode

    override protected def runModeConfiguration: Configuration = Play.current.configuration
  }


  // these values doesn't really matter since the call itself is mocked
  implicit val request = FakeRequest()
  val awrsRef = AccountUtils.getAwrsRefNo(TestUtil.defaultEnrolmentSet)

  lazy val notificationCacheURI = (awrsRef: String) => s"/$awrsRef"

  lazy val notificationViewedStatusURI = (awrsRef: String) => s"${TestAWRSNotificationConnector.markAsViewedURI}/$awrsRef"

  lazy val confirmationEmailURI = TestAWRSNotificationConnector.confirmationEmailURI

  lazy val cancellationEmailURI = TestAWRSNotificationConnector.cancellationEmailURI

  lazy val withdranEmailURI = TestAWRSNotificationConnector.withdrawnEmailURI


  "fetchNotificationCache" should {

    lazy val mindedToReject: Option[StatusNotification] = TestUtil.testStatusNotificationMindedToReject
    lazy val mindedToRejectJson = StatusNotification.writer.writes(mindedToReject.get)

    def mockFetchResponse(responseStatus: Int, responseData: JsValue): Unit =
      when(mockWSHttp.GET[HttpResponse](Matchers.endsWith(notificationCacheURI(awrsRef)))(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(responseStatus, Some(responseData))))

    def mockDeleteResponse(responseStatus: Int): Unit =
      when(mockWSHttp.DELETE[HttpResponse](Matchers.endsWith(notificationCacheURI(awrsRef)))(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(responseStatus)))

    def testFetchCall(implicit  hc: HeaderCarrier, request: Request[AnyContent]) = TestAWRSNotificationConnector.fetchNotificationCache(TestUtil.defaultAuthRetrieval)

    def testDeleteCall(implicit  hc: HeaderCarrier, request: Request[AnyContent]) = TestAWRSNotificationConnector.deleteFromNotificationCache(TestUtil.defaultAuthRetrieval)

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
      statusNotification.storageDatetime shouldBe None
    }

    "parse StatusNotification model correctly" in {
      val statusNotification = Json.parse(revokeJSon).as[StatusNotification]
      statusNotification.storageDatetime shouldBe Some("2017-04-01T0013:07:11")
    }


    "return status as OK | NO_CONTENT, for successful fetch" in {
      mockFetchResponse(OK | NO_CONTENT, mindedToRejectJson)
      val result = testFetchCall
      await(result) shouldBe mindedToReject
    }

    "return status as BAD_REQUEST, for unsuccessful fetch" in {
      mockFetchResponse(BAD_REQUEST, mindedToRejectJson)
      val result = testFetchCall
      val thrown = the[BadRequestException] thrownBy await(result)
    }

    "return status as None, for not NOT_FOUND from fetch" in {
      mockFetchResponse(NOT_FOUND, mindedToRejectJson)
      val result = testFetchCall
      await(result) shouldBe None
    }

    "return status as SERVICE_UNAVAILABLE, for unsuccessful fetch" in {
      mockFetchResponse(SERVICE_UNAVAILABLE, mindedToRejectJson)
      val result = testFetchCall
      val thrown = the[ServiceUnavailableException] thrownBy await(result)
    }

    "return status as INTERNAL_SERVER_ERROR, for unsuccessful fetch" in {
      mockFetchResponse(INTERNAL_SERVER_ERROR, mindedToRejectJson)
      val result = testFetchCall
      val thrown = the[InternalServerException] thrownBy await(result)
    }

    "return status as unexpected status, for unsuccessful fetch" in {
      val otherStatus = 999
      mockFetchResponse(otherStatus, mindedToRejectJson)
      val result = testFetchCall
      val thrown = the[InternalServerException] thrownBy await(result)
    }

    "return true for successful delete (status OK | NO_CONTENT)" in {
      mockDeleteResponse(OK | NO_CONTENT)
      val result = testDeleteCall
      await(result) shouldBe true
    }

    "return false for unsuccessful delete (status 500)" in {
      mockDeleteResponse(INTERNAL_SERVER_ERROR)
      val result = testDeleteCall
      await(result) shouldBe false
    }

    "return false for unsuccessful delete (status 404)" in {
      // N.B. although we perform this test here to test that we handle this situation.
      // However in the current implementation of the awrs notification service it is impossible for this to be the case,
      // because mongo db will return ok on delete even if the entry id not found
      mockDeleteResponse(NOT_FOUND)
      val result = testDeleteCall
      await(result) shouldBe false
    }

  }

  // The status tests are not conducted for the following tests because they share the same handler as the one
  // used by fetchNotificationCache
  // The main benefit of the following tests are to ensure the URL is correct
  "getNotificationViewedStatus" should {

    def mockGetViewedStatusResponse(responseStatus: Int, responseData: Option[JsValue]): Unit =
      when(mockWSHttp.GET[HttpResponse](Matchers.endsWith(notificationViewedStatusURI(awrsRef)))(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(responseStatus, responseData)))

    def testGetViewedStatusCall(implicit  hc: HeaderCarrier, request: Request[AnyContent]) = TestAWRSNotificationConnector.getNotificationViewedStatus(TestUtil.defaultAuthRetrieval)

    "return status as OK or NO_CONTENT, for successful getNotificationViewedStatus" in {
      val viewed = true
      mockGetViewedStatusResponse(OK | NO_CONTENT, Json.parse(s"""{"viewed": $viewed}"""))
      val result = testGetViewedStatusCall
      await(result).get.viewed shouldBe viewed
    }

    "return the value None, for not found status coming from getNotificationViewedStatus" in {
      mockGetViewedStatusResponse(NOT_FOUND, None)
      val result = testGetViewedStatusCall
      await(result) shouldBe None
    }

  }

  "markNotificationViewedStatusAsViewed" should {

    def mockMarkViewedStatusResponse(haveResponse: Boolean): Unit =
      when(mockWSHttp.PUT[Unit, HttpResponse](Matchers.endsWith(notificationViewedStatusURI(awrsRef)), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(haveResponse match {
          case true => Future.successful(HttpResponse(OK | NO_CONTENT))
          case false => Future.successful(HttpResponse(NOT_FOUND))
        })

    def testMarkViewedStatusCall(implicit  hc: HeaderCarrier, request: Request[AnyContent]) = TestAWRSNotificationConnector.markNotificationViewedStatusAsViewed(TestUtil.defaultAuthRetrieval)

    "return the value Some(true), for successful markNotificationViewedStatusAsViewed" in {
      // so long as a response is given then ok is returned
      val marked = true
      mockMarkViewedStatusResponse(haveResponse = true)
      val result = testMarkViewedStatusCall
      await(result).get shouldBe marked
    }

    "return the value None, for not found status coming from markNotificationViewedStatusAsViewed" in {
      mockMarkViewedStatusResponse(haveResponse = false)
      val result = testMarkViewedStatusCall
      await(result) shouldBe None
    }
  }

  "sendConfirmationEmail" should {
    val testEmailRequest = EmailRequest(ApiTypes.API4, "test business", testUtr, "example@example.com", isNewBusiness = true)

    def sendConfirmationEmailResponse(haveResponse: Boolean): Unit =
      when(mockWSHttp.POST[Unit, HttpResponse](Matchers.endsWith(confirmationEmailURI), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(haveResponse match {
          case true => Future.successful(HttpResponse(OK | NO_CONTENT))
          case false => Future.successful(HttpResponse(NOT_FOUND))
        })

    "return true for a successful request" in {
      sendConfirmationEmailResponse(true)
      val result = TestAWRSNotificationConnector.sendConfirmationEmail(testEmailRequest)
      await(result) shouldBe true
    }

    "return false for a successful request" in {
      sendConfirmationEmailResponse(false)
      val result = TestAWRSNotificationConnector.sendConfirmationEmail(testEmailRequest)
      await(result) shouldBe false
    }
  }

  "sendCancellationEmail" should {
    val testEmailRequest = EmailRequest(ApiTypes.API10, "test business", testUtr, "example@example.com", isNewBusiness = false)

    def sendCancellationEmailResponse(haveResponse: Boolean): Unit =
      when(mockWSHttp.POST[Unit, HttpResponse](Matchers.endsWith(cancellationEmailURI), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(haveResponse match {
          case true => Future.successful(HttpResponse(OK | NO_CONTENT))
          case false => Future.successful(HttpResponse(NOT_FOUND))
        })

    "return true for a successful request" in {
      sendCancellationEmailResponse(true)
      val result = TestAWRSNotificationConnector.sendCancellationEmail(testEmailRequest)
      await(result) shouldBe true
    }

    "return false for a successful request" in {
      sendCancellationEmailResponse(false)
      val result = TestAWRSNotificationConnector.sendCancellationEmail(testEmailRequest)
      await(result) shouldBe false
    }
  }

  "sendWithdrawnEmail" should {
    val testEmailRequest = EmailRequest(ApiTypes.API8, "test business", testUtr, "example@example.com", isNewBusiness = false)

    def sendWithdrawnEmailResponse(haveResponse: Boolean): Unit =
      when(mockWSHttp.POST[Unit, HttpResponse](Matchers.endsWith(withdranEmailURI), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(haveResponse match {
          case true => Future.successful(HttpResponse(OK | NO_CONTENT))
          case false => Future.successful(HttpResponse(NOT_FOUND))
        })

    "return true for a successful request" in {
      sendWithdrawnEmailResponse(true)
      val result = TestAWRSNotificationConnector.sendWithdrawnEmail(testEmailRequest)
      await(result) shouldBe true
    }

    "return false for a successful request" in {
      sendWithdrawnEmailResponse(false)
      val result = TestAWRSNotificationConnector.sendWithdrawnEmail(testEmailRequest)
      await(result) shouldBe false
    }
  }
}
