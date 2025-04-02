/*
 * Copyright 2025 HM Revenue & Customs
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
import forms.HaveYouRegisteredForm
import models.HaveYouRegisteredModel
import org.mockito.Mockito.when
import play.api.http.Status.SEE_OTHER
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.ServicesUnitTestFixture
import utils.{AwrsUnitTestTraits, TestUtil}
import views.html.awrs_have_you_registered

import scala.concurrent.Future

class HaveYouRegisteredControllerTest extends AwrsUnitTestTraits with ServicesUnitTestFixture{


  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val template: awrs_have_you_registered = app.injector.instanceOf[views.html.awrs_have_you_registered]

  val mockHaveYouRegisteredController: HaveYouRegisteredController = new HaveYouRegisteredController(
    mockMCC,
    testKeyStoreService,
    mockAuthConnector,
    mockAccountUtils,
    mockDeEnrolService,
    mockAuditable,
    mockAwrsFeatureSwitches,
    mockAppConfig,
    template)

  def testRequest(answer: Option[Boolean]): FakeRequest[AnyContentAsFormUrlEncoded] =
    TestUtil.populateFakeRequest[HaveYouRegisteredModel](FakeRequest(), HaveYouRegisteredForm.haveYouRegisteredForm.form, HaveYouRegisteredModel(answer)).withSession("previousLocation" -> "/previous-page")

  def setUpMocksFeatureFlagOn(): Unit = {
    setAuthMocks()
    setupMockKeystoreServiceForHaveYouRegistered()
    setupEnrolmentJourneyFeatureSwitchMock(true)
  }

  def setUpMocksFeatureFlagOff(): Unit = {
    setAuthMocks()
    setupMockKeystoreServiceForHaveYouRegistered()
    setupEnrolmentJourneyFeatureSwitchMock(false)
  }

  "HaveYouRegisteredController" should {

    "show the HaveYouRegistered page when feature is enabled" in {
      setUpMocksFeatureFlagOn()

      val result = mockHaveYouRegisteredController.showHaveYouRegisteredPage().apply(SessionBuilder.buildRequestWithSession(userId))

      status(result) mustBe 200
    }

    "show not found page when feature is not enabled" in {
      setUpMocksFeatureFlagOff()

      val result = mockHaveYouRegisteredController.showHaveYouRegisteredPage().apply(SessionBuilder.buildRequestWithSession(userId))

      status(result) mustBe 404
    }

    "return a 400 when the form has errors on submission" in {
      setUpMocksFeatureFlagOn()

      val result = mockHaveYouRegisteredController.saveAndContinue().apply(testRequest(None))

      status(result) mustBe 400
    }

    "redirect to awrs urn page after successfully saving data" in {
      setUpMocksFeatureFlagOn()

      val result: Future[Result] = mockHaveYouRegisteredController.saveAndContinue().apply(testRequest(Some(true)))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.AwrsUrnController.showArwsUrnPage.url)
    }
  }

  "redirect to business customer start page if user selects No" in {
    setUpMocksFeatureFlagOn()
    when(mockAppConfig.businessCustomerStartPage).thenReturn("/business-customer-start")

    val result = mockHaveYouRegisteredController.saveAndContinue().apply(testRequest(Some(false)))

    status(result) mustBe SEE_OTHER
    redirectLocation(result) mustBe Some(mockAppConfig.businessCustomerStartPage)
  }

  "redirect to last visited location if stored in session" in {
    setUpMocksFeatureFlagOn()

    val result = mockHaveYouRegisteredController.showLastLocation().apply(testRequest(Some(true)))

    status(result) mustBe SEE_OTHER
    redirectLocation(result) mustBe Some("/previous-page")
  }

}
