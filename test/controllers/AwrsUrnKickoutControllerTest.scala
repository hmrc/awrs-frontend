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
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ServicesUnitTestFixture
import utils.AwrsUnitTestTraits
import views.html.urn_kickout

class AwrsUrnKickoutControllerTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture {

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val template: urn_kickout = app.injector.instanceOf[views.html.urn_kickout]

  val testURNKickOutController: AwrsUrnKickoutController = new AwrsUrnKickoutController(
    mockMCC,
    mockAppConfig,
    mockAwrsFeatureSwitches,
    mockDeEnrolService,
    mockAuthConnector,
    mockAccountUtils,
    mockAuditable,
    template
  )

  "URNKickOutController" must {

    "show the Kickout page when enrolmentJourney is enable" in {
      setAuthMocks()
      setupEnrollmentJourneyFeatureSwitchMock(true)
      val res = testURNKickOutController.showURNKickOutPage().apply(SessionBuilder.buildRequestWithSession(userId))
      status(res) mustBe 200
    }
    "return 404 the Kickout page when enrolmentJourney is ldisable" in {
      setAuthMocks()
      setupEnrollmentJourneyFeatureSwitchMock(false)
      val res = testURNKickOutController.showURNKickOutPage().apply(SessionBuilder.buildRequestWithSession(userId))
      status(res) mustBe 404
    }
  }

}