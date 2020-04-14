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
import forms.BusinessRegistrationDetailsForm
import models._
import org.jsoup.Jsoup
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.DataCacheKeys._
import services.{BusinessMatchingService, ServicesUnitTestFixture}
import utils.TestUtil._
import utils.{AwrsUnitTestTraits, TestUtil}
import utils.TestConstants._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.i18n.Messages

import scala.concurrent.Future

class BusinessRegistrationDetailsControllerTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture {

  val mockBusinessMatchingService: BusinessMatchingService = mock[BusinessMatchingService]

  def testRequest(businessRegistrationDetails: BusinessRegistrationDetails, entityType: String): FakeRequest[AnyContentAsFormUrlEncoded] =
    TestUtil.populateFakeRequest[BusinessRegistrationDetails](FakeRequest(), BusinessRegistrationDetailsForm.businessRegistrationDetailsValidationForm(entityType), businessRegistrationDetails)

  val testBusinessRegistrationDetailsController: BusinessRegistrationDetailsController =
    new BusinessRegistrationDetailsController(mockMCC, mockBusinessMatchingService, testSave4LaterService, mockAuthConnector, mockAuditable, mockAccountUtils, mockAppConfig) {
    override val signInUrl: String = applicationConfig.signIn
  }

  "BusinessRegistrationDetailsController" must {

    "Users who entered from the summary edit view" should {
      "return to the summary view after clicking return" in {
        returnWithAuthorisedUser(testBusinessRegistrationDetails(), "SOP") {
          result =>
            redirectLocation(result).get should include(f"/alcohol-wholesale-scheme/view-section/$businessRegistrationDetailsName")
            verifySave4LaterService(saveBusinessRegistrationDetails = 1)
        }
      }
    }

    groupEntities.foreach {
      legalEntity =>
        s"successfully save the page if the group utr is valid for entity $legalEntity" in {
          returnWithAuthorisedUser(testBusinessRegistrationDetails(doYouHaveUTR = "Yes", utr = testUtr, legalEntity = legalEntity), legalEntity) {
            result =>
              redirectLocation(result).get should include(f"/alcohol-wholesale-scheme/view-section/$businessRegistrationDetailsName")
              verifySave4LaterService(saveBusinessRegistrationDetails = 1)
          }
        }

        s"fail to save the page if the group utr is invalid for entity $legalEntity" in {
          returnWithAuthorisedUser(testBusinessRegistrationDetails(doYouHaveUTR = "Yes", utr = testNonMatchingUtr, legalEntity = legalEntity), legalEntity) {
            result =>
              val doc = Jsoup.parse(contentAsString(result))
              doc.getElementById("utr_errorLink").text shouldBe Messages("awrs.generic.error.utr_invalid_match")
              doc.getElementById("utr-error-0").text shouldBe Messages("awrs.generic.error.utr_invalid_match")
              verifySave4LaterService(saveBusinessRegistrationDetails = 0)
          }
        }

        s"fail to save the page if the group utr is missing for entity $legalEntity" in {
          returnWithAuthorisedUser(testBusinessRegistrationDetails(doYouHaveUTR = "Yes", utr = None, legalEntity = legalEntity), legalEntity) {
            result =>
              val doc = Jsoup.parse(contentAsString(result))
              doc.getElementById("utr_errorLink").text shouldBe Messages("awrs.generic.error.utr_empty")
              doc.getElementById("utr-error-0").text shouldBe Messages("awrs.generic.error.utr_empty")
              verifySave4LaterService(saveBusinessRegistrationDetails = 0)
          }
        }
    }

    def returnWithAuthorisedUser(businessRegistrationDetails: BusinessRegistrationDetails, legalEntity: String = "SOP")(test: Future[Result] => Any) {
      val fakeRequest = testRequest(businessRegistrationDetails, legalEntity)
      setupMockSave4LaterServiceWithOnly(
        fetchBusinessRegistrationDetails = businessRegistrationDetails
      )
      setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
      when(mockBusinessMatchingService.isValidMatchedGroupUtr(ArgumentMatchers.eq(testNonMatchingUtr), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(false)
      when(mockBusinessMatchingService.isValidMatchedGroupUtr(ArgumentMatchers.eq(testUtr), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(true)
      val result = testBusinessRegistrationDetailsController.saveAndReturn().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, testBusinessCustomerDetails(legalEntity).businessType.get))
      test(result)
    }

  }
}
