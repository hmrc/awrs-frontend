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

package controllers

import java.util.UUID

import builders.{AuthBuilder, SessionBuilder}
import config.FrontendAuthConnector
import controllers.auth.Utr._
import forms.SupplierAddressesForm
import models.{Address, Supplier, Suppliers}
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ServicesUnitTestFixture
import utils.{AwrsUnitTestTraits, TestUtil}
import services.DataCacheKeys._
import utils.TestUtil._

import scala.concurrent.Future

class SupplierAddressesControllerTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture {

  def testRequest(supplier: Supplier) =
    TestUtil.populateFakeRequest[Supplier](FakeRequest(), SupplierAddressesForm.supplierAddressesValidationForm, supplier)

  val supplierPageURL = (id: Int) => s"/alcohol-wholesale-scheme/supplier-addresses?id=$id"
  val supplierPage2URL = supplierPageURL(2)

  "SupplierAddressesController" must {
    "use the correct AuthConnector" in {
      SupplierAddressesController.authConnector shouldBe FrontendAuthConnector
    }
  }

  val id = "alcoholSupplier"
  "Authenticated and authorised users" should {

    "validate that first addition of supplier does not throw an exception" in {
      continueWithAuthorisedUserFirstTimeSupplier(1, testRequest(testSupplier())) {
        result =>
          status(result) shouldBe 303
      }
    }

    "validate that passing in an invalid id redirects to NOT FOUND error page" in {
      getWithAuthorisedUserSa(6) {
        result =>
          status(result) shouldBe 404
      }
    }

    "redirect to index page when 'user has selected No suppliers'" in {
      continueWithAuthorisedUserNoSuppliersFirstQuestion(testRequest(testSupplierDefault())) {
        result =>
          redirectLocation(result).get should be("/alcohol-wholesale-scheme/index")
          verifySave4LaterService(saveSuppliers = 1)
      }
    }

    "redirect to next supplier page when valid data is provided and 'Additional Suppliers' is Yes" in {
      continueWithAuthorisedUser(testRequest(testSupplierOthersYes())) {
        result =>
          redirectLocation(result).get should include(supplierPage2URL)
          verifySave4LaterService(saveSuppliers = 1)
      }
    }

    "redirect to Supplier addresses page when valid data is provided and 'Additional Suppliers' is Yes" in {
      continueWithAuthorisedUserMultipleSuppliersYesAdditionalSuppliers(testRequest(testSupplierOthersYes())) {
        result =>
          redirectLocation(result).get should include(supplierPageURL(3))
          verifySave4LaterService(saveSuppliers = 1)
      }
    }

    "save form data in Save4Later if the data is valid" in {
      continueWithAuthorisedUser(testRequest(testSupplier())) {
        result =>
          status(result) should be(SEE_OTHER)
          verifySave4LaterService(saveSuppliers = 1)
      }
    }

    "redirect to index page when 'user has selected No additional supplier'" in {
      continueWithAuthorisedUser(testRequest(testSupplier())) {
        result =>
          redirectLocation(result).get should be("/alcohol-wholesale-scheme/index")
          verifySave4LaterService(saveSuppliers = 1)
      }
    }

    "redirect to next additional supplier page when 'user has selected Yes additional supplier'" in {
      continueWithAuthorisedUser(testRequest(testSupplierOthersYes())) {
        result =>
          redirectLocation(result).get should be("/alcohol-wholesale-scheme/supplier-addresses?id=2")
          verifySave4LaterService(saveSuppliers = 1)
      }
    }
  }

  "Users who entered from the summary edit view" should {
    "return to the summary view after clicking return" in {
      returnWithAuthorisedUser(testRequest(testSupplier())) {
        result =>
          redirectLocation(result).get should include(f"/alcohol-wholesale-scheme/view-section/$suppliersName")
          verifySave4LaterService(saveSuppliers = 1)
      }
    }
  }

  "When no data has previously been cached, entering no supplier and pressing save and continue" should {
    "cache the supplier and redirect to the index page" in {
      continueWithAuthorisedUserNoSupplier(testRequest(testSupplierDefault())) {
        result =>
          redirectLocation(result).get should be("/alcohol-wholesale-scheme/index")
      }
    }
  }

  "Entering a valid supplier, selecting add additional and pressing Save and Continue" should {
    "cache the supplier, load a blank Supplier page and increment the supplier number" in {
      continueWithAuthorisedUser(testRequest(testSupplierOthersYes())) {
        result =>
          redirectLocation(result).get should include("/alcohol-wholesale-scheme/supplier-addresses?id=2")
          verifySave4LaterService(saveSuppliers = 1)
      }
    }
  }


  "When loading the page with 5 suppliers already entered we" should {
    "see to the 1st supplier" in {
      getWithAuthorisedUser5Suppliers {
        result =>
          status(result) shouldBe 200
      }
    }
  }

