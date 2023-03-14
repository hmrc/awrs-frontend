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

import controllers.auth.StandardAuthRetrievals
import exceptions.{DESValidationException, DuplicateSubscriptionException, GovernmentGatewayException, PendingDeregistrationException}
import models.FormBundleStatus.Pending
import models.StatusContactType.MindedToReject
import models._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.libs.json._
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import utils.AwrsTestJson._
import utils.TestConstants._
import utils.{AWRSFeatureSwitches, AwrsUnitTestTraits, FeatureSwitch, TestUtil}
import uk.gov.hmrc.auth.core.User

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AWRSConnectorSpec extends AwrsUnitTestTraits {

  val retrievalsWithAwrsEnrolment: StandardAuthRetrievals = StandardAuthRetrievals(TestUtil.defaultEnrolmentSet, Some(AffinityGroup.Organisation), "fakeGGCredID", Some(User))
  val mockWSHttp: DefaultHttpClient = mock[DefaultHttpClient]

  override def beforeEach: Unit = {
    reset(mockWSHttp, mockAuditable, mockAccountUtils, mockAppConfig)

    when(mockAppConfig.servicesConfig)
      .thenReturn(mockServicesConfig)
    when(mockServicesConfig.baseUrl(ArgumentMatchers.eq("awrs")))
      .thenReturn("testURL")
    when(mockAccountUtils.authLink(any()))
      .thenReturn("org/UNUSED")
    when(mockAccountUtils.getAwrsRefNo(any()))
      .thenReturn("0123456")
    when(mockAccountUtils.lookupAwrsRefNo(any()))
      .thenReturn(Some("0123456"))
  }

  val testAWRSConnector = new AWRSConnector(mockWSHttp, mockAuditable, mockAccountUtils, mockAppConfig)

  val subscribeSuccessResponse: SuccessfulSubscriptionResponse = SuccessfulSubscriptionResponse(processingDate = "2001-12-17T09:30:47Z", awrsRegistrationNumber = "ABCDEabcde12345", etmpFormBundleNumber = "123456789012345")
  val subscribeAcceptedResponse: SelfHealSubscriptionResponse = SelfHealSubscriptionResponse("123456")
  val api6SuccessResponse: SuccessfulUpdateSubscriptionResponse = SuccessfulUpdateSubscriptionResponse(processingDate = "2001-12-17T09:30:47Z", etmpFormBundleNumber = "123456789012345")
  val api3SucecssResponse: SuccessfulUpdateGroupBusinessPartnerResponse = SuccessfulUpdateGroupBusinessPartnerResponse(processingDate = "2001-12-17T09:30:47Z")
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
  lazy val acceptedResponseJson: JsValue = Json.toJson(subscribeAcceptedResponse)


  "AWRSConnector subscribing to AWRS" must {

    "return status as OK, for successful subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(OK, subscribeSuccessResponseJson.toString())))
      val result = testAWRSConnector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)
      await(result) mustBe Right(subscribeSuccessResponse)
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any())
    }

    "return status as OK, for Partnership legal entity" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(OK, subscribeSuccessResponseJson.toString())))
      val result = testAWRSConnector.submitAWRSData(api4PartnerJson, retrievalsWithAwrsEnrolment)
      await(result) mustBe Right(subscribeSuccessResponse)
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any())
    }
    "return status as OK, for LTD legal entity" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(OK, subscribeSuccessResponseJson.toString())))
      val result = testAWRSConnector.submitAWRSData(api5LTDJson, retrievalsWithAwrsEnrolment)
      await(result) mustBe Right(subscribeSuccessResponse)
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any())
    }

    "return status ACCEPTED, for a organisation self heal case" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(ACCEPTED, acceptedResponseJson.toString())))
      val result = testAWRSConnector.submitAWRSData(api5LTDJson, retrievalsWithAwrsEnrolment)
      await(result) mustBe Left(subscribeAcceptedResponse)
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any())
    }

    "return status as BAD_REQUEST and catch all, for bad data sent for subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(BAD_REQUEST, subscribeFailureResponseJson.toString())))
      val result = testAWRSConnector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)

      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage must include("Bad Request")
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any())
    }

    "return status as BAD_REQUEST and throw DESValidationException, for unsuccessful subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(BAD_REQUEST, subscribeFailureDesValResponse.toString())))
      val result = testAWRSConnector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)

      val thrown = the[DESValidationException] thrownBy await(result)
      thrown.getMessage mustBe "Validation against schema failed"
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any())
    }

    "return status as BAD_REQUEST and throw GovernmentGatewayException, for unsuccessful subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(BAD_REQUEST, subscribeFailureGovGateResponse.toString())))
      val result = testAWRSConnector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)

      val thrown = the[GovernmentGatewayException] thrownBy await(result)
      thrown.getMessage mustBe "There was a problem with the admin service"
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any())
    }

    "return status as BAD_REQUEST and throw DuplicateSubscriptionException, for unsuccessful subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(BAD_REQUEST, subscribeFailureDupSubsResponse.toString())))
      val result = testAWRSConnector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)

      val thrown = the[DuplicateSubscriptionException] thrownBy await(result)
      thrown.getMessage mustBe "This subscription already exists"
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any())
    }

    "return status as BAD_REQUEST and throw PendingDeregistrationException, for unsuccessful subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(BAD_REQUEST, subscribeFailurePendingDeregResponse.toString())))
      val result = testAWRSConnector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)

      val thrown = the[PendingDeregistrationException] thrownBy await(result)
      thrown.getMessage mustBe "You cannot submit a new application while your cancelled application is still pending"
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any())
    }

    "return status as NOT_FOUND, for bad data sent for subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(NOT_FOUND, subscribeFailureResponseJson.toString())))
      val result = testAWRSConnector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)
      val thrown = the[NotFoundException] thrownBy await(result)
      thrown.getMessage mustBe "URL not found"
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any())
    }
    "return status as SERVICE_UNAVAILABLE, for bad data sent for subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(SERVICE_UNAVAILABLE, subscribeFailureResponseJson.toString())))
      val result = testAWRSConnector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)
      val thrown = the[ServiceUnavailableException] thrownBy await(result)
      thrown.getMessage mustBe "Service unavailable"
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any())
    }
    "return status INTERNAL SERVER ERROR" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, subscribeFailureResponseJson.toString())))
      val result = testAWRSConnector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)
      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage mustBe "Internal server error"
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any())
    }

    "return status as INTERNAL SERVER ERROR and throw GovernmentGatewayException, for unsuccessful subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, subscribeFailureGovGateResponse.toString())))
      val result = testAWRSConnector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)

      val thrown = the[GovernmentGatewayException] thrownBy await(result)
      thrown.getMessage mustBe "There was a problem with the admin service"
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any())
    }

    "return status anything else" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(777, subscribeFailureResponseJson.toString())))
      val result = testAWRSConnector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)
      val thrown = the[RuntimeException] thrownBy await(result)
      thrown.getMessage mustBe "Unknown response"
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any())
    }

    "return status anything else and throw GovernmentGatewayException, for unsuccessful subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(777, subscribeFailureGovGateResponse.toString())))
      val result = testAWRSConnector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)

      val thrown = the[GovernmentGatewayException] thrownBy await(result)
      thrown.getMessage mustBe "There was a problem with the admin service"
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any())
    }
  }

  "A user with an AWRS enrolment " must {
    "try to lookup data from ETMP via API5 endpoint" in {
      when(mockWSHttp.GET[HttpResponse](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(OK, api5LTDJson.toString())))

      val result = testAWRSConnector.lookupAWRSData(retrievalsWithAwrsEnrolment)

      await(result) mustBe api5LTDJson
    }

    "return status as BAD_REQUEST, for bad request made for API 5" in {
      when(mockWSHttp.GET[HttpResponse](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(BAD_REQUEST, api5LTDJson.toString())))

      val result = testAWRSConnector.lookupAWRSData(retrievalsWithAwrsEnrolment)

      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage mustBe "The Submission has not passed validation"
    }

    "return status as NOT_FOUND for API 5" in {
      when(mockWSHttp.GET[HttpResponse](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(NOT_FOUND, api5LTDJson.toString())))

      val result = testAWRSConnector.lookupAWRSData(retrievalsWithAwrsEnrolment)

      val thrown = the[NotFoundException] thrownBy await(result)
      thrown.getMessage mustBe "The remote endpoint has indicated that no data can be found"
    }

    "return status as SERVICE_UNAVAILABLE for API 5" in {
      when(mockWSHttp.GET[HttpResponse](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(SERVICE_UNAVAILABLE, api5LTDJson.toString())))

      val result = testAWRSConnector.lookupAWRSData(retrievalsWithAwrsEnrolment)

      val thrown = the[ServiceUnavailableException] thrownBy await(result)
      thrown.getMessage mustBe "Dependant systems are currently not responding"
    }

    "return status INTERNAL_SERVER_ERROR for API 5" in {
      when(mockWSHttp.GET[HttpResponse](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, api5LTDJson.toString())))

      val result = testAWRSConnector.lookupAWRSData(retrievalsWithAwrsEnrolment)

      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage mustBe "WSO2 is currently experiencing problems that require live service intervention"
    }
  }

  "AWRSConnector updating an AWRS application" must {

    "return status as OK, for successful subscription" in {
      when(mockWSHttp.PUT[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(OK, subscribeApi6SuccessResponseJson.toString())))
      val result = testAWRSConnector.updateAWRSData(api6LTDJson, retrievalsWithAwrsEnrolment)
      await(result) mustBe api6SuccessResponse
      verify(mockWSHttp, times(1)).PUT[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any())
    }

    "return status as BAD_REQUEST, for unsuccessful subscription" in {
      when(mockWSHttp.PUT[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(BAD_REQUEST, badRequestResponse.toString())))
      val result = testAWRSConnector.updateAWRSData(api6LTDJson, retrievalsWithAwrsEnrolment)

      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage mustBe "[API6] - The Submission has not passed validation"
    }

    "return status as NOT_FOUND, for unsuccessful subscription" in {
      when(mockWSHttp.PUT[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(NOT_FOUND, notFoundResponse.toString())))
      val result = testAWRSConnector.updateAWRSData(api6LTDJson, retrievalsWithAwrsEnrolment)

      val thrown = the[NotFoundException] thrownBy await(result)
      thrown.getMessage mustBe "[API6] - The remote endpoint has indicated that no data can be found"
    }

    "return status as SERVICE_UNAVAILABLE, for unsuccessful subscription" in {
      when(mockWSHttp.PUT[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(SERVICE_UNAVAILABLE, serviceUnavailableResponse.toString())))
      val result = testAWRSConnector.updateAWRSData(api6LTDJson, retrievalsWithAwrsEnrolment)

      val thrown = the[ServiceUnavailableException] thrownBy await(result)
      thrown.getMessage mustBe "[API6] - Dependant systems are currently not responding"
    }

    "return status as INTERNAL_SERVER_ERROR, for unsuccessful subscription" in {
      when(mockWSHttp.PUT[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, internalServerErrorResponse.toString())))
      val result = testAWRSConnector.updateAWRSData(api6LTDJson, retrievalsWithAwrsEnrolment)

      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage must include("[API6] - WSO2 is currently experiencing problems that require live service intervention")
    }
  }

  "AWRS withdrawal connector" must {
    import utils.WithdrawalTestUtils._
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

    "return OK on successful return of data" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(OK, withdrawResponseJson.toString())))
      val result = testAWRSConnector.withdrawApplication(retrievalsWithAwrsEnrolment, withdrawalJsonToSend)
      await(result) mustBe witdrawalResponse()
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any())
    }

    "return status as BAD_REQUEST, for unsuccessful subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(BAD_REQUEST, badRequestResponse.toString())))
      val result = testAWRSConnector.withdrawApplication(retrievalsWithAwrsEnrolment, withdrawalJsonToSend)
      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage mustBe "The Submission has not passed validation"
    }

    "return status as NOT_FOUND, for unsuccessful subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(NOT_FOUND, notFoundResponse.toString())))
      val result = testAWRSConnector.withdrawApplication(retrievalsWithAwrsEnrolment, withdrawalJsonToSend)

      val thrown = the[NotFoundException] thrownBy await(result)
      thrown.getMessage mustBe "The remote endpoint has indicated that no data can be found"
    }

    "return status as SERVICE_UNAVAILABLE, for unsuccessful subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(SERVICE_UNAVAILABLE, serviceUnavailableResponse.toString())))
      val result = testAWRSConnector.withdrawApplication(retrievalsWithAwrsEnrolment, withdrawalJsonToSend)

      val thrown = the[ServiceUnavailableException] thrownBy await(result)
      thrown.getMessage mustBe "Dependant systems are currently not responding"
    }

    "return status as INTERNAL_SERVER_ERROR, for unsuccessful subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, internalServerErrorResponse.toString())))
      val result = testAWRSConnector.withdrawApplication(retrievalsWithAwrsEnrolment, withdrawalJsonToSend)

      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage must include("WSO2 is currently experiencing problems that require live service intervention")
    }
  }

  "getStatusInfo" must {
    lazy val getStatusInfoURI = (awrsRef: String, contactNumber: String) => f"/awrs/status-info/$awrsRef/$contactNumber"

    // these values doesn't really matter since the call itself is mocked
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    val contactNumber = "0123456789"
    val statusInfoResponseSuccess = StatusInfoType(Some(StatusInfoSuccessResponseType("", "")))
    val statusInfoResponseSuccessJson = StatusInfoType.writter.writes(statusInfoResponseSuccess)
    val awrsRef = "0123456"

    val statusInfoResponseFailure = StatusInfoType(Some(StatusInfoFailureResponseType("")))
    val statusInfoResponseFailureJson = StatusInfoType.writter.writes(statusInfoResponseFailure)

    val statusInfoCorruptResponseJson = Json.parse("false")

    def mockResponse(responseStatus: Int, responseData: JsValue): Unit =
      when(mockWSHttp.GET[HttpResponse](ArgumentMatchers.endsWith(getStatusInfoURI(awrsRef, contactNumber)), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(responseStatus, responseData.toString())))

    def testCall(implicit  hc: HeaderCarrier, request: Request[AnyContent]) = testAWRSConnector.getStatusInfo(contactNumber, Pending, Some(MindedToReject), retrievalsWithAwrsEnrolment)

    "return status as OK, for successful de-enrolment" in {
      mockResponse(OK, statusInfoResponseSuccessJson)
      val result = testCall
      await(result) mustBe statusInfoResponseSuccess

      mockResponse(OK, statusInfoResponseFailureJson)
      val resultFailure = testCall
      the[BadRequestException] thrownBy await(resultFailure)

      mockResponse(OK, statusInfoCorruptResponseJson)
      val resultCorrupt = testCall
      the[BadRequestException] thrownBy await(resultCorrupt)
    }

    "return status as BAD_REQUEST, for unsuccessful de-enrolment" in {
      mockResponse(BAD_REQUEST, statusInfoResponseSuccessJson)
      val result = testCall
      the[BadRequestException] thrownBy await(result)
    }

    "return status as NOT_FOUND, for unsuccessful de-enrolment" in {
      mockResponse(NOT_FOUND, statusInfoResponseSuccessJson)
      val result = testCall
      the[NotFoundException] thrownBy await(result)
    }

    "return status as SERVICE_UNAVAILABLE, for unsuccessful de-enrolment" in {
      mockResponse(SERVICE_UNAVAILABLE, statusInfoResponseSuccessJson)
      val result = testCall
      the[ServiceUnavailableException] thrownBy await(result)
    }

    "return status as INTERNAL_SERVER_ERROR, for unsuccessful de-enrolment" in {
      mockResponse(INTERNAL_SERVER_ERROR, statusInfoResponseSuccessJson)
      val result = testCall
      the[InternalServerException] thrownBy await(result)
    }

    "return status as unexpected status, for unsuccessful de-enrolment" in {
      val otherStatus = 999
      mockResponse(otherStatus, statusInfoResponseSuccessJson)
      val result = testCall
      the[InternalServerException] thrownBy await(result)
    }
  }

  "deRegistration" must {
    lazy val deRegisterURI = (awrsRef: String) => f"/awrs/de-registration/$awrsRef"

    // these values doesn't really matter since the call itself is mocked
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    val awrsRef = "0123456"
    val deRegistrationResponseSuccess = DeRegistrationType(Some(DeRegistrationSuccessResponseType("")))
    val deRegistrationResponseSuccessJson = DeRegistrationType.writter.writes(deRegistrationResponseSuccess)

    val deRegistrationResponseFailure = DeRegistrationType(Some(DeRegistrationFailureResponseType("")))
    val deRegistrationResponseFailureJson = DeRegistrationType.writter.writes(deRegistrationResponseFailure)

    val deRegistrationCorruptResponseJson = Json.parse("false")

    val deRegistrationRequest = DeRegistration("", "", None)

    def mockResponse(responseStatus: Int, responseData: JsValue): Unit =
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.endsWith(deRegisterURI(awrsRef)), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(responseStatus, responseData.toString())))

    def testCall(implicit  hc: HeaderCarrier, request: Request[AnyContent]) = testAWRSConnector.deRegistration(deRegistrationRequest, retrievalsWithAwrsEnrolment)

    "return status as OK, for successful de-enrolment" in {
      mockResponse(OK, deRegistrationResponseSuccessJson)
      val result = testCall
      await(result) mustBe deRegistrationResponseSuccess

      mockResponse(OK, deRegistrationResponseFailureJson)
      val resultFailure = testCall
      await(resultFailure) mustBe deRegistrationResponseFailure

      mockResponse(OK, deRegistrationCorruptResponseJson)
      val resultCorrupt = testCall
      the[BadRequestException] thrownBy await(resultCorrupt)
    }

    "return status as BAD_REQUEST, for unsuccessful de-enrolment" in {
      mockResponse(BAD_REQUEST, deRegistrationResponseSuccessJson)
      val result = testCall
      the[BadRequestException] thrownBy await(result)
    }

    "return status as NOT_FOUND, for unsuccessful de-enrolment" in {
      mockResponse(NOT_FOUND, deRegistrationResponseSuccessJson)
      val result = testCall
      the[NotFoundException] thrownBy await(result)
    }

    "return status as SERVICE_UNAVAILABLE, for unsuccessful de-enrolment" in {
      mockResponse(SERVICE_UNAVAILABLE, deRegistrationResponseSuccessJson)
      val result = testCall
      the[ServiceUnavailableException] thrownBy await(result)
    }

    "return status as INTERNAL_SERVER_ERROR, for unsuccessful de-enrolment" in {
      mockResponse(INTERNAL_SERVER_ERROR, deRegistrationResponseSuccessJson)
      val result = testCall
      the[InternalServerException] thrownBy await(result)
    }

    "return status as unexpected status, for unsuccessful de-enrolment" in {
      val otherStatus = 999
      mockResponse(otherStatus, deRegistrationResponseSuccessJson)
      val result = testCall
      the[InternalServerException] thrownBy await(result)
    }

  }

  "checkUsersEnrolments" must {
    val testCredID = "credID-123"
    val testSafeID = "safeID-123"
    val mockAwrsUsers = Json.toJson(AwrsUsers(List("principalUserId-One","principalUserId-Two"), List("delegatedUserId-One"))).toString
    val testAwrsUsers = AwrsUsers(List("principalUserId-One","principalUserId-Two"), List("delegatedUserId-One"))
    implicit val ec = scala.concurrent.ExecutionContext.global

    "return OK when the call to awrs is successful" in {
      when(mockWSHttp.GET[HttpResponse](any(), any(), any())(any(), any(), any())).thenReturn(Future.successful(HttpResponse.apply(200,mockAwrsUsers)))
      val result = testAWRSConnector.checkUsersEnrolments(testSafeID,testCredID)(hc,ec)
      await(result) mustBe Some(testAwrsUsers)
    }
    "return an internal server exception if the call fails" in {
      when(mockWSHttp.GET[HttpResponse](any(), any(), any())(any(), any(), any())).thenReturn(Future.successful(HttpResponse.apply(500,"")))
      val result = testAWRSConnector.checkUsersEnrolments(testSafeID,testCredID)(hc,ec)
      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage mustBe "[awrs-frontend][checkUsersEnrolments] returned status code: 500"
    }
  }

  "AWRSConnector update group business partner" must {

    val address = BCAddressApi3(addressLine1 = "", addressLine2 = "")
    val updatedDataRequest = new UpdateRegistrationDetailsRequest(false, Some(OrganisationName("testName")), address, ContactDetails(), false, false)

    "return status as OK, for successful update" in {
      when(mockWSHttp.PUT[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(OK, updateGrpPartnerSuccessReponseJson.toString())))
      val result = testAWRSConnector.updateGroupBusinessPartner("","","",updatedDataRequest, retrievalsWithAwrsEnrolment)
      await(result) mustBe api3SucecssResponse
      verify(mockWSHttp, times(1)).PUT[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any())
    }

    "return status as BAD_REQUEST, for unsuccessful update" in {
      when(mockWSHttp.PUT[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(BAD_REQUEST, badRequestResponse.toString())))
      val result = testAWRSConnector.updateGroupBusinessPartner("","","",updatedDataRequest, retrievalsWithAwrsEnrolment)

      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage mustBe "[API3 - Update Group Business Partner] - The Submission has not passed validation"
    }

    "return status as NOT_FOUND, for unsuccessful update" in {
      when(mockWSHttp.PUT[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(NOT_FOUND, notFoundResponse.toString())))
      val result = testAWRSConnector.updateGroupBusinessPartner("","","",updatedDataRequest, retrievalsWithAwrsEnrolment)

      val thrown = the[NotFoundException] thrownBy await(result)
      thrown.getMessage mustBe "[API3 - Update Group Business Partner] - The remote endpoint has indicated that no data can be found"
    }

    "return status as SERVICE_UNAVAILABLE, for unsuccessful update" in {
     when(mockWSHttp.PUT[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
       .thenReturn(Future.successful(HttpResponse.apply(SERVICE_UNAVAILABLE, serviceUnavailableResponse.toString())))
     val result = testAWRSConnector.updateGroupBusinessPartner("","","",updatedDataRequest, retrievalsWithAwrsEnrolment)

     val thrown = the[ServiceUnavailableException] thrownBy await(result)
     thrown.getMessage mustBe "[API3 - Update Group Business Partner] - Dependant systems are currently not responding"
   }

   "return status as INTERNAL_SERVER_ERROR, for unsuccessful update" in {
     when(mockWSHttp.PUT[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
       .thenReturn(Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, internalServerErrorResponse.toString())))
     val result = testAWRSConnector.updateGroupBusinessPartner(testTradingName,"","",updatedDataRequest, retrievalsWithAwrsEnrolment)

     val thrown = the[InternalServerException] thrownBy await(result)
     thrown.getMessage must include("[API3 - Update Group Business Partner] - WSO2 is currently experiencing problems that require live service intervention")
   }

    "return status as FORBIDDEN, for unsuccessful update" in {
      when(mockWSHttp.PUT[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(FORBIDDEN, forbiddenResponse.toString())))
      val result = testAWRSConnector.updateGroupBusinessPartner("","","",updatedDataRequest, retrievalsWithAwrsEnrolment)

      val thrown = the[ForbiddenException] thrownBy await(result)
      thrown.getMessage must include("[API3 - Update Group Business Partner] - ETMP has returned a error code003 with a status of NOT_OK - record is not editable")
    }
  }

  "Check ETMP call to backend" must {
    val testBCAddress = BCAddress("addressLine1", "addressLine2", Option("addressLine3"), Option("addressLine4"), Option("Ne4 9hs"), Option("country"))
    val testBusinessCustomer = BusinessCustomerDetails("ACME", Some("SOP"), testBCAddress, "sap123", "safe123", false, Some("agent123"))

    def mockResponse(responseStatus: Int, responseData: Option[JsValue] = None): Unit =
      when(mockWSHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(responseStatus, responseData.getOrElse(Json.obj()), Map.empty[String, Seq[String]])))
    def testCall(implicit  hc: HeaderCarrier): Future[Option[SelfHealSubscriptionResponse]] = testAWRSConnector.checkEtmp(testBusinessCustomer, "SOP")

    val checkRegimeModelResponseSuccess = SelfHealSubscriptionResponse("123456")
    val checkRegimeModelResponseSuccessJson = Json.toJson(checkRegimeModelResponseSuccess)

    "Return a SelfHealSubscriptionResponse after receiving an OK response" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())
      mockResponse(OK, checkRegimeModelResponseSuccessJson)
      val result: Option[SelfHealSubscriptionResponse] = await(testCall)

      result mustBe Some(subscribeAcceptedResponse)
    }

    "Return None after receiving a NO CONTENT response" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())
      mockResponse(NO_CONTENT)
      val result: Option[SelfHealSubscriptionResponse] = await(testCall)

      result mustBe None
    }

    "Return None after receiving a BAD REQUEST response" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())
      mockResponse(BAD_REQUEST)
      val result: Option[SelfHealSubscriptionResponse] = await(testCall)

      result mustBe None
    }

    "Return None after an exception has been thrown" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())
      def mockInvalidResponse(responseStatus: Int): Unit =
        when(mockWSHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.failed(new RuntimeException()))
      mockInvalidResponse(BAD_REQUEST)
      val result: Option[SelfHealSubscriptionResponse] = await(testCall)

      result mustBe None
    }

    "Return None when feature flag is false" in {
      FeatureSwitch.disable(AWRSFeatureSwitches.regimeCheck())
      mockResponse(OK)
      val result: Option[SelfHealSubscriptionResponse] = await(testCall)

      result mustBe None
    }
  }
}
