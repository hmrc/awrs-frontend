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
import models.{AwrsEnrolmentUtr, AwrsRegisteredPostcode, Business, Info, SearchResult}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{verify, when}
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{EnrolService, ServicesUnitTestFixture}
import services.mocks.{MockIndexService, MockKeyStoreService}
import utils.{AwrsUnitTestTraits, TestUtil}
import views.html.awrs_utr

import scala.concurrent.Future

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
  val mockEnrolService: EnrolService = mock[EnrolService]

  val testAwrsUtrController: AwrsUtrController = new AwrsUtrController(mockMCC,
    testKeyStoreService, mockDeEnrolService, mockAuthConnector,
    mockAuditable, mockAccountUtils, mockMatchingService,
    mockEnrolService, mockAwrsFeatureSwitches, mockAppConfig, template)

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
      when(mockAccountUtils.isSaAccount(ArgumentMatchers.any())).thenReturn(true)
      val res = testAwrsUtrController.showArwsUtrPage().apply(SessionBuilder.buildRequestWithSession(userId))
      status(res) mustBe 200
    }

    "enroll SA UTR for AWRS  if no errors" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUtr(utr = Some(AwrsEnrolmentUtr("6232113818078")),
        registeredPostcode = Some(AwrsRegisteredPostcode("NE98 1ZZ")),
        searchResult = Some(testSearchResult("TestAWRSRef")))
      setupEnrollmentJourneyFeatureSwitchMock(true)
      when(mockAccountUtils.isSaAccount(ArgumentMatchers.any())).thenReturn(true)
      when(mockMatchingService.isValidUTRandPostCode(ArgumentMatchers.any(), ArgumentMatchers.any(),
        ArgumentMatchers.any(), ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(true))
      when(mockEnrolService.enrolAWRS(ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any())
      (ArgumentMatchers.any(),ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val res = testAwrsUtrController.saveAndContinue().apply(testRequest("6232113818078"))
      status(res) mustBe 303
      verify(mockEnrolService).enrolAWRS(ArgumentMatchers.eq("TestAWRSRef"),
        ArgumentMatchers.eq(AwrsRegisteredPostcode("NE98 1ZZ")),
        ArgumentMatchers.eq(AwrsEnrolmentUtr("6232113818078")),
        ArgumentMatchers.eq("SOP"))(ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    "enroll CT UTR for AWRS  if no errors" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUtr(utr = Some(AwrsEnrolmentUtr("6232113818078")),
        registeredPostcode = Some(AwrsRegisteredPostcode("NE98 1ZZ")),
        searchResult = Some(testSearchResult("TestAWRSRef")))
      setupEnrollmentJourneyFeatureSwitchMock(true)
      when(mockAccountUtils.isSaAccount(ArgumentMatchers.any())).thenReturn(false)
      when(mockMatchingService.isValidUTRandPostCode(ArgumentMatchers.any(), ArgumentMatchers.any(),
        ArgumentMatchers.any(), ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(true))
      when(mockEnrolService.enrolAWRS(ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any())
      (ArgumentMatchers.any(),ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val res = testAwrsUtrController.saveAndContinue().apply(testRequest("6232113818078"))
      status(res) mustBe 303
      verify(mockEnrolService).enrolAWRS(ArgumentMatchers.eq("TestAWRSRef"),
        ArgumentMatchers.eq(AwrsRegisteredPostcode("NE98 1ZZ")),
        ArgumentMatchers.eq(AwrsEnrolmentUtr("6232113818078")),
        ArgumentMatchers.eq("CT"))(ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    "save should return 400 if form has errors" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUtr()
      setupEnrollmentJourneyFeatureSwitchMock(true)
      when(mockAccountUtils.isSaAccount(ArgumentMatchers.any())).thenReturn(true)
      val res = testAwrsUtrController.saveAndContinue().apply(testRequest("SomthingWithError"))
      status(res) mustBe 400
    }

  }

}
