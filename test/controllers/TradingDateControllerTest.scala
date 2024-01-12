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
import forms.TradingDateForm
import models._

import java.time.LocalDate
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ServicesUnitTestFixture
import utils.TestUtil._
import utils.{AwrsUnitTestTraits, TestUtil}
import views.Configuration.NewApplicationMode
import views.html.awrs_trading_date

import scala.concurrent.Future

class TradingDateControllerTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture {

  val newBusinessName = "Changed"

  def testRequest(answer: TupleDate, past: Boolean): FakeRequest[AnyContentAsFormUrlEncoded] =
    TestUtil.populateFakeRequest[TupleDate](FakeRequest(), TradingDateForm.tradingDateForm(past, Some(true)), answer)

  val template: awrs_trading_date = app.injector.instanceOf[views.html.awrs_trading_date]

  val tradingDateController: TradingDateController =
    new TradingDateController(mockMCC, testSave4LaterService, mockBusinessDetailsService, testKeyStoreService,
      mockDeEnrolService, mockAuthConnector, mockAuditable, mockAccountUtils, mockMainStoreSave4LaterConnector, mockAppConfig, template) {
    override val signInUrl = "/sign-in"
  }

  override def beforeEach(): Unit = {
    reset(mockAccountUtils)

    super.beforeEach()
  }

  def getExtendedBusinessDetails(updatedBusinessName: Boolean) : ExtendedBusinessDetails = {
    if (updatedBusinessName) {
      testExtendedBusinessDetails(businessName = newBusinessName)
    } else {
      testExtendedBusinessDetails()
    }
  }

  "showBusinessDetails" must {
    "show the business details page" when {
      "a user is logged in" in {
        val businessType = "test"

        setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
        setupMockSave4LaterServiceWithOnly(
          fetchBusinessCustomerDetails = testBusinessCustomerDetails(businessType),
          fetchBusinessDetails = testBusinessDetails(),
          fetchNewApplicationType = testNewApplicationType
        )
        setupMockKeyStoreService(fetchAlreadyTrading = Future.successful(Some(true)))
        when(mockMainStoreSave4LaterConnector.fetchData4Later[NewAWBusiness](ArgumentMatchers.any(), ArgumentMatchers.eq("tradingStartDetails"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Option(NewAWBusiness("Yes", None))))

        when(mockBusinessDetailsService.businessDetailsPageRenderMode(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(NewApplicationMode))

        val res = tradingDateController.showBusinessDetails(false)
          .apply(SessionBuilder.buildRequestWithSession(userId, businessType))

        status(res) mustBe 200
      }
    }
  }

  "save" must {
    "save the trading date" when {
      "provided with a date for the past before 31 March 2016" in {
        val businessType = "test"
        val fakeRequest = testRequest(TupleDate("20", "2", "2016"), true)

        setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
        setupMockSave4LaterServiceWithOnly(
          fetchBusinessCustomerDetails = testBusinessCustomerDetails(businessType),
          fetchBusinessDetails = testBusinessDetails(),
          fetchNewApplicationType = testNewApplicationType
        )
        setupMockKeyStoreService(fetchAlreadyTrading = Future.successful(Some(true)))
        when(mockMainStoreSave4LaterConnector.fetchData4Later[NewAWBusiness](ArgumentMatchers.any(), ArgumentMatchers.eq("tradingStartDetails"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Option(NewAWBusiness("Yes", None))))

        when(mockBusinessDetailsService.businessDetailsPageRenderMode(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(NewApplicationMode))

        val res = tradingDateController.saveAndReturn()
          .apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, businessType).withMethod("POST"))

        status(res) mustBe 303
        redirectLocation(res).get must include("/alcohol-wholesale-scheme/view-section/businessDetails")
      }


      "provided with a date for the past after 31 March 2016" in {
        val businessType = "test"
        val fakeRequest = testRequest(TupleDate("20", "12", "2016"), true)

        setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
        setupMockSave4LaterServiceWithOnly(
          fetchBusinessCustomerDetails = testBusinessCustomerDetails(businessType),
          fetchBusinessDetails = testBusinessDetails(),
          fetchNewApplicationType = testNewApplicationType
        )
        setupMockKeyStoreService(fetchAlreadyTrading = Future.successful(Some(true)))
        when(mockMainStoreSave4LaterConnector.fetchData4Later[NewAWBusiness](ArgumentMatchers.any(), ArgumentMatchers.eq("tradingStartDetails"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Option(NewAWBusiness("No", None))))

        when(mockBusinessDetailsService.businessDetailsPageRenderMode(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(NewApplicationMode))

        val res = tradingDateController.saveAndReturn()
          .apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, businessType).withMethod("POST"))

        status(res) mustBe 303
        redirectLocation(res).get must include("/alcohol-wholesale-scheme/view-section/businessDetails")
      }

      "provided with a date for the future" when {
        "when trading has already started" in {
          val businessType = "test"
          val dateFromNow = LocalDate.now()
          val fakeRequest = testRequest(TupleDate(
            dateFromNow.getDayOfMonth.toString,
            dateFromNow.getMonth.getValue.toString,
            dateFromNow.minusYears(1).getYear.toString
          ), false)

          setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
          setupMockSave4LaterServiceWithOnly(
            fetchBusinessCustomerDetails = testBusinessCustomerDetails(businessType),
            fetchBusinessDetails = testBusinessDetails(),
            fetchNewApplicationType = testNewApplicationType
          )
          setupMockKeyStoreService(fetchAlreadyTrading = Future.successful(Some(true)))
          when(mockMainStoreSave4LaterConnector.fetchData4Later[NewAWBusiness](ArgumentMatchers.any(), ArgumentMatchers.eq("tradingStartDetails"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(Option(NewAWBusiness("No", None))))

          when(mockBusinessDetailsService.businessDetailsPageRenderMode(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(NewApplicationMode))

          val res = tradingDateController.saveAndReturn()
            .apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, businessType).withMethod("POST"))

          status(res) mustBe 303
          redirectLocation(res).get must include("/alcohol-wholesale-scheme/view-section/businessDetails")
        }

        "when trading hasn't started" in {
          val businessType = "test"
          val dateFromNow = LocalDate.now().plusMonths(3)
          val fakeRequest = testRequest(TupleDate(
            dateFromNow.getDayOfMonth.toString,
            dateFromNow.getMonth.getValue.toString,
            dateFromNow.getYear.toString
          ), false)

          setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
          setupMockSave4LaterServiceWithOnly(
            fetchBusinessCustomerDetails = testBusinessCustomerDetails(businessType),
            fetchBusinessDetails = testBusinessDetails(),
            fetchNewApplicationType = testNewApplicationType
          )
          setupMockKeyStoreService(fetchAlreadyTrading = Future.successful(Some(false)))
          when(mockMainStoreSave4LaterConnector.fetchData4Later[NewAWBusiness](ArgumentMatchers.any(), ArgumentMatchers.eq("tradingStartDetails"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(Option(NewAWBusiness("No", None))))

          when(mockBusinessDetailsService.businessDetailsPageRenderMode(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(NewApplicationMode))

          val res = tradingDateController.saveAndReturn()
            .apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, businessType).withMethod("POST"))

          status(res) mustBe 303
          redirectLocation(res).get must include("/alcohol-wholesale-scheme/view-section/businessDetails")
        }
      }
    }
  }
}
