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

import com.github.tomakehurst.wiremock.client.WireMock.{postRequestedFor, putRequestedFor, urlMatching}
import controllers.auth.StandardAuthRetrievals
import exceptions.{DESValidationException, DuplicateSubscriptionException, GovernmentGatewayException, PendingDeregistrationException}
import models.FormBundleStatus.Pending
import models.StatusContactType.MindedToReject
import models._
import org.scalatest.matchers.must.Matchers
import play.api.libs.json._
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Request}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.auth.core.{AffinityGroup, User}
import uk.gov.hmrc.helpers.IntegrationSpec
import uk.gov.hmrc.http._
import utils.AwrsTestJson._
import utils.TestConstants._
import utils.{AWRSFeatureSwitches, FeatureSwitch, TestUtil}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AWRSConnectorISpec extends IntegrationSpec with Injecting with Matchers {

  val retrievalsWithAwrsEnrolment: StandardAuthRetrievals = StandardAuthRetrievals(TestUtil.defaultEnrolmentSet, Some(AffinityGroup.Organisation), "fakePlainTextCredID", "fakeGGCredID", Some(User))

  val connector = inject[AWRSConnector]

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

  def verifySubmitData(): Unit = wireMockServer.verify(1, postRequestedFor(urlMatching("/awrs/send-data")))
  def verifyUpdateData(): Unit = wireMockServer.verify(1, putRequestedFor(urlMatching("/awrs/update/0123456")))
  def verifyWithdrawal(): Unit = wireMockServer.verify(1, postRequestedFor(urlMatching("/awrs/withdrawal/0123456")))
  def verifyUpdateGrpPartner(): Unit = wireMockServer.verify(1, putRequestedFor(urlMatching("/0123456/registration-details/XE1234")))



  "AWRSConnector subscribing to AWRS" must {

    "return status as OK, for successful subscription" in {
      stubbedPost("/awrs/send-data", OK, subscribeSuccessResponseJson.toString())
      val result = connector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)
      await(result) mustBe Right(subscribeSuccessResponse)
      verifySubmitData()
    }

    "return status as OK, for Partnership legal entity" in {
      stubbedPost("/awrs/send-data", OK, subscribeSuccessResponseJson.toString())
      val result = connector.submitAWRSData(api4PartnerJson, retrievalsWithAwrsEnrolment)
      await(result) mustBe Right(subscribeSuccessResponse)
      verifySubmitData()
    }

    "return status as OK, for LTD legal entity" in {
      stubbedPost("/awrs/send-data", OK, subscribeSuccessResponseJson.toString())
      val result = connector.submitAWRSData(api5LTDJson, retrievalsWithAwrsEnrolment)
      await(result) mustBe Right(subscribeSuccessResponse)
      verifySubmitData()
    }

    "return status ACCEPTED, for a organisation self heal case" in {
      stubbedPost("/awrs/send-data", ACCEPTED, acceptedResponseJson.toString())
      val result = connector.submitAWRSData(api5LTDJson, retrievalsWithAwrsEnrolment)
      await(result) mustBe Left(subscribeAcceptedResponse)
      verifySubmitData()
    }

    "return status as BAD_REQUEST and catch all, for bad data sent for subscription" in {
      stubbedPost("/awrs/send-data", BAD_REQUEST, subscribeFailureResponseJson.toString())
      val result = connector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)
      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage must include("Bad Request")
      verifySubmitData()
    }

    "return status as BAD_REQUEST and throw DESValidationException, for unsuccessful subscription" in {
      stubbedPost("/awrs/send-data", BAD_REQUEST, subscribeFailureDesValResponse.toString())
      val result = connector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)

      val thrown = the[DESValidationException] thrownBy await(result)
      thrown.getMessage mustBe "Validation against schema failed"
      verifySubmitData()
    }

    "return status as BAD_REQUEST and throw GovernmentGatewayException, for unsuccessful subscription" in {
      stubbedPost("/awrs/send-data", BAD_REQUEST, subscribeFailureGovGateResponse.toString())
      val result = connector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)
      val thrown = the[GovernmentGatewayException] thrownBy await(result)
      thrown.getMessage mustBe "There was a problem with the admin service"
      verifySubmitData()
    }

    "return status as BAD_REQUEST and throw DuplicateSubscriptionException, for unsuccessful subscription" in {
      stubbedPost("/awrs/send-data", BAD_REQUEST, subscribeFailureDupSubsResponse.toString())
      val result = connector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)
      val thrown = the[DuplicateSubscriptionException] thrownBy await(result)
      thrown.getMessage mustBe "This subscription already exists"
      verifySubmitData()
    }

    "return status as BAD_REQUEST and throw PendingDeregistrationException, for unsuccessful subscription" in {
      stubbedPost("/awrs/send-data", BAD_REQUEST, subscribeFailurePendingDeregResponse.toString())
      val result = connector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)
      val thrown = the[PendingDeregistrationException] thrownBy await(result)
      thrown.getMessage mustBe "You cannot submit a new application while your cancelled application is still pending"
      verifySubmitData()
    }

    "return status as NOT_FOUND, for bad data sent for subscription" in {
      stubbedPost("/awrs/send-data", NOT_FOUND, subscribeFailureResponseJson.toString())
      val result = connector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)
      val thrown = the[NotFoundException] thrownBy await(result)
      thrown.getMessage mustBe "URL not found"
      verifySubmitData()
    }

    "return status as SERVICE_UNAVAILABLE, for bad data sent for subscription" in {
      stubbedPost("/awrs/send-data", SERVICE_UNAVAILABLE, subscribeFailureResponseJson.toString())
      val result = connector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)
      val thrown = the[ServiceUnavailableException] thrownBy await(result)
      thrown.getMessage mustBe "Service unavailable"
      verifySubmitData()
    }

    "return status INTERNAL SERVER ERROR" in {
      stubbedPost("/awrs/send-data", INTERNAL_SERVER_ERROR, subscribeFailureResponseJson.toString())
      val result = connector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)
      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage mustBe "Internal server error"
      verifySubmitData()
    }

    "return status as INTERNAL SERVER ERROR and throw GovernmentGatewayException, for unsuccessful subscription" in {
      stubbedPost("/awrs/send-data", INTERNAL_SERVER_ERROR, subscribeFailureGovGateResponse.toString())
      val result = connector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)
      val thrown = the[GovernmentGatewayException] thrownBy await(result)
      thrown.getMessage mustBe "There was a problem with the admin service"
      verifySubmitData()
    }

    "return status anything else" in {
      stubbedPost("/awrs/send-data", 777, subscribeFailureResponseJson.toString())
      val result = connector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)
      val thrown = the[RuntimeException] thrownBy await(result)
      thrown.getMessage mustBe "Unknown response"
      verifySubmitData()
    }

    "return status anything else and throw GovernmentGatewayException, for unsuccessful subscription" in {
      stubbedPost("/awrs/send-data", 777, subscribeFailureGovGateResponse.toString())
      val result = connector.submitAWRSData(api4SOPJson, retrievalsWithAwrsEnrolment)
      val thrown = the[GovernmentGatewayException] thrownBy await(result)
      thrown.getMessage mustBe "There was a problem with the admin service"
      verifySubmitData()
    }
  }

  "A user with an AWRS enrolment " must {
    "try to lookup data from ETMP via API5 endpoint" in {
      stubbedGet("/awrs/lookup/0123456", OK, api5LTDJson.toString())
      val result = connector.lookupAWRSData(retrievalsWithAwrsEnrolment)
      await(result) mustBe api5LTDJson
    }

    "return status as BAD_REQUEST, for bad request made for API 5" in {
      stubbedGet("/awrs/lookup/0123456", BAD_REQUEST, api5LTDJson.toString())
      val result = connector.lookupAWRSData(retrievalsWithAwrsEnrolment)
      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage mustBe "The Submission has not passed validation"
    }

    "return status as NOT_FOUND for API 5" in {
      stubbedGet("/awrs/lookup/0123456", NOT_FOUND, api5LTDJson.toString())
      val result = connector.lookupAWRSData(retrievalsWithAwrsEnrolment)
      val thrown = the[NotFoundException] thrownBy await(result)
      thrown.getMessage mustBe "The remote endpoint has indicated that no data can be found"
    }

    "return status as SERVICE_UNAVAILABLE for API 5" in {
      stubbedGet("/awrs/lookup/0123456", SERVICE_UNAVAILABLE, api5LTDJson.toString())
      val result = connector.lookupAWRSData(retrievalsWithAwrsEnrolment)
      val thrown = the[ServiceUnavailableException] thrownBy await(result)
      thrown.getMessage mustBe "Dependant systems are currently not responding"
    }

    "return status INTERNAL_SERVER_ERROR for API 5" in {
      stubbedGet("/awrs/lookup/0123456", INTERNAL_SERVER_ERROR, api5LTDJson.toString())
      val result = connector.lookupAWRSData(retrievalsWithAwrsEnrolment)
      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage mustBe "WSO2 is currently experiencing problems that require live service intervention"
    }
  }

  "AWRSConnector updating an AWRS application" must {

    "return status as OK, for successful subscription" in {
      stubbedPut("/awrs/update/0123456", OK, subscribeApi6SuccessResponseJson.toString())
      val result = connector.updateAWRSData(api6LTDJson, retrievalsWithAwrsEnrolment)
      await(result) mustBe api6SuccessResponse
      verifyUpdateData()
    }

    "return status as BAD_REQUEST, for unsuccessful subscription" in {
      stubbedPut("/awrs/update/0123456", BAD_REQUEST, badRequestResponse.toString())
      val result = connector.updateAWRSData(api6LTDJson, retrievalsWithAwrsEnrolment)
      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage mustBe "[API6] - The Submission has not passed validation"
      verifyUpdateData()
    }

    "return DESValidationException when backend returns BAD_REQUEST, for subscription which failed DES validation" in {
      stubbedPut("/awrs/update/0123456", BAD_REQUEST, subscribeFailureDesValResponse.toString())
      val result = connector.updateAWRSData(api6LTDJson, retrievalsWithAwrsEnrolment)
      val thrown = the[DESValidationException] thrownBy await(result)
      thrown.getMessage mustBe "Validation against schema failed"
      verifyUpdateData()
    }

    "return status as NOT_FOUND, for unsuccessful subscription" in {
      stubbedPut("/awrs/update/0123456", NOT_FOUND, notFoundResponse.toString())
      val result = connector.updateAWRSData(api6LTDJson, retrievalsWithAwrsEnrolment)
      val thrown = the[NotFoundException] thrownBy await(result)
      thrown.getMessage mustBe "[API6] - The remote endpoint has indicated that no data can be found"
      verifyUpdateData()
    }

    "return status as SERVICE_UNAVAILABLE, for unsuccessful subscription" in {
      stubbedPut("/awrs/update/0123456", SERVICE_UNAVAILABLE, serviceUnavailableResponse.toString())
      val result = connector.updateAWRSData(api6LTDJson, retrievalsWithAwrsEnrolment)
      val thrown = the[ServiceUnavailableException] thrownBy await(result)
      thrown.getMessage mustBe "[API6] - Dependant systems are currently not responding"
      verifyUpdateData()
    }

    "return status as INTERNAL_SERVER_ERROR, for unsuccessful subscription" in {
      stubbedPut("/awrs/update/0123456", INTERNAL_SERVER_ERROR, internalServerErrorResponse.toString())
      val result = connector.updateAWRSData(api6LTDJson, retrievalsWithAwrsEnrolment)
      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage must include("[API6] - WSO2 is currently experiencing problems that require live service intervention")
      verifyUpdateData()
    }
  }

  "AWRS withdrawal connector" must {
    import utils.WithdrawalTestUtils._
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

    "return OK on successful return of data" in {
      stubbedPost("/awrs/withdrawal/0123456", OK, withdrawResponseJson.toString())
      val result = connector.withdrawApplication(retrievalsWithAwrsEnrolment, withdrawalJsonToSend)
      await(result) mustBe witdrawalResponse()
      verifyWithdrawal()
    }

    "return status as BAD_REQUEST, for unsuccessful subscription" in {
      stubbedPost("/awrs/withdrawal/0123456", BAD_REQUEST, badRequestResponse.toString())
      val result = connector.withdrawApplication(retrievalsWithAwrsEnrolment, withdrawalJsonToSend)
      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage mustBe "The Submission has not passed validation"
      verifyWithdrawal()
    }

    "return status as NOT_FOUND, for unsuccessful subscription" in {
      stubbedPost("/awrs/withdrawal/0123456", NOT_FOUND, notFoundResponse.toString())
      val result = connector.withdrawApplication(retrievalsWithAwrsEnrolment, withdrawalJsonToSend)
      val thrown = the[NotFoundException] thrownBy await(result)
      thrown.getMessage mustBe "The remote endpoint has indicated that no data can be found"
      verifyWithdrawal()
    }

    "return status as SERVICE_UNAVAILABLE, for unsuccessful subscription" in {
      stubbedPost("/awrs/withdrawal/0123456", SERVICE_UNAVAILABLE, serviceUnavailableResponse.toString())
      val result = connector.withdrawApplication(retrievalsWithAwrsEnrolment, withdrawalJsonToSend)
      val thrown = the[ServiceUnavailableException] thrownBy await(result)
      thrown.getMessage mustBe "Dependant systems are currently not responding"
      verifyWithdrawal()
    }

    "return status as INTERNAL_SERVER_ERROR, for unsuccessful subscription" in {
      stubbedPost("/awrs/withdrawal/0123456", INTERNAL_SERVER_ERROR, internalServerErrorResponse.toString())
      val result = connector.withdrawApplication(retrievalsWithAwrsEnrolment, withdrawalJsonToSend)
      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage must include("WSO2 is currently experiencing problems that require live service intervention")
      verifyWithdrawal()
    }
  }

  "getStatusInfo" must {
    lazy val getStatusInfoURI = (awrsRef: String, contactNumber: String) => s"/awrs/status-info/$awrsRef/$contactNumber"

    // these values doesn't really matter since the call itself is mocked
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    val contactNumber = "0123456789"
    val statusInfoResponseSuccess = StatusInfoType(Some(StatusInfoSuccessResponseType("", "")))
    val statusInfoResponseSuccessJson = StatusInfoType.writter.writes(statusInfoResponseSuccess)
    val awrsRef = "0123456"

    val statusInfoResponseFailure = StatusInfoType(Some(StatusInfoFailureResponseType("failed")))
    val statusInfoResponseFailureJson = StatusInfoType.writter.writes(statusInfoResponseFailure)

    val statusInfoCorruptResponseJson = Json.parse("false")

    def mockResponse(responseStatus: Int, responseData: JsValue): Unit = {
      stubbedGet(getStatusInfoURI(awrsRef, contactNumber), responseStatus, responseData.toString())
    }

    def testCall(implicit  hc: HeaderCarrier, request: Request[AnyContent]) = connector.getStatusInfo(contactNumber, Pending, Some(MindedToReject), retrievalsWithAwrsEnrolment)

    "return a StatusInfoSuccessResponseType when backend returns OK and success data" in {
      mockResponse(OK, statusInfoResponseSuccessJson)
      val result = testCall
      await(result) mustBe statusInfoResponseSuccess
    }

    "return a BadRequestException when backend returns OK and failure data" in {
      mockResponse(OK, statusInfoResponseFailureJson)
      val resultFailure = testCall
      val thrown = the[BadRequestException] thrownBy await(resultFailure)
      thrown.getMessage mustBe "Failure response returned:\nfailed"
    }

    "return a BadRequestException when backend returns OK and invalid json" in {
      mockResponse(OK, statusInfoCorruptResponseJson)
      val resultCorrupt = testCall
      val thrown = the[BadRequestException] thrownBy await(resultCorrupt)
      thrown.getMessage mustBe "Unknown response returned:\nNone"
    }

    "return status BadRequestException, when backend returns BAD_REQUEST" in {
      mockResponse(BAD_REQUEST, statusInfoResponseSuccessJson)
      val result = testCall
      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage mustBe "The Submission has not passed validation"
    }

    "return NotFoundException when backend returns NOT_FOUND" in {
      mockResponse(NOT_FOUND, statusInfoResponseSuccessJson)
      val result = testCall
      val thrown = the[NotFoundException] thrownBy await(result)
      thrown.getMessage mustBe "The remote endpoint has indicated that no data can be found"
    }

    "return status a ServiceUnavailableException when backend returns SERVICE_UNAVAILABLE" in {
      mockResponse(SERVICE_UNAVAILABLE, statusInfoResponseSuccessJson)
      val result = testCall
      val thrown = the[ServiceUnavailableException] thrownBy await(result)
      thrown.getMessage mustBe "Dependant systems are currently not responding"
    }

    "return an InternalServerException when backend returns INTERNAL_SERVER_ERROR" in {
      mockResponse(INTERNAL_SERVER_ERROR, statusInfoResponseSuccessJson)
      val result = testCall
      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage mustBe "WSO2 is currently experiencing problems that require live service intervention"
    }

    "return InternalServerException when an unexpected status is returned from backend" in {
      val otherStatus = 999
      mockResponse(otherStatus, statusInfoResponseSuccessJson)
      val result = testCall
      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage mustBe "Unsuccessful return of data. Status code: 999"
    }
  }

  "deRegistration" must {
    lazy val deRegisterURI = (awrsRef: String) => s"/awrs/de-registration/$awrsRef"

    // these values doesn't really matter since the call itself is mocked
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    val awrsRef = "0123456"
    val deRegistrationResponseSuccess = DeRegistrationType(Some(DeRegistrationSuccessResponseType("")))
    val deRegistrationResponseSuccessJson = DeRegistrationType.writter.writes(deRegistrationResponseSuccess)

    val deRegistrationResponseFailure = DeRegistrationType(Some(DeRegistrationFailureResponseType("")))
    val deRegistrationResponseFailureJson = DeRegistrationType.writter.writes(deRegistrationResponseFailure)

    val deRegistrationCorruptResponseJson = Json.parse("false")

    val deRegistrationRequest = DeRegistration("", "", None)

    def mockResponse(responseStatus: Int, responseData: JsValue): Unit = {
      stubbedPost(deRegisterURI(awrsRef), responseStatus, responseData.toString())
    }

    def testCall(implicit  hc: HeaderCarrier, request: Request[AnyContent]) = connector.deRegistration(deRegistrationRequest, retrievalsWithAwrsEnrolment)

    "return successful de-registration response when backend returns OK and successful de-registration" in {
      mockResponse(OK, deRegistrationResponseSuccessJson)
      val result = testCall
      await(result) mustBe deRegistrationResponseSuccess
    }

    "return failed de-registration response when backend returns OK and failed de-registration" in {
      mockResponse(OK, deRegistrationResponseFailureJson)
      val resultFailure = testCall
      await(resultFailure) mustBe deRegistrationResponseFailure
    }

    "return BadRequestException response when backend returns OK and invalid json" in {
      mockResponse(OK, deRegistrationCorruptResponseJson)
      val resultCorrupt = testCall
      val thrown = the[BadRequestException] thrownBy await(resultCorrupt)
      thrown.getMessage mustBe "Unknown response returned:\nNone"

    }

    "return BadRequestException, when backend returns BAD_REQUEST" in {
      mockResponse(BAD_REQUEST, deRegistrationResponseSuccessJson)
      val result = testCall
      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage mustBe "The Submission has not passed validation"
    }

    "return NotFoundException when backendd returns NOT_FOUND" in {
      mockResponse(NOT_FOUND, deRegistrationResponseSuccessJson)
      val result = testCall
      val thrown = the[NotFoundException] thrownBy await(result)
      thrown.getMessage mustBe "The remote endpoint has indicated that no data can be found"
    }

    "return status ServiceUnavailableException when backend returns SERVICE_UNAVAILABLE" in {
      mockResponse(SERVICE_UNAVAILABLE, deRegistrationResponseSuccessJson)
      val result = testCall
      val thrown = the[ServiceUnavailableException] thrownBy await(result)
      thrown.getMessage mustBe "Dependant systems are currently not responding"
    }

    "return status as InternalServerException when backend returns INTERNAL_SERVER_ERROR" in {
      mockResponse(INTERNAL_SERVER_ERROR, deRegistrationResponseSuccessJson)
      val result = testCall
      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage mustBe "WSO2 is currently experiencing problems that require live service intervention"
    }

    "return status as unexpected status, for unsuccessful de-registration" in {
      val otherStatus = 999
      mockResponse(otherStatus, deRegistrationResponseSuccessJson)
      val result = testCall
      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage mustBe s"Unsuccessful return of data. Status code: $otherStatus"
    }

  }

  "checkUsersEnrolments" must {
    val testSafeID = "safeID-123"
    val testAwrsUsers = AwrsUsers(List("principalUserId-One","principalUserId-Two"), List("delegatedUserId-One"))
    val testResponse = Json.toJson(testAwrsUsers).toString
    implicit val ec = scala.concurrent.ExecutionContext.global

    "return OK when the call to awrs is successful" in {
      stubbedGet(s"/awrs/status-info/users/$testSafeID", OK, testResponse)
      val result = connector.checkUsersEnrolments(testSafeID)(hc,ec)
      await(result) mustBe Some(testAwrsUsers)
    }
    "return None if the call fails" in {
      stubbedGet(s"/awrs/status-info/users/$testSafeID", INTERNAL_SERVER_ERROR, "")
      val result = connector.checkUsersEnrolments(testSafeID)(hc,ec)
      await(result) mustBe None
    }
  }

  "AWRSConnector update group business partner" must {

    val address = BCAddressApi3(addressLine1 = "", addressLine2 = "")
    val updatedDataRequest = new UpdateRegistrationDetailsRequest(false, Some(OrganisationName("testName")), address, ContactDetails(), false, false)
    val safeId = "XE1234"
    val url = s"/0123456/registration-details/$safeId"

    "return SuccessfulUpdateGroupBusinessPartnerResponse when backend responds OK for successful update" in {
      stubbedPut(url, OK, updateGrpPartnerSuccessReponseJson.toString())
      val result = connector.updateGroupBusinessPartner("", "", safeId, updatedDataRequest, retrievalsWithAwrsEnrolment)
      await(result) mustBe api3SucecssResponse
      verifyUpdateGrpPartner()
    }

    "return BadRequestException when backend responds with 400" in {
      stubbedPut(url, BAD_REQUEST, badRequestResponse.toString())
      val result = connector.updateGroupBusinessPartner("", "", safeId, updatedDataRequest, retrievalsWithAwrsEnrolment)
      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage mustBe "[API3 - Update Group Business Partner] - The Submission has not passed validation"
    }

    "return NotFoundException, when backend responds with 404" in {
      stubbedPut(url, NOT_FOUND, notFoundResponse.toString())
      val result = connector.updateGroupBusinessPartner("", "", safeId, updatedDataRequest, retrievalsWithAwrsEnrolment)
      val thrown = the[NotFoundException] thrownBy await(result)
      thrown.getMessage mustBe "[API3 - Update Group Business Partner] - The remote endpoint has indicated that no data can be found"
    }

    "return status as SERVICE_UNAVAILABLE, for unsuccessful update" in {
      stubbedPut(url, SERVICE_UNAVAILABLE, serviceUnavailableResponse.toString())
      val result = connector.updateGroupBusinessPartner("", "", safeId, updatedDataRequest, retrievalsWithAwrsEnrolment)
      val thrown = the[ServiceUnavailableException] thrownBy await(result)
      thrown.getMessage mustBe "[API3 - Update Group Business Partner] - Dependant systems are currently not responding"
    }

    "return status as INTERNAL_SERVER_ERROR, for unsuccessful update" in {
      stubbedPut(url, INTERNAL_SERVER_ERROR, internalServerErrorResponse.toString())
      val result = connector.updateGroupBusinessPartner(testTradingName, "", safeId, updatedDataRequest, retrievalsWithAwrsEnrolment)
      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage must include("[API3 - Update Group Business Partner] - WSO2 is currently experiencing problems that require live service intervention")
    }

    "return status as FORBIDDEN, for unsuccessful update" in {
      stubbedPut(url, FORBIDDEN, forbiddenResponse.toString())
      val result = connector.updateGroupBusinessPartner("", "", safeId, updatedDataRequest, retrievalsWithAwrsEnrolment)
      val thrown = the[ForbiddenException] thrownBy await(result)
      thrown.getMessage must include("[API3 - Update Group Business Partner] - ETMP has returned a error code003 with a status of NOT_OK - record is not editable")
    }
  }

  "Check ETMP call to backend" must {
    val testBCAddress = BCAddress("addressLine1", "addressLine2", Option("addressLine3"), Option("addressLine4"), Option("Ne4 9hs"), Option("country"))
    val testBusinessCustomer = BusinessCustomerDetails("ACME", Some("SOP"), testBCAddress, "sap123", "safe123", false, Some("agent123"))

    def mockResponse(responseStatus: Int, responseData: String = ""): Unit =
      stubbedPost("/regime-etmp-check", responseStatus, responseData)
    def testCall(implicit  hc: HeaderCarrier): Future[Option[SelfHealSubscriptionResponse]] = connector.checkEtmp(testBusinessCustomer, "SOP")

    val checkRegimeModelResponseSuccess = SelfHealSubscriptionResponse("123456")
    val checkRegimeModelResponseSuccessJson = Json.toJson(checkRegimeModelResponseSuccess)

    "Return a SelfHealSubscriptionResponse after receiving an OK response" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())
      mockResponse(OK, checkRegimeModelResponseSuccessJson.toString())
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
      stubbedPost("/regime-etmp-check", OK, "invalid")
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
