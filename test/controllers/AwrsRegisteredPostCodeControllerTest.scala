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
import config.ApplicationConfig
import org.mockito.Mockito.when
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ServicesUnitTestFixture
import utils.{AWRSFeatureSwitches, AwrsUnitTestTraits, FeatureSwitch}
import views.html.awrs_registered_postcode

import scala.concurrent.Future

class AwrsRegisteredPostCodeControllerTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture {

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val mockTemplate: awrs_registered_postcode = app.injector.instanceOf[views.html.awrs_registered_postcode]
  override val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]


  "AwrsRegisteredPostcodeController" must {

    "show the postcode page when enrolmentJourney is enable" in {

      continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("legalEntity" -> "LTD")) {
        result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get mustBe "/alcohol-wholesale-scheme/index"
      }
    }
    "return 404 the Kickout page when enrolmentJourney is ldisable" in {
      val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
      when(mockAppConfig.enrolmentJourney).thenReturn(false)

      val testAwrsRegisteredPostcode: AwrsRegisteredPostcodeController = new AwrsRegisteredPostcodeController(mockMCC, mockAppConfig, mockAuthConnector, mockAccountUtils, mockDeEnrolService, mockAuditable, mockTemplate) {
        override val signInUrl: String = applicationConfig.signIn
      }
          val res = testAwrsRegisteredPostcode.showPostCode().apply(SessionBuilder.buildRequestWithSession(userId))
          status(res) mustBe 404
    }
  }

  private def continueWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any): Unit = {

    setupMockSave4LaterService()
    setAuthMocks()
    when(mockAppConfig.enrolmentJourney).thenReturn(true)
    val testAwrsRegisteredPostcode: AwrsRegisteredPostcodeController = new AwrsRegisteredPostcodeController(mockMCC, mockAppConfig, mockAuthConnector, mockAccountUtils, mockDeEnrolService, mockAuditable, mockTemplate) {
      override val signInUrl: String = applicationConfig.signIn
    }
    val result = testAwrsRegisteredPostcode.saveAndContinue().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId).withMethod("POST"))
    test(result)
  }

}