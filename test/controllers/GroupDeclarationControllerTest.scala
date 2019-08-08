/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.mocks.MockSave4LaterService
import utils.AwrsUnitTestTraits

import scala.concurrent.Future

class GroupDeclarationControllerTest extends AwrsUnitTestTraits
  with MockAuthConnector
  with MockSave4LaterService {

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val formId = "groupDeclaration"
  val testGroupDeclarationController: GroupDeclarationController = new GroupDeclarationController(mockMCC, testSave4LaterService, mockAuthConnector, mockAuditable, mockAccountUtils, mockAppConfig)

  "Submitting the application declaration form with " should {
    "Authenticated and authorised users" should {
      "redirect to Index page when valid data is provided" in {
        continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("groupRepConfirmation" -> "true")) {
          result =>
            redirectLocation(result).get should include("/alcohol-wholesale-scheme/index")
        }
      }
      "save form data to Save4Later and redirect to Index page " in {
        continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("groupRepConfirmation" -> "true")) {
          result =>
            status(result) should be(SEE_OTHER)
            verifySave4LaterService(saveGroupDeclaration = 1)
        }
      }
    }
  }

  private def continueWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceOnlySaveFunctions()
    setAuthMocks()
    val result = testGroupDeclarationController.sendConfirmation().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

}
