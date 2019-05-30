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

package services

import _root_.models._
import services.mocks.MockSave4LaterService
import utils.{AwrsUnitTestTraits, TestUtil}
import utils.TestUtil._

class Save4LaterServiceTest extends AwrsUnitTestTraits
  with MockSave4LaterService {

  val sourceId: String = "AWRS"
  val testReviewBusinessDetails = BusinessCustomerDetails("ACME", Some("SOP"), BCAddress("line1", "line2", Option("line3"), Option("line4"), Option("postcode"), Option("country")), "sap123", "safe123", false, Some("agent123"))

  // this section of unit test tests the named save4later methods implemented for Save4later Service's main store
  "Save4later service functions" should {

    "business type" in {
      val data = testLegalEntity
      setupMockSave4LaterServiceWithOnly(fetchBusinessType = data)
      // the save function is also mocked by the above setup
      val saveResult = TestSave4LaterService.mainStore.saveBusinessType(data, TestUtil.defaultAuthRetrieval)
      await(saveResult) shouldBe data

      val fetchResult = TestSave4LaterService.mainStore.fetchBusinessType(TestUtil.defaultAuthRetrieval)
      await(fetchResult) shouldBe Some(data)
    }

    "business customer details" in {
      val data = testBusinessCustomerDetails("SOP")
      setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = data)

      val saveResult = TestSave4LaterService.mainStore.saveBusinessCustomerDetails(TestUtil.defaultAuthRetrieval, data)
      await(saveResult) shouldBe data

      val fetchResult = TestSave4LaterService.mainStore.fetchBusinessCustomerDetails(TestUtil.defaultAuthRetrieval)
      await(fetchResult) shouldBe Some(data)
    }

    "new application type" in {
      val data = testNewApplicationType
      setupMockSave4LaterServiceWithOnly(fetchNewApplicationType = data)

      val saveResult = TestSave4LaterService.mainStore.saveNewApplicationType(data, TestUtil.defaultAuthRetrieval)
      await(saveResult) shouldBe data

      val fetchResult = TestSave4LaterService.mainStore.fetchNewApplicationType(TestUtil.defaultAuthRetrieval)
      await(fetchResult) shouldBe Some(data)
    }

    "group member details" in {
      val data = testGroupMemberDetails
      setupMockSave4LaterServiceWithOnly(fetchGroupMemberDetails = data)

      val saveResult = TestSave4LaterService.mainStore.saveGroupMembers(TestUtil.defaultAuthRetrieval, data)
      await(saveResult) shouldBe data

      val fetchResult = TestSave4LaterService.mainStore.fetchGroupMembers(TestUtil.defaultAuthRetrieval)
      await(fetchResult) shouldBe Some(data)
    }

    "group declaration" in {
      val data = testGroupDeclaration
      setupMockSave4LaterServiceWithOnly(fetchGroupDeclaration = data)

      val saveResult = TestSave4LaterService.mainStore.saveGroupDeclaration(TestUtil.defaultAuthRetrieval, data)
      await(saveResult) shouldBe data

      val fetchResult = TestSave4LaterService.mainStore.fetchGroupDeclaration(TestUtil.defaultAuthRetrieval)
      await(fetchResult) shouldBe Some(data)
    }

    "business directors" in {
      val data = BusinessDirectors(List(testBusinessDirector))
      setupMockSave4LaterServiceWithOnly(fetchBusinessDirectors = data)

      val saveResult = TestSave4LaterService.mainStore.saveBusinessDirectors(TestUtil.defaultAuthRetrieval, data)
      await(saveResult) shouldBe data

      val fetchResult = TestSave4LaterService.mainStore.fetchBusinessDirectors(TestUtil.defaultAuthRetrieval)
      await(fetchResult) shouldBe Some(data)
    }

    "partner details" in {
      val data = testPartnerDetails
      setupMockSave4LaterServiceWithOnly(fetchPartnerDetails = data)

      val saveResult = TestSave4LaterService.mainStore.savePartnerDetails(TestUtil.defaultAuthRetrieval, data)
      await(saveResult) shouldBe data

      val fetchResult = TestSave4LaterService.mainStore.fetchPartnerDetails(TestUtil.defaultAuthRetrieval)
      await(fetchResult) shouldBe Some(data)
    }

    "additional premises list" in {
      val data = testAdditionalPremisesList
      setupMockSave4LaterServiceWithOnly(fetchAdditionalBusinessPremisesList = data)

      val saveResult = TestSave4LaterService.mainStore.saveAdditionalBusinessPremisesList(TestUtil.defaultAuthRetrieval, data)
      await(saveResult) shouldBe data

      val fetchResult = TestSave4LaterService.mainStore.fetchAdditionalBusinessPremisesList(TestUtil.defaultAuthRetrieval)
      await(fetchResult) shouldBe Some(data)
    }

    "trading activity" in {
      val data = testTradingActivity()
      setupMockSave4LaterServiceWithOnly(fetchTradingActivity = data)

      val saveResult = TestSave4LaterService.mainStore.saveTradingActivity(TestUtil.defaultAuthRetrieval, data)
      await(saveResult) shouldBe data

      val fetchResult = TestSave4LaterService.mainStore.fetchTradingActivity(TestUtil.defaultAuthRetrieval)
      await(fetchResult) shouldBe Some(data)
    }

    "products" in {
      val data = testProducts()
      setupMockSave4LaterServiceWithOnly(fetchProducts = data)

      val saveResult = TestSave4LaterService.mainStore.saveProducts(TestUtil.defaultAuthRetrieval, data)
      await(saveResult) shouldBe data

      val fetchResult = TestSave4LaterService.mainStore.fetchProducts(TestUtil.defaultAuthRetrieval)
      await(fetchResult) shouldBe Some(data)
    }

    "suppliers" in {
      val data = testSuppliers
      setupMockSave4LaterServiceWithOnly(fetchSuppliers = data)

      val saveResult = TestSave4LaterService.mainStore.saveSuppliers(TestUtil.defaultAuthRetrieval, data)
      await(saveResult) shouldBe data

      val fetchResult = TestSave4LaterService.mainStore.fetchSuppliers(TestUtil.defaultAuthRetrieval)
      await(fetchResult) shouldBe Some(data)
    }

    "application declaration" in {
      val data = testApplicationDeclaration
      setupMockSave4LaterServiceWithOnly(fetchApplicationDeclaration = data)

      val saveResult = TestSave4LaterService.mainStore.saveApplicationDeclaration(TestUtil.defaultAuthRetrieval, data)
      await(saveResult) shouldBe data

      val fetchResult = TestSave4LaterService.mainStore.fetchApplicationDeclaration(TestUtil.defaultAuthRetrieval)
      await(fetchResult) shouldBe Some(data)
    }

    "business details" in {
      val data = testBusinessDetails()
      setupMockSave4LaterServiceWithOnly(fetchBusinessDetails = data)

      val saveResult = TestSave4LaterService.mainStore.saveBusinessDetails(TestUtil.defaultAuthRetrieval, data)
      await(saveResult) shouldBe data

      val fetchResult = TestSave4LaterService.mainStore.fetchBusinessDetails(TestUtil.defaultAuthRetrieval)
      await(fetchResult) shouldBe Some(data)
    }

    "business contacts" in {
      val data = testBusinessContactsDefault()
      setupMockSave4LaterServiceWithOnly(fetchBusinessContacts = data)

      val saveResult = TestSave4LaterService.mainStore.saveBusinessContacts(TestUtil.defaultAuthRetrieval, data)
      await(saveResult) shouldBe data

      val fetchResult = TestSave4LaterService.mainStore.fetchBusinessContacts(TestUtil.defaultAuthRetrieval)
      await(fetchResult) shouldBe Some(data)
    }
  }

  // this section of unit test tests the named save4later methods implemented by Save4later's api5 store
  "Save4LaterService.api" should {

    "subscriptionTypeFrontEnd" in {
      val data = testSubscriptionTypeFrontEnd()
      setupMockApiSave4LaterServiceWithOnly(fetchSubscriptionTypeFrontEnd = data)
      // the save function is also mocked by the above setup
      val saveResult = TestSave4LaterService.api.saveSubscriptionTypeFrontEnd(data, TestUtil.defaultAuthRetrieval)
      await(saveResult) shouldBe data

      val fetchResult = TestSave4LaterService.api.fetchSubscriptionTypeFrontEnd(TestUtil.defaultAuthRetrieval)
      await(fetchResult) shouldBe Some(data)
    }

    "businessDetailsSupport when it is already stored" in {
      val data = testBusinessDetailsSupport(true)
      setupMockApiSave4LaterServiceWithOnly(fetchBusinessDetailsSupport = data)

      val saveResult = TestSave4LaterService.api.saveBusinessDetailsSupport(data, TestUtil.defaultAuthRetrieval)
      await(saveResult) shouldBe data

      val fetchResult = TestSave4LaterService.api.fetchBusinessDetailsSupport(TestUtil.defaultAuthRetrieval)
      await(fetchResult) shouldBe Some(data)
    }

    "businessDetailsSupport when it is not already stored but subscriptionTypeFrontEnd is" in {
      val subscriptionTypeFrontEnd = testSubscriptionTypeFrontEnd()
      val data = testBusinessDetailsSupport(subscriptionTypeFrontEnd)
      val cache: Option[BusinessDetailsSupport] = None

      setupMockApiSave4LaterServiceWithOnly(
        fetchSubscriptionTypeFrontEnd = subscriptionTypeFrontEnd,
        fetchBusinessDetailsSupport = cache
      )

      val fetchResult = TestSave4LaterService.api.fetchBusinessDetailsSupport(TestUtil.defaultAuthRetrieval)

      await(fetchResult) shouldBe Some(data)

      verifyApiSave4LaterService(
        fetchSubscriptionTypeFrontEnd = 1,
        fetchBusinessDetailsSupport = 1,
        saveBusinessDetailsSupport = 1
      )
    }


  }
}
