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

import builders.SessionBuilder
import config.FrontendAuthConnector
import connectors.mock._
import models._
import play.api.mvc.{Action, AnyContent, AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ServicesUnitTestFixture
import utils.{AwrsSessionKeys, AwrsUnitTestTraits, TestUtil}
import utils.TestConstants._


import scala.concurrent.Future

class BusinessTypeControllerTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture {

  val request = FakeRequest()

  val businessCustomerDetailsFormID = "businessCustomerDetails"
  val formId = "legalEntity"

  lazy val testBusinessType = BusinessType(legalEntity = Option("LTD_GRP"), isSaAccount = None, isOrgAccount = None)
  lazy val testBCAddress = BCAddress("addressLine1", "addressLine2", Option("addressLine3"), Option("addressLine4"), Option(testPostcode), Option("country"))

  lazy val testBusinessCustomerGroup = BusinessCustomerDetails("ACME", Some("SOP"), testBCAddress, "sap123", "safe123", true, Some("agent123"))
  lazy val testBusinessCustomer = BusinessCustomerDetails("ACME", Some("SOP"), testBCAddress, "sap123", "safe123", false, Some("agent123"))
  lazy val testNewApplicationType = NewApplicationType(Some(true))

  lazy val testSubscriptionTypeFrontEnd = TestUtil.testSubscriptionTypeFrontEnd()

  object TestBusinessTypeController extends BusinessTypeController {
    override val authConnector = mockAuthConnector
    override val save4LaterService = TestSave4LaterService
    override val api5 = TestAPI5
  }


  "BusinessTypeController" must {
    "use the correct AuthConnector" in {
      BusinessTypeController.authConnector shouldBe FrontendAuthConnector
    }
  }

  "Submitting the Business Type form with " should {

    "redirect to index page when User with Organisation and Sa GGW account selects 'Business Type' as Corporate Body" in {
      continueWithAuthorisedSaOrgUser(FakeRequest().withFormUrlEncodedBody("legalEntity" -> "LTD", "isSaAccount" -> "true", "isOrgAccount" -> "true"), isGroup = false) {
        result =>
          status(result) should be(SEE_OTHER)
          redirectLocation(result).get shouldBe "/alcohol-wholesale-scheme/index"
      }
    }

    "redirect to index page when User with Organisation GGW account selects 'Business Type' as Corporate Body" in {
      continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("legalEntity" -> "LTD", "isSaAccount" -> "false"), isGroup = false) {
        result =>
          status(result) should be(SEE_OTHER)
          redirectLocation(result).get shouldBe "/alcohol-wholesale-scheme/index"
      }
    }

    "redirect to index page when User with Individual GGW account selects 'Business Type' as Sole Trader" in {
      continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("legalEntity" -> "SOP", "isSaAccount" -> "true"), isGroup = false) {
        result =>
          status(result) should be(SEE_OTHER)
          redirectLocation(result).get shouldBe "/alcohol-wholesale-scheme/index"
      }
    }

    "save form data to Save4Later and redirect to Index page " in {
      continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("legalEntity" -> "LTD_GRP"), isGroup = false) {
        result =>
          status(result) should be(SEE_OTHER)
          verifySave4LaterService(saveBusinessType = 1)
      }
    }

    "isAGroup is true " should {
      "Save and continue should redirect to the Group Declaration " in {
        continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("legalEntity" -> "LTD_GRP"), isGroup = true) {
          result =>
            status(result) should be(SEE_OTHER)
            redirectLocation(result).get shouldBe "/alcohol-wholesale-scheme/group-declaration"
            verifySave4LaterService(saveBusinessType = 1)
        }
      }
    }
  }

  def assertBusinessName(expected: String)(implicit result: Future[Result]) =
    await(result).session(FakeRequest()).data.getOrElse(AwrsSessionKeys.sessionBusinessName, "") shouldBe expected

  def assertBusinessType(expected: String)(implicit result: Future[Result]) =
    await(result).session(FakeRequest()).data.getOrElse(AwrsSessionKeys.sessionBusinessType, "") shouldBe expected

  "Session management in BusinessTypeController" should {
    val testBusinessType = "LTD_GRP"
    val validSubmission = FakeRequest().withFormUrlEncodedBody("legalEntity" -> testBusinessType)
    val invalidSubmission = FakeRequest().withFormUrlEncodedBody("legalEntity" -> "")

    "add the correct businessType and businessName to the session for API4 user" in {
      api4User(validSubmission)(TestBusinessTypeController.showBusinessType()) {
        implicit result =>
          status(result) shouldBe OK
          assertBusinessName(testBusinessCustomer.businessName)
          // should display whatever business type that was in the business customer details fetched
          // from save4later
          assertBusinessType(testBusinessCustomer.businessType.fold("")(x => x))
      }
      api4User(invalidSubmission)(TestBusinessTypeController.saveAndContinue) {
        implicit result =>
          status(result) shouldBe BAD_REQUEST
          assertBusinessName(testBusinessCustomer.businessName)
          assertBusinessType("") // should not have any business type added since the submission was false
      }
      api4User(validSubmission)(TestBusinessTypeController.saveAndContinue) {
        implicit result =>
          status(result) shouldBe SEE_OTHER
          assertBusinessName(testBusinessCustomer.businessName)
          assertBusinessType(testBusinessType)
      }
    }

    "add the correct businessType and businessName to the session for API5 user" in {
      api5User(validSubmission)(TestBusinessTypeController.showBusinessType(showBusinessType = false)) {
        implicit result =>
          status(result) shouldBe SEE_OTHER // this page should be skipped in the api 5 journey
          assertBusinessName(testBusinessCustomer.businessName)
          assertBusinessType(testBusinessCustomer.businessType.fold("")(x => x))
      }
      api5User(invalidSubmission)(TestBusinessTypeController.saveAndContinue) {
        implicit result =>
          status(result) shouldBe BAD_REQUEST
          assertBusinessName(testBusinessCustomer.businessName)
          assertBusinessType("") // should not have any business type added since the submission was false
      }
      api5User(validSubmission)(TestBusinessTypeController.saveAndContinue) {
        implicit result =>
          status(result) shouldBe SEE_OTHER
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
    setupMockSave4LaterService(fetchBusinessCustomerDetails = Some(testBusCustomer))
    val result = TestBusinessTypeController.saveAndContinue().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

  private def continueWithAuthorisedSaOrgUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded], isGroup: Boolean)(test: Future[Result] => Any) {
    setUser(SoleTrader)
    val testBusCustomer = isGroup match {
      case true => testBusinessCustomerGroup
      case false => testBusinessCustomer
    }
    setupMockSave4LaterService(fetchBusinessCustomerDetails = Some(testBusCustomer))
    val result = TestBusinessTypeController.saveAndContinue().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

  private def api4User(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(methodToTest: Action[AnyContent])(test: Future[Result] => Any): Unit = {
    setupMockSave4LaterService(fetchBusinessCustomerDetails = Some(testBusinessCustomer))
    val result = methodToTest.apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

  private def api5User(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(methodToTest: Action[AnyContent])(test: Future[Result] => Any): Unit = {
    setUser(hasAwrs = true)
    setupMockSave4LaterService(fetchBusinessCustomerDetails = Some(testBusinessCustomer))
    setupMockApiSave4LaterService(fetchSubscriptionTypeFrontEnd = Some(testSubscriptionTypeFrontEnd))
    val result = methodToTest.apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

}