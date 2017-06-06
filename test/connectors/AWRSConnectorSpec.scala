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

package connectors

import audit.TestAudit
import builders.AuthBuilder
import exceptions.{DESValidationException, DuplicateSubscriptionException, GovernmentGatewayException, PendingDeregistrationException}
import models.FormBundleStatus.Pending
import models.StatusContactType.MindedToReject
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.libs.json._
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.ws.{WSGet, WSPost, WSPut}
import uk.gov.hmrc.play.http.{HeaderCarrier, _}
import utils.AwrsTestJson._
import utils.TestConstants._
import utils.{AccountUtils, AwrsUnitTestTraits}

import scala.concurrent.Future

class AWRSConnectorSpec extends AwrsUnitTestTraits {

  val MockAuditConnector = mock[AuditConnector]

  class MockHttp extends WSGet with WSPost with WSPut with HttpAuditing {
    override val hooks = Seq(AuditingHook)

    override def auditConnector: AuditConnector = MockAuditConnector

    override def appName = "awrs-frontend"
  }

  val mockWSHttp = mock[MockHttp]

  override def beforeEach = {
    reset(mockWSHttp)
  }

  object TestAWRSConnector extends AWRSConnector {
    override val http: HttpGet with HttpPost with HttpPut = mockWSHttp
    override val audit: Audit = new TestAudit
  }

  val subscribeSuccessResponse = SuccessfulSubscriptionResponse(processingDate = "2001-12-17T09:30:47Z", awrsRegistrationNumber = "ABCDEabcde12345", etmpFormBundleNumber = "123456789012345")
  val api6SuccessResponse = SuccessfulUpdateSubscriptionResponse(processingDate = "2001-12-17T09:30:47Z", etmpFormBundleNumber = "123456789012345")
  val api3SucecssResponse = SuccessfulUpdateGroupBusinessPartnerResponse(processingDate = "2001-12-17T09:30:47Z")
  lazy val updateGrpPartnerSuccessReponseJson = Json.toJson(api3SucecssResponse)
  lazy val subscribeApi6SuccessResponseJson = Json.toJson(api6SuccessResponse)
  lazy val badRequestResponse = Json.parse( """{"reason": "Some other bad request reason"}""")
  lazy val notFoundResponse = Json.parse( """{"reason": "Resource not found"}""")
  lazy val serviceUnavailableResponse = Json.parse( """{"reason": "Dependant systems are currently not responding"}""")
  lazy val internalServerErrorResponse = Json.parse( """{"reason": "WSO2 is currently experiencing problems that require live service intervention"}""")
  lazy val forbiddenResponse = Json.parse( """{"reason": "ETMP has returned a error code003 with a status of NOT_OK - record is not editable"}""")
  lazy val subscribeFailureResponseJson = Json.parse( """{"status" : "Error occurred"}""")
  lazy val subscribeFailureDesValResponse = Json.parse( """{"reason": "Your submission contains one or more errors"}""")
  lazy val subscribeFailureGovGateResponse = Json.parse( """{"hostname":"government-gateway-admin", "reason": "Some other reason"}""")
  lazy val subscribeFailureDupSubsResponse = Json.parse( """{"reason": "Business Partner already has an active AWRS subscription"}""")
  lazy val subscribeFailurePendingDeregResponse = Json.parse( """{"reason": "You cannot submit new application whilst previous one is Under Appeal/Review or being Deregistered"}""")
  lazy val subscribeSuccessResponseJson = Json.toJson(subscribeSuccessResponse)

