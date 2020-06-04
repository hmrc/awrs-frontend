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

package controllers

import builders.SessionBuilder
import config.ApplicationConfig
import connectors.mock.MockAuthConnector
import forms.AWRSEnums.BooleanRadioEnum
import forms.PartnershipDetailsForm
import models._
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.DataCacheKeys._
import services.mocks.MockSave4LaterService
import utils.AwrsUnitTestTraits
import utils.TestUtil
import utils.TestUtil._
import utils.TestConstants._

import scala.concurrent.Future

class BusinessPartnersControllerTest extends AwrsUnitTestTraits
  with MockSave4LaterService with MockAuthConnector {

  val businessPartnerDetails = Partner(None, Some("business partner first name"), Some("business partner last name"), None, None, Some("Yes"), Some(testNino), None, None, Some("Yes"), None, None, None, None)
  implicit val mockConfig: ApplicationConfig = mockAppConfig

  private def testRequest(partner: Partner) =
    TestUtil.populateFakeRequest[Partner](FakeRequest(), PartnershipDetailsForm.partnershipDetailsValidationForm, partner)

  val testBusinessPartnersController: BusinessPartnersController =
    new BusinessPartnersController(mockMCC, testSave4LaterService, mockDeEnrolService, mockAuthConnector, mockAuditable, mockAccountUtils, mockAppConfig) {
    override val signInUrl = "/sign-in"
  }

  private def testPartner(haveMore: Boolean = true) = TestUtil.testPartner(
    firstName = "Bob",
    lastName = "Smith",
    otherPartners = {
      haveMore match {
        case true => BooleanRadioEnum.YesString
        case false => BooleanRadioEnum.NoString
      }
    }
  )

  lazy val testPartnerDetails = Partners(List(testPartner(), testPartner(), testPartner(false)))

  "Users who entered from the summary page using the linear view" should {
    "be redirected to the additional premises view after submitting a valid form" in {
      continueWithAuthorisedUser()(testRequest(testPartner(false))) {
        result =>
          redirectLocation(result).get should include("/alcohol-wholesale-scheme/additional-premises")
          verifySave4LaterService(savePartnerDetails = 1)
      }
    }
  }

  "Users who entered from the summary edit view" should {
    "return to the summary view after clicking return" in {
      returnWithAuthorisedUser()(testRequest(testPartner(false))) {
        result =>
          redirectLocation(result).get should include(f"/alcohol-wholesale-scheme/view-section/$partnersName")
          verifySave4LaterService(savePartnerDetails = 1)
      }
    }
  }

  "When loading the delete page we" should {
    "see to the selected partner's delete confirmation page" in {
      showDeleteWithAuthorisedUser() {
        result =>
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("deleteConfirmation-heading").text shouldBe Messages("awrs.delete.confirmation_heading", Messages("awrs.view_application.partner"))
          status(result) shouldBe 200
      }
    }
  }

  "When submitting the delete confirmation page we" should {
    "be routed back to the summary page after confirming Yes" in {
      deleteWithAuthorisedUser()(deleteConfirmation_Yes) {
        result =>
          status(result) shouldBe 303
          redirectLocation(result).get should include("/alcohol-wholesale-scheme/view-section/partnerDetails")
          verifySave4LaterService(savePartnerDetails = 1)
      }
    }
    "be routed back to the summary page after confirming No" in {
      deleteWithAuthorisedUser()(deleteConfirmation_No) {
        result =>
          status(result) shouldBe 303
          redirectLocation(result).get should include("/alcohol-wholesale-scheme/view-section/partnerDetails")
          verifySave4LaterService(savePartnerDetails = 0)
      }
    }
    "be shown an error if nothing is selected" in {
      deleteWithAuthorisedUser()(deleteConfirmation_None) {
        result =>
          status(result) shouldBe 400
      }
    }
    "be routed back to the summary page after confirming Yes for a record that is not the first" in {
      deleteWithAuthorisedUser(id = 2, partners = testPartnerDetails)(deleteConfirmation_Yes) {
        result =>
          status(result) shouldBe 303
          redirectLocation(result).get should include("/alcohol-wholesale-scheme/view-section/partnerDetails")
          verifySave4LaterService(savePartnerDetails = 1)
      }
    }
  }

  private def continueWithAuthorisedUser(partnerId: Int = 1, businessType: String = "LLP")(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchPartnerDetails = testPartnerDetails, fetchAdditionalBusinessPremisesList = None)
    setAuthMocks()
    val result = testBusinessPartnersController.saveAndContinue(partnerId, true).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, businessType))
    test(result)
  }

  private def returnWithAuthorisedUser(partnerId: Int = 1)(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchPartnerDetails = testPartnerDetails)
    setAuthMocks()
    val result = testBusinessPartnersController.saveAndReturn(partnerId, true).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

  private def showDeleteWithAuthorisedUser(id: Int = 1, partners: Partners = Partners(List(testPartner())))(test: Future[Result] => Any): Future[Any] = {
    setupMockSave4LaterServiceWithOnly(fetchPartnerDetails = partners)
    setAuthMocks()
    val result = testBusinessPartnersController.showDelete(id).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def deleteWithAuthorisedUser(id: Int = 1, partners: Partners = Partners(List(testPartner())))(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchPartnerDetails = partners)
    setAuthMocks()
    val result = testBusinessPartnersController.actionDelete(id).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

}
