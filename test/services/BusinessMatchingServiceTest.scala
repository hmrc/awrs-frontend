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
import connectors._
import forms.AWRSEnums
import models.Organisation
import org.mockito.Matchers
import org.mockito.Mockito.when
import services.mocks.MockKeyStoreService
import utils.AwrsTestJson._
import utils.AwrsUnitTestTraits
import utils.TestConstants.testUtr

import scala.concurrent.Future

class BusinessMatchingServiceTest extends AwrsUnitTestTraits with MockKeyStoreService {

  val mockBusinessMatchingConnector: BusinessMatchingConnector = mock[BusinessMatchingConnector]

  object BusinessMatchingServiceTest extends BusinessMatchingService {
    override val businessMatchingConnector = mockBusinessMatchingConnector
    override val keyStoreService = TestKeyStoreService
  }

  "Business Matching Services" should {
    "use correct Connector" in {
      BusinessMatchingService.businessMatchingConnector shouldBe BusinessMatchingConnector
    }

    "validate a UTR is correct by business matching" in {
      when(mockBusinessMatchingConnector.lookup(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(matchSuccessResponseJson))
      val result = BusinessMatchingServiceTest.matchBusinessWithUTR(testUtr, Some(Organisation("Acme", AWRSEnums.CorporateBodyString)))
      await(result) shouldBe true
    }

    "validate a UTR is incorrect by business matching" in {
      when(mockBusinessMatchingConnector.lookup(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(matchFailureResponseJson))
      val result = BusinessMatchingServiceTest.matchBusinessWithUTR(testUtr, Some(Organisation("Acme", AWRSEnums.CorporateBodyString)))
      await(result) shouldBe false
    }
  }
}
