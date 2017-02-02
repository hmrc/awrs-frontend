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

package services

import builders.AuthBuilder
import connectors.mock.{MockAWRSNotificationConnector, MockAuthConnector}
import models.FormBundleStatus.{Approved, ApprovedWithConditions, Pending}
import models.{ApiTypes, ConfirmationEmailRequest, FormBundleStatus}
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.test.FakeRequest
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

  "Email service" should {

    Seq(
      (createApi4User(), None),
      (createApi6User(), Pending),
      (createApi6User(), Approved),
      (createApi6User(), ApprovedWithConditions)).foreach { case (user, status) =>
      implicit lazy val request = status match {
        case None => FakeRequest().withSession(AwrsSessionKeys.sessionBusinessName -> businessName)
        case s: FormBundleStatus => FakeRequest().withSession(AwrsSessionKeys.sessionBusinessName -> businessName, AwrsSessionKeys.sessionStatusType -> s.name)
      }
      implicit val impUser = user

      val apiType = AccountUtils.hasAwrs(user) match {
        case true => status match {
          case Pending => ApiTypes.API6Pending
          case Approved | ApprovedWithConditions => ApiTypes.API6Approved
        }
        case false => ApiTypes.API4
      }
      val userType = s"$apiType $status user"

      s"build the correct request object for the connectors for $userType" in {
        val email = "example@example.com"
        val reference = testRefNo
        val isNewBusiness = true
        val expected = ConfirmationEmailRequest(
          apiType = apiType,
          businessName = businessName,
          reference = reference,
          email = email,
          isNewBusiness = isNewBusiness
        )

        // the following mocks are ordered so that false is returned for any input that differs from the expected
        when(mockAWRSNotificationConnector.sendConfirmationEmail(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(false)
        when(mockAWRSNotificationConnector.sendConfirmationEmail(Matchers.eq(expected))(Matchers.any(), Matchers.any())).thenReturn(true)
        val result = TestEmailService.sendConfirmationEmail(email = email, reference = reference, isNewBusiness = isNewBusiness)

        // if the incorrect request object is placed into sendConfirmationEmail then false would be returned
        await(result) shouldBe true
      }
    }

  }

  private def createApi4User() = {
    AuthBuilder.createUserAuthContextIndCt(userId, userName, testUtr)
  }

  private def createApi6User() = {
    AuthBuilder.createUserAuthContextOrgWithAWRS(userId, userName, testUtr)
  }

}
