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

package views

import builders.SessionBuilder
import controllers.TradingDateController
import models.NewAWBusiness
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ServicesUnitTestFixture
import utils.{AwrsFieldConfig, AwrsUnitTestTraits}
import views.Configuration.NewApplicationMode

import scala.concurrent.Future

class TradingDateViewTest extends AwrsUnitTestTraits with ServicesUnitTestFixture with AwrsFieldConfig {

  trait Setup {
    val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    val template = app.injector.instanceOf[views.html.awrs_trading_date]
    val tradingDateController: TradingDateController = new TradingDateController(mockMCC, testSave4LaterService, mockBusinessDetailsService, testKeyStoreService, mockDeEnrolService, mockAuthConnector, mockAuditable, mockAccountUtils, mockMainStoreSave4LaterConnector, mockAppConfig, template) {
      override val signInUrl: String = applicationConfig.signIn
    }
  }

  "the trading date view" must {
    "display content for dates in the future" when {
      "the view is in the future mode" in new Setup {
        setAuthMocks()
        when(mockBusinessDetailsService.businessDetailsPageRenderMode(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(NewApplicationMode))
        when(mockMainStoreSave4LaterConnector.fetchData4Later[NewAWBusiness](ArgumentMatchers.any(), ArgumentMatchers.eq("tradingStartDetails"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Option(NewAWBusiness("Yes", None))))
        setupMockKeyStoreService(fetchAlreadyTrading = Future.successful(Some(false)))

        val result: Future[Result] = tradingDateController.showBusinessDetails(false).apply(SessionBuilder.buildRequestWithSession(userId, "LTD"))
        val document: String = contentAsString(result)

        document must include(messages("awrs.generic.what_date_will_you"))
      }
    }

    "display content for dates in the past" when {
      "the view is in the past mode" in new Setup {
        setAuthMocks()
        when(mockBusinessDetailsService.businessDetailsPageRenderMode(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(NewApplicationMode))
        when(mockMainStoreSave4LaterConnector.fetchData4Later[NewAWBusiness](ArgumentMatchers.any(), ArgumentMatchers.eq("tradingStartDetails"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Option(NewAWBusiness("Yes", None))))
        setupMockKeyStoreService(fetchAlreadyTrading = Future.successful(Some(true)))

        val result: Future[Result] = tradingDateController.showBusinessDetails(false).apply(SessionBuilder.buildRequestWithSession(userId, "LTD"))
        val document: String = contentAsString(result)

        document must include(messages("awrs.generic.what_date_did_you"))
        document must include(messages("awrs.business_details.what_date_did_p_warn"))
      }
    }
  }

}
