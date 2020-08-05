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
import controllers.BusinessPartnersController
import forms.AWRSEnums.BooleanRadioEnum
import models._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.test.Helpers._
import services.DataCacheKeys._
import services.JourneyConstants
import services.mocks.MockSave4LaterService
import utils.TestConstants._
import utils.TestUtil.testBusinessCustomerDetails
import utils.{AwrsUnitTestTraits, TestUtil}

import scala.annotation.tailrec
import scala.concurrent.Future

class BusinessPartnersViewTest extends AwrsUnitTestTraits
  with MockSave4LaterService with MockAuthConnector {

  val template = app.injector.instanceOf[views.html.awrs_partner_member_details]

  val businessPartnerDetails = Partner(None, Some("business partner first name"), Some("business partner last name"), None, None, Some("Yes"), testNino, None, None, Some("Yes"), None, None, None, None)

  val testBusinessPartnersController: BusinessPartnersController =
    new BusinessPartnersController(mockMCC, testSave4LaterService, mockDeEnrolService, mockAuthConnector, mockAuditable, mockAccountUtils, mockAppConfig, template) {
      override val signInUrl = "/sign-in"
    }

  private def testPartner(haveMore: Boolean = true) = TestUtil.testPartner(
    firstName = "Bob",
    lastName = "Smith",
    otherPartners = {
      if (haveMore) {
        BooleanRadioEnum.YesString
      } else {
        BooleanRadioEnum.NoString
      }
    }
  )

  override def beforeEach(): Unit = {
    super.beforeEach()

    when(mockAppConfig.countryCodes)
      .thenReturn(mockCountryCodes)
    when(mockCountryCodes.getAddressWithCountry(ArgumentMatchers.any()))
      .thenReturn(Some(TestUtil.testAddress))
  }

  lazy val testPartnerDetails = Partners(List(testPartner(), testPartner(), testPartner(false)))


  "Business partner page" must {

    List("Partnership", "LLP", "LP").foreach {
      partnerType =>

        s"display the correct heading on the first page for for $partnerType" in {

          linearJourney(1, partnerType) {
            result =>
              val document = Jsoup.parse(contentAsString(result))
              document.select("#business_partner-title").text must be(Messages("awrs.business-partner.heading_1", Messages("awrs.generic.tell_us_about")))
          }
          editJourney(1, partnerType) {
            result =>
              val document = Jsoup.parse(contentAsString(result))
              document.select("#business_partner-title").text must be(Messages("awrs.business-partner.heading_1", Messages("awrs.generic.edit")))
          }
          postLinearJourneyAddition(1, partnerType) {
            result =>
              val document = Jsoup.parse(contentAsString(result))
              document.select("#business_partner-title").text must be(Messages("awrs.business-partner.heading_1", Messages("awrs.generic.tell_us_about")))
          }
        }

        s"display the correct heading on the subsequent pages for $partnerType" in {
          (2 to 3).foreach {
            id =>
              linearJourney(id, partnerType) {
                result =>
                  status(result) mustBe OK
                  val document = Jsoup.parse(contentAsString(result))
                  document.getElementById("business_partner-title").text must be(Messages("awrs.business-partner.heading_2_or_more", Messages("awrs.generic.tell_us_about"), views.html.helpers.ordinalIntSuffix(id - 1)))
              }
              editJourney(id, partnerType) {
                result =>
                  status(result) mustBe OK
                  val document = Jsoup.parse(contentAsString(result))
                  document.getElementById("business_partner-title").text must be(Messages("awrs.business-partner.heading_2_or_more", Messages("awrs.generic.edit"), views.html.helpers.ordinalIntSuffix(id - 1)))
              }
              postLinearJourneyAddition(id, partnerType) {
                result =>
                  val document = Jsoup.parse(contentAsString(result))
                  document.getElementById("business_partner-title").text must be(Messages("awrs.business-partner.heading_2_or_more", Messages("awrs.generic.tell_us_about"), views.html.helpers.ordinalIntSuffix(id - 1)))
              }
          }
        }
    }

  }

  "Request to showPartnerMemberDetails" must {

    "return required field for individual class" in {
      setupMockSave4LaterServiceWithOnly(fetchPartnerDetails = None)

      getWithAuthorisedUserSa(2) {
        result =>
          status(result) mustBe OK
          val document = Jsoup.parse(contentAsString(result))

          val entityType = document.getElementById("entityType_field")
          entityType must not be null
          entityType.getElementById("entityType-individual") must not be null
          entityType.getElementById("entityType-corporate_body") must not be null
          entityType.getElementById("entityType-sole_trader") must not be null

          val individualDocument = document.getElementsByClass("individual").toString
          individualDocument must include("firstName_field")
          individualDocument must include("lastName_field")
          individualDocument must include("partnerAddress.postcode_field")
          individualDocument must include("partnerAddress.addressLine1_field")
          individualDocument must include("partnerAddress.addressLine2_field")
          individualDocument must include("partnerAddress.addressLine3_field")
          individualDocument must include("partnerAddress.addressLine4_field")
          individualDocument must include("doYouHaveNino")
          individualDocument must include("nino_field")
      }
    }

    "return required field for corporate_body class" in {
      setupMockSave4LaterServiceWithOnly(fetchPartnerDetails = None)

      getWithAuthorisedUserSa(2) {
        result =>
          status(result) mustBe OK
          val document = Jsoup.parse(contentAsString(result))

          val entityType = document.getElementById("entityType_field")
          entityType must not be null
          entityType.getElementById("entityType-individual") must not be null
          entityType.getElementById("entityType-corporate_body") must not be null
          entityType.getElementById("entityType-sole_trader") must not be null

          val corporateDocument = document.getElementsByClass("corporate_body").toString
          corporateDocument must include("companyNames.businessName_field")
          corporateDocument must include("companyNames.tradingName_field")
          corporateDocument must include("partnerAddress.postcode_field")
          corporateDocument must include("partnerAddress.addressLine1_field")
          corporateDocument must include("partnerAddress.addressLine2_field")
          corporateDocument must include("partnerAddress.addressLine3_field")
          corporateDocument must include("partnerAddress.addressLine4_field")
          corporateDocument must include("doYouHaveVRN")
          corporateDocument must include("vrn_field")
          corporateDocument must include("utr_field")
      }
    }

    "return required field for sole_trader class" in {
      setupMockSave4LaterServiceWithOnly(fetchPartnerDetails = None)

      getWithAuthorisedUserSa(2) {
        result =>
          status(result) mustBe OK
          val document = Jsoup.parse(contentAsString(result))

          val entityType = document.getElementById("entityType_field")
          entityType must not be null
          entityType.getElementById("entityType-individual") must not be null
          entityType.getElementById("entityType-corporate_body") must not be null
          entityType.getElementById("entityType-sole_trader") must not be null

          val soleTraderDocument = document.getElementsByClass("sole_trader").toString
          soleTraderDocument must include("firstName_field")
          soleTraderDocument must include("lastName_field")
          soleTraderDocument must include("tradingName_field")
          soleTraderDocument must include("partnerAddress.postcode_field")
          soleTraderDocument must include("partnerAddress.addressLine1_field")
          soleTraderDocument must include("partnerAddress.addressLine2_field")
          soleTraderDocument must include("partnerAddress.addressLine3_field")
          soleTraderDocument must include("partnerAddress.addressLine4_field")
          soleTraderDocument must include("doYouHaveNino")
          soleTraderDocument must include("nino_field")
          soleTraderDocument must include("doYouHaveVRN")
          soleTraderDocument must include("vrn_field")
          soleTraderDocument must include("utr_field")
      }
    }

    partnerEntities.foreach {
      legalEntity =>
        s"$legalEntity" must {
          Seq(true, false).foreach {
            isLinear =>
              s"see a progress message for the isLinearJourney is set to $isLinear" in {
                val test: Future[Result] => Unit = result => {
                  implicit val doc = Jsoup.parse(contentAsString(result))
                  testId(shouldExist = true)(targetFieldId = "progress-text")
                  val journey = JourneyConstants.getJourney(legalEntity)
                  val expectedSectionNumber = journey.indexOf(partnersName) + 1
                  val totalSectionsForBusinessType = journey.size
                  val expectedSectionName = Messages("awrs.index_page.business_partners_text")
                  val expected = Messages("awrs.generic.section_progress", expectedSectionNumber, totalSectionsForBusinessType, expectedSectionName)
                  testText(expectedText = expected)(targetFieldId = "progress-text")
                }
                eitherJourney(isLinearJourney = isLinear, entityType = legalEntity)(test)
              }
          }
        }
    }
  }

  private def getWithAuthorisedUserSa(partnerId: Int)(test: Future[Result] => Any): Future[Any] = {
    setupMockSave4LaterServiceWithOnly(fetchPartnerDetails = testPartnerDetails)
    setAuthMocks()

    val result = testBusinessPartnersController.showPartnerMemberDetails(partnerId, isLinearMode = true, isNewRecord = true).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def linearJourney(partnerId: Int, businessType: String)(test: Future[Result] => Any): Future[Any] = {
    setupMockSave4LaterServiceWithOnly(fetchPartnerDetails = testPartnerDetails)
    setAuthMocks()

    val result = testBusinessPartnersController.showPartnerMemberDetails(partnerId, isLinearMode = true, isNewRecord = true).apply(SessionBuilder.buildRequestWithSession(userId, businessType))
    test(result)
  }

  private def editJourney(partnerId: Int, businessType: String)(test: Future[Result] => Any): Future[Any] = {
    setupMockSave4LaterServiceWithOnly(fetchPartnerDetails = testPartnerDetails)
    setAuthMocks()
    val result = testBusinessPartnersController.showPartnerMemberDetails(partnerId, isLinearMode = false, isNewRecord = false).apply(SessionBuilder.buildRequestWithSession(userId, businessType))
    test(result)
  }

  private def postLinearJourneyAddition(partnerId: Int, businessType: String)(test: Future[Result] => Any): Future[Any] = {
    val partners: Option[Partners] = partnerId - 1 match {
      case 0 => None
      case num =>
        @tailrec
        def loop(list: List[Partner], numLeft: Int): List[Partner] = numLeft match {
          case 0 => list
          case 1 => loop(list.:+(testPartner(false)), 0)
          case _ => loop(list.:+(testPartner(true)), numLeft - 1)
        }

        val list = loop(List(), partnerId - 1)
        Partners(partners = list)
    }
    setupMockSave4LaterServiceWithOnly(fetchPartnerDetails = partners)
    setAuthMocks()

    val result = testBusinessPartnersController.showPartnerMemberDetails(partnerId, isLinearMode = false, isNewRecord = true).apply(SessionBuilder.buildRequestWithSession(userId, businessType))

    test(result)
  }

  def eitherJourney(id: Int = 1, isLinearJourney: Boolean, isNewRecord: Boolean = true, entityType: String)(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(
      fetchBusinessCustomerDetails = testBusinessCustomerDetails(entityType),
      fetchPartnerDetails = testPartnerDetails
    )
    setAuthMocks()

    val result = testBusinessPartnersController.showPartnerMemberDetails(id = id, isLinearMode = isLinearJourney, isNewRecord = isNewRecord).apply(SessionBuilder.buildRequestWithSession(userId, entityType))
    test(result)
  }

}
