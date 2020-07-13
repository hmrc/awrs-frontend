/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.mocks.MockSave4LaterService
import utils.AwrsUnitTestTraits

import scala.concurrent.Future

class ResetControllerTest extends AwrsUnitTestTraits
  with MockAuthConnector
  with MockSave4LaterService {

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val testResetController: ResetController = new ResetController(
    mockMCC, testSave4LaterService, mockDeEnrolService, mockAuthConnector, mockAuditable, mockAccountUtils, mockAppConfig)

  "Reset application page " should {
    "return a Signed out view for SA utr" in {
      getWithAuthorisedUserSa {
        result =>
          status(result) shouldBe 303
          redirectLocation(result).get should be("/alcohol-wholesale-scheme/logout")
          verifySave4LaterService(removeAll = 1)
          verifyApiSave4LaterService(removeAll = 0)
      }
    }

    "return a Signed out view for CT utr" in {
      getWithAuthorisedUserCt {
        result =>
          status(result) shouldBe 303
          redirectLocation(result).get should be("/alcohol-wholesale-scheme/logout")
          verifySave4LaterService(removeAll = 1)
          verifyApiSave4LaterService(removeAll = 0)
      }
    }
  }

  "Update reset page " should {
    "return a signed out view" in {
      getUpdateWithAuthorisedUserCt {
        result =>
          status(result) shouldBe 303
          redirectLocation(result).get should be("/alcohol-wholesale-scheme/logout")
          verifySave4LaterService(removeAll = 1)
          verifyApiSave4LaterService(removeAll = 1)
      }
    }
  }

  private def getWithAuthorisedUserSa(test: Future[Result] => Any) {
    setAuthMocks()
    val result = testResetController.resetApplication.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def getWithAuthorisedUserCt(test: Future[Result] => Any) {
    setAuthMocks()
    val result = testResetController.resetApplication.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def getUpdateWithAuthorisedUserCt(test: Future[Result] => Any) {
    setAuthMocks()
    val result = testResetController.resetApplicationUpdate.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

}
