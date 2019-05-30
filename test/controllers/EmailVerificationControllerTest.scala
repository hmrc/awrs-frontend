/*
 * Copyright 2019 HM Revenue & Customs
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
import controllers.auth.ExternalUrls
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Result
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

  val mockEmailVerificationService = mock[EmailVerificationService]

  object TestEmailVerificationControllerEmailEnabled extends EmailVerificationController {
    override val authConnector = mockAuthConnector
    override val save4LaterService = TestSave4LaterService
    override val emailVerificationService = mockEmailVerificationService
    override val isEmailVerificationEnabled = true
    val signInUrl = ExternalUrls.signIn
  }

  object TestEmailVerificationControllerEmailNotEnabled extends EmailVerificationController {
    override val authConnector = mockAuthConnector
    override val save4LaterService = TestSave4LaterService
    override val emailVerificationService = mockEmailVerificationService
    override val isEmailVerificationEnabled = false
    val signInUrl = ExternalUrls.signIn
  }

  "Page load for Authorised users" should {

    "redirect to the declaration page if the email verification is turned off" in {
      checkEmailVerification(testController = TestEmailVerificationControllerEmailNotEnabled) {
        result =>
          redirectLocation(result).get shouldBe controllers.routes.ApplicationDeclarationController.showApplicationDeclaration.url
      }
    }

    "redirect to the declaration page if the email has been verified" in {
      checkEmailVerification(testController = TestEmailVerificationControllerEmailEnabled, isVerified = true) {
        result =>
          redirectLocation(result).get shouldBe controllers.routes.ApplicationDeclarationController.showApplicationDeclaration.url
      }
    }

    "display the email verification reminder page if the email has not yet been verified" in {
      checkEmailVerification(testController = TestEmailVerificationControllerEmailEnabled, isVerified = false) {
        result =>
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("email-verification-error-header").text shouldBe Messages("awrs.email_verification_error.heading")
          document.getElementById("email-verification-error-lede").text shouldBe Messages("awrs.email_verification_error.lede")
          document.getElementById("email-verification-error-description-1").text shouldBe Messages("awrs.email_verification_error.info_1", testEmail + ".")
          document.getElementById("email-verification-error-description-2").text shouldBe Messages("awrs.email_verification_error.info_2")
          document.getElementById("email-verification-error-description-3").text should include(Messages("awrs.email_verification_error.contacts_link"))
          document.getElementById("email-verification-error-description-3").text should include(Messages("awrs.email_verification_error.resend_link"))
          document.getElementById("return-to-summary").text shouldBe Messages("awrs.generic.return_to_app_summary")
      }
    }

    "resend email and reload the reminder screen with resent text" in {
      resendEmail(isVerified = true) {
        result =>
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("email-verification-error-header").text shouldBe Messages("awrs.email_verification_error.heading")
          document.getElementById("email-verification-error-lede").text shouldBe Messages("awrs.email_verification_error.lede")
          document.getElementById("email-verification-error-description-1").text shouldBe Messages("awrs.email_verification_error.info_1", testEmail + ".")
          document.getElementById("email-verification-error-description-2").text shouldBe Messages("awrs.email_verification_error.info_2")
          document.getElementById("email-verification-error-description-3").text should include(Messages("awrs.email_verification_error.contacts_link"))
          document.getElementById("email-verification-error-description-3").text should include(Messages("awrs.email_verification_error.resend_link"))
          document.getElementById("email-verification-error-resent").text should include(Messages("awrs.email_verification_error.resent", testEmail + "."))
          document.getElementById("continue").text shouldBe Messages("awrs.confirmation.button")
      }
    }

    "display error screen if resend fails" in {
      resendEmail(isVerified = false) {
        result =>
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("application-error-header").text shouldBe Messages("awrs.generic.error.title")
          document.getElementById("application-error-description").text shouldBe Messages("awrs.generic.error.status")
      }
    }

    "show success message" in {
      showSuccess {
        result =>
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("email-verification-success-header").text shouldBe Messages("awrs.email_verification_success.heading")
          document.getElementById("email-verification-success-lede").text shouldBe Messages("awrs.email_verification_success.lede", ".")
          document.getElementById("return-to-summary").text shouldBe Messages("awrs.generic.return_to_app_summary")
      }
    }
  }

  def checkEmailVerification[T <: EmailVerificationController](testController: T, isVerified: Boolean = true)(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(
      fetchBusinessCustomerDetails = testBusinessCustomerDetails("SOP"),
      fetchBusinessContacts = testBusinessContactsDefault()
    )
    setAuthMocks()
    when(mockEmailVerificationService.isEmailVerified(Matchers.eq(Some(testBusinessContactsDefault())))(Matchers.any())).thenReturn(Future.successful(isVerified))
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
    when(mockEmailVerificationService.sendVerificationEmail(Matchers.eq(testEmail))(Matchers.any())).thenReturn(Future.successful(isVerified))
    val request = SessionBuilder.buildRequestWithSession(userId)
    val result = TestEmailVerificationControllerEmailEnabled.resend().apply(request)
    test(result)
  }

  def showSuccess(test: Future[Result] => Any) {
    val request = SessionBuilder.buildRequestWithSession(userId)
    val result = TestEmailVerificationControllerEmailEnabled.showSuccess().apply(request)
    test(result)
  }


}
