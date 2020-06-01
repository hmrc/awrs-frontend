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

import builders.SessionBuilder
import connectors.mock.MockAuthConnector
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.i18n.{Lang, Messages}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import services.EmailVerificationService
import services.mocks.MockSave4LaterService
import utils.AwrsUnitTestTraits
import utils.TestConstants._
import utils.TestUtil.{testBusinessContactsDefault, testBusinessCustomerDetails}

import scala.concurrent.Future

class EmailVerificationControllerTest extends AwrsUnitTestTraits
  with MockAuthConnector
  with MockSave4LaterService {

  val mockEmailVerificationService: EmailVerificationService = mock[EmailVerificationService]

  implicit val mcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val realMessages: Messages = mcc.messagesApi.preferred(Seq(Lang.defaultLang))

  val testEmailVerificationControllerEmailEnabled: EmailVerificationController =
    new EmailVerificationController(mcc, mockEmailVerificationService, mockAuditable, mockAccountUtils, testSave4LaterService, mockAuthConnector, mockAppConfig) {
      override lazy val signInUrl: String = applicationConfig.signIn
      override lazy val isEmailVerificationEnabled = true
  }

  val testEmailVerificationControllerEmailNotEnabled: EmailVerificationController =
    new EmailVerificationController(mcc, mockEmailVerificationService, mockAuditable, mockAccountUtils, testSave4LaterService, mockAuthConnector, mockAppConfig) {
      override lazy val isEmailVerificationEnabled = false
      override lazy val signInUrl: String = applicationConfig.signIn
  }

  "Page load for Authorised users" should {

    "redirect to the declaration page if the email verification is turned off" in {
      checkEmailVerification(testController = testEmailVerificationControllerEmailNotEnabled) {
        result =>
          redirectLocation(result).get shouldBe controllers.routes.ApplicationDeclarationController.showApplicationDeclaration.url
      }
    }

    "redirect to the declaration page if the email has been verified" in {
      checkEmailVerification(testController = testEmailVerificationControllerEmailEnabled, isVerified = true) {
        result =>
          redirectLocation(result).get shouldBe controllers.routes.ApplicationDeclarationController.showApplicationDeclaration.url
      }
    }

    "display the email verification reminder page if the email has not yet been verified" in {
      checkEmailVerification(testController = testEmailVerificationControllerEmailEnabled, isVerified = false) {
        result =>
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("email-verification-error-header").text shouldBe realMessages("awrs.email_verification_error.heading")
          document.getElementById("email-verification-error-lede").text shouldBe realMessages("awrs.email_verification_error.lede")
          document.getElementById("email-verification-error-description-1").text shouldBe realMessages("awrs.email_verification_error.info_1", testEmail + ".")
          document.getElementById("email-verification-error-description-2").text shouldBe realMessages("awrs.email_verification_error.info_2")
          document.getElementById("email-verification-error-description-3").text should include(realMessages("awrs.email_verification_error.contacts_link"))
          document.getElementById("email-verification-error-description-3").text should include(realMessages("awrs.email_verification_error.resend_link"))
          document.getElementById("return-to-summary").text shouldBe realMessages("awrs.generic.return_to_app_summary")
      }
    }

    "resend email and reload the reminder screen with resent text" in {
      resendEmail(isVerified = true) {
        result =>
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("email-verification-error-header").text shouldBe realMessages("awrs.email_verification_error.heading")
          document.getElementById("email-verification-error-lede").text shouldBe realMessages("awrs.email_verification_error.lede")
          document.getElementById("email-verification-error-description-1").text shouldBe realMessages("awrs.email_verification_error.info_1", testEmail + ".")
          document.getElementById("email-verification-error-description-2").text shouldBe realMessages("awrs.email_verification_error.info_2")
          document.getElementById("email-verification-error-description-3").text should include(realMessages("awrs.email_verification_error.contacts_link"))
          document.getElementById("email-verification-error-description-3").text should include(realMessages("awrs.email_verification_error.resend_link"))
          document.getElementById("email-verification-error-resent").text should include(realMessages("awrs.email_verification_error.resent", testEmail + "."))
          document.getElementById("continue").text shouldBe realMessages("awrs.confirmation.button")
      }
    }

    "display error screen if resend fails" in {
      resendEmail(isVerified = false) {
        result =>
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("application-error-header").text shouldBe realMessages("awrs.generic.error.title")
          document.getElementById("application-error-description").text shouldBe realMessages("awrs.generic.error.status")
      }
    }

    "return to error page if Internal Error thrown" in {
      resendEmail(isVerified = false) {
        result =>
          status(result) shouldBe INTERNAL_SERVER_ERROR

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("application-error-header").text shouldBe realMessages("awrs.generic.error.title")
          document.getElementById("application-error-description").text shouldBe realMessages("awrs.generic.error.status")
      }
    }

    "show success message" in {
      showSuccess {
        result =>
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("email-verification-success-header").text shouldBe realMessages("awrs.email_verification_success.heading")
          document.getElementById("email-verification-success-lede").text shouldBe realMessages("awrs.email_verification_success.lede", ".")
          document.getElementById("return-to-summary").text shouldBe realMessages("awrs.generic.return_to_app_summary")
      }
    }
  }

  def checkEmailVerification[T <: EmailVerificationController](testController: T, isVerified: Boolean = true)(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(
      fetchBusinessCustomerDetails = testBusinessCustomerDetails("SOP"),
      fetchBusinessContacts = testBusinessContactsDefault()
    )
    setAuthMocks()
    when(mockEmailVerificationService.isEmailVerified(ArgumentMatchers.eq(Some(testBusinessContactsDefault())))(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(isVerified))
    val request = SessionBuilder.buildRequestWithSession(userId)
    val result = testController.checkEmailVerification.apply(request)
    test(result)
  }

  def resendEmail(isVerified: Boolean = true)(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(
      fetchBusinessCustomerDetails = testBusinessCustomerDetails("SOP"),
      fetchBusinessContacts = testBusinessContactsDefault()
    )
    setAuthMocks()
    when(mockEmailVerificationService.sendVerificationEmail(ArgumentMatchers.eq(testEmail))(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(isVerified))
    val request = SessionBuilder.buildRequestWithSession(userId)
    val result = testEmailVerificationControllerEmailEnabled.resend().apply(request)
    test(result)
  }

  def showSuccess(test: Future[Result] => Any) {
    val request = SessionBuilder.buildRequestWithSession(userId)
    val result = testEmailVerificationControllerEmailEnabled.showSuccess().apply(request)
    test(result)
  }


}
