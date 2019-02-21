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

package connectors

import config.{AwrsSessionCache, BusinessCustomerSessionCache}
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import uk.gov.hmrc.http.cache.client.SessionCache
import utils.AwrsUnitTestTraits
import utils.TestConstants._
import utils.TestUtil._

import scala.concurrent.Future

class KeyStoreConnectorSpec extends AwrsUnitTestTraits {

  val mockSessionCache = mock[SessionCache]

  object TestKeyStoreConnector extends KeyStoreConnector {
    override val sessionCache: SessionCache = mockSessionCache
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSessionCache)
  }

  "KeyStoreConnector" should {
    "use the correct session cache for BC" in {
      BusinessCustomerDataCacheConnector.sessionCache shouldBe BusinessCustomerSessionCache
    }

    "use the correct cache for keystore" in {
      AwrsKeyStoreConnector.sessionCache shouldBe AwrsSessionCache
    }


    "fetch saved data from keystore" in {
      when(mockSessionCache.fetchAndGetEntry[SubscriptionStatusType](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(testSubscriptionStatusTypePendingGroup)))
      val result = TestKeyStoreConnector.fetchDataFromKeystore[SubscriptionStatusType](testUtr)
      await(result) shouldBe Some(testSubscriptionStatusTypePendingGroup)
    }

    "save data to keystore" in {
      when(mockSessionCache.cache[SubscriptionStatusType](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(returnedKeystoreCacheMap))
      val result = TestKeyStoreConnector.saveDataToKeystore[SubscriptionStatusType](testUtr, testSubscriptionStatusTypePendingGroup)
      await(result) shouldBe returnedKeystoreCacheMap
    }

  }
}
