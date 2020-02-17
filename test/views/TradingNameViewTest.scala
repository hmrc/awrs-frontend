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

package views

import builders.SessionBuilder
import controllers.{TradingDateController, TradingNameController}
import models.{BusinessNameDetails, NewAWBusiness}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ServicesUnitTestFixture
import utils.TestUtil.{testBusinessCustomerDetails, testBusinessDetails, testNewApplicationType}
import utils.{AwrsFieldConfig, AwrsUnitTestTraits}
import views.Configuration.NewApplicationMode

import scala.concurrent.Future

class TradingNameViewTest extends AwrsUnitTestTraits with ServicesUnitTestFixture with AwrsFieldConfig {

  trait Setup {
    val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    val tradingDateController: TradingNameController = new TradingNameController(mockMCC, testSave4LaterService, testKeyStoreService, mockBusinessDetailsService, mockAuthConnector, mockAuditable, mockAccountUtils, mockMainStoreSave4LaterConnector, mockAppConfig) {
      override val signInUrl: String = applicationConfig.signIn
    }
  }

  "the trading name view" should {
    "not display the business name field" when {
      "the business is not a group in edit mode" in new Setup {
        val businessType = "test"

        setAuthMocks()
        setupMockSave4LaterServiceWithOnly(
          fetchBusinessCustomerDetails = testBusinessCustomerDetails(businessType),
          fetchBusinessDetails = testBusinessDetails(),
          fetchNewApplicationType = testNewApplicationType
        )
        when(mockBusinessDetailsService.businessDetailsPageRenderMode(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(NewApplicationMode))
        when(mockMainStoreSave4LaterConnector.fetchData4Later[BusinessNameDetails](ArgumentMatchers.any(), ArgumentMatchers.eq("businessNameDetails"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Option(BusinessNameDetails(Some("test"), None, None))))
        setupMockKeyStoreService(fetchAlreadyTrading = Some(false))

        val result: Future[Result] = tradingDateController.showTradingName(false).apply(SessionBuilder.buildRequestWithSession(userId, "LTD"))
        val document: String = contentAsString(result)

        document shouldNot include(messages("awrs.generic.business_name"))
      }
    }

    "display the business name field" when {
      "the business is a group in edit mode" in new Setup {
        val businessType = "test"

        setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
        setupMockSave4LaterServiceWithOnly(
          fetchBusinessCustomerDetails = testBusinessCustomerDetails(businessType),
          fetchBusinessDetails = testBusinessDetails(),
          fetchNewApplicationType = testNewApplicationType
        )
        when(mockBusinessDetailsService.businessDetailsPageRenderMode(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(NewApplicationMode))
        when(mockMainStoreSave4LaterConnector.fetchData4Later[BusinessNameDetails](ArgumentMatchers.any(), ArgumentMatchers.eq("businessNameDetails"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Option(BusinessNameDetails(Some("test"), None, None))))
        setupMockKeyStoreService(fetchAlreadyTrading = Some(true))

        val result: Future[Result] = tradingDateController.showTradingName(false).apply(SessionBuilder.buildRequestWithSession(userId, "LTD_GRP"))
        val document: String = contentAsString(result)

        document should include(messages("awrs.generic.business_name"))
      }
    }
  }

}
