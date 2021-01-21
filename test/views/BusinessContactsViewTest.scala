/*
 * Copyright 2021 HM Revenue & Customs
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
import controllers.BusinessContactsController
import forms.BusinessContactsForm
import models.BusinessContacts
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.DataCacheKeys._
import services.{EmailVerificationService, JourneyConstants, ServicesUnitTestFixture}
import utils.TestUtil._
import utils.{AwrsUnitTestTraits, TestUtil}

import scala.concurrent.Future

class BusinessContactsViewTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture {

  val template = app.injector.instanceOf[views.html.awrs_business_contacts]

  val businessCustomerDetailsFormId = "businessCustomerDetails"
  val mockEmailVerificationService: EmailVerificationService = mock[EmailVerificationService]

  implicit val mockConfig: ApplicationConfig = mockAppConfig

  def testRequest(premises: BusinessContacts): FakeRequest[AnyContentAsFormUrlEncoded] =
    TestUtil.populateFakeRequest[BusinessContacts](FakeRequest(), BusinessContactsForm.businessContactsValidationForm, premises)

  val testBusinessContactsController: BusinessContactsController =
    new BusinessContactsController(mockMCC, mockEmailVerificationService,testSave4LaterService, mockDeEnrolService, mockAuthConnector, mockAuditable, mockAccountUtils, mockAppConfig, template) {
      override val signInUrl = "/sign-in"
    }

  def reviewDetailMatch(reviewDetail: Element): Any = {

    def testIfExists(someString: Option[String]) =
      someString match {
        case Some(string) => reviewDetail.text() must include(string)
        case _ =>
      }

    val mockData = testBusinessCustomerDetails("SOP")
    reviewDetail.text() must include(mockData.businessAddress.line_1)
    reviewDetail.text() must include(mockData.businessAddress.line_2)
    testIfExists(mockData.businessAddress.line_3)
    testIfExists(mockData.businessAddress.line_4)
    testIfExists(mockData.businessAddress.postcode)
  }

  "BusinessContactsController" must {

    "AWRS Contact Details entered " must {

      "must see the 'Enter' view if they are the linear journey" in {
        linearJourney {
          result =>
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("contact-information-heading").text() mustBe Messages("awrs.business_contacts.heading", Messages("awrs.generic.enter"))
            reviewDetailMatch(document.getElementById("review-details"))
            document.getElementById("contactAddressSame-no-content") mustNot be(null)
        }
      }

      "must see the 'Edit' view if they are on the edit journey" in {
        editJourney {
          result =>
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("contact-information-heading").text() mustBe Messages("awrs.business_contacts.heading", Messages("awrs.generic.edit"))
            reviewDetailMatch(document.getElementById("review-details"))
            document.getElementById("contactAddressSame-no-content") mustNot be(null)
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
                  val expectedSectionNumber = journey.indexOf(businessContactsName) + 1
                  val totalSectionsForBusinessType = journey.size
                  val expectedSectionName = legalEntity match {
                    case "LLP_GRP" | "LTD_GRP" => Messages("awrs.index_page.group_business_contacts_text")
                    case "Partnership" | "LP" | "LLP" => Messages("awrs.index_page.partnership_contacts_text")
                    case _ => Messages("awrs.index_page.business_contacts_text")
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
      fetchBusinessContacts = testBusinessContactsDefault()
    )

    setAuthMocks()
    val result = testBusinessContactsController.showBusinessContacts(false).apply(SessionBuilder.buildRequestWithSession(userId, testBusinessCustomerDetails("SOP").businessType.get))
    test(result)
  }

  private def linearJourney(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(
      fetchBusinessCustomerDetails = testBusinessCustomerDetails("SOP"),
      fetchBusinessContacts = None
    )

    setAuthMocks()
    val result = testBusinessContactsController.showBusinessContacts(true).apply(SessionBuilder.buildRequestWithSession(userId, testBusinessCustomerDetails("SOP").businessType.get))

    test(result)
  }

  private def eitherJourney(isLinearJourney: Boolean, entityType: String)(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(
      fetchBusinessCustomerDetails = testBusinessCustomerDetails(entityType),
      fetchBusinessContacts = testBusinessContactsDefault()
    )

    setAuthMocks()
    val result = testBusinessContactsController.showBusinessContacts(isLinearMode = isLinearJourney).apply(SessionBuilder.buildRequestWithSession(userId, entityType))
    test(result)
  }

}
