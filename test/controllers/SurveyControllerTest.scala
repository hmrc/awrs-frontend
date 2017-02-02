/*
 * Copyright 2017 HM Revenue & Customs
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

import java.util.UUID

import audit.TestAudit
import builders.{AuthBuilder, SessionBuilder}
import connectors.mock.MockAuthConnector
import controllers.auth.Utr._
import forms.SurveyForm
import models.Survey
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AwrsUnitTestTraits, TestUtil}

import scala.concurrent.Future

class SurveyControllerTest extends AwrsUnitTestTraits with MockAuthConnector {

  val mockAudit = mock[Audit]

  object TestSurveyController extends SurveyController {
    override val appName = "awrs-frontend"
    override val authConnector = mockAuthConnector
    override val audit: Audit = new TestAudit
  }

  def testRequest(survey: Survey) =
    TestUtil.populateFakeRequest[Survey](FakeRequest(), SurveyForm.surveyForm.form, survey)

  "Survey Controller" should {

    "redirect to signout page" in {
      signoutAuthorisedUser {

        result =>
          status(result) shouldBe 303
          redirectLocation(result).get should include("/gg/sign-out")
      }
    }

    "submit survey and signout" in {
      submitAndSignoutAuthorisedUser(testRequest(Survey(satisfactionRating = None,
        visitReason = None,
        easeOfAccess = None,
        easeOfUse = None,
        helpNeeded = None,
        howDidYouFindOut = None,
        comments = "tdfyguhijoihugfvbjnkm",
        contactFullName = None,
        contactEmail = None,
        contactTelephone = None
      ))) {

        result =>
          status(result) shouldBe 303
          redirectLocation(result).get should include("/gg/sign-out")
      }
    }

  }

  private def signoutAuthorisedUser(test: Future[Result] => Any) {
    val result = TestSurveyController.optOutSurvey.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def submitAndSignoutAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val result = TestSurveyController.submitSurvey.apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }
}
