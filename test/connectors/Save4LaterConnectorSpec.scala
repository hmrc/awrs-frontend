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

package connectors

import models._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.http.Status
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.cache.client.ShortLivedCache
import utils.AwrsUnitTestTraits
import utils.TestConstants._
import utils.TestUtil._

import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class Save4LaterConnectorSpec extends AwrsUnitTestTraits {

  val mockShortLivedCache: ShortLivedCache = mock[ShortLivedCache]

  val testSave4LaterConnector: Save4LaterConnector = new Save4LaterConnector() {
    override val shortLivedCache: ShortLivedCache = mockShortLivedCache
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockShortLivedCache)
  }

  "ShortLivedCache" must {
    "fetch saved BusinessDetails from save4later" in {
      val key = "Business Details"
      when(mockShortLivedCache.fetchAndGetEntry[BusinessCustomerDetails](any(), ArgumentMatchers.eq(key))(any(), any(), any())).thenReturn(Future.successful(Some(reviewDetails)))
      val result = testSave4LaterConnector.fetchData4Later[BusinessCustomerDetails](testUtr, key)
      await(result) mustBe Some(reviewDetails)
    }

    "save business details into save4later" in {
      val key = "BC_Business_Details"
      when(mockShortLivedCache.cache(any(), ArgumentMatchers.eq(key), any())(any(), any(), any())).thenReturn(Future.successful(returnedCacheMap))
      val result = testSave4LaterConnector.saveData4Later(testUtr, key, reviewDetails)
      await(result).get mustBe reviewDetails
    }

    "fetch all data from save4later by utr" in {
      when(mockShortLivedCache.fetch(any())(any(), any())).thenReturn(Future.successful(Some(returnedCacheMap)))
      val result = testSave4LaterConnector.fetchAll(testUtr)

      await(result).get.toString must include("BC_Business_Details")
      await(result).get.toString must include("Supplier")
    }

    "remove everything from save4later" in {
      when(mockShortLivedCache.remove(any())(any(), any())).thenReturn(Future.successful(HttpResponse.apply(Status.OK, "")))

      val result = testSave4LaterConnector.removeAll("TEST")

      await(result) mustBe ()
      verify(mockShortLivedCache, times(1)).remove(ArgumentMatchers.eq("TEST"))(any(),any())
    }

  }
}
