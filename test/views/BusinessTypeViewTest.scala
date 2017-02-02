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

package views

import builders.SessionBuilder
import controllers.BusinessTypeController
import models._
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ServicesUnitTestFixture
import utils.TestUtil._
import utils.{AwrsUnitTestTraits, TestUtil}

import scala.concurrent.Future

class BusinessTypeViewTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture {

  val request = FakeRequest()

  val businessCustomerDetailsFormID = "businessCustomerDetails"
  val formId = "legalEntity"

  lazy val testBusinessType = BusinessType(legalEntity = Option("LTD_GRP"), isSaAccount = None, isOrgAccount = None)
  lazy val testBCAddress = BCAddress("addressLine1", "addressLine2", Option("addressLine3"), Option("addressLine4"), Option("Ne4 9hs"), Option("country"))

  lazy val testBusinessCustomerGroup = BusinessCustomerDetails("ACME", Some("SOP"), testBCAddress, "sap123", "safe123", true, Some("agent123"))
  lazy val testBusinessCustomer = BusinessCustomerDetails("ACME", Some("SOP"), testBCAddress, "sap123", "safe123", false, Some("agent123"))
  lazy val testNewApplicationType = NewApplicationType(Some(true))

  lazy val testSubscriptionTypeFrontEnd = TestUtil.testSubscriptionTypeFrontEnd()

  object TestBusinessTypeController extends BusinessTypeController {
    override val authConnector = mockAuthConnector
    override val save4LaterService = TestSave4LaterService
    override val api5 = TestAPI5
  }

  "Submitting the Business Type form with " should {

    "Authenticated and authorised users" should {
      "display validation error when 'Business Type' is not provided" in {
        continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("typeOfBusiness" -> ""), false) {
          result =>
            val document = Jsoup.parse(contentAsString(result))

            val id = "legalEntity"
            val expectedErrorKey = "awrs.business_verification.error.type_of_business_empty"

            testErrorMessageValidation(document, id, expectedErrorKey)
        }
      }

      "display validation error when User with Individual GGW account selects 'Business Type' as Limited Company" in {
        continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("legalEntity" -> "LTD", "isSaAccount" -> "true"), false) {
          result =>
            val document = Jsoup.parse(contentAsString(result))

            val id = "legalEntity"
            val expectedErrorKey = "awrs.business_verification.error.type_of_business_individual_invalid"

            testErrorMessageValidation(document, id, expectedErrorKey)
        }
      }

      "display validation error when User with Organisation GGW account selects 'Business Type' as Sole Trader" in {
        continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("legalEntity" -> "SOP", "isOrgAccount" -> "true"), false) {
          result =>
            val document = Jsoup.parse(contentAsString(result))

            val id = "legalEntity"
            val expectedErrorKey = "awrs.business_verification.error.type_of_business_organisation_invalid"

            testErrorMessageValidation(document, id, expectedErrorKey)
        }
      }

      "isAGroup is true " should {
        "result in only the LTD and LLP radio buttons displayed" in {
          loadDataFromS4LWithAuthorisedUser {
            result =>
              val document = Jsoup.parse(contentAsString(result))
              document.getElementById("legalEntity_field").text() should include(Messages("awrs.business_verification.limited_company"))
              document.getElementById("legalEntity_field").text() should include(Messages("awrs.business_verification.limited_liability_partnership"))
              document.getElementById("legalEntity_field").text() should not include Messages("awrs.business_verification.sole_trader")
              document.getElementById("legalEntity_field").text() should not include Messages("awrs.business_verification.business_partnership")
              document.getElementById("legalEntity_field").text() should not include Messages("awrs.business_verification.limited_partnership")
          }
        }

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

  private def loadDataFromS4LWithAuthorisedUser(test: Future[Result] => Any) {
    setupMockSave4LaterService(
      fetchBusinessType = testBusinessType,
      fetchBusinessCustomerDetails = testBusinessCustomerGroup
    )
    val result = TestBusinessTypeController.showBusinessType().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }
}
