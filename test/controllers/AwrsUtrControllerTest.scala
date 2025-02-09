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
import connectors.mock.MockAuthConnector
import forms.AwrsEnrolmentUtrForm
import models.AwrsStatus.Approved
import models.{AwrsEnrolmentUtr, Business, Info, SearchResult}
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ServicesUnitTestFixture
import services.mocks.{MockIndexService, MockKeyStoreService}
import utils.{AwrsUnitTestTraits, TestUtil}
import views.html.awrs_utr

class AwrsUtrControllerTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture with MockAuthConnector
  with MockKeyStoreService
  with MockIndexService {

  def testRequest(answer: String): FakeRequest[AnyContentAsFormUrlEncoded] =
    TestUtil.populateFakeRequest[AwrsEnrolmentUtr](FakeRequest(), AwrsEnrolmentUtrForm.awrsEnrolmentUtrForm.form, AwrsEnrolmentUtr(answer))

  def testSearchResult(ref:String) = SearchResult(List(
    Business(ref,
      Some("12/12/2013"),
      Approved,
      Info(Some("Business Name"), Some("Trading Name"), Some("Full name"), None),
      None)))
  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val template: awrs_utr = app.injector.instanceOf[views.html.awrs_utr]

  val testAwrsUtrController: AwrsUtrController = new AwrsUtrController(mockMCC,
    testKeyStoreService, mockDeEnrolService, mockAuthConnector,
    mockAuditable, mockAccountUtils, testLookupService, mockAwrsFeatureSwitches, mockAppConfig, template)

  "AwrsUtrController" must {
    "show not found when feature is not enabled" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUtr()
      setupEnrollmentJourneyFeatureSwitchMock(false)
      val res = testAwrsUtrController.showArwsUtrPage().apply(SessionBuilder.buildRequestWithSession(userId))
      status(res) mustBe 404
    }

    "show the UTR page when enrolmentJourney is enabled" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUtr()
      setupEnrollmentJourneyFeatureSwitchMock(true)
      val res = testAwrsUtrController.showArwsUtrPage().apply(SessionBuilder.buildRequestWithSession(userId))
      status(res) mustBe 200
    }

    "save the UTR to keystore if no errors" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUtr()
      setupEnrollmentJourneyFeatureSwitchMock(true)
      val res = testAwrsUtrController.saveAndContinue().apply(testRequest("6232113818078"))
      status(res) mustBe 200
    }

    "save should return 400 if form has errors" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUtr()
      setupEnrollmentJourneyFeatureSwitchMock(true)
      val res = testAwrsUtrController.saveAndContinue().apply(testRequest("SomthingWithError"))
      status(res) mustBe 400
    }
    
     "save should lookup the urn and save result found in keystore" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUtr()
      setupEnrollmentJourneyFeatureSwitchMock(true)

      val res = testAwrsUtrController.saveAndContinue().apply(testRequest("6232113818078"))
      status(res) mustBe 200

    }
  }

}
