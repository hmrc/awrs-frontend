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

package services.apis

import java.util.UUID
import connectors.AWRSConnector
import connectors.mock.MockAuthConnector
import controllers.auth.StandardAuthRetrievals
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.test.FakeRequest
import services.mocks.MockKeyStoreService
import uk.gov.hmrc.auth.core.Enrolment
import utils.TestConstants._

import scala.concurrent.Future
import uk.gov.hmrc.http.SessionKeys
import utils.TestUtil

import scala.concurrent.ExecutionContext.Implicits.global

class AwrsAPI10Test extends MockKeyStoreService with MockAuthConnector {
  import MockKeyStoreService._

  lazy val mockAWRSConnector: AWRSConnector = mock[AWRSConnector]
  val testAwrsAPI10: AwrsAPI10 = new AwrsAPI10(mockAccountUtils, mockAWRSConnector, testKeyStoreService)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAWRSConnector)
  }

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
    when(mockAWRSConnector.deRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(deRegSuccessData.get))
  }

  def mocks(haveDeRegDate: Boolean = true,
            haveDeRegReason: Boolean = true,
            deRegSuccess: Boolean = true,
            deRegistrationCorrupt: Boolean = false,
            awrsNo: Boolean = true): Unit = {
    setupMockKeyStoreServiceForDeRegistrationOrWithdrawal(haveDeRegDate = haveDeRegDate, haveDeRegReason = haveDeRegReason)
    mockAPI10(deRegSuccess = deRegSuccess, deRegistrationCorrupt = deRegistrationCorrupt)

    when(mockAccountUtils.hasAwrs(ArgumentMatchers.any()))
      .thenReturn(awrsNo)
  }

  "AwrsAPI10 " should {

    "deRegistration should call ETMP when both date and reason are present" in {
      mocks()

      val result = testAwrsAPI10.deRegistration(TestUtil.defaultAuthRetrieval)
      await(result) shouldBe deRegistrationSuccessData
    }

    "deRegistration should not call ETMP when there is no date" in {
      mocks(haveDeRegDate = false)

      val result = testAwrsAPI10.deRegistration(TestUtil.defaultAuthRetrieval)
      await(result) shouldBe None
    }

    "deRegistration should not call ETMP when there is no reason" in {
      mocks(haveDeRegReason = false)

      val result = testAwrsAPI10.deRegistration(TestUtil.defaultAuthRetrieval)
      await(result) shouldBe None
    }

    "deRegistration should not call ETMP when the user does not have an AWRS reg number" in {
      mocks(awrsNo = false)

      val result = testAwrsAPI10.deRegistration(StandardAuthRetrievals(Set.empty[Enrolment], None, "fakeCredID"))
      await(result) shouldBe None
    }
  }


}
