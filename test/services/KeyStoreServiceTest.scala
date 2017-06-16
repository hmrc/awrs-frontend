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
import connectors.mock.MockAuthConnector
import models.BCAddressApi3
import services.mocks.MockKeyStoreService
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import utils.TestUtil._

class KeyStoreServiceTest extends MockKeyStoreService with MockAuthConnector {

  import MockKeyStoreService._

  import FetchNoneType._

  "KeyStore service functions and deleteDeRegistrationDate" should {

    "saveDeRegistrationDate" in {
      setupMockKeyStoreServiceForDeRegistrationOrWithdrawal()
      val result = TestKeyStoreService.saveDeRegistrationDate(defaultDeRegistrationDateData)
      await(result) shouldBe returnedCacheMap

      val resultDelete = TestKeyStoreService.deleteDeRegistrationDate
      await(resultDelete) shouldBe returnedCacheMap

      // this also tests the implementation of differentiating save and delete calls
      verifyKeyStoreService(
        saveDeRegistrationDate = 1,
        deleteDeRegistrationDate = 1)
    }

    "fetchDeRegistrationDate" in {
      setupMockKeyStoreServiceForDeRegistrationOrWithdrawal()
      val result = TestKeyStoreService.fetchDeRegistrationDate
      await(result) shouldBe defaultDeRegistrationDate

      setupMockKeyStoreServiceForDeRegistrationOrWithdrawal(haveDeRegDate = false)
      val resultNoData = TestKeyStoreService.fetchDeRegistrationDate
      await(resultNoData) shouldBe None

      setupMockKeyStoreServiceForDeRegistrationOrWithdrawal(haveDeRegDate = false, fetchNoneType = DeletedData)
      val resultDeletedData = TestKeyStoreService.fetchDeRegistrationDate
      await(resultDeletedData) shouldBe None

      verifyKeyStoreService(fetchDeRegistrationDate = 3)
    }

    "saveDeRegistrationReason and deleteDeRegistrationReason" in {
      setupMockKeyStoreServiceForDeRegistrationOrWithdrawal()
      val result = TestKeyStoreService.saveDeRegistrationReason(defaultDeRegistrationReasonData)
      await(result) shouldBe returnedCacheMap

      val resultDelete = TestKeyStoreService.deleteDeRegistrationReason
      await(resultDelete) shouldBe returnedCacheMap

      // this also tests the implementation of differentiating save and delete calls
      verifyKeyStoreService(
        saveDeRegistrationReason = 1,
        deleteDeRegistrationReason = 1)
    }

    "fetchDeRegistrationReason" in {
      setupMockKeyStoreServiceForDeRegistrationOrWithdrawal()
      val result = TestKeyStoreService.fetchDeRegistrationReason
      await(result) shouldBe defaultDeRegistrationReason

      setupMockKeyStoreServiceForDeRegistrationOrWithdrawal(haveDeRegReason = false)
      val resultNoData = TestKeyStoreService.fetchDeRegistrationReason
      await(resultNoData) shouldBe None

      setupMockKeyStoreServiceForDeRegistrationOrWithdrawal(haveDeRegReason = false, fetchNoneType = DeletedData)
      val resultDeletedData = TestKeyStoreService.fetchDeRegistrationReason
      await(resultDeletedData) shouldBe None

      verifyKeyStoreService(fetchDeRegistrationReason = 3)
    }

    "fetchBusinessCustomerAddress" in {
      setupMockKeyStoreServiceForBusinessCustomerAddress()
      val result = TestKeyStoreService.fetchBusinessCustomerAddresss
      await(result) shouldBe Some(defaultBCAddressApi3)
    }
  }

}
