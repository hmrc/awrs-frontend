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

package views


import builders.SessionBuilder
import connectors.mock.MockAuthConnector
import controllers.FeedbackController
import forms.FeedbackForm
import models.Feedback
import org.jsoup.Jsoup
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.model.Audit
import utils.TestUtil._
import utils.{AwrsUnitTestTraits, TestUtil}

import scala.concurrent.Future

class FeedbackViewTest extends AwrsUnitTestTraits
  with MockAuthConnector {

  val mockAudit: Audit = mock[Audit]
  val testFeedbackController: FeedbackController = new FeedbackController(mockMCC, mockAuthConnector, mockAuditable, mockDeEnrolService, mockAccountUtils, mockAppConfig)


  def testRequest(feedback: Feedback) =
    TestUtil.populateFakeRequest[Feedback](FakeRequest(), FeedbackForm.feedbackForm.form, feedback)

  "Feedback Controller" should {

    "submit feedback containing special characters will show an error" in {
      submitAuthorisedUser(testRequest(Feedback(visitReason = "ß∂ƒ©None", satisfactionRating = None, comments = "∂ƒ©˙tdfyguhijoihugfvbjnkm"))) {

        result =>
          val document = Jsoup.parse(contentAsString(result))
          status(result) shouldBe BAD_REQUEST

          val errMsgKey = "awrs.generic.error.character_invalid"

          testErrorMessageValidation(document, "comments", errMsgKey, "comments")
          testErrorMessageValidation(document, "visitReason", errMsgKey, "visit reason")
      }
    }
  }

  def submitAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setAuthMocks()
    val result = testFeedbackController.submitFeedback.apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }
}
