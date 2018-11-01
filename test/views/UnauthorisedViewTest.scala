/*
 * Copyright 2018 HM Revenue & Customs
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
import controllers.IndexController
import models.{SubscriptionStatusType, IndexStatus => _}
import org.jsoup.Jsoup
import play.api.libs.json.JsValue
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsString, _}
import services.ServicesUnitTestFixture
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AwrsUnitTestTraits
import utils.TestUtil.testReviewDetails
import view_models.IndexViewModel

import scala.concurrent.Future

class UnauthorisedViewTest extends AwrsUnitTestTraits with ServicesUnitTestFixture{


  object TestIndexController extends IndexController {
    override val authConnector = mockAuthConnector
    override val save4LaterService = TestSave4LaterService
    override val indexService = mockIndexService
    override val api9 = TestAPI9
    override val applicationService = mockApplicationService
  }

  lazy val cachemap = CacheMap("", Map[String, JsValue]())
  lazy val noStatus = IndexViewModel(Nil)



  "Unathorised Template" should {

    "Display the correct heading" in {
      fetchContentFor("heading") shouldBe "You cannot use this service"
    }

    "Display the correct reason start" in {
      fetchContentFor("reason_start") shouldBe "If you are:"
    }

    "Display the correct reason 1" in {
      fetchContentFor("reason_1") shouldBe "a sole trader, you must"
    }

    "Display the correct reason 2" in {
      fetchContentFor("reason_2") shouldBe "an agent, you cannot apply on behalf of your clients"
    }
  }

  def fetchContentFor(element: String): String = {
    val document = Jsoup.parse(contentAsString(showUnauthorised))
    document.getElementById(element).text
  }

  private def showUnauthorised(): Future[Result] = {
    setUser(hasAwrs = true)
    setupMockSave4LaterService()
    setupMockAwrsAPI9(keyStore = None)
    setupMockApplicationService()
    setupMockIndexService()
    TestIndexController.unauthorised.apply(SessionBuilder.buildRequestWithSession(userId, "SOP"))
  }
}
