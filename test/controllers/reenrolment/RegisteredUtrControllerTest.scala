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
import forms.reenrolment.RegisteredUtrForm
import models.reenrolment.AwrsRegisteredPostcode
import models.{AwrsEnrolmentUrn, AwrsEnrolmentUtr, EnrolResponse, Identifier}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, verifyNoInteractions, when}
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.mocks.{MockIndexService, MockKeyStoreService}
import services.{EnrolService, EnrolmentStoreProxyService, ServicesUnitTestFixture}
import uk.gov.hmrc.http.BadRequestException
import utils.{AwrsUnitTestTraits, TestUtil}
import views.html.reenrolment.awrs_registered_utr

import scala.concurrent.Future

class RegisteredUtrControllerTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture
  with MockAuthConnector
  with MockKeyStoreService
  with MockIndexService
  with BeforeAndAfterEach {

  private val testGroupId = "TestGroupId"
  private val testUtr = "6232113818078"
  private val testAwrsRef = "TestAWRSRef"
  private val testPostcode = "NE98 1ZZ"

  private val template: awrs_registered_utr = app.injector.instanceOf[views.html.reenrolment.awrs_registered_utr]
  private val mockEnrolService: EnrolService = mock[EnrolService]
  private val mockEnrolmentStoreService: EnrolmentStoreProxyService = mock[EnrolmentStoreProxyService]

  private val controller = new RegisteredUtrController(
    mockMCC,
    testKeyStoreService,
    mockDeEnrolService,
    mockAuthConnector,
    mockAuditable,
    mockAccountUtils,
    mockEnrolService,
    mockEnrolmentStoreService,
    mockAwrsFeatureSwitches,
    mockAppConfig,
    template
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    resetMocks()
    setupEnrolmentJourneyFeatureSwitchMock(true)
  }

  private def resetMocks(): Unit = {
    reset(
      mockDeEnrolService,
      mockAuthConnector,
      mockAuditable,
      mockAccountUtils,
      mockEnrolService,
      mockAwrsFeatureSwitches,
      mockDeEnrolService,
      mockEnrolmentStoreService,
      mockAppConfig
    )
  }

  private def testRequest(answer: String): FakeRequest[AnyContentAsFormUrlEncoded] =
    TestUtil.populateFakeRequest[AwrsEnrolmentUtr](
      FakeRequest(),
      RegisteredUtrForm.awrsEnrolmentUtrForm.form,
      AwrsEnrolmentUtr(answer)
    )

  private def setupSuccessfulVerification(isVerified: Boolean = true): Unit = {
    when(mockEnrolmentStoreService.verifyKnownFacts(any(), any(), any(), any())(any(), any()))
      .thenReturn(Future.successful(isVerified))
  }

  private def setupSuccessfulDeEnrolment(success: Boolean = true): Unit = {
    when(mockDeEnrolService.deEnrolAwrs(any(), any())(any(), any()))
      .thenReturn(Future.successful(success))
  }

  private def setupSuccessfulEnrolment(): Unit = {
    when(mockEnrolService.enrolAWRS(
      ArgumentMatchers.eq(testAwrsRef),
      ArgumentMatchers.eq(testPostcode),
      ArgumentMatchers.eq(Some(testUtr)),
      ArgumentMatchers.eq("SOP"),
      ArgumentMatchers.eq(Map.empty)
    )(any(), any()))
      .thenReturn(Future.successful(EnrolResponse("serviceName", "state", List(Identifier("AWRS", "AWRS_Ref_No")))))
  }

  private def setupTestData(hasGroupId: Boolean = true): Unit = {
    setupMockKeystoreServiceForAwrsUtr(
      urn = Some(AwrsEnrolmentUrn(testAwrsRef)),
      utr = Some(AwrsEnrolmentUtr(testUtr)),
      registeredPostcode = Some(AwrsRegisteredPostcode(testPostcode)),
      groupId = if (hasGroupId) Some(testGroupId) else None
    )
  }

  "RegisteredUtrController" when {

    "showArwsUtrPage is called" must {
      "return 404 when feature is not enabled" in {
        setAuthMocks()
        setupTestData()
        setupEnrolmentJourneyFeatureSwitchMock(false)

        val result = controller.showArwsUtrPage().apply(SessionBuilder.buildRequestWithSession(userId))

        status(result) mustBe 404
      }

      "return 200 when enrolment journey is enabled" in {
        setAuthMocks()
        setupTestData()
        when(mockAccountUtils.isSaAccount(any())).thenReturn(true)

        val result = controller.showArwsUtrPage().apply(SessionBuilder.buildRequestWithSession(userId))

        status(result) mustBe 200
      }
    }

    "saveAndContinue is called" must {
      "redirect to kickout page when enrolment fails" in {
        setAuthMocks()
        setupTestData()
        setupSuccessfulVerification()
        setupSuccessfulDeEnrolment()
        when(mockAccountUtils.isSaAccount(any())).thenReturn(true)
        when(mockEnrolService.enrolAWRS(any(), any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.failed(new BadRequestException("")))

        val result = controller.saveAndContinue().apply(testRequest(testUtr))

        status(result) mustBe 303
        redirectLocation(result) mustBe Some(controllers.reenrolment.routes.KickoutController.showURNKickOutPage.url)
      }

      "redirect to kickout page when de-enrolment fails" in {
        setAuthMocks()
        setupTestData()
        when(mockAccountUtils.isSaAccount(any())).thenReturn(true)
        setupSuccessfulVerification()
        setupSuccessfulDeEnrolment(success = false)

        val result = controller.saveAndContinue().apply(testRequest(testUtr))

        status(result) mustBe 303
        redirectLocation(result) mustBe Some(controllers.reenrolment.routes.KickoutController.showURNKickOutPage.url)
      }

      "redirect to kickout page when verifyKnownFacts returns false" in {
        setAuthMocks()
        setupTestData()
        when(mockAccountUtils.isSaAccount(any())).thenReturn(true)
        setupSuccessfulVerification(isVerified = false)
        setupSuccessfulDeEnrolment()
        setupSuccessfulEnrolment()
        val result = controller.saveAndContinue().apply(testRequest(testUtr))

        status(result) mustBe 303
        redirectLocation(result) mustBe Some(controllers.reenrolment.routes.KickoutController.showURNKickOutPage.url)
      }

      "redirect to successful enrolment page when both de-enrolment and enrolment succeed" in {
        setAuthMocks()
        setupTestData(hasGroupId = true)
        when(mockAccountUtils.isSaAccount(any())).thenReturn(true)
        setupSuccessfulVerification()
        setupSuccessfulDeEnrolment()
        setupSuccessfulEnrolment()

        val result = controller.saveAndContinue().apply(testRequest(testUtr))

        status(result) mustBe 303
        redirectLocation(result) mustBe Some(controllers.reenrolment.routes.SuccessfulEnrolmentController.showSuccessfulEnrolmentPage.url)
      }

      "redirect to kickout page when no existing groupId found" in {
        setAuthMocks()
        setupTestData(hasGroupId = false)
        when(mockAccountUtils.isSaAccount(any())).thenReturn(true)
        setupSuccessfulVerification()
        setupSuccessfulEnrolment()

        val result = controller.saveAndContinue().apply(testRequest(testUtr))

        status(result) mustBe 303
        redirectLocation(result) mustBe Some(controllers.reenrolment.routes.KickoutController.showURNKickOutPage.url)
        verifyNoInteractions(mockDeEnrolService)
      }

      "handle Corporation Tax UTR when user is not SA" in {
        setAuthMocks()
        setupTestData()
        when(mockAccountUtils.isSaAccount(any())).thenReturn(false)
        setupSuccessfulVerification()
        setupSuccessfulDeEnrolment()

        when(mockEnrolService.enrolAWRS(
          eqTo(testAwrsRef),
          eqTo(testPostcode),
          eqTo(Some(testUtr)),
          eqTo("CT"),  // Should use CT for non-SA users
          eqTo(Map.empty)
        )(any(), any()))
          .thenReturn(Future.successful(EnrolResponse("serviceName", "state", List(Identifier("AWRS", "AWRS_Ref_No")))))

        val result = controller.saveAndContinue().apply(testRequest(testUtr))

        status(result) mustBe 303
        redirectLocation(result) mustBe Some(controllers.reenrolment.routes.SuccessfulEnrolmentController.showSuccessfulEnrolmentPage.url)
      }


      "redirect to kickout page when verifyKnownFacts fails with an exception" in {
        setAuthMocks()
        setupTestData()
        when(mockAccountUtils.isSaAccount(any())).thenReturn(true)
        when(mockEnrolmentStoreService.verifyKnownFacts(any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.failed(new RuntimeException("Service unavailable")))

        val result = controller.saveAndContinue().apply(testRequest(testUtr))

        status(result) mustBe 303
        redirectLocation(result) mustBe Some(controllers.reenrolment.routes.KickoutController.showURNKickOutPage.url)
      }

      "redirect to kickout page when deEnrolAwrs fails with an exception" in {
        setAuthMocks()
        setupTestData()
        when(mockAccountUtils.isSaAccount(any())).thenReturn(true)
        setupSuccessfulVerification()
        when(mockDeEnrolService.deEnrolAwrs(any(), any())(any(), any()))
          .thenReturn(Future.failed(new RuntimeException("De-enrolment failed")))

        val result = controller.saveAndContinue().apply(testRequest(testUtr))

        status(result) mustBe 303
        redirectLocation(result) mustBe Some(controllers.reenrolment.routes.KickoutController.showURNKickOutPage.url)
      }
    }
  }
}
