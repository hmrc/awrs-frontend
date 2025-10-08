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
import models.reenrolment._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ServicesUnitTestFixture
import services.mocks.{MockIndexService, MockKeyStoreService}
import services.reenrolment.RegisteredUrnService
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AwrsUnitTestTraits, TestUtil}
import views.html.reenrolment.awrs_registered_urn

import scala.concurrent.{ExecutionContext, Future}

class RegisteredUrnControllerTest
    extends AwrsUnitTestTraits
    with ServicesUnitTestFixture
    with MockAuthConnector
    with MockKeyStoreService
    with MockIndexService {

  def testRequest(answer: String): FakeRequest[AnyContentAsFormUrlEncoded] =
    TestUtil.populateFakeRequest[AwrsEnrolmentUrn](FakeRequest(), RegisteredUrnForm.awrsEnrolmentUrnForm.form, AwrsEnrolmentUrn(answer))

  val testAwrsRef  = "XXAW00000000051"
  val testUtr      = "1234567890"
  val testPostcode = "SW1A 1AA"

  val testKnownFactsResponse = KnownFactsResponse(
    "HMRC-AWRS-ORG",
    Seq(
      Enrolment(
        identifiers = Seq(Identifier("AWRSRefNumber", testAwrsRef)),
        verifiers = Seq(Verifier("SAUTR", testUtr), Verifier("Postcode", testPostcode))
      ))
  )

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val template: awrs_registered_urn                = app.injector.instanceOf[views.html.reenrolment.awrs_registered_urn]

  val registeredUrnService = new RegisteredUrnService(
    keyStoreService = testKeyStoreService,
    enrolmentStoreConnector = mockEnrolmentStoreProxyConnector,
    applicationConfig = mockAppConfig)

  val testAwrsUrnController: RegisteredUrnController = new RegisteredUrnController(
    mockMCC,
    testKeyStoreService,
    mockDeEnrolService,
    mockAuthConnector,
    mockAuditable,
    mockAccountUtils,
    mockAppConfig,
    registeredUrnService,
    template)

  "AwrsUrnController" must {

    "show the URN page" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUrn()
      val res = testAwrsUrnController.showArwsUrnPage().apply(SessionBuilder.buildRequestWithSession(userId))
      status(res) mustBe 200
    }

    "save the URN and known facts to keystore if no errors" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUrn()
      when(
        mockEnrolmentStoreProxyConnector
          .lookupEnrolments(ArgumentMatchers.eq(AwrsKnownFacts(testAwrsRef)))(any[HeaderCarrier](), any[ExecutionContext]()))
        .thenReturn(Future.successful(Some(testKnownFactsResponse)))

      val EnrolmentResponse = EnrolledUserIds(
        principalUserIds = Seq(testAwrsRef)
      )

      when(
        mockEnrolmentStoreProxyConnector
          .queryForAssignedPrincipalUsersOfAWRSEnrolment(ArgumentMatchers.eq(testAwrsRef))(any[HeaderCarrier](), any[ExecutionContext]()))
        .thenReturn(Future.successful(Some(EnrolmentResponse)))

      val res = testAwrsUrnController.saveAndContinue().apply(testRequest(testAwrsRef))
      status(res) mustBe 303
      verifyKeyStoreService(saveKnownFacts = 1, saveAwrsUrn = 1)
    }

    "save should return 400 if form has errors" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUrn()
      val res = testAwrsUrnController.saveAndContinue().apply(testRequest("SomthingWithError"))
      status(res) mustBe 400
    }

    "save should redirect to kickout page if ES20 does not return known facts" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUrn()
      when(
        mockEnrolmentStoreProxyConnector
          .lookupEnrolments(ArgumentMatchers.eq(AwrsKnownFacts(testAwrsRef)))(any[HeaderCarrier](), any[ExecutionContext]()))
        .thenReturn(Future.successful(None))
      val res = testAwrsUrnController.saveAndContinue().apply(testRequest(testAwrsRef))

      val result = await(res)
      result.header.status mustBe 303
      result.header.headers("Location") mustBe controllers.reenrolment.routes.KickoutController.showKickOutPage.url
      verifyKeyStoreService(saveKnownFacts = 0)

    }

    "save should redirect to kickout page when lookupEnrolments returns an error" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUrn()

      when(
        mockEnrolmentStoreProxyConnector
          .lookupEnrolments(ArgumentMatchers.eq(AwrsKnownFacts(testAwrsRef)))(any[HeaderCarrier](), any[ExecutionContext]()))
        .thenReturn(Future.failed(new RuntimeException("ES20 service unavailable")))

      val res = testAwrsUrnController.saveAndContinue().apply(testRequest(testAwrsRef))

      status(res) mustBe 303
      redirectLocation(res) mustBe Some(controllers.reenrolment.routes.KickoutController.showKickOutPage.url)
    }

    "save should redirect to confirm de-enrolment" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUrn()
      when(
        mockEnrolmentStoreProxyConnector
          .lookupEnrolments(ArgumentMatchers.eq(AwrsKnownFacts(testAwrsRef)))(any[HeaderCarrier](), any[ExecutionContext]()))
        .thenReturn(Future.successful(Some(testKnownFactsResponse)))

      val EnrolmentResponse = EnrolledUserIds(
        principalUserIds = Seq(testAwrsRef)
      )

      when(
        mockEnrolmentStoreProxyConnector
          .queryForAssignedPrincipalUsersOfAWRSEnrolment(ArgumentMatchers.eq(testAwrsRef))(any[HeaderCarrier](), any[ExecutionContext]()))
        .thenReturn(Future.successful(Some(EnrolmentResponse)))

      val res = testAwrsUrnController.saveAndContinue().apply(testRequest(testAwrsRef))
      status(res) mustBe 303
      verifyKeyStoreService(saveKnownFacts = 1, saveAwrsUrn = 1)
    }

  }

}
