/*
 * Copyright 2020 HM Revenue & Customs
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

import config.ApplicationConfig
import connectors.TaxEnrolmentsConnector
import connectors.mock.MockAuthConnector
import forms.AWRSEnums.BooleanRadioEnum
import models.FormBundleStatus.{DeRegistered, Rejected, RejectedUnderReviewOrAppeal, Revoked, RevokedUnderReviewOrAppeal, Withdrawal}
import models._
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.apis.AwrsAPI10
import services.mocks.{MockKeyStoreService, MockSave4LaterService}
import services.{DeEnrolService, EmailService}
import uk.gov.hmrc.http.SessionKeys
import utils.{AwrsSessionKeys, CountryCodes}
import utils.TestUtil.cachedData

import scala.concurrent.Future

class DeRegistrationControllerTest extends MockAuthConnector with MockKeyStoreService with MockSave4LaterService {

  import MockKeyStoreService._

  val mockApi10: AwrsAPI10 = mock[AwrsAPI10]
  val mockTaxEnrolmentsConnector: TaxEnrolmentsConnector = mock[TaxEnrolmentsConnector]
  val mockEmailService: EmailService = mock[EmailService]

  override val mockDeEnrolService: DeEnrolService = new DeEnrolService(mockTaxEnrolmentsConnector)

  val mockTemplate = app.injector.instanceOf[views.html.awrs_de_registration]
  val mockTemplateConfirm = app.injector.instanceOf[views.html.awrs_de_registration_confirm]
  val mockTemplateReason = app.injector.instanceOf[views.html.awrs_de_registration_reason]
  val mockTemplateEvidence = app.injector.instanceOf[views.html.awrs_de_registration_confirmation_evidence]

  val injectedAppConfig = app.injector.instanceOf[ApplicationConfig]

  val testDeRegistrationController: DeRegistrationController =
    new DeRegistrationController(mockMCC, mockApi10, mockEmailService, mockDeEnrolService, testKeyStoreService, testSave4LaterService, mockAuthConnector, mockAuditable, mockAccountUtils, injectedAppConfig,
      mockTemplate, mockTemplateConfirm, mockTemplateReason, mockTemplateEvidence) {
    override val signInUrl = "/sign-in"
  }

  val permittedStatusTypes: Set[FormBundleStatus] = testDeRegistrationController.permittedStatusTypes
  val forbiddenStatusTypes = FormBundleStatus.allStatus.diff(permittedStatusTypes)

  override def beforeEach(): Unit = {
    reset(mockAuthConnector, mockApi10, mockTaxEnrolmentsConnector, mockAccountUtils)
    reset(mockMainStoreSave4LaterConnector)
    reset(mockApiSave4LaterConnector)

    super.beforeEach()
  }

  def mockAPI10AndDeEnroll(deRegSuccess: Boolean = true): Unit = {
    val deRegSuccessData = if (deRegSuccess) {
      deRegistrationSuccessData
    } else {
      deRegistrationFailureData
    }
    when(mockApi10.deRegistration(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(deRegSuccessData))
    when(mockTaxEnrolmentsConnector.deEnrol(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(defaultDeEnrollResponseSuccessData))
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
    deRegistration ifDefinedThen (count => verify(mockApi10, times(count)).deRegistration(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
    deEnrol ifDefinedThen (count => verify(mockTaxEnrolmentsConnector, times(count)).deEnrol(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
  }

  val reasonURL = "/alcohol-wholesale-scheme/cancel-registration"
  val dateURL = "/alcohol-wholesale-scheme/cancellation-date"
  val confirmURL = "/alcohol-wholesale-scheme/confirm-cancellation"
  val confirmationURL = "/alcohol-wholesale-scheme/de-register-confirmation"
  val indexURL = "/alcohol-wholesale-scheme/index"

  "DeRegistration" must {
    for (pStatusType <- permittedStatusTypes) {
      f"allow usage if type is $pStatusType" in {
        mocks()

        testShowDate(pStatusType) {
          result =>
            status(result) mustBe OK
        }

        testShowReason(pStatusType) {
          result =>
            status(result) mustBe OK
        }

        testShowConfirm(pStatusType) {
          result =>
            status(result) mustBe OK
        }

        testShowConfirmation(pStatusType) {
          result =>
            status(result) mustBe OK
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
              case Rejected | RejectedUnderReviewOrAppeal | Revoked | RevokedUnderReviewOrAppeal | Withdrawal | DeRegistered =>
                status(result) mustBe SEE_OTHER
              case _ => status(result) mustBe NOT_FOUND
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
            status(result) mustBe SEE_OTHER
            redirectLocation(result).get must endWith(reasonURL)
        }

        testShowConfirm(pStatusType) {
          result =>
            status(result) mustBe SEE_OTHER
            redirectLocation(result).get must endWith(reasonURL)
        }

        testShowConfirmation(pStatusType) {
          result =>
            status(result) mustBe SEE_OTHER
            redirectLocation(result).get must endWith(reasonURL)
        }
      }
    }

    for (pStatusType <- permittedStatusTypes) {
      f"redirect $pStatusType users to 'de-register-awrs-date' from confirm and confirmation pages when date is not answered" in {
        mocks(haveDeRegDate = false)

        testShowConfirm(pStatusType) {
          result =>
            status(result) mustBe SEE_OTHER
            redirectLocation(result).get must endWith(dateURL)
        }

        testShowConfirmation(pStatusType) {
          result =>
            status(result) mustBe SEE_OTHER
            redirectLocation(result).get must endWith(dateURL)
        }
      }
    }

    for (pStatusType <- permittedStatusTypes) {
      f"redirect $pStatusType users to confirm from 'de-register-awrs-date' " in {
        mocks()

        testSubmitDate(pStatusType) {
          result =>
            status(result) mustBe SEE_OTHER
            redirectLocation(result).get must endWith(confirmURL)
        }
      }
    }

    for (pStatusType <- permittedStatusTypes) {
      f"redirect $pStatusType users to confirm from 'de-register-awrs-reason' " in {
        mocks()

        testSubmitReason(pStatusType) {
          result =>
            status(result) mustBe SEE_OTHER
            redirectLocation(result).get must endWith(dateURL)
        }
      }
    }

    for (pStatusType <- permittedStatusTypes) {
      f"redirect $pStatusType users to confirmation from 'de-register-awrs-confirm' " in {
        mocks()
        when(mockEmailService.sendCancellationEmail(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(),ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(true))
        setupMockSave4LaterService(fetchAll = cachedData())

        testSubmitConfirm(pStatusType) {
          result =>
            status(result) mustBe SEE_OTHER
            redirectLocation(result).get must endWith(confirmationURL)
        }
      }
    }

    for (pStatusType <- permittedStatusTypes) {
      f"redirect $pStatusType users to index from 'de-register-awrs-confirm' if they answer no" in {
        mocks()

        testSubmitConfirm(pStatusType, BooleanRadioEnum.No) {
          result =>
            status(result) mustBe SEE_OTHER
            redirectLocation(result).get must endWith(indexURL)
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
            status(result) mustBe BAD_REQUEST
        }
        testSubmitDateOldDate(pStatusType) {
          result =>
            status(result) mustBe BAD_REQUEST
        }
        testSubmitReasonEmpty(pStatusType) {
          result =>
            status(result) mustBe BAD_REQUEST
        }
        testSubmitConfirmEmpty(pStatusType) {
          result =>
            status(result) mustBe BAD_REQUEST

        }
      }
    }

    for (pStatusType <- permittedStatusTypes) {
      f"$pStatusType users who have submitted a dereg date show confirmation page" in {
        mocks()

        testShowConfirm(pStatusType) {
          result =>
            status(result) mustBe OK
        }
      }
    }

    for (pStatusType <- permittedStatusTypes) {
      s"$pStatusType users who experienced API10 failures must not have save 4 later cleared up" in {
        mocks(deRegSuccess = false)

        testSubmitConfirm(pStatusType) {
          result =>
            status(result) mustBe INTERNAL_SERVER_ERROR
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
      s"$pStatusType users who went through successesful API 10 and de-enrol must have their save 4 later cleared up" in {
        mocks(deRegSuccess = true)
        when(mockEmailService.sendCancellationEmail(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(),ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(true))
        setupMockSave4LaterService(fetchAll = cachedData())

        testSubmitConfirm(pStatusType) {
          result =>
            status(result) mustBe SEE_OTHER
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

  def testShowReason(status: FormBundleStatus)(test: Future[Result] => Any) = getWithAuthorisedAgentUser(status, testDeRegistrationController.showReason())(test)

  def testSubmitReason(status: FormBundleStatus)(test: Future[Result] => Any) = submits(status, testDeRegistrationController.submitReason, ("deRegistrationReason", "Ceases to be registerable for the scheme"))(test)

  def testSubmitReasonEmpty(status: FormBundleStatus)(test: Future[Result] => Any) = submits(status, testDeRegistrationController.submitReason, ("deRegistrationReason", ""))(test)

  def testShowDate(status: FormBundleStatus)(test: Future[Result] => Any) = getWithAuthorisedAgentUser(status, testDeRegistrationController.showDate())(test)

  def testSubmitDate(status: FormBundleStatus)(test: Future[Result] => Any) = submits(status, testDeRegistrationController.submitDate, ("proposedEndDate.day", LocalDate.now().getDayOfMonth.toString), ("proposedEndDate.month", LocalDate.now().getMonthOfYear.toString), ("proposedEndDate.year", LocalDate.now().getYear.toString))(test)

  def testSubmitDateOldDate(status: FormBundleStatus)(test: Future[Result] => Any) = submits(status, testDeRegistrationController.submitDate, ("proposedEndDate.day", "1"), ("proposedEndDate.month", "1"), ("proposedEndDate.year", "2016"))(test)

  def testSubmitDateEmpty(status: FormBundleStatus)(test: Future[Result] => Any) = submits(status, testDeRegistrationController.submitDate, ("proposedEndDate.day", ""), ("proposedEndDate.month", ""), ("proposedEndDate.year", ""))(test)

  def testShowConfirm(status: FormBundleStatus)(test: Future[Result] => Any) = getWithAuthorisedAgentUser(status, testDeRegistrationController.showConfirm())(test)

  def testSubmitConfirm(status: FormBundleStatus, answer: BooleanRadioEnum.Value = BooleanRadioEnum.Yes)(test: Future[Result] => Any) = submits(status, testDeRegistrationController.callToAction, ("deRegistrationConfirmation", answer.toString))(test)

  def testSubmitConfirmEmpty(status: FormBundleStatus)(test: Future[Result] => Any) = submits(status, testDeRegistrationController.callToAction, ("deRegistrationConfirmation", ""))(test)

  def testShowConfirmation(status: FormBundleStatus)(test: Future[Result] => Any) = getWithAuthorisedAgentUser(status, testDeRegistrationController.showConfirmation(false))(test)

  def getWithAuthorisedAgentUser(status: FormBundleStatus, call: Action[AnyContent])(test: Future[Result] => Any) {
    resetAuthConnector()
    setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
    val result = call.apply(buildRequestWithSession(userId, status.name))
    test(result)
  }

  def submits(status: FormBundleStatus, call: Action[AnyContent], data: (String, String)*)(test: Future[Result] => Any) {
    resetAuthConnector()
    setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
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
