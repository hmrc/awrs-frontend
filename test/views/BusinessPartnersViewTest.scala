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

package views

import builders.SessionBuilder
import connectors.mock.MockAuthConnector
import controllers.BusinessPartnersController
import forms.AWRSEnums.BooleanRadioEnum
import models._
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Result
import play.api.test.Helpers._
import services.DataCacheKeys._
import services.JourneyConstants
import services.mocks.MockSave4LaterService
import utils.TestConstants._
import utils.TestUtil.testBusinessCustomerDetails
import utils.{AccountUtils, AwrsUnitTestTraits, TestUtil}
import org.mockito.Matchers
import org.mockito.Mockito._
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment, Enrolments}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.play.frontend.auth

import scala.annotation.tailrec
import scala.concurrent.Future

class BusinessPartnersViewTest extends AwrsUnitTestTraits
  with MockSave4LaterService with MockAuthConnector {

  val businessPartnerDetails = Partner(None, Some("business partner first name"), Some("business partner last name"), None, None, Some("Yes"), testNino, None, None, Some("Yes"), None, None, None, None)

  object TestBusinessPartnersController extends BusinessPartnersController {
    override val authConnector = mockAuthConnector
    override val save4LaterService = TestSave4LaterService
    val signInUrl = "/sign-in"
  }

  private def testPartner(haveMore: Boolean = true) = TestUtil.testPartner(
    firstName = "Bob",
    lastName = "Smith",
    otherPartners = {
      haveMore match {
        case true => BooleanRadioEnum.YesString
        case false => BooleanRadioEnum.NoString
      }
    }
  )

  lazy val testPartnerDetails = Partners(List(testPartner(), testPartner(), testPartner(false)))


  "Business partner page" should {

    List("Partnership", "LLP", "LP").foreach {
      partnerType =>

        s"display the correct heading on the first page for for $partnerType" in {

          linearJourney(1, partnerType) {
            result =>
              val document = Jsoup.parse(contentAsString(result))
              document.select("#business_partner-title").text should be(Messages("awrs.business-partner.heading_1", Messages("awrs.generic.tell_us_about")))
          }
          editJourney(1, partnerType) {
            result =>
              val document = Jsoup.parse(contentAsString(result))
              document.select("#business_partner-title").text should be(Messages("awrs.business-partner.heading_1", Messages("awrs.generic.edit")))
          }
          postLinearJourneyAddition(1, partnerType) {
            result =>
              val document = Jsoup.parse(contentAsString(result))
              document.select("#business_partner-title").text should be(Messages("awrs.business-partner.heading_1", Messages("awrs.generic.tell_us_about")))
          }
        }

        s"display the correct heading on the subsequent pages for $partnerType" in {
          (2 to 3).foreach {
            id =>
              linearJourney(id, partnerType) {
                result =>
                  status(result) shouldBe OK
                  val document = Jsoup.parse(contentAsString(result))
                  document.getElementById("business_partner-title").text should be(Messages("awrs.business-partner.heading_2_or_more", Messages("awrs.generic.tell_us_about"), views.html.helpers.ordinalIntSuffix(id - 1)))
              }
              editJourney(id, partnerType) {
                result =>
                  status(result) shouldBe OK
                  val document = Jsoup.parse(contentAsString(result))
                  document.getElementById("business_partner-title").text should be(Messages("awrs.business-partner.heading_2_or_more", Messages("awrs.generic.edit"), views.html.helpers.ordinalIntSuffix(id - 1)))
              }
              postLinearJourneyAddition(id, partnerType) {
                result =>
                  val document = Jsoup.parse(contentAsString(result))
                  document.getElementById("business_partner-title").text should be(Messages("awrs.business-partner.heading_2_or_more", Messages("awrs.generic.tell_us_about"), views.html.helpers.ordinalIntSuffix(id - 1)))
              }
          }
        }
    }

  }

  "Request to showPartnerMemberDetails" should {

    "return required field for individual class" in {
      setupMockSave4LaterServiceWithOnly(fetchPartnerDetails = None)

      getWithAuthorisedUserSa(2) {
        result =>
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))

          val entityType = document.getElementById("entityType_field")
          entityType should not be null
          entityType.getElementById("entityType-individual") should not be null
          entityType.getElementById("entityType-corporate_body") should not be null
          entityType.getElementById("entityType-sole_trader") should not be null

          val individualDocument = document.getElementsByClass("individual").toString
          individualDocument should include("firstName_field")
          individualDocument should include("lastName_field")
          individualDocument should include("partnerAddress.postcode_field")
          individualDocument should include("partnerAddress.addressLine1_field")
          individualDocument should include("partnerAddress.addressLine2_field")
          individualDocument should include("partnerAddress.addressLine3_field")
          individualDocument should include("partnerAddress.addressLine4_field")
          individualDocument should include("doYouHaveNino")
          individualDocument should include("nino_field")
      }
    }

    "return required field for corporate_body class" in {
      setupMockSave4LaterServiceWithOnly(fetchPartnerDetails = None)

      getWithAuthorisedUserSa(2) {
        result =>
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))

          val entityType = document.getElementById("entityType_field")
          entityType should not be null
          entityType.getElementById("entityType-individual") should not be null
          entityType.getElementById("entityType-corporate_body") should not be null
          entityType.getElementById("entityType-sole_trader") should not be null

          val corporateDocument = document.getElementsByClass("corporate_body").toString
          corporateDocument should include("companyNames.businessName_field")
          corporateDocument should include("companyNames.tradingName_field")
          corporateDocument should include("partnerAddress.postcode_field")
          corporateDocument should include("partnerAddress.addressLine1_field")
          corporateDocument should include("partnerAddress.addressLine2_field")
          corporateDocument should include("partnerAddress.addressLine3_field")
          corporateDocument should include("partnerAddress.addressLine4_field")
          corporateDocument should include("doYouHaveVRN")
          corporateDocument should include("vrn_field")
          corporateDocument should include("utr_field")
      }
    }

    "return required field for sole_trader class" in {
      setupMockSave4LaterServiceWithOnly(fetchPartnerDetails = None)

      getWithAuthorisedUserSa(2) {
        result =>
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))

          val entityType = document.getElementById("entityType_field")
          entityType should not be null
          entityType.getElementById("entityType-individual") should not be null
          entityType.getElementById("entityType-corporate_body") should not be null
          entityType.getElementById("entityType-sole_trader") should not be null

          val soleTraderDocument = document.getElementsByClass("sole_trader").toString
          soleTraderDocument should include("firstName_field")
          soleTraderDocument should include("lastName_field")
          soleTraderDocument should include("tradingName_field")
          soleTraderDocument should include("partnerAddress.postcode_field")
          soleTraderDocument should include("partnerAddress.addressLine1_field")
          soleTraderDocument should include("partnerAddress.addressLine2_field")
          soleTraderDocument should include("partnerAddress.addressLine3_field")
          soleTraderDocument should include("partnerAddress.addressLine4_field")
          soleTraderDocument should include("doYouHaveNino")
          soleTraderDocument should include("nino_field")
          soleTraderDocument should include("doYouHaveVRN")
          soleTraderDocument should include("vrn_field")
          soleTraderDocument should include("utr_field")
      }
    }

    partnerEntities.foreach {
      legalEntity =>
        s"$legalEntity" should {
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

    val result = TestBusinessPartnersController.showPartnerMemberDetails(partnerId, isLinearMode = true, isNewRecord = true).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def linearJourney(partnerId: Int, businessType: String)(test: Future[Result] => Any): Future[Any] = {
    setupMockSave4LaterServiceWithOnly(fetchPartnerDetails = testPartnerDetails)
    setAuthMocks()

    val result = TestBusinessPartnersController.showPartnerMemberDetails(partnerId, isLinearMode = true, isNewRecord = true).apply(SessionBuilder.buildRequestWithSession(userId, businessType))
    test(result)
  }

  private def editJourney(partnerId: Int, businessType: String)(test: Future[Result] => Any): Future[Any] = {
    setupMockSave4LaterServiceWithOnly(fetchPartnerDetails = testPartnerDetails)
    setAuthMocks()
    val result = TestBusinessPartnersController.showPartnerMemberDetails(partnerId, isLinearMode = false, isNewRecord = false).apply(SessionBuilder.buildRequestWithSession(userId, businessType))
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

    val result = TestBusinessPartnersController.showPartnerMemberDetails(partnerId, isLinearMode = false, isNewRecord = true).apply(SessionBuilder.buildRequestWithSession(userId, businessType))

    test(result)
  }

  def eitherJourney(id: Int = 1, isLinearJourney: Boolean, isNewRecord: Boolean = true, entityType: String)(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(
      fetchBusinessCustomerDetails = testBusinessCustomerDetails(entityType),
      fetchPartnerDetails = testPartnerDetails
    )
    setAuthMocks()

    val result = TestBusinessPartnersController.showPartnerMemberDetails(id = id, isLinearMode = isLinearJourney, isNewRecord = isNewRecord).apply(SessionBuilder.buildRequestWithSession(userId, entityType))
    test(result)
  }

}
