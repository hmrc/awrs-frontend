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

import builders.SessionBuilder
import forms.BusinessRegistrationDetailsForm
import models._
import org.jsoup.Jsoup
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.DataCacheKeys._
import services.{Save4LaterService, ServicesUnitTestFixture}
import utils.TestUtil._
import utils.{AwrsUnitTestTraits, MatchingUtil, TestUtil}
import utils.TestConstants._
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._

import scala.concurrent.Future

class BusinessRegistrationDetailsControllerTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture {

  val mockMatchingUtil: MatchingUtil = mock[MatchingUtil]

  def testRequest(businessRegistrationDetails: BusinessRegistrationDetails, entityType: String) =
    TestUtil.populateFakeRequest[BusinessRegistrationDetails](FakeRequest(), BusinessRegistrationDetailsForm.businessRegistrationDetailsValidationForm(entityType), businessRegistrationDetails)

  object TestBusinessRegistrationDetailsController extends BusinessRegistrationDetailsController {
    override val authConnector = mockAuthConnector
    override val save4LaterService = TestSave4LaterService
    override val matchingUtil = mockMatchingUtil
  }

  "BusinessRegistrationDetailsController" must {

    "use the correct AwrsService" in {
      BusinessRegistrationDetailsController.save4LaterService shouldBe Save4LaterService
    }

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
              doc.getElementById("utr_errorLink").text shouldBe Messages("awrs.generic.error.utr_invalid_match.summary")
              doc.getElementById("utr-error-0").text shouldBe Messages("awrs.generic.error.utr_invalid_match")
              verifySave4LaterService(saveBusinessRegistrationDetails = 0)
          }
        }

        s"fail to save the page if the group utr is missing for entity $legalEntity" in {
          returnWithAuthorisedUser(testBusinessRegistrationDetails(doYouHaveUTR = "Yes", utr = None, legalEntity = legalEntity), legalEntity) {
            result =>
              val doc = Jsoup.parse(contentAsString(result))
              doc.getElementById("utr_errorLink").text shouldBe Messages("awrs.generic.error.utr_empty.summary")
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
      when(mockMatchingUtil.isValidMatchedGroupUtr(Matchers.eq(testNonMatchingUtr))(Matchers.any(), Matchers.any())).thenReturn(false)
      when(mockMatchingUtil.isValidMatchedGroupUtr(Matchers.eq(testUtr))(Matchers.any(), Matchers.any())).thenReturn(true)
      val result = TestBusinessRegistrationDetailsController.saveAndReturn().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, testBusinessCustomerDetails(legalEntity).businessType.get))
      test(result)
    }

  }
}
