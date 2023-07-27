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
import connectors.mock.MockAuthConnector
import models.GroupDeclaration
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.mocks.MockSave4LaterService
import utils.AwrsUnitTestTraits
import views.html.awrs_group_declaration

import scala.concurrent.Future

class GroupDeclarationControllerTest extends AwrsUnitTestTraits
  with MockAuthConnector
  with MockSave4LaterService {

  val template: awrs_group_declaration = app.injector.instanceOf[views.html.awrs_group_declaration]

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val formId = "groupDeclaration"
  val testGroupDeclarationController: GroupDeclarationController = new GroupDeclarationController(
    mockMCC, testSave4LaterService, mockDeEnrolService, mockAuthConnector, mockAuditable, mockAccountUtils, mockAppConfig, template)

  "Submitting the application declaration form with " must {
    "Authenticated and authorised users" must {
      "show an ok when the group declaration is available" in {
        showWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("groupRepConfirmation" -> "true")) {
          result =>
            status(result) mustBe OK
        }
      }

      "redirect to Index page when valid data is provided" in {
        continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("groupRepConfirmation" -> "true")) {
          result =>
            redirectLocation(result).get must include("/alcohol-wholesale-scheme/index")
        }
      }
      "save form data to Save4Later and redirect to Index page " in {
        continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("groupRepConfirmation" -> "true")) {
          result =>
            status(result) must be(SEE_OTHER)
            verifySave4LaterService(saveGroupDeclaration = 1)
        }
      }
    }
  }

  private def continueWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any): Unit = {
    setupMockSave4LaterServiceOnlySaveFunctions()
    setAuthMocks()
    val result = testGroupDeclarationController.sendConfirmation().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId).withMethod("POST"))
    test(result)
  }

  private def showWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any): Unit = {
    setupMockSave4LaterServiceWithOnly(
      fetchGroupDeclaration = Some(GroupDeclaration(true))
    )
    setAuthMocks()
    val result = testGroupDeclarationController.showGroupDeclaration.apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

}
