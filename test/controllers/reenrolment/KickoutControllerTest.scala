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
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ServicesUnitTestFixture
import utils.AwrsUnitTestTraits
import views.html.reenrolment.awrs_reenrolment_kickout

class KickoutControllerTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture {

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val template: awrs_reenrolment_kickout = app.injector.instanceOf[views.html.reenrolment.awrs_reenrolment_kickout]

  val testKickOutController: KickoutController = new KickoutController(
    mockMCC,
    mockAppConfig,
    mockDeEnrolService,
    mockAuthConnector,
    mockAccountUtils,
    mockAuditable,
    template
  )

  "KickOutController" must {

    "show the Kickout page" in {
      setAuthMocks()
      val res = testKickOutController.showKickOutPage().apply(SessionBuilder.buildRequestWithSession(userId))
      status(res) mustBe 200
    }
  }
}