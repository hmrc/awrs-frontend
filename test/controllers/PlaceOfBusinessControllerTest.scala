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

package controllers

import builders.SessionBuilder
import forms.PlaceOfBusinessForm
import models._
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.DataCacheKeys._
import services.ServicesUnitTestFixture
import utils.TestUtil._
import utils.{AwrsUnitTestTraits, TestUtil}

import scala.concurrent.Future

class PlaceOfBusinessControllerTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture {

  def testRequest(pob: PlaceOfBusiness): FakeRequest[AnyContentAsFormUrlEncoded] =
    TestUtil.populateFakeRequest[PlaceOfBusiness](FakeRequest(), PlaceOfBusinessForm.placeOfBusinessValidationForm(mockAppConfig), pob)

  val mockTemplate = app.injector.instanceOf[views.html.awrs_principal_place_of_business]

  val testPlaceOfBusinessController: PlaceOfBusinessController =
    new PlaceOfBusinessController(mockMCC, testSave4LaterService, testKeyStoreService, mockAuthConnector, mockDeEnrolService, mockAuditable, mockAccountUtils, mockAppConfig, mockTemplate) {
    override val signInUrl: String = applicationConfig.signIn
  }

  "BusinessRegistrationDetailsController" must {

    "Display the Place of Business page" when {
      "PlaceOfBusiness is fetched from save4Later" in {
        getWithAuthorisedUser(placeOfBusiness = testPlaceOfBusinessDefault(placeOfBusinessAddressLast3Years = testAddress(addressLine1 = Some("address line 21"), postcode = "NE28 8ES")), None) {
          result =>
            status(result) mustBe OK
        }
      }

      "BusinessCustomerDetails are fetched from save4Later" in {
        getWithAuthorisedUser(None, testBusinessCustomerDetails("LTD_GRP")) {
          result =>
            status(result) mustBe OK
        }
      }
    }

    "Display an error page if neither PlaceOfBusiness or BusinessCustomerDetails can be retrieved from save4Later" in {
      getWithAuthorisedUser(None, None) {
        result =>
          status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

      "Users who entered from the summary edit view" must {
      "return to the summary view after clicking return when valid form data submitted" in {
        returnWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody(
          "mainPlaceOfBusiness" -> "No",
          "mainAddress.addressLine1" -> "address Line1",
          "mainAddress.addressLine2" -> "address Line2",
          "mainAddress.addressLine3" -> "address Line3",
          "mainAddress.addressLine4" -> "address Line4",
          "mainAddress.postcode" -> "NE28 8ER",
          "placeOfBusinessLast3Years" -> "No",
          "placeOfBusinessAddressLast3Years.addressLine1" -> "address Line 21",
          "placeOfBusinessAddressLast3Years.addressLine2" -> "address Line2",
          "placeOfBusinessAddressLast3Years.addressLine3" -> "address Line3",
          "placeOfBusinessAddressLast3Years.addressLine4" -> "address Line4",
          "placeOfBusinessAddressLast3Years.postcode" -> "NE28 8ES",
          "operatingDuration" -> "Less than 2 years"),
          "LTD_GRP") {
          result =>
            redirectLocation(result).get must include (f"/alcohol-wholesale-scheme/view-section/$placeOfBusinessName")
            status(result) mustBe SEE_OTHER
            verifySave4LaterService(savePlaceOfBusiness = 1)
            verifyKeyStoreService(saveBusinessCustomerAddress = 1)
        }
      }

      "be presented with the Place of Business page with errors, and a 400 status when invalid form data submitted" in {
        returnWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody(
          "mainPlaceOfBusiness" -> "No",
          "mainAddress.addressLine1" -> "",
          "mainAddress.addressLine2" -> "address Line2",
          "mainAddress.addressLine3" -> "address Line3",
          "mainAddress.addressLine4" -> "address Line4",
          "mainAddress.postcode" -> "NE28 8ER",
          "placeOfBusinessLast3Years" -> "No",
          "placeOfBusinessAddressLast3Years.addressLine1" -> "address Line 21",
          "placeOfBusinessAddressLast3Years.addressLine2" -> "address Line2",
          "placeOfBusinessAddressLast3Years.addressLine3" -> "address Line3",
          "placeOfBusinessAddressLast3Years.addressLine4" -> "address Line4",
          "placeOfBusinessAddressLast3Years.postcode" -> "NE28 8ES",
          "operatingDuration" -> "Less than 2 years"),
          "LTD_GRP") {
          result =>
            status(result) mustBe BAD_REQUEST
            verifySave4LaterService(savePlaceOfBusiness = 0)
            verifyKeyStoreService(saveBusinessCustomerAddress = 0)
        }
      }
    }

    "Users who entered from the linear view" must {
      "continue to the next section after submitting the form" in {
        continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody(
          "mainPlaceOfBusiness" -> "No",
          "mainAddress.addressLine1" -> "address Line1",
          "mainAddress.addressLine2" -> "address Line2",
          "mainAddress.addressLine3" -> "address Line3",
          "mainAddress.addressLine4" -> "address Line4",
          "mainAddress.postcode" -> "NE28 8ER",
          "placeOfBusinessLast3Years" -> "No",
          "placeOfBusinessAddressLast3Years.addressLine1" -> "address Line 21",
          "placeOfBusinessAddressLast3Years.addressLine2" -> "address Line2",
          "placeOfBusinessAddressLast3Years.addressLine3" -> "address Line3",
          "placeOfBusinessAddressLast3Years.addressLine4" -> "address Line4",
          "placeOfBusinessAddressLast3Years.postcode" -> "NE28 8ES",
          "operatingDuration" -> "Less than 2 years"),
          "LTD_GRP") {
          result =>
            redirectLocation(result).get must include (f"/alcohol-wholesale-scheme/business-contacts")
            status(result) mustBe SEE_OTHER
            verifySave4LaterService(savePlaceOfBusiness = 1)
            verifyKeyStoreService(saveBusinessCustomerAddress = 1)
        }
      }
    }

