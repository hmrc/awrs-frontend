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

package controllers

import builders.SessionBuilder
import connectors.mock.MockAuthConnector
import controllers.auth.ExternalUrls
import models.BusinessDetailsEntityTypes._
import models.{ApplicationDeclaration, Suppliers, _}
import org.jsoup.Jsoup
import play.api.libs.json.{Format, JsValue, Json}
import play.api.mvc.Result
import play.api.test.Helpers._
import services.DataCacheKeys._
import services.ServicesUnitTestFixture
import services.mocks.{MockKeyStoreService, MockSave4LaterService}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AwrsUnitTestTraits
import utils.TestConstants._
import utils.TestUtil._

import scala.concurrent.Future

class ViewApplicationControllerTest extends AwrsUnitTestTraits
  with MockAuthConnector
  with MockSave4LaterService
  with MockKeyStoreService
  with ServicesUnitTestFixture {

  object TestViewApplicationController extends ViewApplicationController {
    override val authConnector = mockAuthConnector
    override val save4LaterService = TestSave4LaterService
    override val keyStoreService = TestKeyStoreService
    override val applicationService = mockApplicationService
    override val indexService = mockIndexService
    val signInUrl = ExternalUrls.signIn
  }

  def getCustomizedMap(businessType: Option[BusinessType] = testBusinessDetailsEntityTypes(CorporateBody),
                       businessCustomerDetails: Option[BusinessCustomerDetails] = None,
                       businessDetails: Option[BusinessDetails] = None,
                       businessContacts: Option[BusinessContacts] = None,
                       partnerDetails: Option[Partners] = None,
                       groupMembers: Option[GroupMembers] = None,
                       additionalBusinessPremises: Option[AdditionalBusinessPremisesList] = None,
                       businessDirectors: Option[List[BusinessDirectors]] = None,
                       tradingActivity: Option[TradingActivity] = None,
                       products: Option[Products] = None,
                       suppliers: Option[Suppliers] = None,
                       applicationDeclaration: Option[ApplicationDeclaration] = None
                      ) = {
    val id = testUtr
    val cacheMap = Map[String, JsValue]() ++
      prepMap[BusinessType](businessTypeName, businessType) ++
      prepMap[BusinessDetails](businessDetailsName, businessDetails) ++
      prepMap[BusinessContacts](businessContactsName, businessContacts) ++
      prepMap[BusinessCustomerDetails](businessCustomerDetailsName, businessCustomerDetails) ++
      prepMap[Partners](partnersName, partnerDetails) ++
      prepMap[AdditionalBusinessPremisesList](additionalBusinessPremisesName, additionalBusinessPremises) ++
      prepMap[List[BusinessDirectors]](businessDirectorsName, businessDirectors) ++
      prepMap[TradingActivity](tradingActivityName, tradingActivity) ++
      prepMap[Products](productsName, products) ++
      prepMap[Suppliers](suppliersName, suppliers) ++
      prepMap[ApplicationDeclaration](applicationDeclarationName, applicationDeclaration) ++
      prepMap[GroupMembers](groupMembersName, groupMembers)

    CacheMap(id, cacheMap)
  }

  def prepMap[T](key: String, optionParam: Option[T])(implicit format: Format[T]) =
    optionParam match {
      case Some(param) => Map[String, JsValue](key -> Json.toJson(param))
      case _ => Map[String, JsValue]()
    }

  "showViewApplication" should {

    "redirect the user back one page using the back link" in {

      showViewApplication("/alcohol-wholesale-scheme/index") {
        result =>
          val document = Jsoup.parse(contentAsString(result))
          status(result) shouldBe OK
          document.getElementById("back").attr("href") shouldBe "javascript:history.back()"
      }

      showViewApplication("/alcohol-wholesale-scheme/status-page") {
        result =>
          val document = Jsoup.parse(contentAsString(result))
          status(result) shouldBe OK
          document.getElementById("back").attr("href") shouldBe "javascript:history.back()"
      }

    }

    def showViewApplication(previousLocation: Option[String])(test: Future[Result] => Any) {
      setupMockSave4LaterService(fetchAll = getCustomizedMap())
      setAuthMocks()
      val result = TestViewApplicationController.show(printFriendly = false).apply(SessionBuilder.buildRequestWithSession(userId, "SOP", previousLocation))
      test(result)
    }

  }

  // N.B. the tests for backFrom is moved to BackButtonFunctionalityTest
}
