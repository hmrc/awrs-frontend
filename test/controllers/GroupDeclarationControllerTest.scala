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
import config.FrontendAuthConnector
import connectors.mock.MockAuthConnector
import models._
import org.jsoup.Jsoup
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.mocks.MockSave4LaterService
import utils.AwrsUnitTestTraits
import utils.TestUtil._

import scala.concurrent.Future

class GroupDeclarationControllerTest extends AwrsUnitTestTraits
  with MockAuthConnector
  with MockSave4LaterService {

  val request = FakeRequest()

  val formId = "groupDeclaration"

  val testGroupDeclaration = GroupDeclaration(true)

  object TestGroupDeclarationController extends GroupDeclarationController {
    override val authConnector = mockAuthConnector
    override val save4LaterService = TestSave4LaterService
  }

  "GroupDeclarationController" must {
    "use the correct AuthConnector" in {
      GroupDeclarationController.authConnector shouldBe FrontendAuthConnector
    }
  }

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
    val result = TestGroupDeclarationController.sendConfirmation().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

}
