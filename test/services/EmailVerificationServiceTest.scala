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

package services

import connectors.EmailVerificationConnector
import utils.AwrsUnitTestTraits
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import utils.TestUtil._
import utils.TestConstants._

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global


class EmailVerificationServiceTest extends AwrsUnitTestTraits {

  val mockEmailVerificationConnector: EmailVerificationConnector = mock[EmailVerificationConnector]
  val emailVerificationServiceTest: EmailVerificationService = new EmailVerificationService(mockEmailVerificationConnector)

  "Email Verification Service" should {
    "return true if the email is sent" in {
      when(mockEmailVerificationConnector.sendVerificationEmail(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(true))
      val result = emailVerificationServiceTest.sendVerificationEmail(testEmail)
      await(result) shouldBe true
    }

    "return false if the email is not sent" in {
      when(mockEmailVerificationConnector.sendVerificationEmail(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(false))
      val result = emailVerificationServiceTest.sendVerificationEmail(testEmail)
      await(result) shouldBe false
    }

    "return true if the email is verified" in {
      when(mockEmailVerificationConnector.isEmailAddressVerified(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(true))
      val result = emailVerificationServiceTest.isEmailVerified(testBusinessContactsDefault())
      await(result) shouldBe true
    }

    "return false if the email is not verified" in {
      when(mockEmailVerificationConnector.isEmailAddressVerified(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(false))
      val result = emailVerificationServiceTest.isEmailVerified(testBusinessContactsDefault())
      await(result) shouldBe false
    }

    "return false if the business contacts are empty" in {
      val result = emailVerificationServiceTest.isEmailVerified(None)
      await(result) shouldBe false
    }
  }


}
