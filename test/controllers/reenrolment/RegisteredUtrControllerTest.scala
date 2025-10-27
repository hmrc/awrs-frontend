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
import models.reenrolment.{KnownFactsResponse, Verifier}
import models.{AwrsEnrolmentUrn, AwrsEnrolmentUtr, AwrsPostcodeModel, EnrolResponse, Identifier}
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
    mockAppConfig,
    template
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    resetMocks()
  }

  private def resetMocks(): Unit = {
    val mocks: Seq[AnyRef] = Seq(
      mockDeEnrolService,
      mockAuthConnector,
      mockAuditable,
      mockAccountUtils,
      mockEnrolService,
      mockEnrolmentStoreService,
      mockAppConfig
    )
    reset(mocks: _*)
  }

  private def testRequest(answer: String): FakeRequest[AnyContentAsFormUrlEncoded] =
    TestUtil.populateFakeRequest[AwrsEnrolmentUtr](
      FakeRequest(),
      RegisteredUtrForm.awrsEnrolmentUtrForm.form,
      AwrsEnrolmentUtr(answer)
    )


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

  private def setupTestData(knownFactsResponse: Option[KnownFactsResponse] = None): Unit = {
    setupMockKeystoreServiceForAwrsUtr(
      urn = Some(AwrsEnrolmentUrn(testAwrsRef)),
      utr = Some(AwrsEnrolmentUtr(testUtr)),
      registeredPostcode = Some(AwrsPostcodeModel(testPostcode)),
      knownFactsResponse = knownFactsResponse
    )
  }

  "RegisteredUtrController" when {

    "showArwsUtrPage is called" must {
      "return 200" in {
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
        setupTestData(knownFactsResponse = Some(
          KnownFactsResponse("HMRC-AWRS-ORG",
            Seq(models.reenrolment.Enrolment(
              identifiers = Seq(models.reenrolment.Identifier("AWRSRefNumber", testAwrsRef)),
              verifiers = Seq(Verifier("SAUTR", "1234567890"))
            ))
          )))
        setupSuccessfulDeEnrolment()
        when(mockAccountUtils.isSaAccount(any())).thenReturn(true)
        when(mockEnrolmentStoreProxyConnector.queryForPrincipalGroupIdOfAWRSEnrolment(any())(any(), any()))
          .thenReturn(Future.successful(Some(testGroupId)))
        when(mockEnrolService.enrolAWRS(any(), any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.failed(new BadRequestException("")))

        val result = controller.saveAndContinue().apply(testRequest(testUtr))

        status(result) mustBe 303
        redirectLocation(result) mustBe Some(controllers.reenrolment.routes.KickoutController.showKickOutPage.url)
      }

      "redirect to kickout page when de-enrolment fails" in {
        setAuthMocks()
        setupTestData(knownFactsResponse = Some(
          KnownFactsResponse("HMRC-AWRS-ORG",
            Seq(models.reenrolment.Enrolment(
              identifiers = Seq(models.reenrolment.Identifier("AWRSRefNumber", testAwrsRef)),
              verifiers = Seq(Verifier("SAUTR", testUtr))
            ))
          )))
        when(mockAccountUtils.isSaAccount(any())).thenReturn(true)
        when(mockEnrolmentStoreService.queryForPrincipalGroupIdOfAWRSEnrolment(any())(any(), any()))
          .thenReturn(Future.successful(Some(testGroupId)))
        setupSuccessfulDeEnrolment(success = false)

        val result = controller.saveAndContinue().apply(testRequest(testUtr))

        status(result) mustBe 303
        redirectLocation(result) mustBe Some(controllers.reenrolment.routes.KickoutController.showKickOutPage.url)
      }

      "redirect to kickout page when known facts are not verified" in {
        setAuthMocks()
        setupTestData(knownFactsResponse = Some(
          KnownFactsResponse("HMRC-AWRS-ORG",
            Seq(models.reenrolment.Enrolment(
              identifiers = Seq(models.reenrolment.Identifier("AWRSRefNumber", testAwrsRef)),
              verifiers = Seq(Verifier("SAUTR", "1234567890"))
            ))
          )))
        when(mockEnrolmentStoreService.queryForPrincipalGroupIdOfAWRSEnrolment(any())(any(), any()))
          .thenReturn(Future.successful(Some(testGroupId)))
        when(mockAccountUtils.isSaAccount(any())).thenReturn(true)
        setupSuccessfulDeEnrolment()
        setupSuccessfulEnrolment()
        val result = controller.saveAndContinue().apply(testRequest(testUtr))

        status(result) mustBe 303
        redirectLocation(result) mustBe Some(controllers.reenrolment.routes.KickoutController.showKickOutPage.url)
      }

      "redirect to successful enrolment page when both de-enrolment and enrolment succeed" in {
        setAuthMocks()
        setupTestData(knownFactsResponse = Some(
          KnownFactsResponse("HMRC-AWRS-ORG",
            Seq(models.reenrolment.Enrolment(
              identifiers = Seq(models.reenrolment.Identifier("AWRSRefNumber", testAwrsRef)),
              verifiers = Seq(Verifier("SAUTR", testUtr), Verifier("Postcode", testPostcode))
            ))
          )))
        when(mockEnrolmentStoreService.queryForPrincipalGroupIdOfAWRSEnrolment(any())(any(), any()))
          .thenReturn(Future.successful(Some(testGroupId)))
        when(mockAccountUtils.isSaAccount(any())).thenReturn(true)
        setupSuccessfulDeEnrolment()
        setupSuccessfulEnrolment()

        val result = controller.saveAndContinue().apply(testRequest(testUtr))

        status(result) mustBe 303
        redirectLocation(result) mustBe Some(controllers.reenrolment.routes.SuccessfulEnrolmentController.showSuccessfulEnrolmentPage.url)
      }

      "do successful enrolment without de-enrolment when group id is not found" in {
        setAuthMocks()
        setupTestData(knownFactsResponse = Some(
          KnownFactsResponse("HMRC-AWRS-ORG",
            Seq(models.reenrolment.Enrolment(
              identifiers = Seq(models.reenrolment.Identifier("AWRSRefNumber", testAwrsRef)),
              verifiers = Seq(Verifier("SAUTR", testUtr), Verifier("Postcode", testPostcode))
            ))
          )))
        when(mockEnrolmentStoreService.queryForPrincipalGroupIdOfAWRSEnrolment(any())(any(), any()))
          .thenReturn(Future.successful(None))
        when(mockAccountUtils.isSaAccount(any())).thenReturn(true)
        setupSuccessfulEnrolment()

        val result = controller.saveAndContinue().apply(testRequest(testUtr))

        status(result) mustBe 303
        redirectLocation(result) mustBe Some(controllers.reenrolment.routes.SuccessfulEnrolmentController.showSuccessfulEnrolmentPage.url)
        verifyNoInteractions(mockDeEnrolService)
        verifyKeyStoreService(saveAwrsUtr = 1)
      }

      "handle Corporation Tax UTR when user is not SA" in {
        setAuthMocks()
        setupTestData(knownFactsResponse = Some(
          KnownFactsResponse("HMRC-AWRS-ORG",
            Seq(models.reenrolment.Enrolment(
              identifiers = Seq(models.reenrolment.Identifier("AWRSRefNumber", testAwrsRef)),
              verifiers = Seq(Verifier("CTUTR", testUtr), Verifier("Postcode", testPostcode))
            ))
          )))

        when(mockAccountUtils.isSaAccount(any())).thenReturn(false)
        when(mockEnrolmentStoreService.queryForPrincipalGroupIdOfAWRSEnrolment(any())(any(), any()))
          .thenReturn(Future.successful(Some(testGroupId)))
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
        verifyKeyStoreService(saveAwrsUtr = 1)
      }

      "redirect to kickout page when ES9/ enrolment fails with an exception" in {
        setAuthMocks()
        setupTestData()
        when(mockAccountUtils.isSaAccount(any())).thenReturn(false)
        when(mockEnrolmentStoreService.queryForPrincipalGroupIdOfAWRSEnrolment(any())(any(), any()))
          .thenReturn(Future.successful(Some(testGroupId)))
        setupSuccessfulDeEnrolment()

        when(mockEnrolService.enrolAWRS(
          eqTo(testAwrsRef),
          eqTo(testPostcode),
          eqTo(Some(testUtr)),
          eqTo("CT"),  // Should use CT for non-SA users
          eqTo(Map.empty)
        )(any(), any()))
          .thenReturn(Future.failed(new RuntimeException("Service unavailable")))


        val result = controller.saveAndContinue().apply(testRequest(testUtr))

        status(result) mustBe 303
        redirectLocation(result) mustBe Some(controllers.reenrolment.routes.KickoutController.showKickOutPage.url)
      }

      "redirect to kickout page when ES1 (group id query) fails with an exception" in {
        setAuthMocks()
        setupTestData()
        when(mockAccountUtils.isSaAccount(any())).thenReturn(true)
        when(mockEnrolmentStoreService.queryForPrincipalGroupIdOfAWRSEnrolment(any())(any(), any()))
          .thenReturn(Future.failed(new RuntimeException("Service unavailable")))

        val result = controller.saveAndContinue().apply(testRequest(testUtr))

        status(result) mustBe 303
        redirectLocation(result) mustBe Some(controllers.reenrolment.routes.KickoutController.showKickOutPage.url)
      }



    }
  }
}
