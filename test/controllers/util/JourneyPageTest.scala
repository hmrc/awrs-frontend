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


import audit.Auditable
import builders.SessionBuilder
import config.ApplicationConfig
import connectors.mock.MockAuthConnector
import controllers.AdditionalPremisesController
import org.jsoup.Jsoup
import play.api.mvc._
import play.api.test.Helpers._
import services.{DeEnrolService, Save4LaterService}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import utils.{AccountUtils, AwrsSessionKeys, AwrsUnitTestTraits}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class JourneyPageTest extends AwrsUnitTestTraits with MockAuthConnector {

  val mockDataCacheService: Save4LaterService = mock[Save4LaterService]

  object TestPage extends BaseController with JourneyPage {
    override val section: String = "testPageSection"
    override val authConnector: DefaultAuthConnector = mockAuthConnector
    override val deEnrolService: DeEnrolService = mockDeEnrolService
    val noVariableFound = "Not Found"
    val signInUrl = "/sign-in"


    def getJouneyStartLocation: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
      setAuthMocks()
      authorisedAction { implicit ar =>
        restrictedAccessCheck {
          Future.successful(Ok(request.getJourneyStartLocation.getOrElse(noVariableFound)))
        }(request, ar, implicitly, implicitly)
      }(request, implicitly, implicitly, messages)
    }

    override protected def controllerComponents: ControllerComponents = mockMCC

    override val accountUtils: AccountUtils = mockAccountUtils
    override val auditable: Auditable = mockAuditable
    override implicit val applicationConfig: ApplicationConfig = mockAppConfig
  }

  val template = app.injector.instanceOf[views.html.awrs_additional_premises]

  val testAdditionalPremisesController: AdditionalPremisesController =
    new AdditionalPremisesController(mockMCC, mockDataCacheService, mockDeEnrolService, mockAccountUtils, mockAuthConnector, mockAuditable, mockAppConfig, template) {
      override val signInUrl = "/sign-in"
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
