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
import controllers.ProductsController
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.DataCacheKeys._
import services.{JourneyConstants, ServicesUnitTestFixture}
import utils.TestUtil._
import utils.{AwrsFieldConfig, AwrsUnitTestTraits}

import scala.concurrent.Future

class ProductsViewTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture {

  val request = FakeRequest()

  object TestProductsController extends ProductsController {
    override val authConnector = mockAuthConnector
    override val save4LaterService = TestSave4LaterService
    val signInUrl = "/sign-in"
  }

  "Submitting the additional information form with " should {

    "Authenticated and authorised users" should {

      "display validation error when 'Who are the main customers?' is not selected" in {
        continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("mainCustomers[0]" -> "")) {
          result =>
            val document = Jsoup.parse(contentAsString(result))

            val id = "mainCustomers"
            val expectedErrorKey = "awrs.additional_information.error.main_customer"

            testErrorMessageValidation(document, id, expectedErrorKey)
        }
      }

      "if 'Who are the main customers' is Other - additional data must be provided" in {
        continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("wholesalerType[0]" -> "01", "mainCustomers[0]" -> "99", "otherMainCustomers" -> "")) {
          result =>
            val document = Jsoup.parse(contentAsString(result))

            val id = "otherMainCustomers"
            val expectedErrorKey = "awrs.additional_information.error.other_mainCustomers"

            testErrorMessageValidation(document, id, expectedErrorKey)
        }
      }

      "if 'Who are the main customers' is Other - additional data must not be more than 40 chars" in {
        continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody(
          "wholesalerType[0]" -> "01", "mainCustomers[0]" -> "99", "otherMainCustomers" -> "a" * (AwrsFieldConfig.otherCustomersLen + 1))) {

          result =>
            val document = Jsoup.parse(contentAsString(result))

            val id = "otherMainCustomers"
            val expectedErrorKey = "awrs.additional_information.error.maximum_length.customer"

            testErrorMessageValidation(document, id, expectedErrorKey, "other customers", AwrsFieldConfig.otherCustomersLen)
        }
      }

      "if 'type of main customers' is Other - validate input" in {
        continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("wholesalerType[0]" -> "01", "mainCustomers[0]" -> "99", "otherMainCustomers" -> "%^&*", "doesBusinessImportAlcohol" -> "Yes")) {
          result =>
            val document = Jsoup.parse(contentAsString(result))

            val id = "otherMainCustomers"
            val expectedErrorKey = "awrs.additional_information.error.other_mainCustomers_invalid_format"

            testErrorMessageValidation(document, id, expectedErrorKey)
        }
      }

      "if 'What products do you sell?' is Other - additional data must be provided" in {
        continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("productType[0]" -> "99", "otherProductType" -> "")) {
          result =>
            val document = Jsoup.parse(contentAsString(result))

            val id = "otherProductType"
            val expectedErrorKey = "awrs.additional_information.error.type_of_product_other"

            testErrorMessageValidation(document, id, expectedErrorKey)
        }
      }

      "if 'What products do you sell?' is Other - additional data must not be more than 40 chars" in {
        continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("productType[0]" -> "99", "otherProductType" -> "a" * (AwrsFieldConfig.otherProductsLen + 1))) {
          result =>
            val document = Jsoup.parse(contentAsString(result))

            val id = "otherProductType"
            val expectedErrorKey = "awrs.additional_information.error.maximum_length.product"

            testErrorMessageValidation(document, id, expectedErrorKey, "other products", AwrsFieldConfig.otherProductsLen)
        }
      }

      "display validation error when 'What products do you sell?' is not selected" in {
        continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("wholesalerType[0]" -> "1", "doesBusinessImportAlcohol" -> "Yes", "productType" -> "")) {
          result =>
            val document = Jsoup.parse(contentAsString(result))

            val id = "productType"
            val expectedErrorKey = "awrs.additional_information.error.type_of_product"

            testErrorMessageValidation(document, id, expectedErrorKey)
        }
      }

      "if 'What products do you sell?' is Other - validate input" in {
        continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("productType[0]" -> "99", "otherProductType" -> "%^&*")) {
          result =>
            val document = Jsoup.parse(contentAsString(result))

            val id = "otherProductType"
            val expectedErrorKey = "awrs.additional_information.error.type_of_product_other_validation"

            testErrorMessageValidation(document, id, expectedErrorKey)
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
                    val expectedSectionNumber = journey.indexOf(productsName) + 1
                    val totalSectionsForBusinessType = journey.size
                    val expectedSectionName = Messages("awrs.index_page.products_text")
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

  private def continueWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchSuppliers = None)
    setAuthMocks()
    val result = TestProductsController.saveAndContinue().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

  def eitherJourney(isLinearJourney: Boolean, entityType: String)(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(
      fetchBusinessCustomerDetails = testBusinessCustomerDetails(entityType),
      fetchProducts = testProducts()
    )
    setAuthMocks()
    val result = TestProductsController.showProducts(isLinearMode = isLinearJourney).apply(SessionBuilder.buildRequestWithSession(userId, entityType))
    test(result)
  }

}
