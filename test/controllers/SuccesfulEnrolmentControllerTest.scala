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
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ServicesUnitTestFixture
import utils.{AWRSFeatureSwitches, AwrsUnitTestTraits, FeatureSwitch}
import views.html.awrs_successful_enrolment

class SuccesfulEnrolmentControllerTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture {
  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val template: awrs_successful_enrolment = app.injector.instanceOf[views.html.awrs_successful_enrolment]
  val testSuccessfulEnrolmentController: SuccessfulEnrolmentController = new SuccessfulEnrolmentController( mockMCC, mockAppConfig, template)

  "SuccessfulEnrolmentController" must {

    "show the Successful Enrolment page when enrolmentJourney is enable" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.enrolmentJourney())
      val res = testSuccessfulEnrolmentController.showSuccessfulEnrolmentPage().apply(SessionBuilder.buildRequestWithSession(userId))
      status(res) mustBe 200
    }
    "return 404 the Kickout page when enrolmentJourney is ldisable" in {
      FeatureSwitch.disable(AWRSFeatureSwitches.enrolmentJourney())
      val res = testSuccessfulEnrolmentController.showSuccessfulEnrolmentPage().apply(SessionBuilder.buildRequestWithSession(userId))
      status(res) mustBe 404
    }
  }

}
