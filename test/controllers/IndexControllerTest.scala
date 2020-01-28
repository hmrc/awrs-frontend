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
import org.jsoup.Jsoup
import org.mockito.Mockito._
import play.api.libs.json.JsValue
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.TestUtil._
import utils.AwrsSessionKeys

import scala.concurrent.Future

class IndexControllerTest extends ServicesUnitTestFixture {

  lazy val cachemap = CacheMap("", Map[String, JsValue]())

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockApplicationService)
  }

  lazy val testIndexController: IndexController = new IndexController(mockMCC, mockIndexService, testAPI9, mockApplicationService, testSave4LaterService, mockAuthConnector, mockAuditable, mockAccountUtils, mockAppConfig) {
    override val signInUrl = "/sign-in"
  }

  "IndexController" must {
    "The showIndex method should function correctly" should {
      "if businessType exists in session then display the index page" in {
        val result = callShowIndex(businessType = "SOP")
        status(result) shouldBe OK
      }
      "if the business type is missing from session then redirect back to the home controller" in {
        val result = callShowIndex(businessType = None)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/alcohol-wholesale-scheme"
      }
      "the sessionJouneyStartLocation is cleared when index is shown" in {
        val result = callShowIndex(startSection = testInitSessionJouneyStartLocation)
        val responseSessionMap = await(result).session(FakeRequest()).data
        val doc = Jsoup.parse(contentAsString(result))
        // the session variable for sessionJouneyStartLocation in the response should have been removed
        responseSessionMap.get(AwrsSessionKeys.sessionJouneyStartLocation) shouldBe None
      }
    }

    "The showLastLocation method should function correctly" should {
      "If the previous page exists in session history then route to that page" in {
        val previousLoc = "/alcohol-wholesale-scheme/supplier-addresses/edit"
        callShowLastLocationWith(previousLocation = previousLoc) {
          result =>
            redirectLocation(result).get shouldBe previousLoc
        }
      }
      "If the previous page does not exists in session history then route back to index " in {
        callShowLastLocationWith(previousLocation = None) {
          result =>
            redirectLocation(result).get shouldBe "/alcohol-wholesale-scheme/index"
        }
      }
    }

  }

  private def callShowLastLocationWith(previousLocation: Option[String], cacheMap: CacheMap = cachemap)(test: Future[Result] => Any) {
    setAuthMocks()
    val result = testIndexController.showLastLocation.apply(SessionBuilder.buildRequestWithSession(userId, "SOP", previousLocation))
    test(result)
  }

  val testInitSessionJouneyStartLocation = "Some location"

  private def callShowIndex(businessType: Option[String] = "SOP", startSection: Option[String] = None): Future[Result] = {
    setupMockSave4LaterService(
      fetchBusinessCustomerDetails = testReviewDetails,
      fetchAll = None
    )
    setupMockAwrsAPI9(keyStore = testSubscriptionStatusTypePending, connector = DoNotConfigure)
    setupMockApplicationService(hasAPI5ApplicationChanged = false)
    setupMockIndexService()
    setAuthMocks()

    val request = businessType match {
      case Some(bt) => SessionBuilder.buildRequestWithSession(userId, bt)
      case _ => SessionBuilder.buildRequestWithSession(userId)
    }
    val requestWithStart = startSection match {
      case Some(definedSection) => request.withSession(request.session.+((AwrsSessionKeys.sessionJouneyStartLocation, definedSection)).data.toSeq: _*)
      case _ => request
    }
    testIndexController.showIndex.apply(requestWithStart)
  }

}
