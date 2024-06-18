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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, urlMatching}
import models._
import org.scalatest.matchers.must.Matchers
import play.api.http.Status.{BAD_REQUEST => _, INTERNAL_SERVER_ERROR => _, NOT_FOUND => _, OK => _, SERVICE_UNAVAILABLE => _}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.helpers.IntegrationSpec
import uk.gov.hmrc.http.{InternalServerException, _}
import utils.TestConstants._
import utils.TestUtil

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AWRSNotificationConnectorISpec extends IntegrationSpec with Injecting with Matchers {

  val connector: AWRSNotificationConnector = inject[AWRSNotificationConnector]

  // these values doesn't really matter since the call itself is mocked
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val awrsRef: String = "0123456"

  lazy val notificationViewedStatusURI: String => String = (awrsRef: String) => s"${connector.markAsViewedURI}/$awrsRef"

  lazy val confirmationEmailURI: String = s"${connector.confirmationEmailURI}"

  lazy val cancellationEmailURI: String = s"${connector.cancellationEmailURI}"

  lazy val withdranEmailURI: String = s"${connector.withdrawnEmailURI}"


  "fetchNotificationCache" must {

    lazy val mindedToReject: Option[StatusNotification] = TestUtil.testStatusNotificationMindedToReject
    lazy val mindedToRejectJson = StatusNotification.writer.writes(mindedToReject.get)

    def mockFetchResponse(responseStatus: Int, responseData: JsValue): Unit = {
      stubbedGet(s"/awrs-notification/cache/$awrsRef", responseStatus, responseData.toString())
    }

    def mockDeleteResponse(responseStatus: Int): Unit = {
      stubbedDelete(s"/awrs-notification/cache/$awrsRef", responseStatus)
    }

    def testFetchCall(implicit  hc: HeaderCarrier): Future[Option[StatusNotification]] = connector.fetchNotificationCache(TestUtil.defaultAuthRetrieval)

    def testDeleteCall(implicit  hc: HeaderCarrier): Future[Boolean] = connector.deleteFromNotificationCache(TestUtil.defaultAuthRetrieval)

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

    "return status as OK, for successful fetch" in {
      mockFetchResponse(OK, mindedToRejectJson)
      val result = testFetchCall
      await(result) mustBe mindedToReject
    }

    "return status as BAD_REQUEST, for unsuccessful fetch" in {
      mockFetchResponse(BAD_REQUEST, mindedToRejectJson)
      val result = testFetchCall
      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage mustBe "The request has not passed validation"
    }

    "return status as None, for not NOT_FOUND from fetch" in {
      mockFetchResponse(NOT_FOUND, mindedToRejectJson)
      val result = testFetchCall
      await(result) mustBe None
    }

    "return status as SERVICE_UNAVAILABLE, for unsuccessful fetch" in {
      mockFetchResponse(SERVICE_UNAVAILABLE, mindedToRejectJson)
      val result = testFetchCall
      val thrown = the[ServiceUnavailableException] thrownBy await(result)
      thrown.getMessage mustBe "Dependant systems are currently not responding"
    }

    "return status as INTERNAL_SERVER_ERROR, for unsuccessful fetch" in {
      mockFetchResponse(INTERNAL_SERVER_ERROR, mindedToRejectJson)
      val result = testFetchCall
      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage mustBe "WSO2 is currently experiencing problems that require live service intervention"
    }

    "return status as unexpected status, for unsuccessful fetch" in {
      val otherStatus = 999
      mockFetchResponse(otherStatus, mindedToRejectJson)
      val result = testFetchCall
      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage mustBe "Unsuccessful return of data. Status code: 999"
    }

    "return true for successful delete (status OK)" in {
      mockDeleteResponse(OK)
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

    def mockGetViewedStatusResponse(responseStatus: Int, responseData: String = ""): Unit = {
      stubbedGet(s"/awrs-notification/cache/viewed/$awrsRef", responseStatus, responseData)
    }

    def testGetViewedStatusCall(implicit  hc: HeaderCarrier): Future[Option[ViewedStatusResponse]] = connector.getNotificationViewedStatus(TestUtil.defaultAuthRetrieval)

    "return status as OK or NO_CONTENT, for successful getNotificationViewedStatus" in {
      val viewed = true
      mockGetViewedStatusResponse(OK, s"""{"viewed": $viewed}""")
      val result = testGetViewedStatusCall
      await(result).get.viewed mustBe viewed
    }

    "return the value None, for not found status coming from getNotificationViewedStatus" in {
      mockGetViewedStatusResponse(NOT_FOUND)
      val result = testGetViewedStatusCall
      await(result) mustBe None
    }

  }

  "markNotificationViewedStatusAsViewed" must {

    def testMarkViewedStatusCall(implicit  hc: HeaderCarrier): Future[Option[Boolean]] = connector.markNotificationViewedStatusAsViewed(TestUtil.defaultAuthRetrieval)

    "return the value Some(true), for successful markNotificationViewedStatusAsViewed" in {
      // so long as a response is given then ok is returned
      stubbedPut(s"/awrs-notification/cache${notificationViewedStatusURI(awrsRef)}", OK, "")
      val result = testMarkViewedStatusCall
      await(result).get mustBe true
    }

    "return the value None, for not found status coming from markNotificationViewedStatusAsViewed" in {
      stubbedPut(s"/awrs-notification/cache${notificationViewedStatusURI(awrsRef)}", NOT_FOUND, "")
      val result = testMarkViewedStatusCall
      await(result) mustBe None
    }

    "return the value None for 500 status coming from markNotificationViewedStatusAsViewed" in {
      stubbedPut(s"/awrs-notification/cache${notificationViewedStatusURI(awrsRef)}", INTERNAL_SERVER_ERROR, "")
      val result = testMarkViewedStatusCall
      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage mustBe "WSO2 is currently experiencing problems that require live service intervention"
    }
  }

  "sendConfirmationEmail" must {
    val testEmailRequest = EmailRequest(ApiTypes.API4, "test business", "example@example.com", Some(testUtr), isNewBusiness = Some(true))
    def testSendConfirmationCall(implicit hc: HeaderCarrier): Future[Boolean] = connector.sendConfirmationEmail(testEmailRequest)

    "return true for a successful request" in {
      stubbedPost(confirmationEmailURI, NO_CONTENT, "")
      val result = testSendConfirmationCall
      await(result) mustBe true
    }

    "return false when notification returns not found" in {
      stubbedPost(confirmationEmailURI, NOT_FOUND, "")
      val result = testSendConfirmationCall
      await(result) mustBe false
    }

    "throw exception when notification returns bad request" in {
      stubbedPost(confirmationEmailURI, BAD_REQUEST, "")
      val thrown = the[BadRequestException] thrownBy await(testSendConfirmationCall)
      thrown.getMessage mustBe "The request has not passed validation"
    }

    "throw exception when notification returns internal server error" in {
      stubbedPost(confirmationEmailURI, INTERNAL_SERVER_ERROR, "")
      val thrown = the[InternalServerException] thrownBy await(testSendConfirmationCall)
      thrown.getMessage mustBe "WSO2 is currently experiencing problems that require live service intervention"
    }

    "throw exception when notification returns service unavailable" in {
      stubbedPost(confirmationEmailURI, SERVICE_UNAVAILABLE, "")
      val thrown = the[ServiceUnavailableException] thrownBy await(testSendConfirmationCall)
      thrown.getMessage mustBe "Dependant systems are currently not responding"
    }
  }

  "sendCancellationEmail" must {
    val testEmailRequest = EmailRequest(ApiTypes.API10, "test business", "example@example.com", Some(testUtr), isNewBusiness = Some(false))
    def testSendCancellationCall: Future[Boolean] = connector.sendCancellationEmail(testEmailRequest)

    "return true for a successful request" in {
      stubbedPost(cancellationEmailURI, NO_CONTENT, "")
      val result = testSendCancellationCall
      await(result) mustBe true
    }

    "return false when notification returns not found" in {
      stubbedPost(cancellationEmailURI, NOT_FOUND, "")
      val result = testSendCancellationCall
      await(result) mustBe false
    }

    "throw exception when notification returns bad request" in {
      stubbedPost(cancellationEmailURI, BAD_REQUEST, "")
      val thrown = the[BadRequestException] thrownBy await(testSendCancellationCall)
      thrown.getMessage mustBe "The request has not passed validation"
    }

    "throw exception when notification returns internal server error" in {
      stubbedPost(cancellationEmailURI, INTERNAL_SERVER_ERROR, "")
      val thrown = the[InternalServerException] thrownBy await(testSendCancellationCall)
      thrown.getMessage mustBe "WSO2 is currently experiencing problems that require live service intervention"
    }

    "throw exception when notification returns service unavailable" in {
      stubbedPost(cancellationEmailURI, SERVICE_UNAVAILABLE, "")
      val thrown = the[ServiceUnavailableException] thrownBy await(testSendCancellationCall)
      thrown.getMessage mustBe "Dependant systems are currently not responding"
    }
  }

  "sendWithdrawnEmail" must {
    val testEmailRequest = EmailRequest(ApiTypes.API8, "test business", "example@example.com", Some(testUtr), isNewBusiness = Some(false))
    def testWithdrawCall: Future[Boolean] = connector.sendWithdrawnEmail(testEmailRequest)

    "return true for a successful request" in {
      stubbedPost(withdranEmailURI, NO_CONTENT, "")
      val result = testWithdrawCall
      await(result) mustBe true
    }

    "return false when notification returns not found" in {
      stubbedPost(withdranEmailURI, NOT_FOUND, "")
      val result = testWithdrawCall
      await(result) mustBe false
    }

    "throw exception when notification returns bad request" in {
      stubbedPost(withdranEmailURI, BAD_REQUEST, "")
      val thrown = the[BadRequestException] thrownBy await(testWithdrawCall)
      thrown.getMessage mustBe "The request has not passed validation"
    }

    "throw exception when notification returns internal server error" in {
      stubbedPost(withdranEmailURI, INTERNAL_SERVER_ERROR, "")
      val thrown = the[InternalServerException] thrownBy await(testWithdrawCall)
      thrown.getMessage mustBe "WSO2 is currently experiencing problems that require live service intervention"
    }

    "throw exception when notification returns service unavailable" in {
      stubbedPost(withdranEmailURI, SERVICE_UNAVAILABLE, "")
      val thrown = the[ServiceUnavailableException] thrownBy await(testWithdrawCall)
      thrown.getMessage mustBe "Dependant systems are currently not responding"
    }
  }
}