  "Saving supplier with a country code" should {
    "retrieve country" in {
      getWithAuthorisedUserSa() {
        result =>
          val document = Jsoup.parse(contentAsString(result))
          status(result) shouldBe OK

          document.getElementById("supplierAddress.addressCountry").`val`() shouldBe "Spain"
      }
    }
  }

  "When loading the delete page we" should {
    "see to the selected suppliers delete confirmation page" in {
      showDeleteWithAuthorisedUser() {
        result =>
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("deleteConfirmation-heading").text shouldBe Messages("awrs.delete.confirmation_heading", Messages("awrs.view_application.supplier"))
          status(result) shouldBe 200
      }
    }
  }

  "When submitting the delete confirmation page we" should {
    "be routed back to the summary page after confirming Yes" in {
      deleteWithAuthorisedUser()(deleteConfirmation_Yes) {
        result =>
          status(result) shouldBe 303
          redirectLocation(result).get should include("/alcohol-wholesale-scheme/view-section/suppliers")
          verifySave4LaterService(saveSuppliers = 1)
      }
    }
    "be routed back to the summary page after confirming No" in {
      deleteWithAuthorisedUser()(deleteConfirmation_No) {
        result =>
          status(result) shouldBe 303
          redirectLocation(result).get should include("/alcohol-wholesale-scheme/view-section/suppliers")
          verifySave4LaterService(saveSuppliers = 0)
      }
    }
    "be shown an error if nothing is selected" in {
      deleteWithAuthorisedUser()(deleteConfirmation_None) {
        result =>
          status(result) shouldBe 400
      }
    }
    "be routed back to the summary page after confirming Yes for a record that is not the first" in {
      deleteWithAuthorisedUser(id = 2, suppliers = Suppliers(List(testSupplier(), testSupplier(), testSupplier())))(deleteConfirmation_Yes) {
        result =>
          status(result) shouldBe 303
          redirectLocation(result).get should include("/alcohol-wholesale-scheme/view-section/suppliers")
          verifySave4LaterService(saveSuppliers = 1)
      }
    }
  }

  private def getWithAuthorisedUserSa(id: Int = 1)(test: Future[Result] => Any): Future[Any] = {
    setupMockSave4LaterServiceWithOnly(fetchSuppliers = testSuppliersInternational)
    setAuthMocks()
    val result = TestSupplierAddressesController.showSupplierAddressesPage(id, isLinearMode = true, isNewRecord = true).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def getWithAuthorisedUser5Suppliers(test: Future[Result] => Any): Future[Any] = {
    setupMockSave4LaterServiceWithOnly(fetchSuppliers = testSuppliers)
    setAuthMocks()
    val result = TestSupplierAddressesController.showSupplierAddressesPage(1, isLinearMode = true, isNewRecord = true).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def continueWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchSuppliers = testSuppliers)
    setAuthMocks()
    val result = TestSupplierAddressesController.saveAndContinue(1, true).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

  private def returnWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchSuppliers = testSuppliers)
    setAuthMocks()
    val result = TestSupplierAddressesController.saveAndReturn(1, true).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

  private def continueWithAuthorisedUserNoSuppliersFirstQuestion(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceOnlySaveFunctions()
    setAuthMocks()
    val result = TestSupplierAddressesController.saveAndContinue(1, true).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

  private def continueWithAuthorisedUserMultipleSuppliersYesAdditionalSuppliers(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchSuppliers = Suppliers(List(testSupplier(), testSupplier(), testSupplier())))
    setAuthMocks()
    val result = TestSupplierAddressesController.saveAndContinue(2, true).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

  private def continueWithAuthorisedUserNoSupplier(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchSuppliers = None)
    setAuthMocks()
    val result = TestSupplierAddressesController.saveAndContinue(1, true).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

  private def showDeleteWithAuthorisedUser(id: Int = 1, suppliers: Suppliers = testSuppliers)(test: Future[Result] => Any): Future[Any] = {
    setupMockSave4LaterServiceWithOnly(fetchSuppliers = suppliers)
    setAuthMocks()
    val result = TestSupplierAddressesController.showDelete(id).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def deleteWithAuthorisedUser(id: Int = 1, suppliers: Suppliers = testSuppliers)(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchSuppliers = suppliers)
    setAuthMocks()
    val result = TestSupplierAddressesController.actionDelete(id).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

  private def continueWithAuthorisedUserFirstTimeSupplier(id: Int, fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchSuppliers = None)
    setAuthMocks()
    val result = TestSupplierAddressesController.saveAndContinue(id, true).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

  object TestSupplierAddressesController extends SupplierAddressesController {
    override val authConnector = mockAuthConnector
    override val save4LaterService = TestSave4LaterService
    val signInUrl = "/sign-in"
  }

}
