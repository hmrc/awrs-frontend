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

package services.reenrolment

import controllers.reenrolment.routes
import models.AwrsEnrolmentUrn
import models.reenrolment._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.test.Helpers._
import services.ServicesUnitTestFixture
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.auth.core.{AffinityGroup, CredentialRole, Enrolments}
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtil.testAwrsRef

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class RegisteredUrnServiceTest extends ServicesUnitTestFixture {

  private val svc = new RegisteredUrnService(
    keyStoreService = testKeyStoreService,
    authConnector = mockAuthConnector,
    enrolmentStoreService = testEnrolmentStoreProxyService,
    applicationConfig = mockAppConfig
  )

  private val testFacts =
    KnownFactsResponse(
      service = "HMRC-AWRS-ORG",
      enrolments = Seq(
        Enrolment(
          identifiers = Seq(Identifier("AWRSRefNumber", testAwrsRef)),
          verifiers = Seq(Verifier("SAUTR", "1234567890"), Verifier("SAUTR", "1234567890"))
        )
      )
    )

  "RegisteredUrnService handleEnrolmentConfirmationFlow" must {

    "redirect to de‑enrolment confirmation when user is assigned to AWRS enrolment" ignore {
      setAuthMocks()

      when(
        mockAuthConnector.authorise[Enrolments ~ Option[AffinityGroup] ~ Option[Credentials] ~ Option[CredentialRole]](
          ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(authResultDefault()))

      setupMockKeystoreServiceForAwrsUrn(AwrsEnrolmentUrn(testAwrsRef))

      val result = svc.handleEnrolmentConfirmationFlow(AwrsEnrolmentUrn(testAwrsRef))

      status(result) mustBe SEE_OTHER
    }

    "redirect to kickout when user is not assigned to AWRS enrolment" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUrn()

      when(
        mockEnrolmentStoreProxyConnector
          .lookupEnrolments(ArgumentMatchers.any)(any[HeaderCarrier](), any[ExecutionContext]()))
        .thenReturn(Future.successful(testFacts))

      when(
        mockEnrolmentStoreProxyConnector
          .queryForAssignedPrincipalUsersOfAWRSEnrolment(ArgumentMatchers.eq(testAwrsRef))(any[HeaderCarrier](), any[ExecutionContext]()))
        .thenReturn(Future.successful(Some(EnrolledUserIds(principalUserIds = Seq.empty))))

      val result = svc.handleEnrolmentConfirmationFlow(AwrsEnrolmentUrn(testAwrsRef))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.KickoutController.showURNKickOutPage.url)
    }

    "redirect to kickout when no known facts are returned" in {
      when(
        mockEnrolmentStoreProxyConnector
          .lookupEnrolments(ArgumentMatchers.any)(any[HeaderCarrier](), any[ExecutionContext]()))
        .thenReturn(Future.successful(None))

      when(
        mockEnrolmentStoreProxyConnector
          .queryForAssignedPrincipalUsersOfAWRSEnrolment(ArgumentMatchers.eq(testAwrsRef))(any[HeaderCarrier](), any[ExecutionContext]()))
        .thenReturn(Future.successful(Some(EnrolledUserIds(principalUserIds = Seq.empty))))

      setAuthMocks()
      setupMockKeystoreServiceForAwrsUrn()
      val result = svc.handleEnrolmentConfirmationFlow(AwrsEnrolmentUrn(testAwrsRef))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.KickoutController.showURNKickOutPage.url)
    }

  }

}
