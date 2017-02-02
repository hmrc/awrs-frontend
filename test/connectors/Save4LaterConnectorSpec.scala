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

package connectors

import config.{AwrsAPIShortLivedCache, AwrsShortLivedCache}
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import uk.gov.hmrc.http.cache.client.ShortLivedCache
import utils.AwrsUnitTestTraits
import utils.TestConstants._
import utils.TestUtil._

import scala.concurrent.Future

class Save4LaterConnectorSpec extends AwrsUnitTestTraits {

  val mockShortLivedCache = mock[ShortLivedCache]

  object TestSave4LaterConnector extends Save4LaterConnector {
    override val shortLivedCache: ShortLivedCache = mockShortLivedCache
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockShortLivedCache)
  }

  "ShortLivedCache" should {

    "use the correct cache for Awrs" in {
      AwrsDataCacheConnector.shortLivedCache shouldBe AwrsShortLivedCache
    }

    "use the correct cache for Awrs api" in {
      AwrsAPIDataCacheConnector.shortLivedCache shouldBe AwrsAPIShortLivedCache
    }

    "fetch saved BusinessDetails from save4later" in {
      val key = "Business Details"
      when(mockShortLivedCache.fetchAndGetEntry[BusinessCustomerDetails](Matchers.any(), Matchers.eq(key))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(reviewDetails)))
      val result = TestSave4LaterConnector.fetchData4Later[BusinessCustomerDetails](testUtr, key)
      await(result) shouldBe Some(reviewDetails)
    }

    "save business details into save4later" in {
      val key = "BC_Business_Details"
      when(mockShortLivedCache.cache(Matchers.any(), Matchers.eq(key), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(returnedCacheMap))
      val result = TestSave4LaterConnector.saveData4Later(testUtr, key, reviewDetails)
      await(result).get shouldBe reviewDetails
    }

    "fetch all data from save4later by utr" in {
      when(mockShortLivedCache.fetch(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(returnedCacheMap)))
      val result = TestSave4LaterConnector.fetchAll(testUtr)

      await(result).get.toString should include("BC_Business_Details")
      await(result).get.toString should include("Supplier")
    }

    "remove everything from save4later" in {
      TestSave4LaterConnector.removeAll(Matchers.any())(Matchers.any())
      verify(mockShortLivedCache, times(1)).remove(Matchers.any())(Matchers.any())
    }

  }
}
