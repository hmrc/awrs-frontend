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

///*
// * Copyright 2021 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package controllers
//
//import builders.SessionBuilder
//import forms.BusinessDetailsForm
//import models._
//import org.mockito.ArgumentMatchers
//import org.mockito.Mockito._
//import play.api.mvc.AnyContentAsFormUrlEncoded
//import play.api.test.FakeRequest
//import play.api.test.Helpers._
//import services.ServicesUnitTestFixture
//import utils.TestUtil._
//import utils.{AwrsUnitTestTraits, TestUtil}
//import views.Configuration.{NewApplicationMode, ReturnedApplicationMode}
//
//import scala.concurrent.Future
//
//class TradingNameControllerTest extends AwrsUnitTestTraits
//  with ServicesUnitTestFixture {
//
//  val newBusinessName = "Changed"
//
//  def testRequest(extendedBusinessDetails: ExtendedBusinessDetails, entityType: String, hasAwrs: Boolean): FakeRequest[AnyContentAsFormUrlEncoded] =
//    TestUtil.populateFakeRequest[ExtendedBusinessDetails](FakeRequest(), BusinessDetailsForm.businessDetailsValidationForm(entityType, hasAwrs), extendedBusinessDetails)
//
//  val template = app.injector.instanceOf[views.html.awrs_trading_name]
//
//  val testTradingNameController: TradingNameController =
//    new TradingNameController(mockMCC, testSave4LaterService, testKeyStoreService, mockBusinessDetailsService,
//      mockDeEnrolService, mockAuthConnector, mockAuditable, mockAccountUtils, mockMainStoreSave4LaterConnector, mockAppConfig, template) {
//    override val signInUrl = "/sign-in"
//  }
//
//  override def beforeEach(): Unit = {
//    reset(mockAccountUtils)
//
//    super.beforeEach()
//  }
//
//  def getExtendedBusinessDetails(updatedBusinessName: Boolean) : ExtendedBusinessDetails = {
//    if (updatedBusinessName) {
//      testExtendedBusinessDetails(businessName = newBusinessName)
//    } else {
//      testExtendedBusinessDetails()
//    }
//  }
//
//  "showBusinessDetails" must {
//    "show the business details page" when {
//      "a user is logged in" in {
//        val businessType = "test"
//
//        setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
//        setupMockSave4LaterServiceWithOnly(
//          fetchBusinessCustomerDetails = testBusinessCustomerDetails(businessType),
//          fetchBusinessDetails = testBusinessDetails(),
//          fetchNewApplicationType = testNewApplicationType
//        )
//        when(mockMainStoreSave4LaterConnector.fetchData4Later[BusinessNameDetails](ArgumentMatchers.any(), ArgumentMatchers.eq("businessNameDetails"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
//          .thenReturn(Future.successful(Option(BusinessNameDetails(Some("test"), None, None))))
//
//        val res = testTradingNameController.showTradingName(false)
//          .apply(SessionBuilder.buildRequestWithSession(userId, businessType))
//
//        status(res) mustBe 200
//      }
//    }
//  }
//
//  "save" must {
//    "save the trading name details" when {
//      "provided with trading name and business name details for an edit journey" in {
//        val businessType = "test"
//        val updatedBusinessName = true
//        val hasAwrs = true
//
//        val fakeRequest = testRequest(getExtendedBusinessDetails(updatedBusinessName), businessType, hasAwrs)
//
//        setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
//        setupMockSave4LaterServiceWithOnly(
//          fetchBusinessCustomerDetails = testBusinessCustomerDetails(businessType),
//          fetchBusinessDetails = testBusinessDetails(),
//          fetchNewApplicationType = testNewApplicationType
//        )
//
//        when(mockBusinessDetailsService.businessDetailsPageRenderMode(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
//          .thenReturn(Future.successful(ReturnedApplicationMode))
//
//        val res = testTradingNameController.saveAndReturn()
//          .apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, businessType))
//
//        status(res) mustBe 303
//        redirectLocation(res).get must include(f"/alcohol-wholesale-scheme/view-section/businessDetails")
//      }
//
//      "provided with trading name and business name details" in {
//        val businessType = "test"
//        val updatedBusinessName = true
//        val hasAwrs = true
//
//        val fakeRequest = testRequest(getExtendedBusinessDetails(updatedBusinessName), businessType, hasAwrs)
//
//        setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
//        setupMockSave4LaterServiceWithOnly(
//          fetchBusinessCustomerDetails = testBusinessCustomerDetails(businessType),
//          fetchBusinessDetails = testBusinessDetails(),
//          fetchNewApplicationType = testNewApplicationType
//        )
//
//        when(mockBusinessDetailsService.businessDetailsPageRenderMode(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
//          .thenReturn(Future.successful(NewApplicationMode))
//
//        val res = testTradingNameController.saveAndContinue()
//          .apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, businessType))
//
//        status(res) mustBe 303
//        redirectLocation(res).get must include(f"/alcohol-wholesale-scheme/before-2016")
//      }
//
//      "provided with trading name and business name details for LLP_GRP" in {
//        val businessType = "LLP_GRP"
//        val updatedBusinessName = true
//        val hasAwrs = true
//
//        val fakeRequest = testRequest(getExtendedBusinessDetails(updatedBusinessName), businessType, hasAwrs)
//
//        setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
//        setupMockSave4LaterServiceWithOnly(
//          fetchBusinessCustomerDetails = testBusinessCustomerDetails(businessType),
//          fetchBusinessDetails = testBusinessDetails(),
//          fetchNewApplicationType = testNewApplicationType
//        )
//        setupMockKeyStoreService(fetchBusinessNameChange = BusinessNameDetails(Some("test"), None, None))
//
//        when(mockBusinessDetailsService.businessDetailsPageRenderMode(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
//          .thenReturn(Future.successful(NewApplicationMode))
//
//        val res = testTradingNameController.saveAndContinue()
//          .apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, businessType))
//
//        status(res) mustBe 303
//        redirectLocation(res).get must include(f"/alcohol-wholesale-scheme/business-details/group-representative/confirm")
//      }
//    }
//  }
//}
