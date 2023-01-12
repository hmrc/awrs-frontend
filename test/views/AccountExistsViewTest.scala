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

package views

import builders.SessionBuilder
import connectors.mock.MockAuthConnector
import controllers.{AdditionalPremisesController, WrongAccountController}
import models._
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.test.Helpers._
import services.DataCacheKeys._
import services.JourneyConstants
import services.mocks.MockSave4LaterService
import utils.AwrsUnitTestTraits
import utils.TestUtil._

import scala.concurrent.Future

class AccountExistsViewTest extends AwrsUnitTestTraits
  with MockSave4LaterService with MockAuthConnector {

  val template = app.injector.instanceOf[views.html.awrs_account_exists]

  val testWrongAccountController: WrongAccountController =
    new WrongAccountController(mockMCC, testSave4LaterService, mockDeEnrolService, mockAccountUtils, mockAuthConnector, mockAuditable, mockAppConfig, template) {
      override val signInUrl = "/sign-in"
    }

  "Account Exists View" must {
    "display the correct content" in {

    }
  }

}
