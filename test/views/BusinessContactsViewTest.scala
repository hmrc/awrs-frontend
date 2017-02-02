/*
 * Copyright 2017 HM Revenue & Customs
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
import controllers.BusinessContactsController
import forms.BusinessContactsForm
import models.BusinessContacts
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.DataCacheKeys._
import services.{JourneyConstants, ServicesUnitTestFixture}
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.TestUtil._
import utils.{AwrsUnitTestTraits, TestUtil}

import scala.concurrent.Future

class BusinessContactsViewTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture {

  val businessCustomerDetailsFormId = "businessCustomerDetails"

  def testRequest(premises: BusinessContacts) =
    TestUtil.populateFakeRequest[BusinessContacts](FakeRequest(), BusinessContactsForm.businessContactsValidationForm, premises)

  object TestBusinessContactsController extends BusinessContactsController {
    override val authConnector = mockAuthConnector
    override val save4LaterService = TestSave4LaterService
  }

  def reviewDetailMatch(reviewDetail: Element) = {

    def testIfExists(someString: Option[String]) =
      someString match {
        case Some(string) => reviewDetail.text() should include(string)
        case _ =>
      }

    val mockData = testBusinessCustomerDetails("SOP")
    reviewDetail.text() should include(mockData.businessAddress.line_1)
    reviewDetail.text() should include(mockData.businessAddress.line_2)
    testIfExists(mockData.businessAddress.line_3)
    testIfExists(mockData.businessAddress.line_4)
    testIfExists(mockData.businessAddress.postcode)
  }

  "BusinessContactsController" must {

    "AWRS Contact Details entered " should {

      "must see the 'Enter' view if they are the linear journey" in {
        linearJourney {
          result =>
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("contact-information-heading").text() shouldBe Messages("awrs.business_contacts.heading", Messages("awrs.generic.enter"))
            reviewDetailMatch(document.getElementById("review-details"))
            document.getElementById("contactAddressSame-no-content") shouldNot be(null)
        }
      }

      "must see the 'Edit' view if they are on the edit journey" in {
        editJourney {
          result =>
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("contact-information-heading").text() shouldBe Messages("awrs.business_contacts.heading", Messages("awrs.generic.edit"))
            reviewDetailMatch(document.getElementById("review-details"))
            document.getElementById("contactAddressSame-no-content") shouldNot be(null)
        }
      }
    }

    allEntities.foreach {
      legalEntity =>
        s"$legalEntity" should {
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
                  val expected = Messages("awrs.generic.section_progess", expectedSectionNumber, totalSectionsForBusinessType, expectedSectionName)
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
    val result = TestBusinessContactsController.showBusinessContacts(false).apply(SessionBuilder.buildRequestWithSession(userId, testBusinessCustomerDetails("SOP").businessType.get))
    test(result)
  }

  private def linearJourney(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(
      fetchBusinessCustomerDetails = testBusinessCustomerDetails("SOP"),
      fetchBusinessContacts = None
    )
    val result = TestBusinessContactsController.showBusinessContacts(true).apply(SessionBuilder.buildRequestWithSession(userId, testBusinessCustomerDetails("SOP").businessType.get))

    test(result)
  }

  private def eitherJourney(isLinearJourney: Boolean, entityType: String)(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(
      fetchBusinessCustomerDetails = testBusinessCustomerDetails(entityType),
      fetchBusinessContacts = testBusinessContactsDefault()
    )
    val result = TestBusinessContactsController.showBusinessContacts(isLinearMode = isLinearJourney).apply(SessionBuilder.buildRequestWithSession(userId, entityType))
    test(result)
  }

}
