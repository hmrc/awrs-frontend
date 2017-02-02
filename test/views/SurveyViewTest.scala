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

package views


import audit.TestAudit
import builders.SessionBuilder
import connectors.mock.MockAuthConnector
import controllers.SurveyController
import forms.SurveyForm
import models.Survey
import org.jsoup.Jsoup
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.model.Audit
import utils.TestUtil._
import utils.{AwrsUnitTestTraits, TestUtil}

import scala.concurrent.Future

class SurveyViewTest extends AwrsUnitTestTraits with MockAuthConnector {

  val mockAudit = mock[Audit]

  object TestSurveyController extends SurveyController {
    override val appName = "awrs-frontend"
    override val authConnector = mockAuthConnector
    override val audit: Audit = new TestAudit
  }

  def testRequest(survey: Survey) =
    TestUtil.populateFakeRequest[Survey](FakeRequest(), SurveyForm.surveyForm.form, survey)

  "Survey Controller" should {

    "submit survey containing special characters will show an error" in {

      submitAndSignoutAuthorisedUser(testRequest(
        Survey(satisfactionRating = None,
          visitReason = None,
          easeOfAccess = None,
          easeOfUse = None,
          helpNeeded = None,
          howDidYouFindOut = None,
          comments = "∂ƒ©˙ƒ©˙∆˚tdfyguhijoihugfvbjnkm",
          contactFullName = None,
          contactEmail = None,
          contactTelephone = None)
      )) {

        result =>
          val document = Jsoup.parse(contentAsString(result))
          status(result) shouldBe BAD_REQUEST

          val errMsgKey = "awrs.generic.error.character_invalid"

          testErrorMessageValidation(document, "comments", errMsgKey, "comments")
      }
    }

  }

  "Survey view sign out link" should {

    "sign users out rather than redirecting to survey" in {

      showSignOut {
        result =>
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("logOutNavHref").attr("href") should endWith("/gg/sign-out")
      }
    }
  }

  private def showSignOut(test: Future[Result] => Any) {
    val result = TestSurveyController.showSurvey.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def submitAndSignoutAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val result = TestSurveyController.submitSurvey.apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }
}
