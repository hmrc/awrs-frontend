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
import controllers.BusinessRegistrationDetailsController
import forms.BusinessRegistrationDetailsForm
import models._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.DataCacheKeys._
import services.{BusinessMatchingService, JourneyConstants, ServicesUnitTestFixture}
import utils.TestUtil._
import utils.{AwrsUnitTestTraits, TestUtil}
import views.Configuration.NewApplicationMode
import views.html.awrs_business_registration_details

import scala.concurrent.Future

class BusinessRegistrationDetailsViewTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture {

  val mockBusinessMatchingService: BusinessMatchingService = mock[BusinessMatchingService]

  val template: awrs_business_registration_details = app.injector.instanceOf[views.html.awrs_business_registration_details]

  def testRequest(businessRegDetails: BusinessRegistrationDetails, entityType: String): FakeRequest[AnyContentAsFormUrlEncoded] =
    TestUtil.populateFakeRequest[BusinessRegistrationDetails](FakeRequest(), BusinessRegistrationDetailsForm.businessRegistrationDetailsValidationForm(entityType), businessRegDetails)

  val testBusinessRegistrationDetailsController: BusinessRegistrationDetailsController =
    new BusinessRegistrationDetailsController(mockMCC, mockBusinessMatchingService, mockBusinessDetailsService, testSave4LaterService, mockDeEnrolService, mockAuthConnector, mockAuditable, mockAccountUtils, mockAppConfig, template) {
      override val signInUrl: String = applicationConfig.signIn
    }

  val testUtr: String = "2" * 10

  "BusinessRegistrationDetailsController" must {

    Seq("Partnership", "LP", "LLP").foreach(entity =>
      s"Display the correct header for $entity" in {
        linearJourney(entity) {
          result =>
            val doc = Jsoup.parse(contentAsString(result))
            doc.getElementById("additional-information-heading").text() mustBe Messages("awrs.business_registration_details.heading.partnership", Messages("awrs.generic.enter"))
        }
        editJourney(entity) {
          result =>
            val doc = Jsoup.parse(contentAsString(result))
            doc.getElementById("additional-information-heading").text() mustBe Messages("awrs.business_registration_details.heading.partnership", Messages("awrs.generic.edit"))
        }
      }
    )

    Seq("LLP_GRP", "LTD_GRP").foreach(entity =>
      s"Display the correct header for $entity" in {
        linearJourney(entity) {
          result =>
            val doc = Jsoup.parse(contentAsString(result))
            doc.getElementById("additional-information-heading").text() mustBe Messages("awrs.business_registration_details.heading.group", Messages("awrs.generic.enter"))
        }
        editJourney(entity) {
          result =>
            val doc = Jsoup.parse(contentAsString(result))
            doc.getElementById("additional-information-heading").text() mustBe Messages("awrs.business_registration_details.heading.group", Messages("awrs.generic.edit"))
        }
      }
    )

    Seq("SOP", "LTD").foreach(entity =>
      s"Display the correct header for $entity" in {
        linearJourney(entity) {
          result =>
            val doc = Jsoup.parse(contentAsString(result))
            doc.getElementById("additional-information-heading").text() mustBe Messages("awrs.business_registration_details.heading", Messages("awrs.generic.enter"))
        }
        editJourney(entity) {
          result =>
            val doc = Jsoup.parse(contentAsString(result))
            doc.getElementById("additional-information-heading").text() mustBe Messages("awrs.business_registration_details.heading", Messages("awrs.generic.edit"))
        }
      }
    )

    Seq("Partnership", "LP", "LLP", "LLP_GRP", "LTD_GRP", "SOP", "LTD").foreach(entity =>
      s"Display the selection of Ids for $entity" in {
        val ids = getIds(entity)
        val test: Future[Result] => Unit = result => {
          implicit val doc: Document = Jsoup.parse(contentAsString(result))
          testId(shouldExist = ids.utr)(targetFieldId = "utr")
          testId(shouldExist = ids.crn)(targetFieldId = "companyRegDetails.companyRegistrationNumber")
          testId(shouldExist = ids.nino)(targetFieldId = "NINO")
          testId(shouldExist = ids.vrn)(targetFieldId = "vrn")
        }
        linearJourney(entity)(test)
        editJourney(entity)(test)
      }
    )

    Seq("Partnership", "LP", "LLP", "LLP_GRP", "LTD_GRP", "SOP", "LTD").foreach(entity =>
      s"In the linear journey, auto populate utr from business customer frontend for $entity" in {
        linearJourney(entity) {
          result =>
            val doc = Jsoup.parse(contentAsString(result))
            doc.getElementById("utr").`val`() mustBe testUtr
        }
      }
    )

    allEntities.foreach {
      legalEntity =>
        s"$legalEntity" must {
          Seq(true, false).foreach {
            isLinear =>
              s"see a progress message for the isLinearJourney is set to $isLinear" in {
                val test: Future[Result] => Unit = result => {
                  implicit val doc = Jsoup.parse(contentAsString(result))
                  testId(shouldExist = true)(targetFieldId = "progress-text")
                  val journey = JourneyConstants.getJourney(legalEntity)
                  val expectedSectionNumber = journey.indexOf(businessRegistrationDetailsName) + 1
                  val totalSectionsForBusinessType = journey.size
                  val expectedSectionName = legalEntity match {
                    case "LLP_GRP" | "LTD_GRP" => Messages("awrs.index_page.group_business_registration_details_text")
                    case "Partnership" | "LP" | "LLP" => Messages("awrs.index_page.partnership_registration_details_text")
                    case _ => Messages("awrs.index_page.business_registration_details_text")
                  }
                  val expected = Messages("awrs.generic.section_progress", expectedSectionNumber, totalSectionsForBusinessType, expectedSectionName)
                  testText(expectedText = expected)(targetFieldId = "progress-text")
                }
                eitherJourney(isLinearJourney = isLinear, entityType = legalEntity)(test)
              }
          }
        }
    }

  }

