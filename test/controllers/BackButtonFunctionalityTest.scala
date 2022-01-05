/*
 * Copyright 2022 HM Revenue & Customs
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
import models._
import org.scalatest.Assertion
import play.api.mvc.Result
import play.api.test.Helpers._
import services.DataCacheKeys._
import services.mocks.{MockKeyStoreService, MockSave4LaterService}
import services.{JourneyConstants, ServicesUnitTestFixture}
import utils.TestUtil._
import utils.{AwrsSessionKeys, AwrsUnitTestTraits}

import scala.concurrent.Future

class BackButtonFunctionalityTest extends AwrsUnitTestTraits
  with MockAuthConnector
  with MockSave4LaterService
  with MockKeyStoreService
  with ServicesUnitTestFixture {

  val mockTemplate = app.injector.instanceOf[views.html.awrs_application_error]
  val mockTemplateApp = app.injector.instanceOf[views.html.view_application.awrs_view_application]

  val testViewApplicationController: ViewApplicationController = new ViewApplicationController(
    mockMCC, mockApplicationService, mockIndexService, testKeyStoreService, testSave4LaterService, mockDeEnrolService,
    mockAuthConnector, mockAuditable, mockAccountUtils, mockMainStoreSave4LaterConnector, mockAppConfig, mockTemplate, mockTemplateApp)

  lazy val urlMap: Map[String, Option[Int] => String] = Map[String, (Option[Int]) => String](
    (businessDetailsName, (id: Option[Int]) => controllers.routes.TradingNameController.showTradingName(true).url),
    (businessRegistrationDetailsName, (id: Option[Int]) => controllers.routes.BusinessRegistrationDetailsController.showBusinessRegistrationDetails(isLinearMode = true).url),
    (placeOfBusinessName, (id: Option[Int]) => controllers.routes.PlaceOfBusinessController.showPlaceOfBusiness(isLinearMode = true).url),
    (businessContactsName, (id: Option[Int]) => controllers.routes.BusinessContactsController.showBusinessContacts(isLinearMode = true).url),
    (groupMembersName, (id: Option[Int]) => controllers.routes.GroupMemberController.showMemberDetails(id.fold(1)(x => x), isLinearMode = true, isNewRecord = true).url),
    (partnersName, (id: Option[Int]) => controllers.routes.BusinessPartnersController.showPartnerMemberDetails(id.fold(1)(x => x), isLinearMode = true, isNewRecord = true).url),
    (additionalBusinessPremisesName, (id: Option[Int]) => controllers.routes.AdditionalPremisesController.showPremisePage(id.fold(1)(x => x), isLinearMode = true, isNewRecord = true).url),
    (businessDirectorsName, (id: Option[Int]) => controllers.routes.BusinessDirectorsController.showBusinessDirectors(id.fold(1)(x => x), isLinearMode = true, isNewRecord = true).url),
    (tradingActivityName, (id: Option[Int]) => controllers.routes.TradingActivityController.showTradingActivity(isLinearMode = true).url),
    (productsName, (id: Option[Int]) => controllers.routes.ProductsController.showProducts(isLinearMode = true).url),
    (suppliersName, (id: Option[Int]) => controllers.routes.SupplierAddressesController.showSupplierAddressesPage(id.fold(1)(x => x), isLinearMode = true, isNewRecord = true).url)
  )
  
  lazy val urlIndex: String = controllers.routes.IndexController.showIndex.url

  lazy val defaultEntriesForMultiEntryPages = 3

  lazy val testGroup: Int => Option[GroupMembers] = (total: Int) => {
    require(total >= 0)
    total match {
      case 0 => None
      case num => GroupMembers(List.fill(total)(testGroupMember))
    }
  }: Option[GroupMembers]

  lazy val testPartners: Int => Option[Partners] = (total: Int) => {
    require(total >= 0)
    total match {
      case 0 => None
      case num => Partners(List.fill(total)(testPartner()))
    }
  }: Option[Partners]

  lazy val testPremises: Int => Option[AdditionalBusinessPremisesList] = (total: Int) => {
    require(total >= 0)
    total match {
      case 0 => None
      case num => AdditionalBusinessPremisesList(List.fill(total)(testAdditionalBusinessPremises))
    }
  }: Option[AdditionalBusinessPremisesList]

  lazy val testDirectors: Int => Option[BusinessDirectors] = (total: Int) => {
    require(total >= 0)
    total match {
      case 0 => None
      case num => BusinessDirectors(List.fill(total)(testBusinessDirector))
    }
  }: Option[BusinessDirectors]

  lazy val testSuppliers: Int => Option[Suppliers] = (total: Int) => {
    require(total >= 0)
    total match {
      case 0 => None
      case num => Suppliers(List.fill(total)(testSupplier()))
    }
  }: Option[Suppliers]

  def mock(groupMembers: Int = defaultEntriesForMultiEntryPages,
           partners: Int = defaultEntriesForMultiEntryPages,
           premises: Int = defaultEntriesForMultiEntryPages,
           directors: Int = defaultEntriesForMultiEntryPages,
           suppliers: Int = defaultEntriesForMultiEntryPages
          ): Unit = {
    setupMockSave4LaterServiceWithOnly(
      fetchGroupMemberDetails = testGroup(groupMembers),
      fetchPartnerDetails = testPartners(partners),
      fetchAdditionalBusinessPremisesList = testPremises(premises),
      fetchBusinessDirectors = testDirectors(directors),
      fetchSuppliers = testSuppliers(suppliers)
    )
  }

  def getPreviousPageInJourney(journey: Seq[String], startLocation: String, expectedIdFunction: (String) => Option[Int])(section: String, id: Option[Int] = None): String = {
    val idGetOrElse = id.fold(1)(x => x)
    section match {
      // if the current section is the same as where the journey began then the expected back url is the index page
      case `startLocation` if idGetOrElse == 1 => urlIndex
      case _ if idGetOrElse > 1 =>
        // if in a multi section page and the id is > 1 then expect the same page back with the decremented id
        urlMap.get(section).fold(urlIndex)(urlFunction => urlFunction(idGetOrElse - 1))
      case _ =>
        val currentIndex = journey.indexOf(section)
        val startIndex = journey.indexOf(startLocation)
        currentIndex < startIndex match {
          case true => urlIndex // if the the current location must have came before the start location of the journey in the session, then return to index
          case false =>
            // otherwise get the previous section in the journey, or index if the current section is the start of the journey
            currentIndex - 1 match {
              case -1 => urlIndex
              case index =>
                val page = journey(index)
                urlMap.get(page).fold(urlIndex)(urlFunction => urlFunction(expectedIdFunction(page)))
            }
        }
    }
  }

  private def callBackFrom(businessEntity: String, startSection: String)(currentSection: String, id: Option[Int]): Future[Result] = {
    setAuthMocks()
    val request = SessionBuilder.buildRequestWithSession(userId, businessEntity)
    val requestWithStart = request.withSession(request.session.+((AwrsSessionKeys.sessionJouneyStartLocation, startSection)).data.toSeq: _*)
    testViewApplicationController.backFrom(currentSection, id).apply(requestWithStart)
  }

  lazy val legalEntityList = List("SOP", "Partnership", "LTD", "LLP", "LP", "LLP_GRP", "LTD_GRP")

  def testPage(groupMembers: Int = defaultEntriesForMultiEntryPages,
               partners: Int = defaultEntriesForMultiEntryPages,
               premises: Int = defaultEntriesForMultiEntryPages,
               directors: Int = defaultEntriesForMultiEntryPages,
               suppliers: Int = defaultEntriesForMultiEntryPages)
              (legal: String, startSection: String = businessDetailsName)(currentSection: String, id: Option[Int] = None): Assertion = {
    require(groupMembers >= 0)
    require(partners >= 0)
    require(premises >= 0)
    require(directors >= 0)
    require(suppliers >= 0)
    val journey = JourneyConstants.getJourney(legal)
    val result = callBackFrom(legal, startSection)(currentSection, id)
    lazy val expInt = (i: Int) => i match {
      case 0 => None
      case num => Some(num)
    }
    lazy val expectedIdFunction = (section: String) => section match {
      case `groupMembersName` => expInt(groupMembers)
      case `partnersName` => expInt(partners)
      case `additionalBusinessPremisesName` => expInt(premises)
      case `businessDirectorsName` => expInt(directors)
      case `suppliersName` => expInt(suppliers)
      case _ => None
    }

    status(result) mustBe SEE_OTHER
    val expected = getPreviousPageInJourney(journey, startSection, expectedIdFunction)(currentSection, id)
    withClue(s"Current section: $currentSection\n" +
      s"Expected previous = $expected\n\n") {
      redirectLocation(result).get mustBe expected
    }
  }

  def testJourney(groupMembers: Int = defaultEntriesForMultiEntryPages,
                  partners: Int = defaultEntriesForMultiEntryPages,
                  premises: Int = defaultEntriesForMultiEntryPages,
                  directors: Int = defaultEntriesForMultiEntryPages,
                  suppliers: Int = defaultEntriesForMultiEntryPages)
                 (startSection: String = businessDetailsName): String => Unit =
    (legal: String) => {
      require(groupMembers >= 0)
      require(partners >= 0)
      require(premises >= 0)
      require(directors >= 0)
      require(suppliers >= 0)
      mock(groupMembers, partners, premises, directors, suppliers)
      val journey = JourneyConstants.getJourney(legal)
      journey.foreach {
        page =>
          testPage(
            groupMembers,
            partners,
            premises,
            directors,
            suppliers
          )(legal, startSection)(page)
      }
    }

  "The backFrom method" must {
    for (legal <- legalEntityList) {
      s"link to the correct previous pages for $legal journey" in {
        // this version tests that when the previous section is a multi page section then the ids are set to
        // the defaultEntriesForMultiEntryPages
        testJourney()()(legal)
        beforeEach()
        // this version tests that when the previous section is a multi page section then the first entry page is used
        testJourney(0, 0, 0, 0, 0)()(legal)
      }
    }

    "when the user is on the page where their journey began, return to index page" in {
      // LLP_GRP is used here because it has all the sections in its journey
      val LLP_GRP = "LLP_GRP"
      JourneyConstants.getJourney(LLP_GRP)
      testPage()(LLP_GRP, startSection = businessDetailsName)(businessDetailsName)
      beforeEach()
      testPage()(LLP_GRP, startSection = businessRegistrationDetailsName)(businessRegistrationDetailsName)
      beforeEach()
      testPage()(LLP_GRP, startSection = placeOfBusinessName)(placeOfBusinessName)
      beforeEach()
      testPage()(LLP_GRP, startSection = businessContactsName)(businessContactsName)
      beforeEach()
      testPage()(LLP_GRP, startSection = groupMembersName)(groupMembersName)
      beforeEach()
      testPage()(LLP_GRP, startSection = partnersName)(partnersName)
      beforeEach()
      testPage()(LLP_GRP, startSection = additionalBusinessPremisesName)(additionalBusinessPremisesName)
      beforeEach()
      testPage()(LLP_GRP, startSection = businessDirectorsName)(businessDirectorsName)
      beforeEach()
      testPage()(LLP_GRP, startSection = tradingActivityName)(tradingActivityName)
      beforeEach()
      testPage()(LLP_GRP, startSection = productsName)(productsName)
      beforeEach()
      testPage()(LLP_GRP, startSection = suppliersName)(suppliersName)
    }

    "when the user is in a subsection of a multi-page section, goto the previous page in the subsection even if they are on the page where their journey began" in {
      val LLP_GRP = "LLP_GRP"
      JourneyConstants.getJourney(LLP_GRP)
      (1 to defaultEntriesForMultiEntryPages).foreach {
        id =>
          testPage()(LLP_GRP, startSection = groupMembersName)(groupMembersName, id = id)
          beforeEach()
          testPage()(LLP_GRP, startSection = partnersName)(partnersName, id = id)
          beforeEach()
          testPage()(LLP_GRP, startSection = additionalBusinessPremisesName)(additionalBusinessPremisesName, id = id)
          beforeEach()
          testPage()(LLP_GRP, startSection = businessDirectorsName)(businessDirectorsName, id = id)
          beforeEach()
          testPage()(LLP_GRP, startSection = suppliersName)(suppliersName, id = id)
      }
    }

    "if for whatever reason the current section is prior to the session variable for where the journey began, then go back to index" in {
      val LLP_GRP = "LLP_GRP"
      JourneyConstants.getJourney(LLP_GRP)
      testPage()(LLP_GRP, startSection = suppliersName)(businessDetailsName)
      beforeEach()
      testPage()(LLP_GRP, startSection = suppliersName)(businessRegistrationDetailsName)
      beforeEach()
      testPage()(LLP_GRP, startSection = placeOfBusinessName)(placeOfBusinessName)
      beforeEach()
      testPage()(LLP_GRP, startSection = suppliersName)(businessContactsName)
      beforeEach()
      testPage()(LLP_GRP, startSection = suppliersName)(groupMembersName)
      beforeEach()
      testPage()(LLP_GRP, startSection = suppliersName)(partnersName)
      beforeEach()
      testPage()(LLP_GRP, startSection = suppliersName)(additionalBusinessPremisesName)
      beforeEach()
      testPage()(LLP_GRP, startSection = suppliersName)(businessDirectorsName)
      beforeEach()
      testPage()(LLP_GRP, startSection = suppliersName)(tradingActivityName)
      beforeEach()
      testPage()(LLP_GRP, startSection = suppliersName)(productsName)
    }
  }

}
