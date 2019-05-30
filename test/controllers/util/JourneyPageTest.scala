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

package controllers.util

import java.util.UUID

import builders.{AuthBuilder, SessionBuilder}
import connectors.mock.MockAuthConnector
import controllers.auth.Utr._
import org.jsoup.Jsoup
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.{AwrsSessionKeys, AwrsUnitTestTraits}

import scala.concurrent.Future


class JourneyPageTest extends AwrsUnitTestTraits
  with MockAuthConnector {

  object TestPage extends JourneyPage {
    override val section: String = "testPageSection"
    override val authConnector = mockAuthConnector
    val noVariableFound = "Not Found"
    val signInUrl = "/sign-in"


    def getJouneyStartLocation: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
      restrictedAccessCheck {
        setAuthMocks()
        authorisedAction { ar =>
          Future.successful(Ok(request.getJourneyStartLocation.getOrElse(noVariableFound)))
        }
      }
    }
  }

  "JourneyPage trait" should {
    "Retrieve the JourneyStartLocation from the session" in {
      val request = SessionBuilder.buildRequestWithSessionStartLocation(userId, "LTD_GRP", Some(TestPage.section))
      val result = TestPage.getJouneyStartLocation.apply(request)
      val responseSessionMap = await(result).session(request).data
      val doc = Jsoup.parse(contentAsString(result))
      doc.body().text() shouldBe TestPage.section
      responseSessionMap(AwrsSessionKeys.sessionJouneyStartLocation) shouldBe TestPage.section
    }
  }
}
