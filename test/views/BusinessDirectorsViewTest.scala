/*
 * Copyright 2018 HM Revenue & Customs
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
import controllers.BusinessDirectorsController
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

class BusinessDirectorsViewTest extends AwrsUnitTestTraits
  with MockSave4LaterService with MockAuthConnector {

  implicit def businessDirectorWrapper(listOfDirectors: List[BusinessDirector]): Option[BusinessDirectors] = Some(BusinessDirectors(listOfDirectors))

  lazy val testList = BusinessDirectors(List(testBusinessDirectorPerson, testBusinessDirectorPerson, testBusinessDirectorCompany, testBusinessDirectorCompany))

  object TestBusinessDirectorsController extends BusinessDirectorsController {
    override val authConnector = mockAuthConnector
    override val save4LaterService = TestSave4LaterService
  }

  "Business Director Template" should {

    "display h1 with correct director number for linear mode" in {
      for ((director, index) <- testList.getOrElse(BusinessDirectors(List())).directors.zipWithIndex) {
        val id = index + 1
        showDirector(id) {
          result =>
            status(result) shouldBe OK
            val document = Jsoup.parse(contentAsString(result))
            val heading = document.getElementById("business-directors-heading").text()
            val personOrCompanySection = document.getElementById("personOrCompany_field")
            val companySecretaryOption = document.getElementById("directorsAndCompanySecretaries-company_secretary")
            id match {
              case 1 =>
                heading should be(Messages("awrs.business_directors.heading.first"))
                personOrCompanySection shouldBe null
                companySecretaryOption shouldBe null
              case _ =>
                heading should be(Messages("awrs.business_directors.heading", Messages("awrs.generic.tell_us_about"), views.html.helpers.ordinalIntSuffix(id)))
                personOrCompanySection should not be null
                companySecretaryOption should not be null
            }
        }
      }
    }

    // this is a post linear journey  scenario in which the user went and deleted all of their saved directors, then try to
    // add a new director.
    "display the page correctly in edit mode when adding the first record and no directors found in save4later" in {
      showDirector(id = 1, isLinearMode = false, directors = None, isNewRecord = true) {
        result =>
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))
          val heading = document.getElementById("business-directors-heading").text()
          heading should be(Messages("awrs.business_directors.heading.first"))

          val personOrCompanySection = document.getElementById("personOrCompany_field")
          personOrCompanySection shouldBe null

          val companySecretaryOption = document.getElementById("directorsAndCompanySecretaries-company_secretary")
          companySecretaryOption shouldBe null
      }
    }

    "display the page correctly in edit mode when adding a new record in addition to existing records" in {
      showDirector(id = 2, isLinearMode = false, directors = List(testBusinessDirectorPerson)) {
        result =>
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))
          val heading = document.getElementById("business-directors-heading").text()
          heading should be(Messages("awrs.business_directors.heading", Messages("awrs.generic.tell_us_about"), views.html.helpers.ordinalIntSuffix(2)))

          val personOrCompanySection = document.getElementById("personOrCompany_field")
          personOrCompanySection should not be null

          val companySecretaryOption = document.getElementById("directorsAndCompanySecretaries-company_secretary")
          companySecretaryOption should not be null
      }
    }

    // when viewing the pages, the first page should be the same as all other pages
    "display the page correctly in edit mode when viewing existing records" in {
      for ((director, index) <- testList.getOrElse(BusinessDirectors(List())).directors.zipWithIndex) {
        val id = index + 1
        showDirector(id, isLinearMode = false, isNewRecord = false) {
          result =>
            status(result) shouldBe OK
            val document = Jsoup.parse(contentAsString(result))
            val heading = document.getElementById("business-directors-heading").text()
            id match {
              case 1 =>
                heading should be(Messages("awrs.business_directors.heading.first.edit"))
              case _ =>
                heading should be(Messages("awrs.business_directors.heading", Messages("awrs.generic.edit"), views.html.helpers.ordinalIntSuffix(id)))
            }

            val personOrCompanySection = document.getElementById("personOrCompany_field")
            personOrCompanySection should not be null

            val companySecretaryOption = document.getElementById("directorsAndCompanySecretaries-company_secretary")
            companySecretaryOption should not be null
        }
      }
    }

    directorEntities.foreach {
      legalEntity =>
        s"$legalEntity" should {
          Seq(true, false).foreach {
            isLinear =>
              s"see a progress message for the isLinearJourney is set to $isLinear" in {
                val test: Future[Result] => Unit = result => {
                  implicit val doc = Jsoup.parse(contentAsString(result))
                  testId(shouldExist = true)(targetFieldId = "progress-text")
                  val journey = JourneyConstants.getJourney(legalEntity)
                  val expectedSectionNumber = journey.indexOf(businessDirectorsName) + 1
                  val totalSectionsForBusinessType = journey.size
                  val expectedSectionName = Messages("awrs.index_page.business_directors.index_text")
                  val expected = Messages("awrs.generic.section") + Messages("awrs.generic.section_progess", expectedSectionNumber, totalSectionsForBusinessType, expectedSectionName)
                  testText(expectedText = expected)(targetFieldId = "progress-text")
                }
                eitherJourney(isLinearJourney = isLinear, entityType = legalEntity)(test)
              }
          }
        }
    }

  }


  private def showDirector(id: Int, isLinearMode: Boolean = true, isNewRecord: Boolean = true, directors: Option[BusinessDirectors] = testList)(test: Future[Result] => Any): Future[Any] = {
    setupMockSave4LaterServiceWithOnly(fetchBusinessDirectors = directors)
    val result = TestBusinessDirectorsController.showBusinessDirectors(id = id, isLinearMode = isLinearMode, isNewRecord = isNewRecord).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def eitherJourney(id: Int = 1, isLinearJourney: Boolean, isNewRecord: Boolean = true, entityType: String)(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(
      fetchBusinessCustomerDetails = testBusinessCustomerDetails(entityType),
      fetchBusinessDirectors = testBusinessDirectors
    )
    val result = TestBusinessDirectorsController.showBusinessDirectors(id = id, isLinearMode = isLinearJourney, isNewRecord = isNewRecord).apply(SessionBuilder.buildRequestWithSession(userId, entityType))
    test(result)
  }

}
