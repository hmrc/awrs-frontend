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
import config.ApplicationConfig
import forms.SupplierAddressesForm
import models.{Supplier, Suppliers}
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.DataCacheKeys._
import services.ServicesUnitTestFixture
import utils.TestUtil._
import utils.{AwrsUnitTestTraits, TestUtil}
import views.html.awrs_supplier_addresses

import scala.concurrent.Future

class SupplierAddressesControllerTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture {

  implicit val mockConfig: ApplicationConfig = mockAppConfig
  val template: awrs_supplier_addresses = app.injector.instanceOf[views.html.awrs_supplier_addresses]

  def testRequest(supplier: Supplier): FakeRequest[AnyContentAsFormUrlEncoded] =
    TestUtil.populateFakeRequest[Supplier](FakeRequest(), SupplierAddressesForm.supplierAddressesValidationForm, supplier)

  val supplierPageURL: Int => String = (id: Int) => s"/alcohol-wholesale-scheme/supplier-addresses?id=$id"
  val supplierPage2URL: String = supplierPageURL(2)

  val testSupplierAddressesController: SupplierAddressesController = new SupplierAddressesController(
    mockMCC, testSave4LaterService, mockDeEnrolService, mockAuthConnector, mockAuditable, mockAccountUtils, mockCountryCodes, mockAppConfig, template) {
    override val signInUrl: String = "/sign-in"
    override val countryList: Seq[(String, String)] = Seq()
  }

  val id = "alcoholSupplier"
  "processCountryNonUK" must {
    "do nothing with a non-uk supplier with non-uk address" in {
      val processedSupplier: Supplier = testSupplierAddressesController.processCountryNonUK(testSupplierDefault())

      processedSupplier mustBe testSupplierDefault()
    }

    "alter a uk supplier with non-uk address (country)" in {
      val processedSupplier: Supplier = testSupplierAddressesController.processCountryNonUK(testSupplierMismatch)

      processedSupplier.supplierAddress.get.addressLine1 mustBe testSupplierMismatch.supplierAddress.get.addressLine1
      processedSupplier.supplierAddress.get.addressCountryCode mustBe None
      processedSupplier.supplierAddress.get.addressCountry mustBe None
    }
  }

  "Authenticated and authorised users" must {

    "validate that first addition of supplier does not throw an exception" in {
      continueWithAuthorisedUserFirstTimeSupplier(1, testRequest(testSupplier())) {
        result =>
          status(result) mustBe 303
      }
    }

    "validate that passing in an invalid id redirects to NOT FOUND error page" in {
      getWithAuthorisedUserSa(6) {
        result =>
          status(result) mustBe 404
      }
    }

    "redirect to index page when 'user has selected No suppliers'" in {
      continueWithAuthorisedUserNoSuppliersFirstQuestion(testRequest(testSupplierDefault())) {
        result =>
          redirectLocation(result).get must be("/alcohol-wholesale-scheme/index")
          verifySave4LaterService(saveSuppliers = 1)
      }
    }

    "redirect to index page when 'user has selected No suppliers' and account for a mismatch" in {
      continueWithAuthorisedUserNoSupplier(testRequest(testSupplierMismatch)) {
        result =>
          redirectLocation(result).get must include(supplierPage2URL)
          verifySave4LaterService(saveSuppliers = 1)
      }
    }

    "redirect to next supplier page when valid data is provided and 'Additional Suppliers' is Yes" in {
      continueWithAuthorisedUser(testRequest(testSupplierOthersYes())) {
        result =>
          redirectLocation(result).get must include(supplierPage2URL)
          verifySave4LaterService(saveSuppliers = 1)
      }
    }

    "redirect to Supplier addresses page when valid data is provided and 'Additional Suppliers' is Yes" in {
      continueWithAuthorisedUserMultipleSuppliersYesAdditionalSuppliers(testRequest(testSupplierOthersYes())) {
        result =>
          redirectLocation(result).get must include(supplierPageURL(3))
          verifySave4LaterService(saveSuppliers = 1)
      }
    }

    "save form data in Save4Later if the data is valid" in {
      continueWithAuthorisedUser(testRequest(testSupplier())) {
        result =>
          status(result) must be(SEE_OTHER)
          verifySave4LaterService(saveSuppliers = 1)
      }
    }

    "redirect to index page when 'user has selected No additional supplier'" in {
      continueWithAuthorisedUser(testRequest(testSupplier())) {
        result =>
          redirectLocation(result).get must be("/alcohol-wholesale-scheme/index")
          verifySave4LaterService(saveSuppliers = 1)
      }
    }

    "redirect to next additional supplier page when 'user has selected Yes additional supplier'" in {
      continueWithAuthorisedUser(testRequest(testSupplierOthersYes())) {
        result =>
          redirectLocation(result).get must be("/alcohol-wholesale-scheme/supplier-addresses?id=2")
          verifySave4LaterService(saveSuppliers = 1)
      }
    }
  }

  "Users who entered from the summary edit view" must {
    "return to the summary view after clicking return" in {
      returnWithAuthorisedUser(testRequest(testSupplier())) {
        result =>
          redirectLocation(result).get must include(f"/alcohol-wholesale-scheme/view-section/$suppliersName")
          verifySave4LaterService(saveSuppliers = 1)
      }
    }
  }

