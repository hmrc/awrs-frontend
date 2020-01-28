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

package controllers.util

import builders.SessionBuilder
import connectors.mock.MockAuthConnector
import controllers.TradingActivityController
import controllers.auth.StandardAuthRetrievals
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Request, Result}
import play.api.test.FakeRequest
import services.mocks.MockSave4LaterService
import utils.{AwrsSessionKeys, AwrsUnitTestTraits}
import views.view_application.helpers.ViewApplicationType

import scala.concurrent.Future
import services.DataCacheKeys._

/*
*  this test is created to test the section hash decoding, the rest of the functionality are tested by the controllers
*  which implements this trait instead
*/
class SaveAndRoutableTest extends AwrsUnitTestTraits
  with MockAuthConnector
  with MockSave4LaterService {

  val testController: TradingActivityController = new TradingActivityController(mockMCC, testSave4LaterService, mockAuthConnector, mockAuditable, mockAccountUtils, mockAppConfig) {
    override val signInUrl = "/sign-in"
    override def save(id: Int, redirectRoute: (Option[RedirectParam], Boolean) => Future[Result], viewApplicationType: ViewApplicationType, isNewRecord: Boolean, authRetrievals: StandardAuthRetrievals)(implicit request: Request[AnyContent]): Future[Result]
    = Future.successful(Ok)
  }

  setUser(hasAwrs = true)
  lazy val baseRequest: FakeRequest[AnyContentAsEmpty.type] = SessionBuilder.buildRequestWithSession(userId, "SOP") // use sole trader journey as the foundation for these tests

  "redirectToIndex" should {
    "return true if the section name is index" in {
      val sessionSectionStatusHash = "" // should not matter what this value this is set to
      await(testWasSectionCompletedWhenCurrentJourneyStarted(indexName, sessionSectionStatusHash)) shouldBe true
    }
    "return true if the section is already completed" in {
      val sessionSectionStatusHash = "2" // business registration details is the second page, thus the hash is 2
      await(testWasSectionCompletedWhenCurrentJourneyStarted(businessRegistrationDetailsName, sessionSectionStatusHash)) shouldBe true
    }
    "return false if the section has not started" in {
      val sessionSectionStatusHash = "0" // nothing is completed
      await(testWasSectionCompletedWhenCurrentJourneyStarted(businessRegistrationDetailsName, sessionSectionStatusHash)) shouldBe false
    }
  }

  def testWasSectionCompletedWhenCurrentJourneyStarted(sectionName: String, sessionSectionStatusHash: String): Future[Boolean] = {
    implicit val implicitRequest = baseRequest.withSession(baseRequest.session.+((AwrsSessionKeys.sessionSectionStatus, sessionSectionStatusHash)).data.toSeq: _*)
    testController.redirectToIndex(sectionName)
  }

}
