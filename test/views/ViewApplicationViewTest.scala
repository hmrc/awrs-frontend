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

package views

import builders.SessionBuilder
import connectors.mock.MockAuthConnector
import controllers.ViewApplicationController
import models.BusinessDetailsEntityTypes._
import models.{ApplicationDeclaration, Suppliers, _}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.i18n.Messages
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
import views.view_application.subviews.SubviewIds

import scala.concurrent.Future

class ViewApplicationViewTest extends AwrsUnitTestTraits
  with MockAuthConnector
  with MockSave4LaterService
  with MockKeyStoreService
  with ServicesUnitTestFixture {

  val mockTemplateAppError = app.injector.instanceOf[views.html.awrs_application_error]
  val mockTemplateViewApp = app.injector.instanceOf[views.html.view_application.awrs_view_application]

  val testViewApplicationController: ViewApplicationController = new ViewApplicationController(mockMCC, mockApplicationService, mockIndexService, testKeyStoreService, testSave4LaterService, mockDeEnrolService, mockAuthConnector, mockAuditable, mockAccountUtils, mockMainStoreSave4LaterConnector, mockAppConfig,
    mockTemplateAppError, mockTemplateViewApp)

  def getCustomizedMap(businessType: Option[BusinessType] = testBusinessDetailsEntityTypes(CorporateBody),
                       businessCustomerDetails: Option[BusinessCustomerDetails] = None,
                       businessDetails: Option[BusinessDetails] = None,
                       businessRegistrationDetails: Option[BusinessRegistrationDetails] = None,
                       placeOfBusiness : Option[PlaceOfBusiness] = None,
                       businessContacts: Option[BusinessContacts] = None,
                       partnerDetails: Option[Partners] = None,
                       groupMembers: Option[GroupMembers] = None,
                       additionalBusinessPremises: Option[AdditionalBusinessPremisesList] = None,
                       businessDirectors: Option[BusinessDirectors] = None,
                       tradingActivity: Option[TradingActivity] = None,
                       products: Option[Products] = None,
                       suppliers: Option[Suppliers] = None,
                       applicationDeclaration: Option[ApplicationDeclaration] = None,
                       businessNameDetails: Option[BusinessNameDetails] = None,
                       tradingStartDetails: Option[NewAWBusiness] = None
                      ) = {
    val id = testUtr
    val cacheMap = Map[String, JsValue]() ++
      prepMap[BusinessType](businessTypeName, businessType) ++
      prepMap[BusinessDetails](businessDetailsName, businessDetails) ++
      prepMap[BusinessRegistrationDetails](businessRegistrationDetailsName, businessRegistrationDetails) ++
      prepMap[PlaceOfBusiness](placeOfBusinessName, placeOfBusiness) ++
      prepMap[BusinessContacts](businessContactsName, businessContacts) ++
      prepMap[BusinessCustomerDetails](businessCustomerDetailsName, businessCustomerDetails) ++
      prepMap[Partners](partnersName, partnerDetails) ++
      prepMap[AdditionalBusinessPremisesList](additionalBusinessPremisesName, additionalBusinessPremises) ++
      prepMap[BusinessDirectors](businessDirectorsName, businessDirectors) ++
      prepMap[TradingActivity](tradingActivityName, tradingActivity) ++
      prepMap[Products](productsName, products) ++
      prepMap[Suppliers](suppliersName, suppliers) ++
      prepMap[ApplicationDeclaration](applicationDeclarationName, applicationDeclaration) ++
      prepMap[GroupMembers](groupMembersName, groupMembers) ++
      prepMap[BusinessNameDetails](businessNameDetailsName, businessNameDetails) ++
      prepMap[NewAWBusiness](tradingStartDetailsName, tradingStartDetails)

    CacheMap(id, cacheMap)
  }

  def prepMap[T](key: String, optionParam: Option[T])(implicit format: Format[T]) =
    optionParam match {
      case Some(param) => Map[String, JsValue](key -> Json.toJson(param))
      case _ => Map[String, JsValue]()
    }

  "viewSection" should {
    "should return the edit view for the respective section when the relevant section string is passed in" in {

      val sectionTupleList = List(
        (businessDetailsName, getCustomizedMap(businessNameDetails = testBusinessNameDetails(), tradingStartDetails = newAWBusiness()), SubviewIds.businessDetailsId, Messages("awrs.view_application.business_details_text")),
        (businessRegistrationDetailsName, getCustomizedMap(businessRegistrationDetails = testBusinessRegistrationDetails()), SubviewIds.businessRegistrationDetailsId, Messages("awrs.view_application.business_registration_details_text")),
        (placeOfBusinessName, getCustomizedMap(placeOfBusiness = testPlaceOfBusinessDefault()), SubviewIds.placeOfBusinessId, Messages("awrs.view_application.place_of_business_text")),
        (businessContactsName, getCustomizedMap(businessContacts = testBusinessContactsDefault()), SubviewIds.businessContactsId, Messages("awrs.view_application.business_contacts_text")),
        (partnersName, getCustomizedMap(partnerDetails = Partners(List(testPartner(firstName = "Jon", lastName = "Snow")))), SubviewIds.partnerDetailsId, Messages("awrs.view_application.business_partners_text")),
        (groupMembersName, getCustomizedMap(groupMembers = testGroupMemberDetails), SubviewIds.groupMemberDetailsId, Messages("awrs.view_application.group_member_details_text")),
        (additionalBusinessPremisesName, getCustomizedMap(additionalBusinessPremises = testAdditionalPremisesList), SubviewIds.additionalPremisesId, Messages("awrs.view_application.additional_premises_text")),
        (businessDirectorsName, getCustomizedMap(businessDirectors = testBusinessDirectors), SubviewIds.businessDirectorsId, Messages("awrs.view_application.business_directors.index_text")),
        (tradingActivityName, getCustomizedMap(tradingActivity = testTradingActivity()), SubviewIds.tradingActivityId, Messages("awrs.view_application.trading_activity_text")),
        (productsName, getCustomizedMap(products = testProducts()), SubviewIds.productsId, Messages("awrs.view_application.products_text")),
        (suppliersName, getCustomizedMap(suppliers = testSuppliers), SubviewIds.suppliersId, Messages("awrs.view_application.suppliers_text"))
      )

      for ((sectionName: String, cachedMap: CacheMap, idName: String, expectedTitle: String) <- sectionTupleList) {
        viewSection(sectionName, cachedMap) {
          result =>
            val document = Jsoup.parse(contentAsString(result))
            status(result) shouldBe OK
            document.getElementById(idName).text should include(expectedTitle)
            document.getElementsByTag("title").text shouldBe Messages("awrs.generic.tab.title", Messages("awrs.view_application.section_title", expectedTitle))
        }
      }
    }

    "show not found when a section is not valid" in {
      viewSection("invalidName", getCustomizedMap(businessNameDetails = testBusinessNameDetails(), tradingStartDetails = newAWBusiness())) {
        result =>
          status(result) shouldBe NOT_FOUND
      }
    }

    "show not found when a cache map is empty" in {
      viewSectionEmptyCache("invalidName") {
        result =>
          status(result) shouldBe NOT_FOUND
      }
    }

    def viewSection(sectionName: String, cacheMap: CacheMap, printFriendly: Boolean = false)(test: Future[Result] => Any) = {
      setupMockSave4LaterService(fetchBusinessCustomerDetails = testBusinessCustomerDetails("SOP"), fetchAll = cacheMap)
      setAuthMocks()
      when(mockApplicationService.hasAPI5ApplicationChanged(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn Future.successful(false)
      val result = testViewApplicationController.viewSection(sectionName, printFriendly).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewSectionEmptyCache(sectionName: String, printFriendly: Boolean = false)(test: Future[Result] => Any) = {
      setupMockSave4LaterService(fetchBusinessCustomerDetails = testBusinessCustomerDetails("SOP"), fetchAll = None)
      setAuthMocks()
      when(mockApplicationService.hasAPI5ApplicationChanged(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn Future.successful(false)
      val result = testViewApplicationController.viewSection(sectionName, printFriendly).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }
  }
}