  def editJourney(entityType: String)(test: Future[Result] => Any): Unit = {
    setupMockSave4LaterServiceWithOnly(
      fetchBusinessRegistrationDetails = testBusinessRegistrationDetails(entityType)
    )
    setAuthMocks()
    when(mockBusinessDetailsService.businessDetailsPageRenderMode(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(NewApplicationMode))
    val result = testBusinessRegistrationDetailsController.showBusinessRegistrationDetails(isLinearMode = false).apply(SessionBuilder.buildRequestWithSession(userId, entityType))
    test(result)
  }

  def linearJourney(entityType: String)(test: Future[Result] => Any): Unit = {
    setupMockSave4LaterServiceWithOnly(
      fetchBusinessCustomerDetails = testBusinessCustomerDetails(entityType).copy(utr = testUtr),
      fetchBusinessRegistrationDetails = None

    )
    when(mockBusinessDetailsService.businessDetailsPageRenderMode(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(NewApplicationMode))
    setAuthMocks()
    val result = testBusinessRegistrationDetailsController.showBusinessRegistrationDetails(isLinearMode = true).apply(SessionBuilder.buildRequestWithSession(userId, entityType))
    test(result)
  }

  def eitherJourney(isLinearJourney: Boolean, entityType: String)(test: Future[Result] => Any): Unit = {
    setupMockSave4LaterServiceWithOnly(
      fetchBusinessCustomerDetails = testBusinessCustomerDetails(entityType),
      fetchBusinessRegistrationDetails = testBusinessRegistrationDetails(entityType)
    )
    when(mockBusinessDetailsService.businessDetailsPageRenderMode(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(NewApplicationMode))
    setAuthMocks()
    val result = testBusinessRegistrationDetailsController.showBusinessRegistrationDetails(isLinearMode = isLinearJourney).apply(SessionBuilder.buildRequestWithSession(userId, entityType))
    test(result)
  }

}
