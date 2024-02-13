/*
 * Copyright 2023 HM Revenue & Customs
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
import models.{WithdrawalReason, WithdrawalResponse}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import services.mocks.MockKeyStoreService
import uk.gov.hmrc.auth.core.retrieve.{LegacyCredentials, SimpleRetrieval}
import utils.TestConstants._
import utils.TestUtil

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AwrsAPI8Test extends MockKeyStoreService with MockAuthConnector {

  lazy val mockAWRSConnector: AWRSConnector = mock[AWRSConnector]
  val testAwrsAPI8: AwrsAPI8 = new AwrsAPI8(mockAWRSConnector, testKeyStoreService)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAWRSConnector)
  }

  implicit lazy val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest().withSession(
      "sessionId" -> sessionId,
      SimpleRetrieval("token", LegacyCredentials.reads).toString -> "RANDOMTOKEN",
      "userId"-> userId,
      "businessType" -> "SOP",
      "businessName" -> testTradingName
    )
  }

  "AwrsAPI8" must {
    "withdraw an application" when {
      "there is a result reason" in {
        when(mockAWRSConnector.withdrawApplication(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(WithdrawalResponse("processingDate")))

        val result = testAwrsAPI8.withdrawApplication(Some(WithdrawalReason("Some reason", "Some other")), TestUtil.defaultAuthRetrieval)
        await(result) mustBe WithdrawalResponse("processingDate")
      }
    }

    "throw exception" when {
      "there is no reason" in {
        val result = testAwrsAPI8.withdrawApplication(None, TestUtil.defaultAuthRetrieval)
        intercept[NoSuchElementException](await(result) mustBe WithdrawalResponse("processingDate"))
      }
    }
  }


}
