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

package controllers

import builders.SessionBuilder
import models.{BusinessDirector, BusinessDirectors, CompanyNames}
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.DataCacheKeys._
import services.ServicesUnitTestFixture
import utils.AwrsUnitTestTraits
import utils.TestConstants._
import utils.TestUtil._
import views.html.awrs_business_directors

import scala.concurrent.Future

class BusinessDirectorsControllerTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture {

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val mockTemplate: awrs_business_directors = app.injector.instanceOf[views.html.awrs_business_directors]

  val testBusinessDirectorsController: BusinessDirectorsController =
    new BusinessDirectorsController(mockMCC, testSave4LaterService, mockDeEnrolService, mockAuthConnector, mockAuditable, mockAccountUtils, mockAppConfig, mockTemplate) {
    override val signInUrl = "/sign-in"
  }

  "Users who entered from the summary edit view" must {
    "return to the summary view after clicking return" in {
      returnWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("directorsAndCompanySecretaries" -> "Director", "personOrCompany" -> "person", "firstName" -> "firstName",
        "lastName" -> "lastName", "doTheyHaveNationalInsurance" -> "No", "passportNumber" -> testPassportNo, "isDirectorACompany" -> "No", "otherDirectors" -> "No")) {
        result =>
          redirectLocation(result).get must include(f"/alcohol-wholesale-scheme/view-section/$businessDirectorsName")
          verifySave4LaterService(saveBusinessDirectors = 1)
      }
    }
  }

  "When submitting the delete confirmation page we" must {
    "be routed back to the summary page after confirming Yes" in {
      deleteWithAuthorisedUser()(deleteConfirmation_Yes) {
        result =>
          status(result) mustBe 303
          redirectLocation(result).get must include("/alcohol-wholesale-scheme/view-section/businessDirectors")
          verifySave4LaterService(saveBusinessDirectors = 1)
      }
    }
    "be routed back to the summary page after confirming No" in {
      deleteWithAuthorisedUser()(deleteConfirmation_No) {
        result =>
          status(result) mustBe 303
          redirectLocation(result).get must include("/alcohol-wholesale-scheme/view-section/businessDirectors")
          verifySave4LaterService(saveBusinessDirectors = 0)
      }
    }
    "be shown an error if nothing is selected" in {
      deleteWithAuthorisedUser()(deleteConfirmation_None) {
        result =>
          status(result) mustBe 400
      }
    }
    "be routed back to the summary page after confirming Yes for a record that is not the first" in {
      deleteWithAuthorisedUser(id = 2, directors = BusinessDirectors(List(testBusinessDirector, testBusinessDirector)))(deleteConfirmation_Yes) {
        result =>
          status(result) mustBe 303
          redirectLocation(result).get must include("/alcohol-wholesale-scheme/view-section/businessDirectors")
          verifySave4LaterService(saveBusinessDirectors = 1)
      }
    }
  }

  "processDataPersonOrCompany" when {

    "the director is a person" must {
      "remove the VRN and CompanyNames fields" in {
        val person = businessDirector("person")
        val processedPerson = testBusinessDirectorsController.processDataPersonOrCompany(person)
        processedPerson.doTheyHaveNationalInsurance mustBe Some("Yes")
        processedPerson.companyNames mustBe None
        processedPerson.doYouHaveVRN mustBe None
      }
    }

    "the director is a company" must {
      "remove the do you have NINO field" in {
        val company = businessDirector("company")
        val processedCompany = testBusinessDirectorsController.processDataPersonOrCompany(company)
        processedCompany.doTheyHaveNationalInsurance mustBe None
        processedCompany.companyNames mustBe Some(CompanyNames(businessName = Some("Test Company"), doYouHaveTradingName = Some("Yes"), tradingName = Some("Trade Name")))
        processedCompany.doYouHaveVRN mustBe Some("Yes")
      }
    }
  }

  private def returnWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any): Unit = {
    setupMockSave4LaterServiceWithOnly(fetchBusinessDirectors = testBusinessDirectors)
    setAuthMocks()
    val result = testBusinessDirectorsController.saveAndReturn(1, true).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

  private def deleteWithAuthorisedUser(id: Int = 1, directors: BusinessDirectors = testBusinessDirectors)(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any): Unit = {
    setupMockSave4LaterServiceWithOnly(fetchBusinessDirectors = directors)
    setAuthMocks()
    val result = testBusinessDirectorsController.actionDelete(id).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId).withMethod("POST"))
    test(result)
  }

  private def businessDirector(entity: String): BusinessDirector =
    BusinessDirector(personOrCompany = Some(entity),
      doTheyHaveNationalInsurance = Some("Yes"),
      companyNames = Some(CompanyNames(businessName = Some("Test Company"), doYouHaveTradingName = Some("Yes"), tradingName = Some("Trade Name"))),
      doYouHaveVRN = Some("Yes"))
}
