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
import connectors.mock.MockAuthConnector
import controllers.SupplierAddressesController
import models._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.test.Helpers._
import services.DataCacheKeys._
import services.JourneyConstants
import services.mocks.MockSave4LaterService
import utils.AwrsUnitTestTraits
import utils.TestConstants._
import utils.TestUtil._
import views.html.awrs_supplier_addresses

import scala.concurrent.Future

class AdditionalSupplierViewTest extends AwrsUnitTestTraits
  with MockSave4LaterService with MockAuthConnector {

  val template: awrs_supplier_addresses = app.injector.instanceOf[views.html.awrs_supplier_addresses]

  lazy val testSupplier: Option[String] => Supplier = (_: Option[String]) => testSupplierDefault(alcoholSuppliers = Some("Yes"), supplierName = Some("Supplier Name"), ukSupplier = Some("Yes"), vatRegistered = Some("Yes"), vatNumber = testUtr, supplierAddress = Some(testAddress), additionalSupplier = Some("Yes"))
  lazy val testList = List(testSupplier(Some("Yes")), testSupplier(Some("Yes")), testSupplier(Some("Yes")), testSupplier(Some("Yes")), testSupplier(Some("No")))

  val testSupplierAddressesController: SupplierAddressesController =
    new SupplierAddressesController(mockMCC, testSave4LaterService, mockDeEnrolService, mockAuthConnector, mockAuditable, mockAccountUtils, mockCountryCodes, mockAppConfig, template) {
    override val signInUrl: String = "/sign-in"
    override val countryList: Seq[(String, String)] = Seq()
  }
  "Additional Supplier Template" must {

    "display h1 with correct supplier number for linear mode" in {
      for ((_, index) <- testList.getOrElse(List()).zipWithIndex) {
        val id = index + 1
        showSupplier(id) {
          result =>
            status(result) mustBe OK
            val document = Jsoup.parse(contentAsString(result))
            val heading = document.getElementById("supplier-addresses-title").text()
            id match {
              case 1 => heading must be(Messages("awrs.supplier-addresses.heading.first"))
              case _ => heading must be(Messages("awrs.supplier-addresses.heading", Messages("awrs.generic.tell_us_about"), views.html.helpers.ordinalIntSuffix(id)))
            }
        }
      }
    }

    "display h1 with correct supplier number for edit mode" in {
      for ((_, index) <- testList.getOrElse(List()).zipWithIndex) {
        val id = index + 1
        showSupplier(id, isLinearMode = false, isNewRecord = false) {
          result =>
            status(result) mustBe OK
            val document = Jsoup.parse(contentAsString(result))
            val heading = document.getElementById("supplier-addresses-title").text()
            heading must be(Messages("awrs.supplier-addresses.heading", Messages("awrs.generic.edit"), views.html.helpers.ordinalIntSuffix(id)))
        }
      }
    }

    "display h1 with correct supplier number for edit mode when adding the first record" in {
      showSupplier(id = 1, isLinearMode = false, suppliers = List(Supplier(Some("No"), None, None, None, None, None, None))) {
        result =>
          status(result) mustBe OK
          val document = Jsoup.parse(contentAsString(result))
          val heading = document.getElementById("supplier-addresses-title").text()
          heading must be(Messages("awrs.supplier-addresses.heading.first"))
      }
    }

    "display h1 with correct supplier number for linear mode when adding the next new record" in {
      showSupplier(id = 2, isLinearMode = true, suppliers = List(testSupplier(Some("Yes")))) {
        result =>
          status(result) mustBe OK
          val document = Jsoup.parse(contentAsString(result))
          val heading = document.getElementById("supplier-addresses-title").text()
          heading must be(Messages("awrs.supplier-addresses.heading", Messages("awrs.generic.tell_us_about"), views.html.helpers.ordinalIntSuffix(2)))
      }
    }

    "display h1 with correct supplier number for edit mode when adding the next new record" in {
      showSupplier(id = 2, isLinearMode = false, suppliers = List(testSupplier(Some("Yes")))) {
        result =>
          status(result) mustBe OK
          val document = Jsoup.parse(contentAsString(result))
          val heading = document.getElementById("supplier-addresses-title").text()
          heading must be(Messages("awrs.supplier-addresses.heading", Messages("awrs.generic.edit"), views.html.helpers.ordinalIntSuffix(2)))
      }
    }

    allEntities.foreach {
      legalEntity =>
        s"$legalEntity" must {
          Seq(true, false).foreach {
            isLinear =>
              s"see a progress message for the isLinearJourney is set to $isLinear" in {
                val test: Future[Result] => Unit = result => {
                  implicit val doc: Document = Jsoup.parse(contentAsString(result))
                  testId(shouldExist = true)(targetFieldId = "progress-text")
                  val journey = JourneyConstants.getJourney(legalEntity)
                  val expectedSectionNumber = journey.indexOf(suppliersName) + 1
                  val totalSectionsForBusinessType = journey.size
                  val expectedSectionName = Messages("awrs.index_page.suppliers_text")
                  val expected = Messages("awrs.generic.section_progress", expectedSectionNumber, totalSectionsForBusinessType, expectedSectionName)
                  testText(expectedText = expected)(targetFieldId = "progress-text")
                }
                eitherJourney(isLinearJourney = isLinear, entityType = legalEntity)(test)
              }
          }
        }
    }
  }


  private def showSupplier(id: Int, isLinearMode: Boolean = true, isNewRecord: Boolean = true, suppliers: List[Supplier] = testList)(test: Future[Result] => Any): Future[Any] = {
    setupMockSave4LaterServiceWithOnly(fetchSuppliers = Suppliers(suppliers))
    setAuthMocks()

    val result = testSupplierAddressesController.showSupplierAddressesPage(id = id, isLinearMode = isLinearMode, isNewRecord = isNewRecord).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def eitherJourney(id: Int = 1, isLinearJourney: Boolean, isNewRecord: Boolean = true, entityType: String)(test: Future[Result] => Any): Unit = {
    setupMockSave4LaterServiceWithOnly(
      fetchBusinessCustomerDetails = testBusinessCustomerDetails(entityType),
      fetchSuppliers = testSuppliers
    )
    setAuthMocks()

    val result = testSupplierAddressesController.showSupplierAddressesPage(id = id, isLinearMode = isLinearJourney, isNewRecord = isNewRecord).apply(SessionBuilder.buildRequestWithSession(userId, entityType))
    test(result)
  }

}