    def getWithAuthorisedUser(placeOfBusiness: Option[PlaceOfBusiness], businessCustomerDetails: Option[BusinessCustomerDetails], isLinearMode: Boolean = true)(test: Future[Result] => Any): Future[Any] = {
      setupMockSave4LaterServiceWithOnly(fetchPlaceOfBusiness = placeOfBusiness, fetchBusinessCustomerDetails = businessCustomerDetails)
      setAuthMocks()
      val result = testPlaceOfBusinessController.showPlaceOfBusiness(isLinearMode = true).apply(SessionBuilder.buildRequestWithSession(userId, "LTD_GRP"))
      test(result)
    }

    def returnWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded], legalEntity: String)(test: Future[Result] => Any) {
      setupMockKeyStoreServiceOnlySaveFunctions()
      setupMockSave4LaterServiceWithOnly(
        fetchPlaceOfBusiness = testPlaceOfBusinessDefault(placeOfBusinessAddressLast3Years = testAddress(addressLine1 = Some("address line 21"), postcode = "NE28 8ES"))
      )
      setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
      val result = testPlaceOfBusinessController.saveAndReturn(id = 1, isNewRecord = true).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, testBusinessCustomerDetails(legalEntity).businessType.get))
      test(result)
    }

    def continueWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded], legalEntity: String)(test: Future[Result] => Any) {
      setupMockKeyStoreServiceOnlySaveFunctions()
      setupMockSave4LaterServiceWithOnly(
        fetchPlaceOfBusiness = testPlaceOfBusinessDefault(placeOfBusinessAddressLast3Years = testAddress(addressLine1 = Some("address line 21"), postcode = "NE28 8ES"))
      )
      setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
      val result = testPlaceOfBusinessController.saveAndContinue(id = 1, isNewRecord = true).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, testBusinessCustomerDetails(legalEntity).businessType.get))
      test(result)
    }

  }
}
