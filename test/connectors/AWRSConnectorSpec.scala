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

import config.ApplicationConfig
import controllers.auth.StandardAuthRetrievals
import exceptions.{DESValidationException, DuplicateSubscriptionException, GovernmentGatewayException, PendingDeregistrationException}
import models.FormBundleStatus.Pending
import models.StatusContactType.MindedToReject
import models._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.libs.json._
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup
import utils.AwrsTestJson._
import utils.TestConstants._
import utils.{AWRSFeatureSwitches, AwrsUnitTestTraits, FeatureSwitch, TestUtil}

import scala.concurrent.Future
import uk.gov.hmrc.http.{BadRequestException, ForbiddenException, HeaderCarrier, HttpResponse, InternalServerException, NotFoundException, ServiceUnavailableException}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import utils.TestUtil.testBusinessRegistrationDetails

import scala.concurrent.ExecutionContext.Implicits.global

class AWRSConnectorSpec extends AwrsUnitTestTraits {

  val retrievalsWithAwrsEnrolment = StandardAuthRetrievals(TestUtil.defaultEnrolmentSet, Some(AffinityGroup.Organisation), "fakeGGCredID")
  val mockWSHttp: DefaultHttpClient = mock[DefaultHttpClient]

  override def beforeEach: Unit = {
    reset(mockWSHttp, mockAuditable, mockAccountUtils, mockAppConfig)

    when(mockAppConfig.servicesConfig)
      .thenReturn(mockServicesConfig)
    when(mockServicesConfig.baseUrl(ArgumentMatchers.eq("awrs")))
      .thenReturn("testURL")
    when(mockAccountUtils.getAuthType(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn("org/UNUSED")
    when(mockAccountUtils.authLink(ArgumentMatchers.any()))
      .thenReturn("org/UNUSED")
    when(mockAccountUtils.getAwrsRefNo(ArgumentMatchers.any()))
      .thenReturn("0123456")
  }

  val testAWRSConnector = new AWRSConnector(mockWSHttp, mockAuditable, mockAccountUtils, mockAppConfig)

  val subscribeSuccessResponse = SuccessfulSubscriptionResponse(processingDate = "2001-12-17T09:30:47Z", awrsRegistrationNumber = "ABCDEabcde12345", etmpFormBundleNumber = "123456789012345")
  val api6SuccessResponse = SuccessfulUpdateSubscriptionResponse(processingDate = "2001-12-17T09:30:47Z", etmpFormBundleNumber = "123456789012345")
  val api3SucecssResponse = SuccessfulUpdateGroupBusinessPartnerResponse(processingDate = "2001-12-17T09:30:47Z")
  lazy val updateGrpPartnerSuccessReponseJson: JsValue = Json.toJson(api3SucecssResponse)
  lazy val subscribeApi6SuccessResponseJson: JsValue = Json.toJson(api6SuccessResponse)
  lazy val badRequestResponse: JsValue = Json.parse( """{"reason": "Some other bad request reason"}""")
  lazy val notFoundResponse: JsValue = Json.parse( """{"reason": "Resource not found"}""")
  lazy val serviceUnavailableResponse: JsValue = Json.parse( """{"reason": "Dependant systems are currently not responding"}""")
  lazy val internalServerErrorResponse: JsValue = Json.parse( """{"reason": "WSO2 is currently experiencing problems that require live service intervention"}""")
  lazy val forbiddenResponse: JsValue = Json.parse( """{"reason": "ETMP has returned a error code003 with a status of NOT_OK - record is not editable"}""")
  lazy val subscribeFailureResponseJson: JsValue = Json.parse( """{"status" : "Error occurred"}""")
  lazy val subscribeFailureDesValResponse: JsValue = Json.parse( """{"reason": "Your submission contains one or more errors"}""")
  lazy val subscribeFailureGovGateResponse: JsValue = Json.parse( """{"hostname":"government-gateway-admin", "reason": "Some other reason"}""")
  lazy val subscribeFailureDupSubsResponse: JsValue = Json.parse( """{"reason": "Business Partner already has an active AWRS subscription"}""")
  lazy val subscribeFailurePendingDeregResponse: JsValue = Json.parse( """{"reason": "You cannot submit new application whilst previous one is Under Appeal/Review or being Deregistered"}""")
  lazy val subscribeSuccessResponseJson: JsValue = Json.toJson(subscribeSuccessResponse)

  "AWRSConnector subscribing to AWRS" should {

    "return status as OK, for successful subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(subscribeSuccessResponseJson))))
      val result = testAWRSConnector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)
      await(result) shouldBe subscribeSuccessResponse
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    "return status as OK, for Partnership legal entity" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(subscribeSuccessResponseJson))))
      val result = testAWRSConnector.submitAWRSData(api4PartnerJson, retrievalsWithAwrsEnrolment)
      await(result) shouldBe subscribeSuccessResponse
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }
    "return status as OK, for LTD legal entity" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(subscribeSuccessResponseJson))))
      val result = testAWRSConnector.submitAWRSData(api5LTDJson, retrievalsWithAwrsEnrolment)
      await(result) shouldBe subscribeSuccessResponse
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    "return status as BAD_REQUEST and catch all, for bad data sent for subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(subscribeFailureResponseJson))))
      val result = testAWRSConnector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)

      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage should include("Bad Request")
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    "return status as BAD_REQUEST and throw DESValidationException, for unsuccessful subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(subscribeFailureDesValResponse))))
      val result = testAWRSConnector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)

      val thrown = the[DESValidationException] thrownBy await(result)
      thrown.getMessage shouldBe "Validation against schema failed"
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    "return status as BAD_REQUEST and throw GovernmentGatewayException, for unsuccessful subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(subscribeFailureGovGateResponse))))
      val result = testAWRSConnector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)

      val thrown = the[GovernmentGatewayException] thrownBy await(result)
      thrown.getMessage shouldBe "There was a problem with the admin service"
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    "return status as BAD_REQUEST and throw DuplicateSubscriptionException, for unsuccessful subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(subscribeFailureDupSubsResponse))))
      val result = testAWRSConnector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)

      val thrown = the[DuplicateSubscriptionException] thrownBy await(result)
      thrown.getMessage shouldBe "This subscription already exists"
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    "return status as BAD_REQUEST and throw PendingDeregistrationException, for unsuccessful subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(subscribeFailurePendingDeregResponse))))
      val result = testAWRSConnector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)

      val thrown = the[PendingDeregistrationException] thrownBy await(result)
      thrown.getMessage shouldBe "You cannot submit a new application while your cancelled application is still pending"
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    "return status as NOT_FOUND, for bad data sent for subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(subscribeFailureResponseJson))))
      val result = testAWRSConnector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)
      val thrown = the[NotFoundException] thrownBy await(result)
      thrown.getMessage shouldBe "URL not found"
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }
    "return status as SERVICE_UNAVAILABLE, for bad data sent for subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(subscribeFailureResponseJson))))
      val result = testAWRSConnector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)
      val thrown = the[ServiceUnavailableException] thrownBy await(result)
      thrown.getMessage shouldBe "Service unavailable"
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }
    "return status INTERNAL SERVER ERROR" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(subscribeFailureResponseJson))))
      val result = testAWRSConnector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)
      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage shouldBe "Internal server error"
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    "return status as INTERNAL SERVER ERROR and throw GovernmentGatewayException, for unsuccessful subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(subscribeFailureGovGateResponse))))
      val result = testAWRSConnector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)

      val thrown = the[GovernmentGatewayException] thrownBy await(result)
      thrown.getMessage shouldBe "There was a problem with the admin service"
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    "return status anything else" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(777, Some(subscribeFailureResponseJson))))
      val result = testAWRSConnector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)
      val thrown = the[RuntimeException] thrownBy await(result)
      thrown.getMessage shouldBe "Unknown response"
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    "return status anything else and throw GovernmentGatewayException, for unsuccessful subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(777, Some(subscribeFailureGovGateResponse))))
      val result = testAWRSConnector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)

      val thrown = the[GovernmentGatewayException] thrownBy await(result)
      thrown.getMessage shouldBe "There was a problem with the admin service"
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }
  }

  "A user with an AWRS enrolment " should {
    "try to lookup data from ETMP via API5 endpoint" in {
      when(mockWSHttp.GET[HttpResponse](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(api5LTDJson))))

      val result = testAWRSConnector.lookupAWRSData(retrievalsWithAwrsEnrolment)

      await(result) shouldBe api5LTDJson
    }

    "return status as BAD_REQUEST, for bad request made for API 5" in {
      when(mockWSHttp.GET[HttpResponse](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(api5LTDJson))))

      val result = testAWRSConnector.lookupAWRSData(retrievalsWithAwrsEnrolment)

      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage shouldBe "The Submission has not passed validation"
    }

    "return status as NOT_FOUND for API 5" in {
      when(mockWSHttp.GET[HttpResponse](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(api5LTDJson))))

      val result = testAWRSConnector.lookupAWRSData(retrievalsWithAwrsEnrolment)

      val thrown = the[NotFoundException] thrownBy await(result)
      thrown.getMessage shouldBe "The remote endpoint has indicated that no data can be found"
    }

    "return status as SERVICE_UNAVAILABLE for API 5" in {
      when(mockWSHttp.GET[HttpResponse](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(api5LTDJson))))

      val result = testAWRSConnector.lookupAWRSData(retrievalsWithAwrsEnrolment)

      val thrown = the[ServiceUnavailableException] thrownBy await(result)
      thrown.getMessage shouldBe "Dependant systems are currently not responding"
    }

    "return status INTERNAL_SERVER_ERROR for API 5" in {
      when(mockWSHttp.GET[HttpResponse](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(api5LTDJson))))

      val result = testAWRSConnector.lookupAWRSData(retrievalsWithAwrsEnrolment)

      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage shouldBe "WSO2 is currently experiencing problems that require live service intervention"
    }
  }

  "AWRSConnector updating an AWRS application" should {

    "return status as OK, for successful subscription" in {
      when(mockWSHttp.PUT[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(subscribeApi6SuccessResponseJson))))
      val result = testAWRSConnector.updateAWRSData(api6LTDJson, retrievalsWithAwrsEnrolment)
      await(result) shouldBe api6SuccessResponse
      verify(mockWSHttp, times(1)).PUT[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    "return status as BAD_REQUEST, for unsuccessful subscription" in {
      when(mockWSHttp.PUT[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(badRequestResponse))))
      val result = testAWRSConnector.updateAWRSData(api6LTDJson, retrievalsWithAwrsEnrolment)

      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage shouldBe "[API6] - The Submission has not passed validation"
    }

    "return status as NOT_FOUND, for unsuccessful subscription" in {
      when(mockWSHttp.PUT[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(notFoundResponse))))
      val result = testAWRSConnector.updateAWRSData(api6LTDJson, retrievalsWithAwrsEnrolment)

      val thrown = the[NotFoundException] thrownBy await(result)
      thrown.getMessage shouldBe "[API6] - The remote endpoint has indicated that no data can be found"
    }

    "return status as SERVICE_UNAVAILABLE, for unsuccessful subscription" in {
      when(mockWSHttp.PUT[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(serviceUnavailableResponse))))
      val result = testAWRSConnector.updateAWRSData(api6LTDJson, retrievalsWithAwrsEnrolment)

      val thrown = the[ServiceUnavailableException] thrownBy await(result)
      thrown.getMessage shouldBe "[API6] - Dependant systems are currently not responding"
    }

    "return status as INTERNAL_SERVER_ERROR, for unsuccessful subscription" in {
      when(mockWSHttp.PUT[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(internalServerErrorResponse))))
      val result = testAWRSConnector.updateAWRSData(api6LTDJson, retrievalsWithAwrsEnrolment)

      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage should include("[API6] - WSO2 is currently experiencing problems that require live service intervention")
    }
  }

  "AWRS withdrawal connector" should {
    import utils.WithdrawalTestUtils._
    implicit val request = FakeRequest()

    "return OK on successful return of data" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(withdrawResponseJson))))
      val result = testAWRSConnector.withdrawApplication(retrievalsWithAwrsEnrolment, withdrawalJsonToSend)
      await(result) shouldBe witdrawalResponse()
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    "return status as BAD_REQUEST, for unsuccessful subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(badRequestResponse))))
      val result = testAWRSConnector.withdrawApplication(retrievalsWithAwrsEnrolment, withdrawalJsonToSend)
      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage shouldBe "The Submission has not passed validation"
    }

    "return status as NOT_FOUND, for unsuccessful subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(notFoundResponse))))
      val result = testAWRSConnector.withdrawApplication(retrievalsWithAwrsEnrolment, withdrawalJsonToSend)

      val thrown = the[NotFoundException] thrownBy await(result)
      thrown.getMessage shouldBe "The remote endpoint has indicated that no data can be found"
    }

    "return status as SERVICE_UNAVAILABLE, for unsuccessful subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(serviceUnavailableResponse))))
      val result = testAWRSConnector.withdrawApplication(retrievalsWithAwrsEnrolment, withdrawalJsonToSend)

      val thrown = the[ServiceUnavailableException] thrownBy await(result)
      thrown.getMessage shouldBe "Dependant systems are currently not responding"
    }

    "return status as INTERNAL_SERVER_ERROR, for unsuccessful subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(internalServerErrorResponse))))
      val result = testAWRSConnector.withdrawApplication(retrievalsWithAwrsEnrolment, withdrawalJsonToSend)

      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage should include("WSO2 is currently experiencing problems that require live service intervention")
    }
  }

  "getStatusInfo" should {
    lazy val getStatusInfoURI = (awrsRef: String, contactNumber: String) => f"/awrs/status-info/$awrsRef/$contactNumber"

    // these values doesn't really matter since the call itself is mocked
    implicit val request = FakeRequest()
    val contactNumber = "0123456789"
    val statusInfoResponseSuccess = StatusInfoType(Some(StatusInfoSuccessResponseType("", "")))
    val statusInfoResponseSuccessJson = StatusInfoType.writter.writes(statusInfoResponseSuccess)
    val awrsRef = "0123456"

    val statusInfoResponseFailure = StatusInfoType(Some(StatusInfoFailureResponseType("")))
    val statusInfoResponseFailureJson = StatusInfoType.writter.writes(statusInfoResponseFailure)

    val statusInfoCorruptResponseJson = Json.parse("false")

    def mockResponse(responseStatus: Int, responseData: JsValue): Unit =
      when(mockWSHttp.GET[HttpResponse](ArgumentMatchers.endsWith(getStatusInfoURI(awrsRef, contactNumber)))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(responseStatus, Some(responseData))))

    def testCall(implicit  hc: HeaderCarrier, request: Request[AnyContent]) = testAWRSConnector.getStatusInfo(contactNumber, Pending, Some(MindedToReject), retrievalsWithAwrsEnrolment)

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
    val awrsRef = "0123456"
    val deRegistrationResponseSuccess = DeRegistrationType(Some(DeRegistrationSuccessResponseType("")))
    val deRegistrationResponseSuccessJson = DeRegistrationType.writter.writes(deRegistrationResponseSuccess)

    val deRegistrationResponseFailure = DeRegistrationType(Some(DeRegistrationFailureResponseType("")))
    val deRegistrationResponseFailureJson = DeRegistrationType.writter.writes(deRegistrationResponseFailure)

    val deRegistrationCorruptResponseJson = Json.parse("false")

    val deRegistrationRequest = DeRegistration("", "", None)

    def mockResponse(responseStatus: Int, responseData: JsValue): Unit =
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.endsWith(deRegisterURI(awrsRef)), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(responseStatus, Some(responseData))))

    def testCall(implicit  hc: HeaderCarrier, request: Request[AnyContent]) = testAWRSConnector.deRegistration(deRegistrationRequest, retrievalsWithAwrsEnrolment)

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

    val address = BCAddressApi3(addressLine1 = "", addressLine2 = "")
    val updatedDataRequest = new UpdateRegistrationDetailsRequest(false, Some(OrganisationName("testName")), address, ContactDetails(), false, false)

    "return status as OK, for successful update" in {
      when(mockWSHttp.PUT[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(updateGrpPartnerSuccessReponseJson))))
      val result = testAWRSConnector.updateGroupBusinessPartner("","","",updatedDataRequest, retrievalsWithAwrsEnrolment)
      await(result) shouldBe api3SucecssResponse
      verify(mockWSHttp, times(1)).PUT[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    "return status as BAD_REQUEST, for unsuccessful update" in {
      when(mockWSHttp.PUT[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(badRequestResponse))))
      val result = testAWRSConnector.updateGroupBusinessPartner("","","",updatedDataRequest, retrievalsWithAwrsEnrolment)

      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage shouldBe "[API3 - Update Group Business Partner] - The Submission has not passed validation"
    }

    "return status as NOT_FOUND, for unsuccessful update" in {
      when(mockWSHttp.PUT[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(notFoundResponse))))
      val result = testAWRSConnector.updateGroupBusinessPartner("","","",updatedDataRequest, retrievalsWithAwrsEnrolment)

      val thrown = the[NotFoundException] thrownBy await(result)
      thrown.getMessage shouldBe "[API3 - Update Group Business Partner] - The remote endpoint has indicated that no data can be found"
    }

    "return status as SERVICE_UNAVAILABLE, for unsuccessful update" in {
     when(mockWSHttp.PUT[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
       .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(serviceUnavailableResponse))))
     val result = testAWRSConnector.updateGroupBusinessPartner("","","",updatedDataRequest, retrievalsWithAwrsEnrolment)

     val thrown = the[ServiceUnavailableException] thrownBy await(result)
     thrown.getMessage shouldBe "[API3 - Update Group Business Partner] - Dependant systems are currently not responding"
   }

   "return status as INTERNAL_SERVER_ERROR, for unsuccessful update" in {
     when(mockWSHttp.PUT[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
       .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(internalServerErrorResponse))))
     val result = testAWRSConnector.updateGroupBusinessPartner(testTradingName,"","",updatedDataRequest, retrievalsWithAwrsEnrolment)

     val thrown = the[InternalServerException] thrownBy await(result)
     thrown.getMessage should include("[API3 - Update Group Business Partner] - WSO2 is currently experiencing problems that require live service intervention")
   }

    "return status as FORBIDDEN, for unsuccessful update" in {
      when(mockWSHttp.PUT[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(FORBIDDEN, Some(forbiddenResponse))))
      val result = testAWRSConnector.updateGroupBusinessPartner("","","",updatedDataRequest, retrievalsWithAwrsEnrolment)

      val thrown = the[ForbiddenException] thrownBy await(result)
      thrown.getMessage should include("[API3 - Update Group Business Partner] - ETMP has returned a error code003 with a status of NOT_OK - record is not editable")
    }
  }

  "Check ETMP call to backend" should {
    val testBCAddress = BCAddress("addressLine1", "addressLine2", Option("addressLine3"), Option("addressLine4"), Option("Ne4 9hs"), Option("country"))
    val testBusinessCustomer = BusinessCustomerDetails("ACME", Some("SOP"), testBCAddress, "sap123", "safe123", false, Some("agent123"))
    val testBusinessRegistrationDetails = BusinessRegistrationDetails(
      doYouHaveNino = Some("Yes"),
      nino = testNino,
      doYouHaveVRN = Some("Yes"),
      vrn = testVrn,
      doYouHaveUTR = Some("No"),legalEntity = None, utr = Some("1234"))

    def mockResponse(responseStatus: Int, responseData: JsValue): Unit =
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(responseStatus)))
    def testCall(implicit  hc: HeaderCarrier): Future[Boolean] = testAWRSConnector.checkEtmp(testBusinessCustomer, testBusinessRegistrationDetails)

    val checkRegimeModelResponseSuccess = CheckRegimeModel(testBusinessCustomer, testBusinessRegistrationDetails)
    val checkRegimeModelResponseSuccessJson = CheckRegimeModel.format.writes(checkRegimeModelResponseSuccess)

    "Return true after receiving an OK response" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())
      mockResponse(OK, checkRegimeModelResponseSuccessJson)
      val result: Boolean = await(testCall)

      result shouldBe true
    }

    "Return false after receiving a NO CONTENT response" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())
      mockResponse(NO_CONTENT, checkRegimeModelResponseSuccessJson)
      val result: Boolean = await(testCall)

      result shouldBe false
    }

    "Return false after receiving a BAD REQUEST response" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())
      mockResponse(BAD_REQUEST, checkRegimeModelResponseSuccessJson)
      val result: Boolean = await(testCall)

      result shouldBe false
    }

    "Return false after an exception has been thrown" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())
      def mockInvalidResponse(responseStatus: Int, responseData: JsValue): Unit =
        when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.failed(new RuntimeException()))
      mockInvalidResponse(BAD_REQUEST, checkRegimeModelResponseSuccessJson)
      val result: Boolean = await(testCall)

      result shouldBe false
    }
    "Return false when feature flag is false" in {
      FeatureSwitch.disable(AWRSFeatureSwitches.regimeCheck())
      mockResponse(OK, checkRegimeModelResponseSuccessJson)
      val result: Boolean = await(testCall)

      result shouldBe false
    }
  }
}
