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
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import services.ServicesUnitTestFixture
import utils.AwrsUnitTestTraits
import views.html.reenrolment.awrs_deenrolment_confirmation

class DeEnrollmentConfirmationPageControllerTest extends PlaySpec with AwrsUnitTestTraits with MockAuthConnector with ServicesUnitTestFixture {

  private val template: awrs_deenrolment_confirmation =
    app.injector.instanceOf[views.html.reenrolment.awrs_deenrolment_confirmation]

  private val controller: DeEnrollmentConfirmationPageController = new DeEnrollmentConfirmationPageController(
    mockMCC,
    mockAppConfig,
    mockAwrsFeatureSwitches,
    mockDeEnrolService,
    mockAuthConnector,
    mockAccountUtils,
    mockAuditable,
    template
  )

  "DeEnrollmentConfirmationPageController" must {

    "return 404 when enrolmentJourney feature is disabled" in {
      setAuthMocks()
      setupEnrolmentJourneyFeatureSwitchMock(false)

      val request = SessionBuilder.buildRequestWithSession(userId)
      val result  = controller.showDeEnrollmentConfirmationPage().apply(request)

      status(result) mustBe NOT_FOUND
    }

    "return 200 and render the confirmation form when feature is enabled" in {
      setAuthMocks()
      setupEnrolmentJourneyFeatureSwitchMock(true)

      val request = SessionBuilder.buildRequestWithSession(userId)
      val result  = controller.showDeEnrollmentConfirmationPage().apply(request)

      status(result) mustBe OK

      val body = contentAsString(result)
      body must include("""name="confirmDeEnrollment"""")
      body must include("de-enrollment-confirmation")
    }

  }

  "saveAndContinue" must {

    "redirect to RegisteredPostcodeController when user answers Yes" in {
      setAuthMocks()
      setupEnrolmentJourneyFeatureSwitchMock(true)

      val request = SessionBuilder
        .buildRequestWithSession(userId, "POST", "/")
        .withFormUrlEncodedBody("confirmDeEnrollment" -> "Yes")
      val result = controller.saveAndContinue().apply(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.RegisteredPostcodeController.showPostCode.url)
    }

    "redirect to KickoutController when user answers No" in {
      setAuthMocks()
      setupEnrolmentJourneyFeatureSwitchMock(true)

      val request = SessionBuilder
        .buildRequestWithSession(userId, "POST", "/")
        .withFormUrlEncodedBody("confirmDeEnrollment" -> "No")
      val result = controller.saveAndContinue().apply(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.KickoutController.showURNKickOutPage.url)
    }

    "return BAD_REQUEST when no option is selected" in {
      setAuthMocks()
      setupEnrolmentJourneyFeatureSwitchMock(true)

      val request = SessionBuilder
        .buildRequestWithSession(userId, "POST", "/")
      val result = controller.saveAndContinue().apply(request)

      status(result) mustBe BAD_REQUEST

      val body = contentAsString(result)
      body must include("""name="confirmDeEnrollment"""")
      body must include("error-summary")
    }

  }

}
