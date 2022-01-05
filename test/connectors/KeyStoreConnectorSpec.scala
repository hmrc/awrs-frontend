/*
 * Copyright 2022 HM Revenue & Customs
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

package connectors

import models._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import uk.gov.hmrc.http.cache.client.SessionCache
import utils.AwrsUnitTestTraits
import utils.TestConstants._
import utils.TestUtil._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class KeyStoreConnectorSpec extends AwrsUnitTestTraits {

  val mockSessionCache: SessionCache = mock[SessionCache]

  val testKeyStoreConnector: KeyStoreConnector = new KeyStoreConnector() {
    override val sessionCache: SessionCache = mockSessionCache
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSessionCache)
  }

  "KeyStoreConnector" must {
    "fetch saved data from keystore" in {
      when(mockSessionCache.fetchAndGetEntry[SubscriptionStatusType](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testSubscriptionStatusTypePendingGroup)))
      val result = testKeyStoreConnector.fetchDataFromKeystore[SubscriptionStatusType](testUtr)
      await(result) mustBe Some(testSubscriptionStatusTypePendingGroup)
    }

    "save data to keystore" in {
      when(mockSessionCache.cache[SubscriptionStatusType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(returnedKeystoreCacheMap))
      val result = testKeyStoreConnector.saveDataToKeystore[SubscriptionStatusType](testUtr, testSubscriptionStatusTypePendingGroup)
      await(result) mustBe returnedKeystoreCacheMap
    }

  }
}
