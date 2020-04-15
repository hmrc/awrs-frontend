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

import connectors._
import forms.AWRSEnums
import models._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import services.mocks.{MockKeyStoreService, MockSave4LaterService}
import utils.AwrsTestJson._
import utils.{AwrsUnitTestTraits, TestUtil}
import utils.TestConstants.{testPostcode, testUtr}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


class BusinessMatchingServiceTest extends AwrsUnitTestTraits with MockKeyStoreService with MockSave4LaterService {

  val mockBusinessMatchingConnector: BusinessMatchingConnector = mock[BusinessMatchingConnector]
  val businessMatchingServiceTest: BusinessMatchingService = new BusinessMatchingService(testKeyStoreService, mockBusinessMatchingConnector, testSave4LaterService, mockAuditable)

  "Business Matching Services" should {

    "validate a UTR is correct by business matching" in {
      when(mockBusinessMatchingConnector.lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(matchSuccessResponseJson))
      val result = businessMatchingServiceTest.matchBusinessWithUTR(testUtr, Some(Organisation("Acme", AWRSEnums.CorporateBodyString)), TestUtil.defaultAuthRetrieval)
      await(result) shouldBe true
    }

    "validate a UTR is incorrect by business matching" in {
      when(mockBusinessMatchingConnector.lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(matchFailureResponseJson))
      val result = businessMatchingServiceTest.matchBusinessWithUTR(testUtr, Some(Organisation("Acme", AWRSEnums.CorporateBodyString)), TestUtil.defaultAuthRetrieval)
      await(result) shouldBe false
    }
  }

  "isValidMatchedGroupUtr" should {
    Seq("LTD_GRP", "LLP_GRP") foreach { groupType =>
      s"validate a $groupType utr" in {
        lazy val testBCAddress = BCAddress("addressLine1", "addressLine2", Option("addressLine3"), Option("addressLine4"), Option(testPostcode), Option("country"))
        lazy val testBusinessCustomer = BusinessCustomerDetails("ACME", Some("LTD_GRP"), testBCAddress, "sap123", "safe123", true, Some("agent123"))

        when(mockMainStoreSave4LaterConnector.fetchData4Later[BusinessCustomerDetails](any(), ArgumentMatchers.eq("businessCustomerDetails"))(any(), any(), any()))
          .thenReturn(Future.successful(Option(testBusinessCustomer)))
        when(mockMainStoreSave4LaterConnector.fetchData4Later[BusinessType](any(), ArgumentMatchers.eq("legalEntity"))(any(), any(), any()))
          .thenReturn(Future.successful(Option(BusinessType(Some(groupType), None, None))))
        when(mockBusinessMatchingConnector.lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(matchSuccessResponseJson))

        val result = businessMatchingServiceTest.isValidMatchedGroupUtr(testUtr, TestUtil.defaultAuthRetrieval)
        await(result) shouldBe true
      }
    }

    "fail to validate an invalid group type utr" in {
      lazy val testBCAddress = BCAddress("addressLine1", "addressLine2", Option("addressLine3"), Option("addressLine4"), Option(testPostcode), Option("country"))
      lazy val testBusinessCustomer = BusinessCustomerDetails("ACME", Some("LTD_GRP"), testBCAddress, "sap123", "safe123", true, Some("agent123"))

      when(mockMainStoreSave4LaterConnector.fetchData4Later[BusinessCustomerDetails](any(), ArgumentMatchers.eq("businessCustomerDetails"))(any(), any(), any()))
        .thenReturn(Future.successful(Option(testBusinessCustomer)))
      when(mockMainStoreSave4LaterConnector.fetchData4Later[BusinessType](any(), ArgumentMatchers.eq("legalEntity"))(any(), any(), any()))
        .thenReturn(Future.successful(Option(BusinessType(Some("INVALID"), None, None))))
      when(mockBusinessMatchingConnector.lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(matchSuccessResponseJson))

      val result = businessMatchingServiceTest.isValidMatchedGroupUtr(testUtr, TestUtil.defaultAuthRetrieval)
      intercept[Exception](await(result) shouldBe true)
    }

    "fail to validate not a group" in {
      lazy val testBCAddress = BCAddress("addressLine1", "addressLine2", Option("addressLine3"), Option("addressLine4"), Option(testPostcode), Option("country"))
      lazy val testBusinessCustomer = BusinessCustomerDetails("ACME", Some("LTD_GRP"), testBCAddress, "sap123", "safe123", false, Some("agent123"))

      when(mockMainStoreSave4LaterConnector.fetchData4Later[BusinessCustomerDetails](any(), ArgumentMatchers.eq("businessCustomerDetails"))(any(), any(), any()))
        .thenReturn(Future.successful(Option(testBusinessCustomer)))
      when(mockMainStoreSave4LaterConnector.fetchData4Later[BusinessType](any(), ArgumentMatchers.eq("legalEntity"))(any(), any(), any()))
        .thenReturn(Future.successful(Option(BusinessType(Some("INVALID"), None, None))))
      when(mockBusinessMatchingConnector.lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(matchSuccessResponseJson))

      val result = businessMatchingServiceTest.isValidMatchedGroupUtr(testUtr, TestUtil.defaultAuthRetrieval)
      intercept[Exception](await(result) shouldBe true)
    }
  }
}
