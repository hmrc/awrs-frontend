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

import builders.SessionBuilder
import config.FrontendAuthConnector
import connectors.mock.MockAuthConnector
import forms.AWRSEnums.WithdrawalReasonEnum
import forms.{WithdrawalConfirmationForm, WithdrawalReasonForm}
import models._
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.DataCacheKeys._
import services.{DeEnrolService, EmailService}
import services.apis.AwrsAPI8
import services.apis.mocks.MockAwrsAPI9
import services.mocks.{MockKeyStoreService, MockSave4LaterService}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.TestConstants.testUtr
import utils.TestUtil._
import utils.WithdrawalTestUtils._
import utils.{AwrsUnitTestTraits, TestUtil}

import scala.concurrent.Future
import uk.gov.hmrc.http.HttpResponse

class WithdrawalControllerTest extends AwrsUnitTestTraits
  with MockAuthConnector
  with MockAwrsAPI9
  with MockKeyStoreService
  with MockSave4LaterService {

  val request = FakeRequest()
  val mockAwrsAPI8 = mock[AwrsAPI8]
  val mockDeEnrolService = mock[DeEnrolService]
  val mockEmailService = mock[EmailService]

  object TestWithdrawalController extends WithdrawalController {
    override val authConnector = mockAuthConnector
    override val keyStoreService = TestKeyStoreService
    override val deEnrolService = mockDeEnrolService
    override val api9 = TestAPI9
    override val api8: AwrsAPI8 = mockAwrsAPI8
    override val save4LaterService = TestSave4LaterService
    override val emailService = mockEmailService
  }

  private def testConfirmationRequest(confirmation: WithdrawalConfirmation) =
    TestUtil.populateFakeRequest[WithdrawalConfirmation](FakeRequest(), WithdrawalConfirmationForm.withdrawalConfirmation, confirmation)

  private def testReasonRequest(reason: WithdrawalReason) =
    TestUtil.populateFakeRequest[WithdrawalReason](FakeRequest(), WithdrawalReasonForm.withdrawalReasonForm.form, reason)

  "Withdrawal Controller" must {
    "Withdrawal Confirmation" should {

      "display a validation error on submit if radio button is not selected" in {
        continueUpdateWithAuthorisedUser(testConfirmationRequest(WithdrawalConfirmation(None))) {
          result =>
            status(result) should be(BAD_REQUEST)
        }
      }

    }

    "Withdrawal Reasons" should {

      "display the withdrawal reasons page if user has status of Pending with reasons in cache" in {
        getWithAuthorisedUserCtWithStatusPendingReasons {
          result => status(result) shouldBe OK
        }
      }

      "display error page (Reasons) if user has status of anything else than pending (Approved)" in {
        getWithAuthorisedUserCtWithStatusApprovedReasons {
          result =>
            status(result) shouldBe INTERNAL_SERVER_ERROR

            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("heading-xlarge").text() should be(Messages("awrs.generic.error.title"))
        }
      }

      "display the withdrawal reasons page if user has status of Pending" in {
        getWithAuthorisedUserStatusPendingReasonsNoKeyStore {
          result => status(result) shouldBe OK
        }
      }

      "display a blank withdrawal reasons page if user has status of Pending and nothing is in the keystore" in {
        continueWithAuthorisedUserReasonsNoKeyStore(testSubscriptionStatusTypePending) {
          result => status(result) shouldBe OK
        }
      }

    }

    "Submit Withdrawal Reasons" should {
      "redirect to confirmation page" in {
        continueWithSubmitWithdrawalReasons(
          testReasonRequest(WithdrawalReason(reason = WithdrawalReasonEnum.AppliedInError.toString, reasonOther = None))
        ) {
          result =>
            status(result) shouldBe SEE_OTHER
        }
      }

      "show a form error (Bad Request) if nothing is selected" in {
        continueWithSubmitWithdrawalReasons(
          testReasonRequest(WithdrawalReason(reason = "", reasonOther = None))
        ) {
          result =>
            status(result) shouldBe BAD_REQUEST
        }
      }
    }

    "Submit withdrawal application" should {
      "display success message when deEnrol and withdrawal is successful" in {
        continueWithApplicationSubmit(
          testConfirmationRequest(WithdrawalConfirmation("Yes")), deEnrol = true) {
          result => status(result) shouldBe SEE_OTHER
        }
      }

      "redirect to index message when selects No" in {
        continueWithApplicationSubmit(
          testConfirmationRequest(WithdrawalConfirmation("No")), deEnrol = false) {
          result => status(result) shouldBe SEE_OTHER
        }
      }

      "display error when enrol is not successful" in {
        continueWithApplicationSubmit(
          testConfirmationRequest(WithdrawalConfirmation("Yes")), deEnrol = false) {
          result =>
            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("heading-xlarge").text() should be(Messages("awrs.generic.error.title"))
            status(result) shouldBe INTERNAL_SERVER_ERROR
        }
      }
    }

  }

  private def continueUpdateWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setUser(hasAwrs = true)
    setupMockKeyStoreServiceForDeRegistrationOrWithdrawal(haveWithdrawalReason = true)
    val result = TestWithdrawalController.submitConfirmWithdrawal().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

  private def continueWithAuthorisedUserReasons(userStatus: SubscriptionStatusType)(test: Future[Result] => Any) {
    setupMockAwrsAPI9(keyStore = userStatus)
    setupMockKeyStoreServiceForDeRegistrationOrWithdrawal(haveWithdrawalReason = true)
    val result = TestWithdrawalController.showWithdrawalReasons.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def continueWithSubmitWithdrawalReasons(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockKeyStoreServiceOnlySaveFunctions()
    val result = TestWithdrawalController.submitWithdrawalReasons.apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

  private def continueWithAuthorisedUserReasonsNoKeyStore(userStatus: SubscriptionStatusType)(test: Future[Result] => Any) {
    setupMockAwrsAPI9(keyStore = userStatus)
    setupMockKeyStoreServiceForDeRegistrationOrWithdrawal(haveWithdrawalReason = false)
    val result = TestWithdrawalController.showWithdrawalReasons.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def continueWithApplicationSubmit(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded], deEnrol: Boolean)(test: Future[Result] => Any) {
    setupMockKeyStoreServiceForDeRegistrationOrWithdrawal(
      haveWithdrawalReason = false,
      fetchNoneType = FetchNoneType.DeletedData
    )
    when(mockAwrsAPI8.withdrawApplication(Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(api8Repsonse))
    when(mockDeEnrolService.deEnrolAWRS(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(deEnrol))
    when(mockDeEnrolService.refreshProfile(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
    setupMockSave4LaterServiceWithOnly(removeAll = MockSave4LaterService.defaultRemoveAll)
    when(mockEmailService.sendWithdrawnEmail(Matchers.any())(Matchers.any(),Matchers.any(),Matchers.any())).thenReturn(Future.successful(true))
    setupMockSave4LaterService(fetchAll = cachedData())
    val result = TestWithdrawalController.submitConfirmWithdrawal.apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

  private def getWithAuthorisedUserCtWithStatusPendingReasons = continueWithAuthorisedUserReasons(testSubscriptionStatusTypePending)(_)

  private def getWithAuthorisedUserCtWithStatusApprovedReasons = continueWithAuthorisedUserReasons(testSubscriptionStatusTypeApproved)(_)

  private def getWithAuthorisedUserStatusPendingReasonsNoKeyStore = continueWithAuthorisedUserReasons(testSubscriptionStatusTypePending)(_)

}