  "When no data has previously been cached, entering no supplier and pressing save and continue" must {
    "cache the supplier and redirect to the index page" in {
      continueWithAuthorisedUserNoSupplier(testRequest(testSupplierDefault())) {
        result =>
          redirectLocation(result).get must be("/alcohol-wholesale-scheme/index")
      }
    }
  }

  "Entering a valid supplier, selecting add additional and pressing Save and Continue" must {
    "cache the supplier, load a blank Supplier page and increment the supplier number" in {
      continueWithAuthorisedUser(testRequest(testSupplierOthersYes())) {
        result =>
          redirectLocation(result).get must include("/alcohol-wholesale-scheme/supplier-addresses?id=2")
          verifySave4LaterService(saveSuppliers = 1)
      }
    }
  }

  "When loading the page with 5 suppliers already entered we" must {
    "see to the 1st supplier" in {
      getWithAuthorisedUser5Suppliers {
        result =>
          status(result) mustBe 200
      }
    }
  }

  "Saving supplier with a country code" must {
    "retrieve country" in {
      getWithAuthorisedUserSa() {
        result =>
          Jsoup.parse(contentAsString(result))
          status(result) mustBe OK
      }
    }
  }

  "When loading the delete page we" must {
    "see to the selected suppliers delete confirmation page" in {
      showDeleteWithAuthorisedUser() {
        result =>
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("deleteConfirmation-heading").text mustBe Messages("awrs.delete.confirmation_heading", Messages("awrs.view_application.supplier"))
          status(result) mustBe 200
      }
    }
  }

  "When submitting the delete confirmation page we" must {
    "be routed back to the summary page after confirming Yes" in {
      deleteWithAuthorisedUser()(deleteConfirmation_Yes) {
        result =>
          status(result) mustBe 303
          redirectLocation(result).get must include("/view-section/suppliers")
          verifySave4LaterService(saveSuppliers = 1)
      }
    }
    "be routed back to the summary page after confirming No" in {
      deleteWithAuthorisedUser()(deleteConfirmation_No) {
        result =>
          status(result) mustBe 303
          redirectLocation(result).get must include("/view-section/suppliers")
          verifySave4LaterService(saveSuppliers = 0)
      }
    }
    "be shown an error if nothing is selected" in {
      deleteWithAuthorisedUser()(deleteConfirmation_None) {
        result =>
          status(result) mustBe 400
      }
    }
    "be routed back to the summary page after confirming Yes for a record that is not the first" in {
      deleteWithAuthorisedUser(id = 2, suppliers = Suppliers(List(testSupplier(), testSupplier(), testSupplier())))(deleteConfirmation_Yes) {
        result =>
          status(result) mustBe 303
          redirectLocation(result).get must include("/view-section/suppliers")
          verifySave4LaterService(saveSuppliers = 1)
      }
    }
  }

  private def getWithAuthorisedUserSa(id: Int = 1)(test: Future[Result] => Any): Future[Any] = {
    setupMockSave4LaterServiceWithOnly(fetchSuppliers = testSuppliersInternational)
    setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
    val result = testSupplierAddressesController.showSupplierAddressesPage(id, isLinearMode = true, isNewRecord = true).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def getWithAuthorisedUser5Suppliers(test: Future[Result] => Any): Future[Any] = {
    setupMockSave4LaterServiceWithOnly(fetchSuppliers = testSuppliers)
    setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
    val result = testSupplierAddressesController.showSupplierAddressesPage(1, isLinearMode = true, isNewRecord = true).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def continueWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchSuppliers = testSuppliers)
    setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
    val result = testSupplierAddressesController.saveAndContinue(1, true).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

  private def returnWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchSuppliers = testSuppliers)
    setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
    val result = testSupplierAddressesController.saveAndReturn(1, true).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

  private def continueWithAuthorisedUserNoSuppliersFirstQuestion(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceOnlySaveFunctions()
    setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
    val result = testSupplierAddressesController.saveAndContinue(1, true).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

  private def continueWithAuthorisedUserMultipleSuppliersYesAdditionalSuppliers(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchSuppliers = Suppliers(List(testSupplier(), testSupplier(), testSupplier())))
    setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
    val result = testSupplierAddressesController.saveAndContinue(2, true).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

  private def continueWithAuthorisedUserNoSupplier(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchSuppliers = None)
    setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
    val result = testSupplierAddressesController.saveAndContinue(1, true).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

  private def showDeleteWithAuthorisedUser(id: Int = 1, suppliers: Suppliers = testSuppliers)(test: Future[Result] => Any): Future[Any] = {
    setupMockSave4LaterServiceWithOnly(fetchSuppliers = suppliers)
    setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
    val result = testSupplierAddressesController.showDelete(id).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def deleteWithAuthorisedUser(id: Int = 1, suppliers: Suppliers = testSuppliers)(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchSuppliers = suppliers)
    setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
    val result = testSupplierAddressesController.actionDelete(id).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId).withMethod("POST"))
    test(result)
  }

  private def continueWithAuthorisedUserFirstTimeSupplier(id: Int, fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchSuppliers = None)
    setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
    val result = testSupplierAddressesController.saveAndContinue(id, true).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }
}