  "AWRSConnector subscribing to AWRS" should {

    "return status as OK, for successful subscription" in {
      implicit val user = AuthBuilder.createUserAuthContextIndSa("userId", "joe bloggs", testUtr)
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(subscribeSuccessResponseJson))))
      val result = TestAWRSConnector.submitAWRSData(api4SOPJson)
      await(result) shouldBe subscribeSuccessResponse
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "return status as OK, for Partnership legal entity" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(subscribeSuccessResponseJson))))
      val result = TestAWRSConnector.submitAWRSData(api4PartnerJson)
      await(result) shouldBe subscribeSuccessResponse
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }
    "return status as OK, for LTD legal entity" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(subscribeSuccessResponseJson))))
      val result = TestAWRSConnector.submitAWRSData(api5LTDJson)
      await(result) shouldBe subscribeSuccessResponse
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "return status as BAD_REQUEST and catch all, for bad data sent for subscription" in {
      implicit val user = AuthBuilder.createUserAuthContextIndSa("userId", "joe bloggs", testUtr)
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(subscribeFailureResponseJson))))
      val result = TestAWRSConnector.submitAWRSData(api4SOPJson)

      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage should include("Bad Request")
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "return status as BAD_REQUEST and throw DESValidationException, for unsuccessful subscription" in {
      implicit val user = AuthBuilder.createUserAuthContextIndSa("userId", "joe bloggs", testUtr)
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(subscribeFailureDesValResponse))))
      val result = TestAWRSConnector.submitAWRSData(api4SOPJson)

      val thrown = the[DESValidationException] thrownBy await(result)
      thrown.getMessage shouldBe "Validation against schema failed"
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "return status as BAD_REQUEST and throw GovernmentGatewayException, for unsuccessful subscription" in {
      implicit val user = AuthBuilder.createUserAuthContextIndSa("userId", "joe bloggs", testUtr)
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(subscribeFailureGovGateResponse))))
      val result = TestAWRSConnector.submitAWRSData(api4SOPJson)

      val thrown = the[GovernmentGatewayException] thrownBy await(result)
      thrown.getMessage shouldBe "There was a problem with the admin service"
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "return status as BAD_REQUEST and throw DuplicateSubscriptionException, for unsuccessful subscription" in {
      implicit val user = AuthBuilder.createUserAuthContextIndSa("userId", "joe bloggs", testUtr)
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(subscribeFailureDupSubsResponse))))
      val result = TestAWRSConnector.submitAWRSData(api4SOPJson)

      val thrown = the[DuplicateSubscriptionException] thrownBy await(result)
      thrown.getMessage shouldBe "This subscription already exists"
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "return status as BAD_REQUEST and throw PendingDeregistrationException, for unsuccessful subscription" in {
      implicit val user = AuthBuilder.createUserAuthContextIndSa("userId", "joe bloggs", testUtr)
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(subscribeFailurePendingDeregResponse))))
      val result = TestAWRSConnector.submitAWRSData(api4SOPJson)

      val thrown = the[PendingDeregistrationException] thrownBy await(result)
      thrown.getMessage shouldBe "You cannot submit a new application while your cancelled application is still pending"
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "return status as NOT_FOUND, for bad data sent for subscription" in {
      implicit val user = AuthBuilder.createUserAuthContextIndSa("userId", "joe bloggs", testUtr)
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(subscribeFailureResponseJson))))
      val result = TestAWRSConnector.submitAWRSData(api4SOPJson)
      val thrown = the[NotFoundException] thrownBy await(result)
      thrown.getMessage shouldBe "URL not found"
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }
    "return status as SERVICE_UNAVAILABLE, for bad data sent for subscription" in {
      implicit val user = AuthBuilder.createUserAuthContextIndSa("userId", "joe bloggs", testUtr)
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(subscribeFailureResponseJson))))
      val result = TestAWRSConnector.submitAWRSData(api4SOPJson)
      val thrown = the[ServiceUnavailableException] thrownBy await(result)
      thrown.getMessage shouldBe "Service unavailable"
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }
    "return status INTERNAL SERVER ERROR" in {
      implicit val user = AuthBuilder.createUserAuthContextIndSa("userId", "joe bloggs", testUtr)
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(subscribeFailureResponseJson))))
      val result = TestAWRSConnector.submitAWRSData(api4SOPJson)
      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage shouldBe "Internal server error"
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "return status as INTERNAL SERVER ERROR and throw GovernmentGatewayException, for unsuccessful subscription" in {
      implicit val user = AuthBuilder.createUserAuthContextIndSa("userId", "joe bloggs", testUtr)
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(subscribeFailureGovGateResponse))))
      val result = TestAWRSConnector.submitAWRSData(api4SOPJson)

      val thrown = the[GovernmentGatewayException] thrownBy await(result)
      thrown.getMessage shouldBe "There was a problem with the admin service"
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "return status anything else" in {
      implicit val user = AuthBuilder.createUserAuthContextIndSa("userId", "joe bloggs", testUtr)
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(777, Some(subscribeFailureResponseJson))))
      val result = TestAWRSConnector.submitAWRSData(api4SOPJson)
      val thrown = the[RuntimeException] thrownBy await(result)
      thrown.getMessage shouldBe "Unknown response"
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "return status anything else and throw GovernmentGatewayException, for unsuccessful subscription" in {
      implicit val user = AuthBuilder.createUserAuthContextIndSa("userId", "joe bloggs", testUtr)
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(777, Some(subscribeFailureGovGateResponse))))
      val result = TestAWRSConnector.submitAWRSData(api4SOPJson)

      val thrown = the[GovernmentGatewayException] thrownBy await(result)
      thrown.getMessage shouldBe "There was a problem with the admin service"
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }
  }

  "A user with an AWRS enrolment " should {
    implicit val user = AuthBuilder.createUserAuthContextIndSaWithAWRS(s"/sa/individual/$testUtr", "joe bloggs", testUtr)
    "try to lookup data from ETMP via API5 endpoint" in {
      val awrsRefNo = AccountUtils.getAwrsRefNo.toString()
      when(mockWSHttp.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(api5LTDJson))))

      val result = TestAWRSConnector.lookupAWRSData(awrsRefNo)

      await(result) shouldBe api5LTDJson
    }

    "return status as BAD_REQUEST, for bad request made for API 5" in {
      val awrsRefNo = AccountUtils.getAwrsRefNo.toString()
      when(mockWSHttp.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(api5LTDJson))))

      val result = TestAWRSConnector.lookupAWRSData(awrsRefNo)

      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage shouldBe "The Submission has not passed validation"
    }

    "return status as NOT_FOUND for API 5" in {
      val awrsRefNo = AccountUtils.getAwrsRefNo.toString()
      when(mockWSHttp.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(api5LTDJson))))

      val result = TestAWRSConnector.lookupAWRSData(awrsRefNo)

      val thrown = the[NotFoundException] thrownBy await(result)
      thrown.getMessage shouldBe "The remote endpoint has indicated that no data can be found"
    }

    "return status as SERVICE_UNAVAILABLE for API 5" in {
      val awrsRefNo = AccountUtils.getAwrsRefNo.toString()
      when(mockWSHttp.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(api5LTDJson))))

      val result = TestAWRSConnector.lookupAWRSData(awrsRefNo)

      val thrown = the[ServiceUnavailableException] thrownBy await(result)
      thrown.getMessage shouldBe "Dependant systems are currently not responding"
    }

    "return status INTERNAL_SERVER_ERROR for API 5" in {
      val awrsRefNo = AccountUtils.getAwrsRefNo.toString()
      when(mockWSHttp.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(api5LTDJson))))

      val result = TestAWRSConnector.lookupAWRSData(awrsRefNo)

      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage shouldBe "WSO2 is currently experiencing problems that require live service intervention"
    }
  }

  "AWRSConnector updating an AWRS application" should {

    implicit val user = AuthBuilder.createUserAuthContextOrgWithAWRS(s"/sa/individual/$testUtr", "joe bloggs", testUtr)
    "return status as OK, for successful subscription" in {
      when(mockWSHttp.PUT[JsValue, HttpResponse](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(subscribeApi6SuccessResponseJson))))
      val result = TestAWRSConnector.updateAWRSData(api6LTDJson)
      await(result) shouldBe api6SuccessResponse
      verify(mockWSHttp, times(1)).PUT[JsValue, HttpResponse](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "return status as BAD_REQUEST, for unsuccessful subscription" in {
      when(mockWSHttp.PUT[JsValue, HttpResponse](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(badRequestResponse))))
      val result = TestAWRSConnector.updateAWRSData(api6LTDJson)

      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage shouldBe "[API6] - The Submission has not passed validation"
    }

    "return status as NOT_FOUND, for unsuccessful subscription" in {
      when(mockWSHttp.PUT[JsValue, HttpResponse](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(notFoundResponse))))
      val result = TestAWRSConnector.updateAWRSData(api6LTDJson)

      val thrown = the[NotFoundException] thrownBy await(result)
      thrown.getMessage shouldBe "[API6] - The remote endpoint has indicated that no data can be found"
    }

    "return status as SERVICE_UNAVAILABLE, for unsuccessful subscription" in {
      when(mockWSHttp.PUT[JsValue, HttpResponse](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(serviceUnavailableResponse))))
      val result = TestAWRSConnector.updateAWRSData(api6LTDJson)

      val thrown = the[ServiceUnavailableException] thrownBy await(result)
      thrown.getMessage shouldBe "[API6] - Dependant systems are currently not responding"
    }

    "return status as INTERNAL_SERVER_ERROR, for unsuccessful subscription" in {
      when(mockWSHttp.PUT[JsValue, HttpResponse](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(internalServerErrorResponse))))
      val result = TestAWRSConnector.updateAWRSData(api6LTDJson)

      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage should include("[API6] - WSO2 is currently experiencing problems that require live service intervention")
    }
  }

  "AWRS withdrawal connector" should {
    import utils.WithdrawalTestUtils._
    implicit val user = AuthBuilder.createUserAuthContextIndSaWithAWRS(s"/sa/individual/$testUtr", "joe bloggs", testUtr)
    implicit val request = FakeRequest()
    val awrsRefNo = AccountUtils.getAwrsRefNo.toString()

    "return OK on successful return of data" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(withdrawResponseJson))))
      val result = TestAWRSConnector.withdrawApplication(awrsRefNo, withdrawalJsonToSend)
      await(result) shouldBe witdrawalResponse()
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "return status as BAD_REQUEST, for unsuccessful subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(badRequestResponse))))
      val result = TestAWRSConnector.withdrawApplication(awrsRefNo, withdrawalJsonToSend)
      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage shouldBe "The Submission has not passed validation"
    }

    "return status as NOT_FOUND, for unsuccessful subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(notFoundResponse))))
      val result = TestAWRSConnector.withdrawApplication(awrsRefNo, withdrawalJsonToSend)

      val thrown = the[NotFoundException] thrownBy await(result)
      thrown.getMessage shouldBe "The remote endpoint has indicated that no data can be found"
    }

    "return status as SERVICE_UNAVAILABLE, for unsuccessful subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(serviceUnavailableResponse))))
      val result = TestAWRSConnector.withdrawApplication(awrsRefNo, withdrawalJsonToSend)

      val thrown = the[ServiceUnavailableException] thrownBy await(result)
      thrown.getMessage shouldBe "Dependant systems are currently not responding"
    }

    "return status as INTERNAL_SERVER_ERROR, for unsuccessful subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(internalServerErrorResponse))))
      val result = TestAWRSConnector.withdrawApplication(awrsRefNo, withdrawalJsonToSend)

      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage should include("WSO2 is currently experiencing problems that require live service intervention")
    }
  }

  "getStatusInfo" should {
    lazy val getStatusInfoURI = (awrsRef: String, contactNumber: String) => f"/awrs/status-info/$awrsRef/$contactNumber"

    // these values doesn't really matter since the call itself is mocked
    implicit val request = FakeRequest()
    val contactNumber = "0123456789"
    val awrsRef = AccountUtils.getAwrsRefNo.toString()
    val statusInfoResponseSuccess = StatusInfoType(Some(StatusInfoSuccessResponseType("", "")))
    val statusInfoResponseSuccessJson = StatusInfoType.writter.writes(statusInfoResponseSuccess)

    val statusInfoResponseFailure = StatusInfoType(Some(StatusInfoFailureResponseType("")))
    val statusInfoResponseFailureJson = StatusInfoType.writter.writes(statusInfoResponseFailure)

    val statusInfoCorruptResponseJson = Json.parse("false")

    def mockResponse(responseStatus: Int, responseData: JsValue): Unit =
      when(mockWSHttp.GET[HttpResponse](Matchers.endsWith(getStatusInfoURI(awrsRef, contactNumber)))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(responseStatus, Some(responseData))))

    def testCall(implicit user: AuthContext, hc: HeaderCarrier, request: Request[AnyContent]) = TestAWRSConnector.getStatusInfo(awrsRef, contactNumber, Pending, Some(MindedToReject))

    "return status as OK, for successful de-enrolment" in {
      mockResponse(OK, statusInfoResponseSuccessJson)
      val result = testCall
      await(result) shouldBe statusInfoResponseSuccess

      mockResponse(OK, statusInfoResponseFailureJson)
      val resultFailure = testCall
      val thrownFailure = the[BadRequestException] thrownBy await(resultFailure)

      mockResponse(OK, statusInfoCorruptResponseJson)
      val resultCorrupt = testCall
      val thrown = the[BadRequestException] thrownBy await(resultCorrupt)
    }

    "return status as BAD_REQUEST, for unsuccessful de-enrolment" in {
      mockResponse(BAD_REQUEST, statusInfoResponseSuccessJson)
      val result = testCall
      val thrown = the[BadRequestException] thrownBy await(result)
    }

    "return status as NOT_FOUND, for unsuccessful de-enrolment" in {
      mockResponse(NOT_FOUND, statusInfoResponseSuccessJson)
      val result = testCall
      val thrown = the[NotFoundException] thrownBy await(result)
    }

    "return status as SERVICE_UNAVAILABLE, for unsuccessful de-enrolment" in {
      mockResponse(SERVICE_UNAVAILABLE, statusInfoResponseSuccessJson)
      val result = testCall
      val thrown = the[ServiceUnavailableException] thrownBy await(result)
    }

    "return status as INTERNAL_SERVER_ERROR, for unsuccessful de-enrolment" in {
      mockResponse(INTERNAL_SERVER_ERROR, statusInfoResponseSuccessJson)
      val result = testCall
      val thrown = the[InternalServerException] thrownBy await(result)
    }

    "return status as unexpected status, for unsuccessful de-enrolment" in {
      val otherStatus = 999
      mockResponse(otherStatus, statusInfoResponseSuccessJson)
      val result = testCall
      val thrown = the[InternalServerException] thrownBy await(result)
    }
  }

  "deRegistration" should {
    lazy val deRegisterURI = (awrsRef: String) => f"/awrs/de-registration/$awrsRef"

    // these values doesn't really matter since the call itself is mocked
    implicit val request = FakeRequest()
    val awrsRef = AccountUtils.getAwrsRefNo.toString()
    val deRegistrationResponseSuccess = DeRegistrationType(Some(DeRegistrationSuccessResponseType("")))
    val deRegistrationResponseSuccessJson = DeRegistrationType.writter.writes(deRegistrationResponseSuccess)

    val deRegistrationResponseFailure = DeRegistrationType(Some(DeRegistrationFailureResponseType("")))
    val deRegistrationResponseFailureJson = DeRegistrationType.writter.writes(deRegistrationResponseFailure)

    val deRegistrationCorruptResponseJson = Json.parse("false")

    val deRegistrationRequest = DeRegistration("", "", None)

    def mockResponse(responseStatus: Int, responseData: JsValue): Unit =
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.endsWith(deRegisterURI(awrsRef)), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(responseStatus, Some(responseData))))

    def testCall(implicit user: AuthContext, hc: HeaderCarrier, request: Request[AnyContent]) = TestAWRSConnector.deRegistration(awrsRef, deRegistrationRequest)

    "return status as OK, for successful de-enrolment" in {
      mockResponse(OK, deRegistrationResponseSuccessJson)
      val result = testCall
      await(result) shouldBe deRegistrationResponseSuccess

      mockResponse(OK, deRegistrationResponseFailureJson)
      val resultFailure = testCall
      await(resultFailure) shouldBe deRegistrationResponseFailure

      mockResponse(OK, deRegistrationCorruptResponseJson)
      val resultCorrupt = testCall
      val thrown = the[BadRequestException] thrownBy await(resultCorrupt)
    }

    "return status as BAD_REQUEST, for unsuccessful de-enrolment" in {
      mockResponse(BAD_REQUEST, deRegistrationResponseSuccessJson)
      val result = testCall
      val thrown = the[BadRequestException] thrownBy await(result)
    }

    "return status as NOT_FOUND, for unsuccessful de-enrolment" in {
      mockResponse(NOT_FOUND, deRegistrationResponseSuccessJson)
      val result = testCall
      val thrown = the[NotFoundException] thrownBy await(result)
    }

    "return status as SERVICE_UNAVAILABLE, for unsuccessful de-enrolment" in {
      mockResponse(SERVICE_UNAVAILABLE, deRegistrationResponseSuccessJson)
      val result = testCall
      val thrown = the[ServiceUnavailableException] thrownBy await(result)
    }

    "return status as INTERNAL_SERVER_ERROR, for unsuccessful de-enrolment" in {
      mockResponse(INTERNAL_SERVER_ERROR, deRegistrationResponseSuccessJson)
      val result = testCall
      val thrown = the[InternalServerException] thrownBy await(result)
    }

    "return status as unexpected status, for unsuccessful de-enrolment" in {
      val otherStatus = 999
      mockResponse(otherStatus, deRegistrationResponseSuccessJson)
      val result = testCall
      val thrown = the[InternalServerException] thrownBy await(result)
    }

  }

  "AWRSConnector update group business partner" should {

    val address = Address(addressLine1 = "", addressLine2 = "")
    val updatedDataRequest = new UpdateRegistrationDetailsRequest(false, Some(OrganisationName("testName")), address, ContactDetails(), false, false)

    implicit val user = AuthBuilder.createUserAuthContextOrgWithAWRS(s"/sa/individual/$testUtr", "joe bloggs", testUtr)
    "return status as OK, for successful update" in {
      when(mockWSHttp.PUT[JsValue, HttpResponse](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(updateGrpPartnerSuccessReponseJson))))
      val result = TestAWRSConnector.updateGroupBusinessPartner("","","",updatedDataRequest)
      await(result) shouldBe api3SucecssResponse
      verify(mockWSHttp, times(1)).PUT[JsValue, HttpResponse](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "return status as BAD_REQUEST, for unsuccessful update" in {
      when(mockWSHttp.PUT[JsValue, HttpResponse](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(badRequestResponse))))
      val result = TestAWRSConnector.updateGroupBusinessPartner("","","",updatedDataRequest)

      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage shouldBe "[API3 - Update Group Business Partner] - The Submission has not passed validation"
    }

    "return status as NOT_FOUND, for unsuccessful update" in {
      when(mockWSHttp.PUT[JsValue, HttpResponse](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(notFoundResponse))))
      val result = TestAWRSConnector.updateGroupBusinessPartner("","","",updatedDataRequest)

      val thrown = the[NotFoundException] thrownBy await(result)
      thrown.getMessage shouldBe "[API3 - Update Group Business Partner] - The remote endpoint has indicated that no data can be found"
    }

    "return status as SERVICE_UNAVAILABLE, for unsuccessful update" in {
     when(mockWSHttp.PUT[JsValue, HttpResponse](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
       .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(serviceUnavailableResponse))))
     val result = TestAWRSConnector.updateGroupBusinessPartner("","","",updatedDataRequest)

     val thrown = the[ServiceUnavailableException] thrownBy await(result)
     thrown.getMessage shouldBe "[API3 - Update Group Business Partner] - Dependant systems are currently not responding"
   }

   "return status as INTERNAL_SERVER_ERROR, for unsuccessful update" in {
     when(mockWSHttp.PUT[JsValue, HttpResponse](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
       .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(internalServerErrorResponse))))
     val result = TestAWRSConnector.updateGroupBusinessPartner(testTradingName,"","",updatedDataRequest)

     val thrown = the[InternalServerException] thrownBy await(result)
     thrown.getMessage should include("[API3 - Update Group Business Partner] - WSO2 is currently experiencing problems that require live service intervention")
   }

    "return status as FORBIDDEN, for unsuccessful update" in {
      when(mockWSHttp.PUT[JsValue, HttpResponse](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(FORBIDDEN, Some(forbiddenResponse))))
      val result = TestAWRSConnector.updateGroupBusinessPartner("","","",updatedDataRequest)

      val thrown = the[ForbiddenException] thrownBy await(result)
      thrown.getMessage should include("[API3 - Update Group Business Partner] - ETMP has returned a error code003 with a status of NOT_OK - record is not editable")
    }
  }

}
