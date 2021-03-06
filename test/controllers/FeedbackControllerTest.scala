/*
 * Copyright 2021 HM Revenue & Customs
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
import forms.FeedbackForm
import models.Feedback
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.model.Audit
import utils.{AwrsUnitTestTraits, TestUtil}

import scala.concurrent.Future

class FeedbackControllerTest extends AwrsUnitTestTraits
  with MockAuthConnector {

  val mockAudit: Audit = mock[Audit]
  val mockTemplate = app.injector.instanceOf[views.html.awrs_feedback]
  val mockTemplateThanks = app.injector.instanceOf[views.html.awrs_feedback_thanks]
  val testFeedbackController: FeedbackController = new FeedbackController(mockMCC, mockAuthConnector, mockAuditable, mockDeEnrolService, mockAccountUtils, mockAppConfig, mockTemplate, mockTemplateThanks)

  def testRequest(feedback: Feedback): FakeRequest[AnyContentAsFormUrlEncoded] =
    TestUtil.populateFakeRequest[Feedback](FakeRequest(), FeedbackForm.feedbackForm.form, feedback)

  "Feedback Controller" must {
    "submit feedback and redirect to thank you page" in {
      submitAuthorisedUser(testRequest(Feedback(visitReason = None, satisfactionRating = None, comments = "tdfyguhijoihugfvbjnkm"))) {
        result =>
          status(result) mustBe 303
          redirectLocation(result).get must be("/alcohol-wholesale-scheme/feedback/thanks")
      }
    }
  }

  def submitAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setAuthMocks()
    val result = testFeedbackController.submitFeedback.apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

}
