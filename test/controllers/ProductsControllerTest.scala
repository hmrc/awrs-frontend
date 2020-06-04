/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.DataCacheKeys._
import services.ServicesUnitTestFixture
import utils.AwrsUnitTestTraits

import scala.concurrent.Future

class ProductsControllerTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture {

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val testProductsController: ProductsController = new ProductsController(
    mockMCC, testSave4LaterService, mockDeEnrolService, mockAuthConnector, mockAuditable, mockAccountUtils, mockAppConfig) {
    override val signInUrl: String = applicationConfig.signIn
  }

  "ProductsController" must {
     "redirect to supplier addresses page when valid data is provided" in {
      continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("mainCustomers[0]" -> "2", "productType[0]" -> "01")) {
        result =>
          redirectLocation(result).get should be("/alcohol-wholesale-scheme/supplier-addresses")
      }
    }
  }

  "Users who entered from the summary edit view" should {
    "return to the summary view after clicking return" in {
      returnWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("mainCustomers[0]" -> "2", "productType[0]" -> "01")) {
        result =>
          redirectLocation(result).get should include(f"/alcohol-wholesale-scheme/view-section/$productsName")
          verifySave4LaterService(saveProducts = 1)
      }
    }
  }

  private def continueWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchSuppliers = None)
    setAuthMocks()
    val result = testProductsController.saveAndContinue().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

  private def returnWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceOnlySaveFunctions()
    setAuthMocks()
    val result = testProductsController.saveAndReturn().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

}
