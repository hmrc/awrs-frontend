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
import forms.BusinessDirectorsForm
import models.BusinessDirector
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ServicesUnitTestFixture
import utils.TestConstants._
import utils.TestUtil
import utils.TestUtil._
import views.html.awrs_business_directors

import scala.concurrent.Future

class AdditionalDirectorsControllerTest extends ServicesUnitTestFixture {

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val directorPageURL: Int => String = (id: Int) => s"/alcohol-wholesale-scheme/business-directors?id=$id"
  val directorPage2URL: String = directorPageURL(2)

  val mockTemplate: awrs_business_directors = app.injector.instanceOf[views.html.awrs_business_directors]

  val testBusinessDirectorsController: BusinessDirectorsController =
    new BusinessDirectorsController(mockMCC, testSave4LaterService, mockDeEnrolService, mockAuthConnector, mockAuditable, mockAccountUtils, mockAppConfig, mockTemplate){
    override val signInUrl = "/sign-in"
  }

  def testRequest(director: BusinessDirector): FakeRequest[AnyContentAsFormUrlEncoded] =
    TestUtil.populateFakeRequest[BusinessDirector](FakeRequest(), BusinessDirectorsForm.businessDirectorsValidationForm, director)

  "Business Directors Page load for Authorised users" should {
    "do not load a blank page for Director 2 if cache is empty" in {
      getWithAuthorisedUserNoCache {
        result =>
          status(result) should be(NOT_FOUND)
      }
    }
  }

  "On Business Directors page, for Directors that is a person, pressing Continue for Authorised users" should {

    val personTypes = List("Director", "Company Secretary", "Director and Company Secretary")

    for (directorType <- personTypes) {

      "redirect to trading activity page when valid data is provided and 'Other directors' is No, when director is %s".format(directorType) in {
        continueWithAuthorisedUser(testRequest(testBusinessDirectorDefault(directorsAndCompanySecretaries = directorType, personOrCompany = "person", firstName = "firstName", lastName = "lastName", doTheyHaveNationalInsurance = "No", nino = "", passportNumber = testPassportNo, isDirectorACompany = "No", otherDirectors = "No"))) {
          result =>
            status(result) shouldBe SEE_OTHER
            redirectLocation(result).get should be("/alcohol-wholesale-scheme/trading-activity")
        }
      }

      "redirect to Business Directors view page when valid data is provided and 'Other directors' is Yes, when director is %s".format(directorType) in {

        continueWithAuthorisedUser(testRequest(testBusinessDirectorDefault(directorsAndCompanySecretaries = directorType, personOrCompany = "person", firstName = "firstName", lastName = "lastName", doTheyHaveNationalInsurance = "No", nino = "", passportNumber = testPassportNo, isDirectorACompany = "No", otherDirectors = "Yes"))) {
          result =>
            status(result) shouldBe SEE_OTHER
            redirectLocation(result).get should be(directorPage2URL)
        }
      }

    }
  }

  "On Business Directors page, for Directors that are Companies, pressing Continue for Authorised users" should {
    "redirect to trading activity page when valid data is provided and 'Other directors' is No" in {
      continueWithAuthorisedUser(testRequest(testBusinessDirectorDefault(directorsAndCompanySecretaries = "Director", personOrCompany = "company", businessName = "Simply Wines", doYouHaveTradingName = "No", doYouHaveUTR = "No", doYouHaveCRN = "No", doYouHaveVRN = "Yes", vrn = testVrn, otherDirectors = "No"))) {
        result =>
          status(result) shouldBe SEE_OTHER
          redirectLocation(result).get should be("/alcohol-wholesale-scheme/trading-activity")
      }
    }

    "redirect to Business Directors view page when valid data is provided and 'Other directors' is Yes" in {
      continueWithAuthorisedUser(testRequest(testBusinessDirectorDefault(directorsAndCompanySecretaries = "Director", personOrCompany = "company", businessName = "Simply Wines", doYouHaveTradingName = "No", doYouHaveUTR = "No", doYouHaveCRN = "No", doYouHaveVRN = "Yes", vrn = testVrn, otherDirectors = "Yes"))) {
        result =>
          status(result) shouldBe SEE_OTHER
          redirectLocation(result).get should be(directorPage2URL)
      }
    }
  }

  def getWithAuthorisedUserNoCache(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchBusinessDirectors = None)
    setAuthMocks()
    val result = testBusinessDirectorsController.showBusinessDirectors(2, isLinearMode = true, isNewRecord = true).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def continueWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded], id: Int = 1)(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(
      fetchBusinessDirectors = testBusinessDirectors,
      fetchTradingActivity = None
    )
    setAuthMocks()
    val result = testBusinessDirectorsController.saveAndContinue(id, isNewRecord = true).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

}
