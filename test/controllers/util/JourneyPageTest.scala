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

package controllers.util

import java.util.UUID

import builders.{AuthBuilder, SessionBuilder}
import connectors.mock.MockAuthConnector
import controllers.auth.Utr._
import org.jsoup.Jsoup
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

    def getJouneyStartLocation = asyncRestrictedAccess {
      implicit user => implicit request =>
        Future.successful(Ok(request.getJourneyStartLocation.getOrElse(noVariableFound)))
    }
  }

  "JourneyPage trait" should {
    "add the JouneyStartLocation session variable into the session if it's not already in the session" in {
      val result = testAsync()
      val responseSessionMap = await(result).session(FakeRequest()).data
      val doc = Jsoup.parse(contentAsString(result))
      // the original request should not have contained a session variable for sessionJouneyStartLocation
      doc.body().text() shouldBe TestPage.noVariableFound
      // the response should contain the session varaible for sessionJouneyStartLocation
      responseSessionMap(AwrsSessionKeys.sessionJouneyStartLocation) shouldBe TestPage.section
    }

    "if JouneyStartLocation already exists then do not edit the JouneyStartLocation session variable " in {
      val existingVariable = "existing variable"
      val result = testAsync(existingVariable)
      val responseSessionMap = await(result).session(FakeRequest()).data
      val doc = Jsoup.parse(contentAsString(result))
      // the original request should have contained the specified variable for sessionJouneyStartLocation
      doc.body().text() shouldBe existingVariable
      // the response should contain the same session varaible for sessionJouneyStartLocation
      responseSessionMap(AwrsSessionKeys.sessionJouneyStartLocation) shouldBe existingVariable
    }
  }

  private def testAsync(startSection: Option[String] = None) = {
    val request = SessionBuilder.buildRequestWithSession(userId)
    val requestWithStart = startSection match {
      case Some(definedSection) => request.withSession(request.session.+((AwrsSessionKeys.sessionJouneyStartLocation, definedSection)).data.toSeq: _*)
      case _ => request
    }
    TestPage.getJouneyStartLocation.apply(requestWithStart)
  }

}
