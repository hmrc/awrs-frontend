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
import repositories.ShortLivedCacheRepository
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey}
import utils.AwrsUnitTestBase
import utils.TestConstants._
import utils.TestUtil._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Save4LaterConnectorSpec extends AwrsUnitTestBase {

  val mockShortLivedCacheRepository: ShortLivedCacheRepository = mock[ShortLivedCacheRepository]

  val testSave4LaterConnector: Save4LaterConnector = new Save4LaterConnector() {
    override val shortLivedCacheRepository: ShortLivedCacheRepository = mockShortLivedCacheRepository
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockShortLivedCacheRepository)
  }

  "ShortLivedCache" must {
    "fetch saved BusinessDetails from save4later" in {
      val key = "Business Details"
      when(mockShortLivedCacheRepository.fetchData4Later[BusinessCustomerDetails](any(), any())(any(), any())).thenReturn(Future.successful(Some(reviewDetails)))
      val result = testSave4LaterConnector.fetchData4Later[BusinessCustomerDetails](testUtr, key)
      await(result) mustBe Some(reviewDetails)
    }

    "save business details into save4later" in {
      val key = "BC_Business_Details"
      when(mockShortLivedCacheRepository.saveData4Later[BusinessCustomerDetails](any(), any(), any())(any(), any())).thenReturn(Future.successful(Some(reviewDetails)))
      val result = testSave4LaterConnector.saveData4Later(testUtr, key, reviewDetails)
      await(result).get mustBe reviewDetails
    }

    "fetch all data from save4later by utr" in {
      when(mockShortLivedCacheRepository.fetchAll(any())(any())).thenReturn(Future.successful(Some(testCacheItem)))
      val result = testSave4LaterConnector.fetchAll(testUtr)

      await(result).get.toString must include("BC_Business_Details")
      await(result).get.toString must include("Supplier")
    }

    "remove everything from save4later" in {
      when(mockShortLivedCacheRepository.removeAll(any())(any())).thenReturn(Future.successful(()))

      val result = testSave4LaterConnector.removeAll("TEST")

      await(result) mustBe ()
      verify(mockShortLivedCacheRepository, times(1)).removeAll(ArgumentMatchers.eq("TEST"))(any())
    }

  }
}
