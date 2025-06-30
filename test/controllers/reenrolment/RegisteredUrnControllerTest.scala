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

package controllers.reenrolment

import builders.SessionBuilder
import connectors.mock.MockAuthConnector
import forms.reenrolment.RegisteredUrnForm
import models.AwrsEnrolmentUrn
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
import views.html.reenrolment.awrs_registered_urn

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class RegisteredUrnControllerTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture with MockAuthConnector
  with MockKeyStoreService
  with MockIndexService {

  def testRequest(answer: String): FakeRequest[AnyContentAsFormUrlEncoded] =
    TestUtil.populateFakeRequest[AwrsEnrolmentUrn](FakeRequest(), RegisteredUrnForm.awrsEnrolmentUrnForm.form, AwrsEnrolmentUrn(answer))

  val testGroupId = "TestGroupId"
  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val template: awrs_registered_urn = app.injector.instanceOf[views.html.reenrolment.awrs_registered_urn]

  val testAwrsUrnController: RegisteredUrnController = new RegisteredUrnController(mockMCC,
    testKeyStoreService, mockDeEnrolService, mockAuthConnector,
    mockAuditable, mockAccountUtils, mockAwrsFeatureSwitches,
    testEnrolmentStoreProxyService, mockAppConfig, template)

  "AwrsUrnController" must {
    "show not found when feature is not enabled" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUrn()
      setupEnrolmentJourneyFeatureSwitchMock(false)
      val res = testAwrsUrnController.showArwsUrnPage().apply(SessionBuilder.buildRequestWithSession(userId))
      status(res) mustBe 404
    }

    "show the URN page when enrolmentJourney is enabled" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUrn()
      setupEnrolmentJourneyFeatureSwitchMock(true)
      val res = testAwrsUrnController.showArwsUrnPage().apply(SessionBuilder.buildRequestWithSession(userId))
      status(res) mustBe 200
    }

    "save the URN to keystore if no errors" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUrn()
      setupEnrolmentJourneyFeatureSwitchMock(true)
      when(mockEnrolmentStoreProxyConnector.queryGroupIdForEnrolment(ArgumentMatchers.eq("XAAW00000123456"))
      (any[HeaderCarrier](), any[ExecutionContext]()))
        .thenReturn(Future.successful(Some(testGroupId)))
      val res = testAwrsUrnController.saveAndContinue().apply(testRequest("XAAW00000123456"))
      status(res) mustBe 303
    }

    "save should return 400 if form has errors" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUrn()
      setupEnrolmentJourneyFeatureSwitchMock(true)
      val res = testAwrsUrnController.saveAndContinue().apply(testRequest("SomthingWithError"))
      status(res) mustBe 400
    }
    
     "save should lookup the urn and save result found in keystore" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUrn()
      setupEnrolmentJourneyFeatureSwitchMock(true)

      when(mockEnrolmentStoreProxyConnector.queryGroupIdForEnrolment(ArgumentMatchers.eq("XXAW00000000051"))(any[HeaderCarrier](),any[ExecutionContext]()))
        .thenReturn(Future(Some(testGroupId)))
      val res = testAwrsUrnController.saveAndContinue().apply(testRequest("XXAW00000000051"))
      status(res) mustBe 303
      verifyKeyStoreService(saveGroupId = 1)
    }

    "save should redirect to kickout page if ES1 does not return a groupId" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUrn()
      setupEnrolmentJourneyFeatureSwitchMock(true)
      when(mockEnrolmentStoreProxyConnector.queryGroupIdForEnrolment(ArgumentMatchers.eq("XXAW00000000051"))(any[HeaderCarrier](),any[ExecutionContext]()))
        .thenReturn(Future(None))
      val res = testAwrsUrnController.saveAndContinue().apply(testRequest("XXAW00000000051"))

      val result = await(res)
      result.header.status mustBe 303
      result.header.headers("Location") mustBe controllers.reenrolment.routes.KickoutController.showURNKickOutPage.url
      verifyKeyStoreService(saveGroupId = 0)

    }
  }

}
