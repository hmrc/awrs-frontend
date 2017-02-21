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

package controllers

import java.util.UUID

import builders.{AuthBuilder, SessionBuilder}
import config.FrontendAuthConnector
import connectors.mock.MockAuthConnector
import controllers.auth.Utr._
import forms.BusinessPremisesForm
import models.{AdditionalBusinessPremises, AdditionalBusinessPremisesList}
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.DataCacheKeys._
import services.mocks.MockSave4LaterService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.TestUtil._
import utils.{AwrsSessionKeys, AwrsUnitTestTraits, TestUtil}

import scala.concurrent.Future

class AdditionalPremisesControllerTest extends AwrsUnitTestTraits
  with MockSave4LaterService with MockAuthConnector {

  val mockDataCacheService = TestSave4LaterService

  lazy val testOneBusinessDetails = AdditionalBusinessPremises(additionalPremises = Some("Yes"), Some(testAddress), addAnother = Option("Yes"))
  lazy val testTwoBusinessDetails = AdditionalBusinessPremises(additionalPremises = Some("Yes"), Some(testAddress), addAnother = Option("Yes"))
  lazy val testThreeBusinessDetails = AdditionalBusinessPremises(additionalPremises = Some("Yes"), Some(testAddress), addAnother = Option("No"))

  lazy val testAdditionalBusinessPremisesList = AdditionalBusinessPremisesList(List(testBusinessPremises))

  lazy val testThreeAdditionalBusinessPremisesList = AdditionalBusinessPremisesList(List(testOneBusinessDetails, testTwoBusinessDetails, testThreeBusinessDetails))
  lazy val testTwoAdditionalBusinessPremisesListNoAddAnother = AdditionalBusinessPremisesList(List(testOneBusinessDetails, testThreeBusinessDetails))
  lazy val testTwoAdditionalBusinessPremisesListYesAddAnother = AdditionalBusinessPremisesList(List(testOneBusinessDetails, testTwoBusinessDetails))

  private def testRequest(premises: AdditionalBusinessPremises) =
    TestUtil.populateFakeRequest[AdditionalBusinessPremises](FakeRequest(), BusinessPremisesForm.businessPremisesValidationForm, premises)

  object TestAdditionalPremisesController extends AdditionalPremisesController {
    override val authConnector = mockAuthConnector
    override val save4LaterService = TestSave4LaterService
  }

  "AdditionalPremisesController" should {
    "use the correct AuthConnector" in {
      AdditionalPremisesController.authConnector shouldBe FrontendAuthConnector
    }
  }



  "Pressing Continue within Business Details Page" should {

    "if No Additional Premise is selected, save an object to Save4later and redirect to Index" in {
      continueWithAuthorisedUser(testRequest(testAdditionalBusinessPremisesDefault(additionalPremises = Some("No")))) {
        result =>
          status(result) shouldBe 303
      }
    }

    "redirect LTD/LTD_GRP business type to Business Directors page when valid data is provided and 'Add Another' is 'No'" in {

      val busTypes = List("LTD", "LTD_GRP")

      for (busType <- busTypes) {
        continueWithAuthoriedUserEntityType(busType, testRequest(testAdditionalBusinessPremisesDefault(additionalPremises = Some("Yes"), additionalAddress = Some(testAddress), addAnother = Some("No")))) {
          result =>
            redirectLocation(result).get shouldBe "/alcohol-wholesale-scheme/business-directors"
        }
      }
    }

    "redirect SOP/Partnership/LP/LLP business type to trading activity page when valid data is provided and 'Add Another' is 'No'" in {

      val busTypes = List("SOP", "Partnership", "LP", "LLP", "LLP_GRP")

      for (busType <- busTypes) {
        continueWithAuthoriedUserEntityType(busType, testRequest(testAdditionalBusinessPremisesDefault(additionalPremises = Some("Yes"), additionalAddress = Some(testAddress), addAnother = Some("No")))) {
          result =>
            redirectLocation(result).get shouldBe "/alcohol-wholesale-scheme/trading-activity"
        }
      }
    }

    "redirect LTD/LTD_GRP business type to Business Directors page when 'Additional Premises' is 'No'" in {

      val busTypes = List("LTD", "LTD_GRP")

      for (busType <- busTypes) {
        continueWithAuthoriedUserEntityType(busType, testRequest(testAdditionalBusinessPremisesDefault(additionalPremises = Some("No")))) {
          result =>
            redirectLocation(result).get shouldBe "/alcohol-wholesale-scheme/business-directors"
        }
      }
    }

    "redirect SOP/Partnership/LP/LLP business type to trading activity page when 'Additional Premises' is 'No'" in {

      val busTypes = List("SOP", "Partnership", "LP", "LLP", "LLP_GRP")

      for (busType <- busTypes) {
        continueWithAuthoriedUserEntityType(busType, testRequest(testAdditionalBusinessPremisesDefault(additionalPremises = Some("No")))) {
          result =>
            redirectLocation(result).get shouldBe "/alcohol-wholesale-scheme/trading-activity"
        }
      }
    }

    "redirect to additional premises page when valid data is provided and 'Add Another' is 'Yes'" in {
      continueWithAuthorisedUser(testRequest(testAdditionalBusinessPremisesDefault(additionalPremises = Some("Yes"), additionalAddress = Some(testAddress), addAnother = Some("Yes")))) {
        result =>
          status(result) shouldBe 303
          redirectLocation(result).get shouldBe "/alcohol-wholesale-scheme/additional-premises?id=2"
      }
    }
  }

  "When loading the delete page we" should {
    "see to the selected suppliers delete confirmation page" in {
      showDeleteWithAuthorisedUser() {
        result =>
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("deleteConfirmation-heading").text shouldBe Messages("awrs.delete.confirmation_heading", Messages("awrs.view_application.premises"))
          status(result) shouldBe 200
      }
    }
  }

  "When submitting the delete confirmation page we" should {
    "be routed back to the summary page after confirming Yes" in {
      deleteWithAuthorisedUser()(deleteConfirmation_Yes) {
        result =>
          status(result) shouldBe 303
          redirectLocation(result).get should include("/alcohol-wholesale-scheme/view-section/additionalBusinessPremises")
          verifySave4LaterService(saveAdditionalBusinessPremisesList = 1)
      }
    }
    "be routed back to the summary page after confirming No" in {
      deleteWithAuthorisedUser()(deleteConfirmation_No) {
        result =>
          status(result) shouldBe 303
          redirectLocation(result).get should include("/alcohol-wholesale-scheme/view-section/additionalBusinessPremises")
          verifySave4LaterService(saveAdditionalBusinessPremisesList = 0)
      }
    }
    "be shown an error if nothing is selected" in {
      deleteWithAuthorisedUser()(deleteConfirmation_None) {
        result =>
          status(result) shouldBe 400
      }
    }
    "be routed back to the summary page after confirming Yes for a record that is not the first" in {
      deleteWithAuthorisedUser(id = 2, premises = testThreeAdditionalBusinessPremisesList)(deleteConfirmation_Yes) {
        result =>
          status(result) shouldBe 303
          redirectLocation(result).get should include("/alcohol-wholesale-scheme/view-section/additionalBusinessPremises")
          verifySave4LaterService(saveAdditionalBusinessPremisesList = 1)
      }
    }
  }

  "Users who entered from the summary edit view" should {
    "return to the summary view after clicking return" in {
      returnWithAuthorisedUser(testRequest(testAdditionalBusinessPremisesDefault(additionalPremises = Some("No")))) {
        result =>
          redirectLocation(result).get should include(f"/alcohol-wholesale-scheme/view-section/$additionalBusinessPremisesName")
          verifySave4LaterService(saveAdditionalBusinessPremisesList = 1)
      }
    }
  }

  private def continueWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchAdditionalBusinessPremisesList = testAdditionalBusinessPremisesList,
      fetchBusinessDirectors = None,
      fetchTradingActivity = None)
    val result = TestAdditionalPremisesController.saveAndContinue(1, true).apply(SessionBuilder.updateRequestWithSession(fakeRequest.withSession((AwrsSessionKeys.sessionBusinessType, "LTD")), userId))
    test(result)
  }

  private def returnWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchAdditionalBusinessPremisesList = testAdditionalBusinessPremisesList)
    val result = TestAdditionalPremisesController.saveAndReturn(1, true).apply(SessionBuilder.updateRequestWithSession(fakeRequest.withSession((AwrsSessionKeys.sessionBusinessType, "LTD")), userId))
    test(result)
  }

  private def continueWithAuthoriedUserEntityType(busType: String, fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val businessType = busType
    setupMockSave4LaterServiceWithOnly(fetchAdditionalBusinessPremisesList = testAdditionalBusinessPremisesList,
      fetchBusinessDirectors = None,
      fetchTradingActivity = None)
    val result = TestAdditionalPremisesController.saveAndContinue(1, true).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, businessType))
    test(result)
  }

  private def showDeleteWithAuthorisedUser(id: Int = 1, premises: AdditionalBusinessPremisesList = testAdditionalBusinessPremisesList)(test: Future[Result] => Any): Future[Any] = {
    setupMockSave4LaterServiceWithOnly(fetchAdditionalBusinessPremisesList = premises)
    val result = TestAdditionalPremisesController.showDelete(id).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def deleteWithAuthorisedUser(id: Int = 1, premises: AdditionalBusinessPremisesList = testAdditionalBusinessPremisesList)(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchAdditionalBusinessPremisesList = premises)
    val result = TestAdditionalPremisesController.actionDelete(id).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }
}