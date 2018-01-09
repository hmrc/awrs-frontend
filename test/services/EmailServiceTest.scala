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

package services

import builders.AuthBuilder
import connectors.mock.{MockAWRSNotificationConnector, MockAuthConnector}
import models.FormBundleStatus.{Approved, ApprovedWithConditions, Pending}
import models.{ApiTypes, DeRegistrationDate, EmailRequest, FormBundleStatus}
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.test.FakeRequest
import services.mocks.MockKeyStoreService.defaultDeRegistrationDateData
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{AccountUtils, AwrsSessionKeys, AwrsUnitTestTraits}
import utils.TestConstants._

class EmailServiceTest extends AwrsUnitTestTraits
  with MockAWRSNotificationConnector
  with MockAuthConnector {

  object TestEmailService extends EmailService {
    override val awrsNotificationConnector = mockAWRSNotificationConnector
  }

  lazy val businessName = "test business"
  lazy val userName = "test user"
  lazy val email = "example@example.com"
  lazy val reference = testRefNo
  lazy val isNewBusiness = true
  lazy val deRegistrationDateStr = "12-07-2017"

  "Email Service" should {
    "build the correct request object for the connectors for api4 None user" in {
      implicit val user = createApi4User()
      implicit val request = FakeRequest().withSession(AwrsSessionKeys.sessionBusinessName -> businessName)
      val expected = GetExpectedOutput(ApiTypes.API4)

      when(mockAWRSNotificationConnector.sendConfirmationEmail(Matchers.eq(expected))(Matchers.any(), Matchers.any())).thenReturn(true)
      val result = TestEmailService.sendConfirmationEmail(email = email, reference = reference, isNewBusiness = isNewBusiness)

      await(result) shouldBe true
    }

    "build the correct request object for the connectors for api6.pending Pending(01) user" in {
      implicit val user = createApi6User()
      implicit val request = FakeRequest().withSession(AwrsSessionKeys.sessionBusinessName -> businessName, AwrsSessionKeys.sessionStatusType -> "Pending")
      val expected = GetExpectedOutput(ApiTypes.API6Pending)

      when(mockAWRSNotificationConnector.sendConfirmationEmail(Matchers.eq(expected))(Matchers.any(), Matchers.any())).thenReturn(true)
      val result = TestEmailService.sendConfirmationEmail(email = email, reference = reference, isNewBusiness = isNewBusiness)

      await(result) shouldBe true
    }

    "build the correct request object for the connectors for api6.approved Approved(04) user" in {
      implicit val user = createApi6User()
      implicit val request = FakeRequest().withSession(AwrsSessionKeys.sessionBusinessName -> businessName, AwrsSessionKeys.sessionStatusType -> "Approved")
      val expected = GetExpectedOutput(ApiTypes.API6Approved)

      when(mockAWRSNotificationConnector.sendConfirmationEmail(Matchers.eq(expected))(Matchers.any(), Matchers.any())).thenReturn(true)
      val result = TestEmailService.sendConfirmationEmail(email = email, reference = reference, isNewBusiness = isNewBusiness)

      await(result) shouldBe true
    }

    "build the correct request object for the connectors for api6.approved Conditions(05) user" in {
      implicit val user = createApi6User()
      implicit val request = FakeRequest().withSession(AwrsSessionKeys.sessionBusinessName -> businessName, AwrsSessionKeys.sessionStatusType -> "Approved with Conditions")
      val expected = GetExpectedOutput(ApiTypes.API6Approved)

      when(mockAWRSNotificationConnector.sendConfirmationEmail(Matchers.eq(expected))(Matchers.any(), Matchers.any())).thenReturn(true)
      val result = TestEmailService.sendConfirmationEmail(email = email, reference = reference, isNewBusiness = isNewBusiness)

      await(result) shouldBe true
    }

    "get succesful response when sending withdraw email for API8 user" in {
      implicit val user = AuthBuilder.createUserAuthContextOrgWithAWRS(userId, userName, testUtr)
      implicit val request = FakeRequest().withSession(AwrsSessionKeys.sessionBusinessName -> businessName, AwrsSessionKeys.sessionStatusType -> "Withdrawal")
      val expected = GetExpectedOutput(ApiTypes.API8,None,None,None)

      when(mockAWRSNotificationConnector.sendWithdrawnEmail(Matchers.eq(expected))(Matchers.any(), Matchers.any())).thenReturn(true)
      val result = TestEmailService.sendWithdrawnEmail(email = email)

      await(result) shouldBe true
    }

    "get succesful response when sending cancellation email for API10 user" in {
      implicit val user = AuthBuilder.createUserAuthContextIndSaWithAWRS(userId, userName, testUtr)
      implicit val request = FakeRequest().withSession(AwrsSessionKeys.sessionBusinessName -> businessName, AwrsSessionKeys.sessionStatusType -> "De-Registered")
      val expected = GetExpectedOutput(ApiTypes.API10,None,None,defaultDeRegistrationDateData)
      when(mockAWRSNotificationConnector.sendCancellationEmail(Matchers.eq(expected))(Matchers.any(), Matchers.any())).thenReturn(true)
      val result = TestEmailService.sendCancellationEmail(email = email,defaultDeRegistrationDateData)
      await(result) shouldBe true
    }
  }


  private def GetExpectedOutput(apiType: ApiTypes.ApiType,
                                reference: Option[String] = reference,
                                isNewBusiness: Option[Boolean] = isNewBusiness,
                                deRegistrationDate : Option[DeRegistrationDate] = None) = {
    val deRegistrationDateStr = deRegistrationDate match {
      case Some(deRegDate) => Some(deRegDate.proposedEndDate.toString("dd MMMM yyyy"))
      case _ => None
    }
    EmailRequest(
      apiType = apiType,
      businessName = businessName,
      reference = reference,
      email = email,
      isNewBusiness = isNewBusiness,
      deregistrationDateStr = deRegistrationDateStr
    )
  }

  private def createApi4User() = {
    AuthBuilder.createUserAuthContextIndCt(userId, userName, testUtr)
  }

  private def createApi6User() = {
    AuthBuilder.createUserAuthContextOrgWithAWRS(userId, userName, testUtr)
  }

}
