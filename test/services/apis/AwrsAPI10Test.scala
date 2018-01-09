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

package services.apis

import java.util.UUID

import builders.AuthBuilder
import connectors.AWRSConnector
import connectors.mock.MockAuthConnector
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.test.FakeRequest
import services.mocks.MockKeyStoreService
import uk.gov.hmrc.domain.{AwrsUtr, Nino}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import utils.TestConstants._

import scala.concurrent.Future
import uk.gov.hmrc.http.SessionKeys

class AwrsAPI10Test extends MockKeyStoreService with MockAuthConnector {
  import MockKeyStoreService._

  lazy val mockAWRSConnector = mock[AWRSConnector]

  object TestAwrsAPI10 extends AwrsAPI10 {
    override val awrsConnector = mockAWRSConnector
    override val dataCacheService = TestKeyStoreService
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAWRSConnector)
  }

  def newUser(hasAwrs: Boolean): AuthContext = AuthBuilder.createAuthContextWithOrWithoutAWWRS("userId", testUserName, testUtr,hasAwrs)

  implicit lazy val fakeRequest = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId,
      "businessType" -> "SOP",
      "businessName" -> testTradingName
    )
  }

  def mockAPI10(deRegSuccess: Boolean = true,
                deRegistrationCorrupt: Boolean = false): Unit = {

    val deRegSuccessData = deRegSuccess match {
      case true => deRegistrationSuccessData
      case false => deRegistrationFailureData
    }
    when(mockAWRSConnector.deRegistration(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(deRegSuccessData.get))
  }

  def mocks(haveDeRegDate: Boolean = true,
            haveDeRegReason: Boolean = true,
            deRegSuccess: Boolean = true,
            deRegistrationCorrupt: Boolean = false): Unit = {
    setupMockKeyStoreServiceForDeRegistrationOrWithdrawal(haveDeRegDate = haveDeRegDate, haveDeRegReason = haveDeRegReason)
    mockAPI10(deRegSuccess = deRegSuccess, deRegistrationCorrupt = deRegistrationCorrupt)
  }

  "AwrsAPI10 " should {
    trait HasAwrsUser {
      implicit val user = newUser(true)
    }

    trait NoAwrsUser {
      implicit val user = newUser(false)
    }

    "deRegistration should call ETMP when both date and reason are present" in new HasAwrsUser {
      mocks()

      val result = TestAwrsAPI10.deRegistration()
      await(result) shouldBe deRegistrationSuccessData
    }

    "deRegistration should not call ETMP when there is no date" in new HasAwrsUser {
      mocks(haveDeRegDate = false)

      val result = TestAwrsAPI10.deRegistration()
      await(result) shouldBe None
    }

    "deRegistration should not call ETMP when there is no reason" in new HasAwrsUser {
      mocks(haveDeRegReason = false)

      val result = TestAwrsAPI10.deRegistration()
      await(result) shouldBe None
    }

    "deRegistration should not call ETMP when the user does not have an AWRS reg number" in new NoAwrsUser {
      mocks()

      val result = TestAwrsAPI10.deRegistration()
      await(result) shouldBe None
    }
  }


}
