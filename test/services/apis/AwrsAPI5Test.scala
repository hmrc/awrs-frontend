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

package services.apis

import builders.AuthBuilder
import models.{AWRSFEModel, BusinessType}
import play.api.test.FakeRequest
import services.apis.mocks.MockAwrsAPI5
import utils.AwrsTestJson._
import utils.AwrsUnitTestTraits
import utils.TestConstants._
import utils.TestUtil._

class AwrsAPI5Test extends AwrsUnitTestTraits
  with MockAwrsAPI5 {

  implicit val request = FakeRequest()


  "API 5" should {

    "Return the data from Cache if it is already cached" in {

      val feModel = api5LTDJson.as[AWRSFEModel]

      val businessType = BusinessType(legalEntity = Option("LTD"), None, Some(true))

      setupMockAWRSConnector()

      setupMockSave4LaterServiceWithOnly(fetchBusinessType = businessType)
      setupMockApiSave4LaterServiceWithOnly(fetchSubscriptionTypeFrontEnd = feModel.subscriptionTypeFrontEnd)

      await(TestAPI5.retrieveApplication)

      verifyAWRSConnector(lookupAWRSData = 0)
    }


    "Return the data from lookup if it is not cached" in {

      setupMockAWRSConnector(lookupAWRSData = api5LTDJson)

      setupMockSave4LaterServiceWithOnly(
        fetchBusinessType = None,
        fetchBusinessCustomerDetails = testBusinessCustomerDetails("SOP")
      )
      setupMockApiSave4LaterServiceWithOnly(fetchSubscriptionTypeFrontEnd = None)

      await(TestAPI5.retrieveApplication)

      verifyAWRSConnector(lookupAWRSData = 1)
    }


    "save Limited Company data to save4later" in {

      val feModel = api5LTDJson.as[AWRSFEModel]

      setupMockSave4LaterServiceWithOnly(
        fetchBusinessCustomerDetails = testBusinessCustomerDetails("LTD")
      )

      val result = TestAPI5.saveReturnedApplication(feModel)

      await(result) shouldBe feModel.subscriptionTypeFrontEnd
      await(result).legalEntity.get.legalEntity shouldBe Some("LTD")

      verifySave4LaterService(
        saveBusinessDetails = 1,
        savePlaceOfBusiness = 1,
        saveBusinessContacts = 1,
        saveBusinessDirectors = 1,
        saveAdditionalBusinessPremisesList = 1,
        saveApplicationDeclaration = 1,
        saveTradingActivity = 1,
        saveProducts = 1,
        savePartnerDetails = 0,
        saveGroupDeclaration = 0,
        saveGroupMemberDetails = 0
      )
    }

    "save Sole Trader data to save4later" in {

      val feModel = api5SoleTraderJson.as[AWRSFEModel]

      setupMockSave4LaterServiceWithOnly(
        fetchBusinessCustomerDetails = testBusinessCustomerDetails("SOP")
      )

      val result = TestAPI5.saveReturnedApplication(feModel)

      await(result) shouldBe feModel.subscriptionTypeFrontEnd
      await(result).legalEntity.get.legalEntity shouldBe Some("SOP")

      verifySave4LaterService(
        saveBusinessDetails = 1,
        savePlaceOfBusiness = 1,
        saveBusinessContacts = 1,
        saveBusinessDirectors = 0,
        saveAdditionalBusinessPremisesList = 1,
        saveApplicationDeclaration = 1,
        saveTradingActivity = 1,
        saveProducts = 1,
        savePartnerDetails = 0,
        saveGroupDeclaration = 0,
        saveGroupMemberDetails = 0
      )
    }

    "save Partner data to save4later" in {

      val feModel = api5PartnerJson.as[AWRSFEModel]

      setupMockSave4LaterServiceWithOnly(
        fetchBusinessCustomerDetails = testBusinessCustomerDetails("Partnership")
      )

      val result = TestAPI5.saveReturnedApplication(feModel)

      await(result) shouldBe feModel.subscriptionTypeFrontEnd
      await(result).legalEntity.get.legalEntity shouldBe Some("Partnership")

      verifySave4LaterService(
        saveBusinessDetails = 1,
        savePlaceOfBusiness = 1,
        saveBusinessContacts = 1,
        saveBusinessDirectors = 0,
        saveAdditionalBusinessPremisesList = 1,
        saveApplicationDeclaration = 1,
        saveTradingActivity = 1,
        saveProducts = 1,
        savePartnerDetails = 1,
        saveGroupDeclaration = 0,
        saveGroupMemberDetails = 0
      )
    }

    "save LLP data to save4later" in {

      val feModel = api5LLPJson.as[AWRSFEModel]

      setupMockSave4LaterServiceWithOnly(
        fetchBusinessCustomerDetails = testBusinessCustomerDetails("LLP")
      )

      val result = TestAPI5.saveReturnedApplication(feModel)

      await(result) shouldBe feModel.subscriptionTypeFrontEnd
      await(result).legalEntity.get.legalEntity shouldBe Some("LLP")

      verifySave4LaterService(
        saveBusinessDetails = 1,
        savePlaceOfBusiness = 1,
        saveBusinessContacts = 1,
        saveBusinessDirectors = 0,
        saveAdditionalBusinessPremisesList = 1,
        saveApplicationDeclaration = 1,
        saveTradingActivity = 1,
        saveProducts = 1,
        savePartnerDetails = 1,
        saveGroupDeclaration = 0,
        saveGroupMemberDetails = 0
      )
    }

    "save LLP Group data to save4later" in {

      val feModel = api5LLPGRPJson.as[AWRSFEModel]

      setupMockSave4LaterServiceWithOnly(
        fetchBusinessCustomerDetails = testBusinessCustomerDetails("LLP_GRP")
      )

      val result = TestAPI5.saveReturnedApplication(feModel)

      await(result) shouldBe feModel.subscriptionTypeFrontEnd
      await(result).legalEntity.get.legalEntity shouldBe Some("LLP_GRP")

      verifySave4LaterService(
        saveBusinessDetails = 1,
        savePlaceOfBusiness = 1,
        saveBusinessContacts = 1,
        saveBusinessDirectors = 0,
        saveAdditionalBusinessPremisesList = 1,
        saveApplicationDeclaration = 1,
        saveTradingActivity = 1,
        saveProducts = 1,
        savePartnerDetails = 1,
        saveGroupDeclaration = 1,
        saveGroupMemberDetails = 1
      )
    }

    "save LTD Group data to save4later" in {

      val feModel = api5LTDGRPJson.as[AWRSFEModel]

      setupMockSave4LaterServiceWithOnly(
        fetchBusinessCustomerDetails = testBusinessCustomerDetails("LTD_GRP")
      )

      val result = TestAPI5.saveReturnedApplication(feModel)

      await(result) shouldBe feModel.subscriptionTypeFrontEnd
      await(result).legalEntity.get.legalEntity shouldBe Some("LTD_GRP")

      verifySave4LaterService(
        saveBusinessDetails = 1,
        savePlaceOfBusiness = 1,
        saveBusinessContacts = 1,
        saveBusinessDirectors = 1,
        saveAdditionalBusinessPremisesList = 1,
        saveApplicationDeclaration = 1,
        saveTradingActivity = 1,
        saveProducts = 1,
        savePartnerDetails = 0,
        saveGroupDeclaration = 1,
        saveGroupMemberDetails = 1
      )
    }

  }
}
