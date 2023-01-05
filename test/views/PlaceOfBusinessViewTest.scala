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
import config.ApplicationConfig
import controllers.PlaceOfBusinessController
import forms.PlaceOfBusinessForm
import models.PlaceOfBusiness
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.DataCacheKeys._
import services.{JourneyConstants, ServicesUnitTestFixture}
import utils.TestUtil._
import utils.{AwrsUnitTestTraits, TestUtil}

import scala.concurrent.Future

class PlaceOfBusinessViewTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture {

  val businessCustomerDetailsFormId = "businessCustomerDetails"

  implicit val mockConfig: ApplicationConfig = mockAppConfig

  def testRequest(premises: PlaceOfBusiness) =
    TestUtil.populateFakeRequest[PlaceOfBusiness](FakeRequest(), PlaceOfBusinessForm.placeOfBusinessValidationForm, premises)

  val template = app.injector.instanceOf[views.html.awrs_principal_place_of_business]

  val testPlaceOfBusinessController = new PlaceOfBusinessController(mockMCC, testSave4LaterService, testKeyStoreService, mockAuthConnector, mockDeEnrolService, mockAuditable, mockAccountUtils, mockAppConfig, template)

  "BusinessContactsController" must {

    "AWRS Contact Details entered " must {

      "must see the 'Enter' view if they are on the linear journey" in {
        linearJourney {
          result =>
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("contact-information-heading").text() mustBe Messages("awrs.place_of_business.heading", Messages("awrs.generic.enter"))
            document.getElementById("mainPlaceOfBusiness-no-content") mustBe null
            document.getElementById("mainPlaceOfBusiness-api5-content") mustNot be(null)
        }
      }

      "must see the 'Edit' view if they are on the edit journey" in {
        editJourney {
          result =>
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("contact-information-heading").text() mustBe Messages("awrs.place_of_business.heading", Messages("awrs.generic.edit"))
            document.getElementById("mainPlaceOfBusiness-api5-content") mustNot be(null)
            document.getElementById("mainPlaceOfBusiness-no-content") mustBe null
        }
      }
    }

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
                  val expectedSectionNumber = journey.indexOf(placeOfBusinessName) + 1
                  val totalSectionsForBusinessType = journey.size
                  val expectedSectionName = legalEntity match {
                    case "LLP_GRP" | "LTD_GRP" => Messages("awrs.index_page.group_business_place_of_business_text")
                    case "Partnership" | "LP" | "LLP" => Messages("awrs.index_page.partnership_place_of_business_text")
                    case _ => Messages("awrs.index_page.business_place_of_business_text")
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

  private def editJourney(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(
      fetchBusinessCustomerDetails = testBusinessCustomerDetails("SOP"),
      fetchPlaceOfBusiness = testPlaceOfBusinessDefault()
    )
    setAuthMocks()
    val result = testPlaceOfBusinessController.showPlaceOfBusiness(isLinearMode = false).apply(SessionBuilder.buildRequestWithSession(userId, testBusinessCustomerDetails("SOP").businessType.get))
    test(result)
  }

  private def linearJourney(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(
      fetchBusinessCustomerDetails = testBusinessCustomerDetails("SOP"),
      fetchPlaceOfBusiness = None
    )
    setAuthMocks()
    val result = testPlaceOfBusinessController.showPlaceOfBusiness(isLinearMode = true).apply(SessionBuilder.buildRequestWithSession(userId, testBusinessCustomerDetails("SOP").businessType.get))

    test(result)
  }

  def eitherJourney(isLinearJourney: Boolean, entityType: String)(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(
      fetchBusinessCustomerDetails = testBusinessCustomerDetails(entityType),
      fetchPlaceOfBusiness = testPlaceOfBusinessDefault()
    )
    setAuthMocks()
    val result = testPlaceOfBusinessController.showPlaceOfBusiness(isLinearMode = isLinearJourney).apply(SessionBuilder.buildRequestWithSession(userId, entityType))
    test(result)
  }

}
