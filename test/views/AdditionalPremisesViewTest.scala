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
import controllers.AdditionalPremisesController
import models._
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Result
import play.api.test.Helpers._
import services.DataCacheKeys._
import services.JourneyConstants
import services.mocks.MockSave4LaterService
import utils.AwrsUnitTestTraits
import utils.TestUtil._

import scala.concurrent.Future

class AdditionalPremisesViewTest extends AwrsUnitTestTraits
  with MockSave4LaterService with MockAuthConnector {

  def testPremises(addAnother: Option[String]): AdditionalBusinessPremises = testAdditionalBusinessPremisesDefault(additionalPremises = Some("Yes"),
    additionalAddress = Some(testAddress), addAnother = addAnother)

  lazy val testList = List(testPremises(Some("Yes")), testPremises(Some("Yes")), testPremises(Some("Yes")), testPremises(Some("Yes")), testPremises(Some("No")))

  val testAdditionalPremisesController: AdditionalPremisesController =
    new AdditionalPremisesController(mockMCC, testSave4LaterService, mockDeEnrolService, mockAccountUtils, mockAuthConnector, mockAuditable, mockAppConfig) {
      override val signInUrl = "/sign-in"
    }


  "Additional Premises Template" should {

    "linear journey" should {

      "display h1 with correct premises number for existing records" in {
        for ((premises, index) <- testList.zipWithIndex) {
          val id = index + 1
          showPremises(id, isLinearMode = true) {
            result =>
              status(result) shouldBe OK
              val document = Jsoup.parse(contentAsString(result))
              val heading = document.getElementById("additional-premises-title").text()
              id match {
                case 1 => heading should be(Messages("awrs.additional-premises.top-heading.first"))
                case _ => heading should be(Messages("awrs.additional-premises.top-heading", Messages("awrs.generic.tell_us_about"), views.html.helpers.ordinalIntSuffix(id)))
              }
          }
        }
      }

      "display h1 with correct premises number for linear mode when adding a new record" in {
        val testList2 = List(testPremises(addAnother = Some("Yes")))
        val nextId = testList2.size + 1
        showPremises(id = nextId, isLinearMode = true, premises = testList2) {
          result =>
            status(result) shouldBe OK
            val document = Jsoup.parse(contentAsString(result))
            val heading = document.getElementById("additional-premises-title").text()
            heading should be(Messages("awrs.additional-premises.top-heading", Messages("awrs.generic.tell_us_about"), views.html.helpers.ordinalIntSuffix(nextId)))
        }
      }

    }

    "post linear jouney" should {
      "edit mode" should {
        "display h1 with correct premises number when editing a record" in {
          for ((premises, index) <- testList.zipWithIndex) {
            val id = index + 1
            showPremises(id, isLinearMode = false, isNewRecord = false) {
              result =>
                status(result) shouldBe OK
                val document = Jsoup.parse(contentAsString(result))
                val heading = document.getElementById("additional-premises-title").text()
                heading should be(Messages("awrs.additional-premises.top-heading", Messages("awrs.generic.edit"), views.html.helpers.ordinalIntSuffix(id)))
            }
          }
        }
      }

      "add new record mode" should {
        "display h1 with correct premises number when adding the first record after previously answered no to do you have additional premises" in {
          val noPremises = List(AdditionalBusinessPremises(Some("No"), None, None))
          showPremises(id = 1, isLinearMode = false, isNewRecord = true, premises = noPremises) {
            result =>
              status(result) shouldBe OK
              val document = Jsoup.parse(contentAsString(result))
              val heading = document.getElementById("additional-premises-title").text()
              //              heading should be(Messages("awrs.additional-premises.top-heading.first"))
              heading should be(Messages("awrs.additional-premises.top-heading", Messages("awrs.generic.tell_us_about"), views.html.helpers.ordinalIntSuffix(1)))
          }
        }

        "display h1 with correct premises number when adding additional records" in {
          val testList2 = List(testPremises(addAnother = Some("Yes")))
          val nextId = testList2.size + 1
          showPremises(id = nextId, isLinearMode = false, isNewRecord = true, premises = testList2) {
            result =>
              status(result) shouldBe OK
              val document = Jsoup.parse(contentAsString(result))
              val heading = document.getElementById("additional-premises-title").text()
              heading should be(Messages("awrs.additional-premises.top-heading", Messages("awrs.generic.tell_us_about"), views.html.helpers.ordinalIntSuffix(nextId)))
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
                    val expectedSectionNumber = journey.indexOf(additionalBusinessPremisesName) + 1
                    val totalSectionsForBusinessType = journey.size
                    val expectedSectionName = Messages("awrs.index_page.additional_premises_text")
                    val expected = Messages("awrs.generic.section_progress", expectedSectionNumber, totalSectionsForBusinessType, expectedSectionName)
                    testText(expectedText = expected)(targetFieldId = "progress-text")
                  }
                  eitherJourney(isLinearJourney = isLinear, entityType = legalEntity)(test)
                }
            }
          }
      }

    }
  }

  private def showPremises(id: Int, isLinearMode: Boolean = true, isNewRecord: Boolean = true, premises: List[AdditionalBusinessPremises] = testList)(test: Future[Result] => Any): Future[Any] = {
    setupMockSave4LaterServiceWithOnly(fetchAdditionalBusinessPremisesList = AdditionalBusinessPremisesList(premises))
    setAuthMocks()
    val result = testAdditionalPremisesController.showPremisePage(id = id, isLinearMode = isLinearMode, isNewRecord = isNewRecord).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def eitherJourney(id: Int = 1, isLinearJourney: Boolean, isNewRecord: Boolean = true, entityType: String)(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(
      fetchBusinessCustomerDetails = testBusinessCustomerDetails(entityType),
      fetchAdditionalBusinessPremisesList = testAdditionalPremisesList
    )
    setAuthMocks()
    val result = testAdditionalPremisesController.showPremisePage(id = id, isLinearMode = isLinearJourney, isNewRecord = isNewRecord).apply(SessionBuilder.buildRequestWithSession(userId, entityType))
    test(result)
  }

}
