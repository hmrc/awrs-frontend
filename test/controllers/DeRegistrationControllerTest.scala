/*
 * Copyright 2018 HM Revenue & Customs
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

import connectors.mock.MockAuthConnector
import connectors.{AuthenticatorConnector, TaxEnrolmentsConnector}
import forms.AWRSEnums.BooleanRadioEnum
import models.FormBundleStatus.{Rejected, RejectedUnderReviewOrAppeal, Revoked, RevokedUnderReviewOrAppeal}
import models._
import org.joda.time.LocalDate
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{DeEnrolService, EmailService}
import services.apis.AwrsAPI10
import services.mocks.{MockKeyStoreService, MockSave4LaterService}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import utils.AwrsSessionKeys
import utils.TestConstants._
import utils.TestUtil.cachedData

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HttpResponse, SessionKeys }

class DeRegistrationControllerTest extends MockKeyStoreService with MockSave4LaterService with MockAuthConnector {

  import MockKeyStoreService._

  val mockAuthenticatorConnector = mock[AuthenticatorConnector]
  val mockApi10 = mock[AwrsAPI10]
  val mockTaxEnrolmentsConnector = mock[TaxEnrolmentsConnector]
  val mockEmailService = mock[EmailService]

  object mockDeEnrolService extends DeEnrolService {
    override val taxEnrolmentsConnector = mockTaxEnrolmentsConnector
    override val authenticatorConnector = mockAuthenticatorConnector
  }

  object TestDeRegistrationController extends DeRegistrationController {
    override val api10 = mockApi10
    override val authConnector = mockAuthConnector
    override val keyStoreService = TestKeyStoreService
    override val deEnrolService = mockDeEnrolService
    override val save4LaterService = TestSave4LaterService
    override val emailService = mockEmailService
  }

  val permittedStatusTypes: Set[FormBundleStatus] = TestDeRegistrationController.permittedStatusTypes
  val forbiddenStatusTypes = FormBundleStatus.allStatus.diff(permittedStatusTypes)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthConnector)
    reset(mockApi10)
    reset(mockTaxEnrolmentsConnector)
  }

  def mockAPI10AndDeEnroll(deRegSuccess: Boolean = true): Unit = {
    val deRegSuccessData = deRegSuccess match {
      case true => deRegistrationSuccessData
      case false => deRegistrationFailureData
    }
    when(mockApi10.deRegistration()(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(deRegSuccessData))
    when(mockTaxEnrolmentsConnector.deEnrol(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(defaultDeEnrollResponseSuccessData))
    when(mockDeEnrolService.refreshProfile(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
  }

  def mocks(haveDeRegDate: Boolean = true,
            haveDeRegReason: Boolean = true,
            deRegSuccess: Boolean = true): Unit = {
    setupMockKeyStoreServiceForDeRegistrationOrWithdrawal(haveDeRegDate = haveDeRegDate, haveDeRegReason = haveDeRegReason)
    setupMockSave4LaterServiceWithOnly(removeAll = MockSave4LaterService.defaultRemoveAll)
    setupMockApiSave4LaterServiceWithOnly(removeAll = MockSave4LaterService.defaultRemoveAll)
    mockAPI10AndDeEnroll(deRegSuccess = deRegSuccess)
  }

  def verifyExternCalls(deRegistration: Option[Int] = None,
                        deEnrol: Option[Int] = None) = {
    deRegistration ifDefinedThen (count => verify(mockApi10, times(count)).deRegistration()(Matchers.any(), Matchers.any(), Matchers.any()))
    deEnrol ifDefinedThen (count => verify(mockTaxEnrolmentsConnector, times(count)).deEnrol(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any()))
  }

  val reasonURL = "/alcohol-wholesale-scheme/de-register-reason"
  val dateURL = "/alcohol-wholesale-scheme/de-register-date"
  val confirmURL = "/alcohol-wholesale-scheme/de-register-confirm"
  val confirmationURL = "/alcohol-wholesale-scheme/de-register-confirmation"
  val indexURL = "/alcohol-wholesale-scheme/index"

  "DeRegistration" should {
    for (pStatusType <- permittedStatusTypes) {
      f"allow usage if type is $pStatusType" in {
        mocks()

        testShowDate(pStatusType) {
          result =>
            status(result) shouldBe OK
        }

        testShowReason(pStatusType) {
          result =>
            status(result) shouldBe OK
        }

        testShowConfirm(pStatusType) {
          result =>
            status(result) shouldBe OK
        }

        testShowConfirmation(pStatusType) {
          result =>
            status(result) shouldBe OK
        }
      }
    }

    for (fStatusType <- forbiddenStatusTypes) {
      f"disallow usage if type is $fStatusType" in {
        mocks()

        val verifyForbidden =
          (result: Future[Result]) =>
            fStatusType match {
              // rejected is currently auto redirected
              case Rejected | RejectedUnderReviewOrAppeal | Revoked | RevokedUnderReviewOrAppeal =>
                status(result) shouldBe SEE_OTHER
              case _ => status(result) shouldBe NOT_FOUND
            }

        testShowDate(fStatusType) {
          result =>
            verifyForbidden(result)
        }

        testShowReason(fStatusType) {
          result =>
            verifyForbidden(result)
        }

        testShowConfirm(fStatusType) {
          result =>
            verifyForbidden(result)
        }

        testShowConfirmation(fStatusType) {
          result =>
            verifyForbidden(result)
        }
      }
    }

    for (pStatusType <- permittedStatusTypes) {
      f"redirect $pStatusType users to 'de-register-awrs-reason' from date, confirm and confirmation pages when reason is not answered" in {
        mocks(haveDeRegReason = false)

        testShowDate(pStatusType) {
          result =>
            status(result) shouldBe SEE_OTHER
            redirectLocation(result).get should endWith(reasonURL)
        }

        testShowConfirm(pStatusType) {
          result =>
            status(result) shouldBe SEE_OTHER
            redirectLocation(result).get should endWith(reasonURL)
        }

        testShowConfirmation(pStatusType) {
          result =>
            status(result) shouldBe SEE_OTHER
            redirectLocation(result).get should endWith(reasonURL)
        }
      }
    }

    for (pStatusType <- permittedStatusTypes) {
      f"redirect $pStatusType users to 'de-register-awrs-date' from confirm and confirmation pages when date is not answered" in {
        mocks(haveDeRegDate = false)

        testShowConfirm(pStatusType) {
          result =>
            status(result) shouldBe SEE_OTHER
            redirectLocation(result).get should endWith(dateURL)
        }

        testShowConfirmation(pStatusType) {
          result =>
            status(result) shouldBe SEE_OTHER
            redirectLocation(result).get should endWith(dateURL)
        }
      }
    }

    for (pStatusType <- permittedStatusTypes) {
      f"redirect $pStatusType users to confirm from 'de-register-awrs-date' " in {
        mocks()

        testSubmitDate(pStatusType) {
          result =>
            status(result) shouldBe SEE_OTHER
            redirectLocation(result).get should endWith(confirmURL)
        }
      }
    }

    for (pStatusType <- permittedStatusTypes) {
      f"redirect $pStatusType users to confirm from 'de-register-awrs-reason' " in {
        mocks()

        testSubmitReason(pStatusType) {
          result =>
            status(result) shouldBe SEE_OTHER
            redirectLocation(result).get should endWith(dateURL)
        }
      }
    }

    for (pStatusType <- permittedStatusTypes) {
      f"redirect $pStatusType users to confirmation from 'de-register-awrs-confirm' " in {
        mocks()
        when(mockEmailService.sendCancellationEmail(Matchers.any(), Matchers.any())(Matchers.any(),Matchers.any(),Matchers.any())).thenReturn(Future.successful(true))
        setupMockSave4LaterService(fetchAll = cachedData())

        testSubmitConfirm(pStatusType) {
          result =>
            status(result) shouldBe SEE_OTHER
            redirectLocation(result).get should endWith(confirmationURL)
        }
      }
    }

    for (pStatusType <- permittedStatusTypes) {
      f"redirect $pStatusType users to index from 'de-register-awrs-confirm' if they answer no" in {
        mocks()

        testSubmitConfirm(pStatusType, BooleanRadioEnum.No) {
          result =>
            status(result) shouldBe SEE_OTHER
            redirectLocation(result).get should endWith(indexURL)
            verifyKeyStoreService(
              deleteDeRegistrationDate = 1,
              deleteDeRegistrationReason = 1)
        }
      }
    }

    for (pStatusType <- permittedStatusTypes) {
      f"fail submission for $pStatusType users when forms are empty' " in {
        mocks()

        testSubmitDateEmpty(pStatusType) {
          result =>
            status(result) shouldBe BAD_REQUEST
        }
        testSubmitDateOldDate(pStatusType) {
          result =>
            status(result) shouldBe BAD_REQUEST
        }
        testSubmitReasonEmpty(pStatusType) {
          result =>
            status(result) shouldBe BAD_REQUEST
        }
        testSubmitConfirmEmpty(pStatusType) {
          result =>
            status(result) shouldBe BAD_REQUEST

        }
      }
    }

    for (pStatusType <- permittedStatusTypes) {
      f"$pStatusType users who have submitted a dereg date show confirmation page" in {
        mocks()

        testShowConfirm(pStatusType) {
          result =>
            status(result) shouldBe OK
        }
      }
    }

    for (pStatusType <- permittedStatusTypes) {
      s"$pStatusType users who experienced API10 failures should not have save 4 later cleared up" in {
        mocks(deRegSuccess = false)

        testSubmitConfirm(pStatusType) {
          result =>
            status(result) shouldBe INTERNAL_SERVER_ERROR
            verifySave4LaterService(removeAll = 0)
            verifyApiSave4LaterService(removeAll = 0)
            verifyExternCalls(
              deRegistration = 1,
              deEnrol = 0
            )
        }
      }
    }

    for (pStatusType <- permittedStatusTypes) {
      s"$pStatusType users who went through successesful API 10 and de-enrol should have their save 4 later cleared up" in {
        mocks(deRegSuccess = true)
        when(mockEmailService.sendCancellationEmail(Matchers.any(), Matchers.any())(Matchers.any(),Matchers.any(),Matchers.any())).thenReturn(Future.successful(true))
        setupMockSave4LaterService(fetchAll = cachedData())

        testSubmitConfirm(pStatusType) {
          result =>
            status(result) shouldBe SEE_OTHER
            verifySave4LaterService(removeAll = 1)
            verifyApiSave4LaterService(removeAll = 1)
            verifyExternCalls(
              deRegistration = 1,
              deEnrol = 1
            )
        }
      }
    }

  }

  def testShowReason(status: FormBundleStatus)(test: Future[Result] => Any) = getWithAuthorisedAgentUser(status, TestDeRegistrationController.showReason())(test)

  def testSubmitReason(status: FormBundleStatus)(test: Future[Result] => Any) = submits(status, TestDeRegistrationController.submitReason, ("deRegistrationReason", "Ceases to be registerable for the scheme"))(test)

  def testSubmitReasonEmpty(status: FormBundleStatus)(test: Future[Result] => Any) = submits(status, TestDeRegistrationController.submitReason, ("deRegistrationReason", ""))(test)

  def testShowDate(status: FormBundleStatus)(test: Future[Result] => Any) = getWithAuthorisedAgentUser(status, TestDeRegistrationController.showDate())(test)

  def testSubmitDate(status: FormBundleStatus)(test: Future[Result] => Any) = submits(status, TestDeRegistrationController.submitDate, ("proposedEndDate.day", LocalDate.now().getDayOfMonth.toString), ("proposedEndDate.month", LocalDate.now().getMonthOfYear.toString), ("proposedEndDate.year", LocalDate.now().getYear.toString))(test)

  def testSubmitDateOldDate(status: FormBundleStatus)(test: Future[Result] => Any) = submits(status, TestDeRegistrationController.submitDate, ("proposedEndDate.day", "1"), ("proposedEndDate.month", "1"), ("proposedEndDate.year", "2016"))(test)

  def testSubmitDateEmpty(status: FormBundleStatus)(test: Future[Result] => Any) = submits(status, TestDeRegistrationController.submitDate, ("proposedEndDate.day", ""), ("proposedEndDate.month", ""), ("proposedEndDate.year", ""))(test)

  def testShowConfirm(status: FormBundleStatus)(test: Future[Result] => Any) = getWithAuthorisedAgentUser(status, TestDeRegistrationController.showConfirm())(test)

  def testSubmitConfirm(status: FormBundleStatus, answer: BooleanRadioEnum.Value = BooleanRadioEnum.Yes)(test: Future[Result] => Any) = submits(status, TestDeRegistrationController.callToAction, ("deRegistrationConfirmation", answer.toString))(test)

  def testSubmitConfirmEmpty(status: FormBundleStatus)(test: Future[Result] => Any) = submits(status, TestDeRegistrationController.callToAction, ("deRegistrationConfirmation", ""))(test)

  def testShowConfirmation(status: FormBundleStatus)(test: Future[Result] => Any) = getWithAuthorisedAgentUser(status, TestDeRegistrationController.showConfirmation(false))(test)

  def getWithAuthorisedAgentUser(status: FormBundleStatus, call: Action[AnyContent])(test: Future[Result] => Any) {
    setUser(hasAwrs = true)
    val result = call.apply(buildRequestWithSession(userId, status.name))
    test(result)
  }

  def submits(status: FormBundleStatus, call: Action[AnyContent], data: (String, String)*)(test: Future[Result] => Any) {
    setUser(hasAwrs = true)
    val result = call.apply(buildRequestWithSession(userId, status.name).withFormUrlEncodedBody(data: _*))
    test(result)
  }

  def buildRequestWithSession(userId: String, status: String) = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId,
      "businessType" -> "SOP",
      AwrsSessionKeys.sessionStatusType -> status,
      "businessName" -> "North East Wines"
    )
  }
}
