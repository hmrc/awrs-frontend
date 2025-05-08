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

package controllers.reenrolment

import builders.SessionBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ServicesUnitTestFixture
import utils.AwrsUnitTestTraits
import views.html.reenrolment.awrs_successful_enrolment

class SuccessfulEnrolmentControllerTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture {
  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val template: awrs_successful_enrolment = app.injector.instanceOf[views.html.reenrolment.awrs_successful_enrolment]
  val testSuccessfulEnrolmentController: SuccessfulEnrolmentController = new SuccessfulEnrolmentController(
    mockMCC,
    mockAppConfig,
    mockDeEnrolService,
    mockAuthConnector,
    mockAuditable,
    mockAwrsFeatureSwitches,
    mockAccountUtils,
    template
  )

  "SuccessfulEnrolmentController" must {

    "show the Successful Enrolment page when enrolmentJourney is enable" in {
      setAuthMocks()
      setupEnrolmentJourneyFeatureSwitchMock(true)
      val res = testSuccessfulEnrolmentController.showSuccessfulEnrolmentPage().apply(SessionBuilder.buildRequestWithSession(userId))
      status(res) mustBe 200
    }
    "return 404 the Kickout page when enrolmentJourney is disabled" in {
      setAuthMocks()
      setupEnrolmentJourneyFeatureSwitchMock(false)
      val res = testSuccessfulEnrolmentController.showSuccessfulEnrolmentPage().apply(SessionBuilder.buildRequestWithSession(userId))
      status(res) mustBe 404
    }
    "contain a button linking to the Business Tax Account page" in {
      setAuthMocks()
      setupEnrolmentJourneyFeatureSwitchMock(true)

      val res = testSuccessfulEnrolmentController.showSuccessfulEnrolmentPage().apply(SessionBuilder.buildRequestWithSession(userId))
      val content = contentAsString(res)

      content must include(s"""<a href="" role="button" class="govuk-button" id="bta-redirect-button" data-module="govuk-button">""")
    }
    "redirect to Business Tax Account Page when button is clicked" in {
      setAuthMocks()
      setupEnrolmentJourneyFeatureSwitchMock(true)

      val res = testSuccessfulEnrolmentController.showSuccessfulEnrolmentPage().apply(SessionBuilder.buildRequestWithSession(userId))
      status(res) mustBe 200
    }

    "contain a hyperlink to the correct AWRS guidance page" in {
      setAuthMocks()
      setupEnrolmentJourneyFeatureSwitchMock(true)

      val res = testSuccessfulEnrolmentController.showSuccessfulEnrolmentPage().apply(SessionBuilder.buildRequestWithSession(userId))
      val content = contentAsString(res)
      content must include("""<a href="https://www.gov.uk/guidance/the-alcohol-wholesaler-registration-scheme-awrs" target="_blank"> """)
    }

  }

}