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

package services

import models._
import org.mockito.Mockito._
import play.api.libs.json.Format
import repositories.CacheRepository
import uk.gov.hmrc.mongo.cache.DataKey
import utils.AwrsUnitTestTraits
import utils.TestConstants._
import utils.TestUtil._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Save4LaterCacheServiceSpec extends AwrsUnitTestTraits {

  implicit val format: Format[BusinessCustomerDetails] = BusinessCustomerDetails.formats

  val mockShortLivedCacheRepo: CacheRepository = mock[CacheRepository]

  val testSave4LaterCacheService: Save4LaterCacheService = new Save4LaterCacheService() {
    override val cacheRepository: CacheRepository = mockShortLivedCacheRepo
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockShortLivedCacheRepo)
  }

  "ShortLivedCache" must {
    "fetch saved BusinessDetails from save4later" in {
      val key = "Business Details"
      when(mockShortLivedCacheRepo.get[BusinessCustomerDetails](testUtr, DataKey[BusinessCustomerDetails](key)))
        .thenReturn(Future.successful(Some(reviewDetails)))

      val result = testSave4LaterCacheService.fetchData4Later[BusinessCustomerDetails](testUtr, key)
      await(result) mustBe Some(reviewDetails)
    }

    "save business details into save4later" in {
      val key = "BC_Business_Details"
      when(mockShortLivedCacheRepo.put[BusinessCustomerDetails](testUtr, DataKey[BusinessCustomerDetails](key), reviewDetails))
        .thenReturn(Future.successful(reviewDetails))

      val result = testSave4LaterCacheService.saveData4Later(testUtr, key, reviewDetails)
      await(result).get mustBe reviewDetails
    }

    "fetch all data from save4later by utr" in {
      when(mockShortLivedCacheRepo.findById(testUtr))
        .thenReturn(Future.successful(Some(testCacheItem)))

      val result = testSave4LaterCacheService.fetchAll(testUtr)

      await(result).get.toString must include("BC_Business_Details")
      await(result).get.toString must include("Supplier")
    }

    "remove everything from save4later" in {
      when(mockShortLivedCacheRepo.deleteById("TEST"))
        .thenReturn(Future.successful(()))

      val result = testSave4LaterCacheService.removeAll("TEST")
      await(result) mustBe (())
    }

  }
}
