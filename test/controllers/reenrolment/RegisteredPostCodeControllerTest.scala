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

package controllers.reenrolment

import builders.SessionBuilder
import connectors.mock.MockAuthConnector
import forms.reenrolment.RegisteredPostcodeForm
import models.reenrolment.AwrsRegisteredPostcode
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ServicesUnitTestFixture
import services.mocks.{MockIndexService, MockKeyStoreService}
import utils.{AwrsUnitTestTraits, TestUtil}
import views.html.reenrolment.awrs_registered_postcode

class RegisteredPostCodeControllerTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture with MockAuthConnector
  with MockKeyStoreService
  with MockIndexService {

  def testRequest(answer: String): FakeRequest[AnyContentAsFormUrlEncoded] =
    TestUtil.populateFakeRequest[AwrsRegisteredPostcode](FakeRequest(), RegisteredPostcodeForm.awrsRegisteredPostcodeForm.form, AwrsRegisteredPostcode(answer))

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val template: awrs_registered_postcode = app.injector.instanceOf[views.html.reenrolment.awrs_registered_postcode]

  val testAwrsRegisteredPostcodeController: RegisteredPostcodeController = new RegisteredPostcodeController(mockMCC, mockAppConfig, mockAuthConnector,
    mockAccountUtils, mockDeEnrolService, mockAuditable ,mockAwrsFeatureSwitches,testKeyStoreService, template)

  "AwrsPostcodeController" must {
    "show the postcode page" in {
      setAuthMocks()
      setupMockKeystoreServiceForRegisteredPostcode()
      val res = testAwrsRegisteredPostcodeController.showPostCode().apply(SessionBuilder.buildRequestWithSession(userId))
      status(res) mustBe 200
    }

    "save the postcode to keystore if no errors" in {
      setAuthMocks()
      setupMockKeystoreServiceForRegisteredPostcode()
      val res = testAwrsRegisteredPostcodeController.saveAndContinue().apply(testRequest("NE270JZ"))
      status(res) mustBe 303
    }

    "save should return 400 if form has errors" in {
      setAuthMocks()
      setupMockKeystoreServiceForRegisteredPostcode()
      val res = testAwrsRegisteredPostcodeController.saveAndContinue().apply(testRequest(""))
      status(res) mustBe 400
    }
  }
}