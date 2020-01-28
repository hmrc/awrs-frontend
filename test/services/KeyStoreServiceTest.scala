/*
 * Copyright 2020 HM Revenue & Customs
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
import connectors.mock.MockAuthConnector
import services.mocks.MockKeyStoreService
import utils.TestUtil._

import scala.concurrent.ExecutionContext.Implicits.global

class KeyStoreServiceTest extends MockKeyStoreService with MockAuthConnector {

  import MockKeyStoreService._

  import FetchNoneType._

  "KeyStore service functions and deleteDeRegistrationDate" should {

    "saveDeRegistrationDate" in {
      setupMockKeyStoreServiceForDeRegistrationOrWithdrawal()
      val result = testKeyStoreService.saveDeRegistrationDate(defaultDeRegistrationDateData)
      await(result) shouldBe returnedCacheMap

      val resultDelete = testKeyStoreService.deleteDeRegistrationDate
      await(resultDelete) shouldBe returnedCacheMap

      // this also tests the implementation of differentiating save and delete calls
      verifyKeyStoreService(
        saveDeRegistrationDate = 1,
        deleteDeRegistrationDate = 1)
    }

    "fetchDeRegistrationDate" in {
      setupMockKeyStoreServiceForDeRegistrationOrWithdrawal()
      val result = testKeyStoreService.fetchDeRegistrationDate
      await(result) shouldBe defaultDeRegistrationDate

      setupMockKeyStoreServiceForDeRegistrationOrWithdrawal(haveDeRegDate = false)
      val resultNoData = testKeyStoreService.fetchDeRegistrationDate
      await(resultNoData) shouldBe None

      setupMockKeyStoreServiceForDeRegistrationOrWithdrawal(haveDeRegDate = false, fetchNoneType = DeletedData)
      val resultDeletedData = testKeyStoreService.fetchDeRegistrationDate
      await(resultDeletedData) shouldBe None

      verifyKeyStoreService(fetchDeRegistrationDate = 3)
    }

    "saveDeRegistrationReason and deleteDeRegistrationReason" in {
      setupMockKeyStoreServiceForDeRegistrationOrWithdrawal()
      val result = testKeyStoreService.saveDeRegistrationReason(defaultDeRegistrationReasonData)
      await(result) shouldBe returnedCacheMap

      val resultDelete = testKeyStoreService.deleteDeRegistrationReason
      await(resultDelete) shouldBe returnedCacheMap

      // this also tests the implementation of differentiating save and delete calls
      verifyKeyStoreService(
        saveDeRegistrationReason = 1,
        deleteDeRegistrationReason = 1)
    }

    "fetchDeRegistrationReason" in {
      setupMockKeyStoreServiceForDeRegistrationOrWithdrawal()
      val result = testKeyStoreService.fetchDeRegistrationReason
      await(result) shouldBe defaultDeRegistrationReason

      setupMockKeyStoreServiceForDeRegistrationOrWithdrawal(haveDeRegReason = false)
      val resultNoData = testKeyStoreService.fetchDeRegistrationReason
      await(resultNoData) shouldBe None

      setupMockKeyStoreServiceForDeRegistrationOrWithdrawal(haveDeRegReason = false, fetchNoneType = DeletedData)
      val resultDeletedData = testKeyStoreService.fetchDeRegistrationReason
      await(resultDeletedData) shouldBe None

      verifyKeyStoreService(fetchDeRegistrationReason = 3)
    }

    "fetchBusinessCustomerAddress" in {
      setupMockKeyStoreServiceForBusinessCustomerAddress()
      val result = testKeyStoreService.fetchBusinessCustomerAddresss
      await(result) shouldBe Some(defaultBCAddressApi3)
    }
  }

}
