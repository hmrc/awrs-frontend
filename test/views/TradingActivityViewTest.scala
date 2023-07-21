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
import controllers.TradingActivityController
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}
import services.DataCacheKeys._
import services.{JourneyConstants, ServicesUnitTestFixture}
import utils.TestUtil._
import utils.{AwrsFieldConfig, AwrsUnitTestTraits}
import views.html.awrs_trading_activity

import scala.concurrent.Future

class TradingActivityViewTest extends AwrsUnitTestTraits with ServicesUnitTestFixture with AwrsFieldConfig {

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val template: awrs_trading_activity = app.injector.instanceOf[views.html.awrs_trading_activity]
  val testTradingActivityController: TradingActivityController = new TradingActivityController(mockMCC, testSave4LaterService, mockDeEnrolService, mockAuthConnector, mockAuditable, mockAccountUtils, mockAppConfig, template) {
    override val signInUrl: String = applicationConfig.signIn
  }

  "Submitting the trading activity form with " must {

    "Authenticated and authorised users" must {
      "display validation error when 'What type of wholesaler are you?' is not selected" in {
        continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("wholesalerType[0]" -> "")) {
          result =>
            val document = Jsoup.parse(contentAsString(result))
            val id = "wholesalerType"
            val expectedErrorKey = "awrs.additional_information.error.type_of_wholesaler"

            testErrorMessageValidation(document, id, expectedErrorKey)
        }
      }

      "if 'type of wholesaler' is Other - additional data must be provided" in {
        continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("wholesalerType[0]" -> "99", "otherWholesaler" -> "", "mainCustomers[0]" -> "3", "doesBusinessImportAlcohol" -> "Yes", "productType[0]" -> "01")) {
          result =>
            val document = Jsoup.parse(contentAsString(result))

            val id = "otherWholesaler"
            val expectedErrorKey = "awrs.additional_information.error.other_wholesaler"

            testErrorMessageValidation(document, id, expectedErrorKey)
        }
      }

      "if 'type of wholesaler' is Other - additional data must not be more than 40 chars" in {
        continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody(
          "wholesalerType[0]" -> "99", "otherWholesaler" -> "a" * (otherWholesalerLen + 1),
          "mainCustomers[0]" -> "3", "doesBusinessImportAlcohol" -> "Yes", "productType[0]" -> "01")) {
          result =>
            val document = Jsoup.parse(contentAsString(result))

            val id = "otherWholesaler"
            val expectedErrorKey = "awrs.view_application.error.trading_activity.order.maxlength"

            testErrorMessageValidation(document, id, expectedErrorKey, "wholesaler type other", otherWholesalerLen)
        }
      }

      "if 'type of wholesaler' is Other - validate input" in {
        continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("wholesalerType[0]" -> "99", "otherWholesaler" -> "%^&*")) {
          result =>
            val document = Jsoup.parse(contentAsString(result))

            val id = "otherWholesaler"
            val expectedErrorKey = "awrs.additional_information.error.wholesaler_validation"

            testErrorMessageValidation(document, id, expectedErrorKey)
        }
      }

      "display validation error when 'How do you currently take orders?' is not selected" in {
        continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("typeOfAlcoholOrders[0]" -> "")) {
          result =>
            val document = Jsoup.parse(contentAsString(result))

            val id = "typeOfAlcoholOrders"
            val expectedErrorKey = "awrs.additional_information.error.orders"

            testErrorMessageValidation(document, id, expectedErrorKey)
        }
      }

      "if 'How do you currently take orders?' is Other - additional data must be provided" in {
        continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("typeOfAlcoholOrders[0]" -> "99", "otherTypeOfAlcoholOrders" -> "")) {
          result =>
            val document = Jsoup.parse(contentAsString(result))

            val id = "otherTypeOfAlcoholOrders"
            val expectedErrorKey = "awrs.additional_information.error.other_order"

            testErrorMessageValidation(document, id, expectedErrorKey)
        }
      }

      "if 'How do you currently take orders?' is Other - additional data must not be more than 40 chars" in {
        continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody(
          "typeOfAlcoholOrders[0]" -> "99", "otherTypeOfAlcoholOrders" -> "a" * (otherOrdersLen + 1))) {
          result =>
            val document = Jsoup.parse(contentAsString(result))

            val id = "otherTypeOfAlcoholOrders"
            val expectedErrorKey = "awrs.view_application.error.trading_activity.maxlength"

            testErrorMessageValidation(document, id, expectedErrorKey, "other orders", otherOrdersLen)
        }
      }

      "if 'How do you currently take orders?' is Other - validate input" in {
        continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("typeOfAlcoholOrders[0]" -> "99", "otherTypeOfAlcoholOrders" -> "%^&*")) {
          result =>
            val document = Jsoup.parse(contentAsString(result))

            val id = "otherTypeOfAlcoholOrders"
            val expectedErrorKey = "awrs.additional_information.error.order_validation"

            testErrorMessageValidation(document, id, expectedErrorKey)
        }
      }

      "display validation error when 'does your business currently import alcohol' is not selected" in {
        continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("wholesalerType[0]" -> "1", "doesBusinessImportAlcohol[0]" -> "")) {
          result =>
            val document = Jsoup.parse(contentAsString(result))

            val id = "doesBusinessImportAlcohol"
            val expectedErrorKey = "awrs.additional_information.error.import_alcohol"

            testErrorMessageValidation(document, id, expectedErrorKey)
        }
      }

      "display validation error when 'do you export alcohol' is not selected" in {
        continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("wholesalerType[0]" -> "1", "doYouExportAlcohol" -> "")) {
          result =>
            val document = Jsoup.parse(contentAsString(result))

            val id = "doYouExportAlcohol"
            val expectedErrorKey = "awrs.additional_information.error.do_you_export_alcohol"

            testErrorMessageValidation(document, id, expectedErrorKey)
        }
      }

      "display validation error when 'Do you currently use any 3rd party alcohol storage?' is not selected" in {
        continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("thirdPartyStorage[0]" -> "")) {
          result =>
            val document = Jsoup.parse(contentAsString(result))

            val id = "thirdPartyStorage"
            val expectedErrorKey = "awrs.additional_information.error.third_party_storage"

            testErrorMessageValidation(document, id, expectedErrorKey)
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
                    val expectedSectionNumber = journey.indexOf(tradingActivityName) + 1
                    val totalSectionsForBusinessType = journey.size
                    val expectedSectionName = Messages("awrs.index_page.trading_activity_text")
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

  private def continueWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any): Unit = {
    setupMockSave4LaterServiceWithOnly(fetchProducts = None)
    setAuthMocks()
    val result = testTradingActivityController.saveAndContinue().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId).withMethod("POST"))
    test(result)
  }

  def eitherJourney(isLinearJourney: Boolean, entityType: String)(test: Future[Result] => Any): Unit = {
    setupMockSave4LaterServiceWithOnly(
      fetchBusinessCustomerDetails = testBusinessCustomerDetails(entityType),
      fetchTradingActivity = testTradingActivity()
    )
    setAuthMocks()
    val result = testTradingActivityController.showTradingActivity(isLinearMode = isLinearJourney).apply(SessionBuilder.buildRequestWithSession(userId, entityType))
    test(result)
  }

}
