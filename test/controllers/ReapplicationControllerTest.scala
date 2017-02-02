/*
 * Copyright 2017 HM Revenue & Customs
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

import java.util.UUID

import audit.TestAudit
import builders.{AuthBuilder, SessionBuilder}
import connectors.mock.MockAuthConnector
import connectors.{AuthenticatorConnector, TaxEnrolmentsConnector}
import controllers.auth.Utr._
import forms.{AWRSEnums, ReapplicationForm}
import models.ReapplicationConfirmation
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{DeEnrolService, KeyStoreService}
import services.mocks.{MockKeyStoreService, MockSave4LaterService}
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HttpResponse
import utils.{AwrsUnitTestTraits, TestUtil}

import scala.concurrent.Future

class ReapplicationControllerTest extends AwrsUnitTestTraits
  with MockAuthConnector with MockKeyStoreService with MockSave4LaterService {

  val mockAudit: Audit = mock[Audit]
  val mockAuthenticatorConnector: AuthenticatorConnector = mock[AuthenticatorConnector]
  val mockTaxEnrolmentsConnector: TaxEnrolmentsConnector = mock[TaxEnrolmentsConnector]
  val mockDeEnrolService: DeEnrolService = mock[DeEnrolService]
  val mockKeyStoreService: KeyStoreService = mock[KeyStoreService]

  object TestReapplicationController extends ReapplicationController {
    override val authConnector: AuthConnector = mockAuthConnector
    override val audit: Audit = new TestAudit
    override val keyStoreService: KeyStoreService = mockKeyStoreService
    override val deEnrolService = mockDeEnrolService
    override val save4LaterService = TestSave4LaterService
  }

  def testRequest(reapplication: ReapplicationConfirmation) =
    TestUtil.populateFakeRequest[ReapplicationConfirmation](FakeRequest(), ReapplicationForm.reapplicationForm, reapplication)

  "Reapplication Controller" should {
    "submit confirmation and redirect to root home page" in {
      submitAuthorisedUser(testRequest(ReapplicationConfirmation(AWRSEnums.BooleanRadioEnum.YesString))) {
        result =>
          status(result) shouldBe 303
          redirectLocation(result).get should be("/alcohol-wholesale-scheme")
      }
    }
  }

  def submitAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setUser(hasAwrs = true)
    setupMockSave4LaterService()
    setupMockApiSave4LaterService()
    when(mockDeEnrolService.refreshProfile(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
    when(mockDeEnrolService.deEnrolAWRS(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(true))
    when(mockKeyStoreService.removeAll(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
    val result = TestReapplicationController.submit.apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, "LTD"))
    test(result)
  }

}
