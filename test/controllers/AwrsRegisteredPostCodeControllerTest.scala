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
import connectors.mock.MockAuthConnector
import forms.{AwrsRegisteredPostcodeForm}
import models.{AwrsRegisteredPostcode}
import org.mockito.Mockito.when
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ServicesUnitTestFixture
import services.mocks.{MockIndexService, MockKeyStoreService}
import utils.{AWRSFeatureSwitches, AwrsUnitTestTraits, BooleanFeatureSwitch, FeatureSwitch, TestUtil}
import views.html.awrs_registered_postcode

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AwrsRegisteredPostCodeControllerTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture with MockAuthConnector
  with MockKeyStoreService
  with MockIndexService {

  def testRequest(answer: String): FakeRequest[AnyContentAsFormUrlEncoded] =
    TestUtil.populateFakeRequest[AwrsRegisteredPostcode](FakeRequest(), AwrsRegisteredPostcodeForm.awrsRegisteredPostcodeForm.form, AwrsRegisteredPostcode(answer))

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val template: awrs_registered_postcode = app.injector.instanceOf[views.html.awrs_registered_postcode]

  val testAwrsRegisteredPostcodeController: AwrsRegisteredPostcodeController = new AwrsRegisteredPostcodeController(mockMCC, mockAppConfig, mockAuthConnector,
    mockAccountUtils, mockDeEnrolService, mockAuditable ,mockAwrsFeatureSwitches,testKeyStoreService, template)

  "AwrsPostcodeController" must {
    "show not found when feature is not enabled" in {
      setAuthMocks()
      setupMockKeystoreServiceForRegisteredPostcode()
      setupEnrollmentJourneyFeatureSwitchMock(false)
      val res = testAwrsRegisteredPostcodeController.showPostCode().apply(SessionBuilder.buildRequestWithSession(userId))
      status(res) mustBe 404
    }

    "show the postcode page when enrolmentJourney is enabled" in {
      setAuthMocks()
      setupMockKeystoreServiceForRegisteredPostcode()
      setupEnrollmentJourneyFeatureSwitchMock(true)
      val res = testAwrsRegisteredPostcodeController.showPostCode().apply(SessionBuilder.buildRequestWithSession(userId))
      status(res) mustBe 200
    }

    "save the postcode to keystore if no errors" in {
      setAuthMocks()
      setupMockKeystoreServiceForRegisteredPostcode()
      setupEnrollmentJourneyFeatureSwitchMock(true)
      val res = testAwrsRegisteredPostcodeController.saveAndContinue().apply(testRequest("NE270JZ"))
      status(res) mustBe 303
    }

    "save should return 400 if form has errors" in {
      setAuthMocks()
      setupMockKeystoreServiceForRegisteredPostcode()
      setupEnrollmentJourneyFeatureSwitchMock(true)
      val res = testAwrsRegisteredPostcodeController.saveAndContinue().apply(testRequest(""))
      status(res) mustBe 400
    }
  }
}