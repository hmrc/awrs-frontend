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
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.DataCacheKeys._
import services.ServicesUnitTestFixture
import utils.AwrsUnitTestTraits
import views.html.urn_kickout

import scala.concurrent.Future

class AwrsUrnControllerTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture {

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val template: urn_kickout = app.injector.instanceOf[views.html.urn_kickout]

  val testURNKickOutController: AwrsUrnController = new AwrsUrnController( mockMCC, mockAppConfig, template)

  "URNKickOutController" must {

    "show the Kickout page" in {
            val res = testURNKickOutController.showURNKickOutPage().apply(SessionBuilder.buildRequestWithSession(userId))
              status(res) mustBe 200
        }
  }

}