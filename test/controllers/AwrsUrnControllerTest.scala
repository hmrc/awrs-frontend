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
import forms.AwrsEnrollmentUrnForm
import models.AwrsStatus.Approved
import models.{AwrsEnrollmentUrn, Business, Info, SearchResult}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ServicesUnitTestFixture
import services.mocks.{MockIndexService, MockKeyStoreService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AwrsUnitTestTraits, TestUtil}
import views.html.awrs_urn

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AwrsUrnControllerTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture with MockAuthConnector
  with MockKeyStoreService
  with MockIndexService {

  def testRequest(answer: String): FakeRequest[AnyContentAsFormUrlEncoded] =
    TestUtil.populateFakeRequest[AwrsEnrollmentUrn](FakeRequest(), AwrsEnrollmentUrnForm.awrsEnrolmentUrnForm.form, AwrsEnrollmentUrn(answer))

  def testSearchResult(ref:String) = SearchResult(List(
    Business(ref,
      Some("12/12/2013"),
      Approved,
      Info(Some("Business Name"), Some("Trading Name"), Some("Full name"), None),
      None)))
  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val template: awrs_urn = app.injector.instanceOf[views.html.awrs_urn]

  val testAwrsUrnController: AwrsUrnController = new AwrsUrnController(mockMCC,
    testKeyStoreService, mockDeEnrolService, mockAuthConnector,
    mockAuditable, mockAccountUtils, testLookupService, mockAwrsFeatureSwitches, mockAppConfig, template)

  "AwrsUrnController" must {
    "show not found when feature is not enabled" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUrn()
      setupEnrollmentJourneyFeatureSwitchMock(false)
      val res = testAwrsUrnController.showArwsUrnPage().apply(SessionBuilder.buildRequestWithSession(userId))
      status(res) mustBe 404
    }

    "show the URN page when enrolmentJourney is enabled" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUrn()
      setupEnrollmentJourneyFeatureSwitchMock(true)
      val res = testAwrsUrnController.showArwsUrnPage().apply(SessionBuilder.buildRequestWithSession(userId))
      status(res) mustBe 200
    }

    "save the URN to keystore if no errors" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUrn()
      setupEnrollmentJourneyFeatureSwitchMock(true)
      when(mockLookupConnector.queryByUrn(ArgumentMatchers.eq("XAAW00000123456"))
      (any[HeaderCarrier](),any[ExecutionContext]()))
        .thenReturn(Future.successful(Some(testSearchResult("XAAW00000123456"))))
      val res = testAwrsUrnController.saveAndContinue().apply(testRequest("XAAW00000123456"))
      status(res) mustBe 200
    }

    "save should return 400 if form has errors" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUrn()
      setupEnrollmentJourneyFeatureSwitchMock(true)
      val res = testAwrsUrnController.saveAndContinue().apply(testRequest("SomthingWithError"))
      status(res) mustBe 400
    }

    "save should lookup the urn and save result found in keystore" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUrn()
      setupEnrollmentJourneyFeatureSwitchMock(true)

      when(mockLookupConnector.queryByUrn(ArgumentMatchers.eq("XXAW00000000051"))(any[HeaderCarrier](),any[ExecutionContext]())).thenReturn(Future(Some(testSearchResult("XXAW00000000051"))))
      val res = testAwrsUrnController.saveAndContinue().apply(testRequest("XXAW00000000051"))
      status(res) mustBe 200
      verifyKeyStoreService(saveSearchResults = 1)

    }


  }
}