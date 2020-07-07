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

package controllers

import builders.SessionBuilder
import connectors.mock.MockAuthConnector
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.AwrsUnitTestTraits


class AccessibilityStatementControllerTest extends AwrsUnitTestTraits
  with MockAuthConnector {

  val testAccessibilityStatementController: AccessibilityStatementController =
    new AccessibilityStatementController(mockMCC, mockAppConfig)

    "AccessibilityStatementController" when {

      ".show()" should {

        "return 200 with correct heading" in {
          val result = testAccessibilityStatementController.show().apply(SessionBuilder.buildRequestWithSession(userId))
          val document = Jsoup.parse(contentAsString(result))
          status(result) shouldBe OK
          document.select("#heading").text should be (Messages("awrs.accessibilityStatement.heading"))
        }
      }
    }
}
