/*
 * Copyright 2021 HM Revenue & Customs
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
import models._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{CheckEtmpService, ServicesUnitTestFixture}
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment, EnrolmentIdentifier, Enrolments}
import utils.TestConstants._
import utils.{AwrsSessionKeys, AwrsUnitTestTraits, TestUtil}

import scala.concurrent.Future

class BusinessTypeControllerTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture {

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val businessCustomerDetailsFormID = "businessCustomerDetails"
  val formId = "legalEntity"

  lazy val testBusinessType = BusinessType(legalEntity = Option("LTD_GRP"), isSaAccount = None, isOrgAccount = None)
  lazy val testBCAddress = BCAddress("addressLine1", "addressLine2", Option("addressLine3"), Option("addressLine4"), Option(testPostcode), Option("country"))

  lazy val testBusinessCustomerGroup = BusinessCustomerDetails("ACME", Some("SOP"), testBCAddress, "sap123", "safe123", true, Some("agent123"))
  lazy val testBusinessCustomer = BusinessCustomerDetails("ACME", Some("SOP"), testBCAddress, "sap123", "safe123", false, Some("agent123"))
  lazy val testNewApplicationType = NewApplicationType(Some(true))

  lazy val testSubscriptionTypeFrontEnd: SubscriptionTypeFrontEnd = TestUtil.testSubscriptionTypeFrontEnd()

  val testEtmpCheckService: CheckEtmpService = mock[CheckEtmpService]
  val mockTemplate = app.injector.instanceOf[views.html.awrs_business_type]

  val testBusinessTypeController: BusinessTypeController = new BusinessTypeController(
    mockMCC, testAPI5, testSave4LaterService, mockDeEnrolService, mockAuthConnector, mockAuditable, mockAccountUtils, testEtmpCheckService, mockAppConfig, mockTemplate) {
    override val signInUrl: String = applicationConfig.signIn
  }

  "Submitting the Business Type form with " must {

    "redirect to index page when User with Organisation and Sa GGW account selects 'Business Type' as Corporate Body" in {
      continueWithAuthorisedSaOrgUser(FakeRequest().withFormUrlEncodedBody("legalEntity" -> "LTD", "isSaAccount" -> "true", "isOrgAccount" -> "true"), isGroup = false) {
        result =>
          when(testEtmpCheckService.validateBusinessDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(false))
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get mustBe "/alcohol-wholesale-scheme/index"
      }
    }

    "redirect to index page when User with Organisation GGW account selects 'Business Type' as Corporate Body" in {
      continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("legalEntity" -> "LTD", "isSaAccount" -> "false"), isGroup = false) {
        result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get mustBe "/alcohol-wholesale-scheme/index"
      }
    }

    "redirect to index page when User with Individual GGW account selects 'Business Type' as Sole Trader" in {
      continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("legalEntity" -> "SOP", "isSaAccount" -> "true"), isGroup = false) {
        result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get mustBe "/alcohol-wholesale-scheme/index"
      }
    }

    "save form data to Save4Later and redirect to Index page " in {
      continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("legalEntity" -> "LTD_GRP"), isGroup = false) {
        result =>
          status(result) must be(SEE_OTHER)
          verifySave4LaterService(saveBusinessType = 1)
      }
    }

    "isAGroup is true " must {
      "Save and continue must redirect to the Group Declaration " in {
        continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("legalEntity" -> "LTD_GRP"), isGroup = true) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get mustBe "/alcohol-wholesale-scheme/group-declaration"
            verifySave4LaterService(saveBusinessType = 1)
        }
      }
    }
  }

  def assertBusinessName(expected: String)(implicit result: Future[Result]) =
    await(result).session(FakeRequest()).data.getOrElse(AwrsSessionKeys.sessionBusinessName, "") mustBe expected

  def assertBusinessType(expected: String)(implicit result: Future[Result]) =
    await(result).session(FakeRequest()).data.getOrElse(AwrsSessionKeys.sessionBusinessType, "") mustBe expected

  "Session management in BusinessTypeController" must {
    val testBusinessType = "LTD_GRP"
    val validSubmission = FakeRequest().withFormUrlEncodedBody("legalEntity" -> testBusinessType)
    val invalidSubmission = FakeRequest().withFormUrlEncodedBody("legalEntity" -> "")

    "add the correct businessType and businessName to the session for API4 user" in {
      api4User(validSubmission)(testBusinessTypeController.showBusinessType()) {
        implicit result =>
          status(result) mustBe OK
          assertBusinessName(testBusinessCustomer.businessName)
          // must display whatever business type that was in the business customer details fetched
          // from save4later
          assertBusinessType(testBusinessCustomer.businessType.fold("")(x => x))
      }
      api4User(invalidSubmission)(testBusinessTypeController.saveAndContinue) {
        implicit result =>
          status(result) mustBe BAD_REQUEST
          assertBusinessName(testBusinessCustomer.businessName)
          assertBusinessType("") // must not have any business type added since the submission was false
      }
      api4User(validSubmission)(testBusinessTypeController.saveAndContinue) {
        implicit result =>
          status(result) mustBe SEE_OTHER
          assertBusinessName(testBusinessCustomer.businessName)
          assertBusinessType(testBusinessType)
      }
    }

    "add the correct businessType and businessName to the session for API5 user" in {
      api5User(validSubmission)(testBusinessTypeController.showBusinessType(showBusinessType = false)) {
        implicit result =>
          status(result) mustBe SEE_OTHER // this page must be skipped in the api 5 journey
          assertBusinessName(testBusinessCustomer.businessName)
          assertBusinessType(testBusinessCustomer.businessType.fold("")(x => x))
      }
      api5User(invalidSubmission)(testBusinessTypeController.saveAndContinue) {
        implicit result =>
          status(result) mustBe BAD_REQUEST
          assertBusinessName(testBusinessCustomer.businessName)
          assertBusinessType("") // must not have any business type added since the submission was false
      }
      api5User(validSubmission)(testBusinessTypeController.saveAndContinue) {
        implicit result =>
          status(result) mustBe SEE_OTHER
          assertBusinessName(testBusinessCustomer.businessName)
          assertBusinessType(testBusinessType)
      }
    }
  }

  private def continueWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded], isGroup: Boolean)(test: Future[Result] => Any) {
    val testBusCustomer = isGroup match {
      case true => testBusinessCustomerGroup
      case false => testBusinessCustomer
    }
    setupMockSave4LaterService(fetchBusinessCustomerDetails = Future.successful(Some(testBusCustomer)))
    setAuthMocks()
    val result = testBusinessTypeController.saveAndContinue().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

  private def continueWithAuthorisedSaOrgUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded], isGroup: Boolean)(test: Future[Result] => Any) {
    resetAuthConnector()
    val testBusCustomer = isGroup match {
      case true => testBusinessCustomerGroup
      case false => testBusinessCustomer
    }
    setupMockSave4LaterService(fetchBusinessCustomerDetails = Future.successful(Some(testBusCustomer)))
    setAuthMocks()
    val result = testBusinessTypeController.saveAndContinue().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

  private def api4User(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(methodToTest: Action[AnyContent])(test: Future[Result] => Any): Unit = {
    setupMockSave4LaterService(fetchBusinessCustomerDetails = Future.successful(Some(testBusinessCustomer)))
    setAuthMocks(Future.successful(new ~( new ~(Enrolments(Set(Enrolment("IR-CT", Seq(EnrolmentIdentifier("utr", "0123456")), "activated"))), Some(AffinityGroup.Organisation)), Credentials("fakeCredID", "type"))))
    val result = methodToTest.apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

  private def api5User(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(methodToTest: Action[AnyContent])(test: Future[Result] => Any): Unit = {
    resetAuthConnector()
    setupMockSave4LaterService(fetchBusinessCustomerDetails = Future.successful(Some(testBusinessCustomer)))
    setupMockApiSave4LaterService(fetchSubscriptionTypeFrontEnd = Future.successful(Some(testSubscriptionTypeFrontEnd)))
    setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
    when(mockMainStoreSave4LaterConnector.fetchData4Later[NewAWBusiness](ArgumentMatchers.any(), ArgumentMatchers.eq("tradingStartDetails"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(Option(NewAWBusiness("No", None))))
    val result = methodToTest.apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

}
