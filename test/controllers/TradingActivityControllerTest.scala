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

class TradingActivityControllerTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture {

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val template = app.injector.instanceOf[views.html.awrs_trading_activity]

  val testTradingActivityController: TradingActivityController = new TradingActivityController(
    mockMCC, testSave4LaterService, mockDeEnrolService, mockAuthConnector, mockAuditable, mockAccountUtils, mockAppConfig, template) {
    override val signInUrl: String = applicationConfig.signIn
  }

  "TradingActivityController" must {
    "redirect to products page when valid data is provided" in {
      continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("wholesalerType[0]" -> "01", "wholesalerType[1]" -> "05", "typeOfAlcoholOrders[0]" -> "01",
        "doesBusinessImportAlcohol" -> "Yes", "otherWholesaler" -> "None", "doYouExportAlcohol" -> "No", "thirdPartyStorage" -> "Yes")) {
        result =>
          redirectLocation(result).get must be("/alcohol-wholesale-scheme/products")
      }
    }
  }

  "Users who entered from the summary edit view" must {
    "return to the summary view after clicking return" in {
      returnWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("wholesalerType[0]" -> "01", "wholesalerType[1]" -> "05", "typeOfAlcoholOrders[0]" -> "01",
        "doesBusinessImportAlcohol" -> "Yes", "otherWholesaler" -> "None", "doYouExportAlcohol" -> "No", "thirdPartyStorage" -> "Yes")) {
        result =>
          redirectLocation(result).get must include(f"/alcohol-wholesale-scheme/view-section/$tradingActivityName")
          verifySave4LaterService(saveTradingActivity = 1)
      }
    }
  }

  private def continueWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchProducts = None)
    setAuthMocks()
    val result = testTradingActivityController.saveAndContinue().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

  private def returnWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceOnlySaveFunctions()
    setAuthMocks()
    val result = testTradingActivityController.saveAndReturn().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

}
