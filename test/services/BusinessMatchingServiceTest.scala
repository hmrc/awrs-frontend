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

import connectors._
import forms.AWRSEnums
import models._
import models.reenrolment.AwrsRegisteredPostcode
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

  "isValidUTRandPostCode" must {
    "validate a UTR and PostCode is match for an individual" in {
      when(mockBusinessMatchingConnector.lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(matchSuccessResponseJsonWithPostCode("WD6 5DR")))
      val result = businessMatchingServiceTest.verifyUTRandPostCode(testUtr, AwrsRegisteredPostcode("WD65dr"), TestUtil.defaultAuthRetrieval, true)
      await(result) mustBe true
    }
    "false when UTR does not match" in {
      when(mockBusinessMatchingConnector.lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(matchFailureResponseJson))
      val result = businessMatchingServiceTest.verifyUTRandPostCode(testUtr, AwrsRegisteredPostcode("WD65dr"), TestUtil.defaultAuthRetrieval, true)
      await(result) mustBe false
    }
    "false when PstCode does not match" in {
      when(mockBusinessMatchingConnector.lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(matchSuccessResponseJsonWithPostCode("WD6 5XX")))
      val result = businessMatchingServiceTest.verifyUTRandPostCode(testUtr, AwrsRegisteredPostcode("WD65dr"), TestUtil.defaultAuthRetrieval, true)
      await(result) mustBe false
    }
  }

  "Business Matching Services" must {

    "validate a UTR is correct by business matching" in {
      when(mockBusinessMatchingConnector.lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(matchSuccessResponseJson))
      val result = businessMatchingServiceTest.matchBusinessWithUTR(testUtr, Some(Organisation("Acme", AWRSEnums.CorporateBodyString)), TestUtil.defaultAuthRetrieval)
      await(result) mustBe true
    }

    "validate a UTR is incorrect by business matching" in {
      when(mockBusinessMatchingConnector.lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(matchFailureResponseJson))
      val result = businessMatchingServiceTest.matchBusinessWithUTR(testUtr, Some(Organisation("Acme", AWRSEnums.CorporateBodyString)), TestUtil.defaultAuthRetrieval)
      await(result) mustBe false
    }
  }

  "isValidMatchedGroupUtr" must {
    Seq("LTD_GRP", "LLP_GRP") foreach { groupType =>
      s"validate a $groupType utr" in {
        lazy val testBCAddress = BCAddress("addressLine1", "addressLine2", Option("addressLine3"), Option("addressLine4"), Option(testPostcode), Option("country"))
        lazy val testBusinessCustomer = BusinessCustomerDetails("ACME", Some("LTD_GRP"), testBCAddress, "sap123", "safe123", true, Some("agent123"))

        when(mockMainStoreSave4LaterConnector.fetchData4Later[BusinessCustomerDetails](any(), ArgumentMatchers.eq("businessCustomerDetails"))(any(), any()))
          .thenReturn(Future.successful(Option(testBusinessCustomer)))
        when(mockMainStoreSave4LaterConnector.fetchData4Later[BusinessType](any(), ArgumentMatchers.eq("legalEntity"))(any(), any()))
          .thenReturn(Future.successful(Option(BusinessType(Some(groupType), None, None))))
        when(mockBusinessMatchingConnector.lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(matchSuccessResponseJson))

        val result = businessMatchingServiceTest.isValidMatchedGroupUtr(testUtr, TestUtil.defaultAuthRetrieval)
        await(result) mustBe true
      }
    }

    "fail to validate an invalid group type utr" in {
      lazy val testBCAddress = BCAddress("addressLine1", "addressLine2", Option("addressLine3"), Option("addressLine4"), Option(testPostcode), Option("country"))
      lazy val testBusinessCustomer = BusinessCustomerDetails("ACME", Some("LTD_GRP"), testBCAddress, "sap123", "safe123", true, Some("agent123"))

      when(mockMainStoreSave4LaterConnector.fetchData4Later[BusinessCustomerDetails](any(), ArgumentMatchers.eq("businessCustomerDetails"))(any(), any()))
        .thenReturn(Future.successful(Option(testBusinessCustomer)))
      when(mockMainStoreSave4LaterConnector.fetchData4Later[BusinessType](any(), ArgumentMatchers.eq("legalEntity"))(any(), any()))
        .thenReturn(Future.successful(Option(BusinessType(Some("INVALID"), None, None))))
      when(mockBusinessMatchingConnector.lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(matchSuccessResponseJson))

      val result = businessMatchingServiceTest.isValidMatchedGroupUtr(testUtr, TestUtil.defaultAuthRetrieval)
      intercept[Exception](await(result) mustBe true)
    }

    "fail to validate not a group" in {
      lazy val testBCAddress = BCAddress("addressLine1", "addressLine2", Option("addressLine3"), Option("addressLine4"), Option(testPostcode), Option("country"))
      lazy val testBusinessCustomer = BusinessCustomerDetails("ACME", Some("LTD_GRP"), testBCAddress, "sap123", "safe123", false, Some("agent123"))

      when(mockMainStoreSave4LaterConnector.fetchData4Later[BusinessCustomerDetails](any(), ArgumentMatchers.eq("businessCustomerDetails"))(any(),  any()))
        .thenReturn(Future.successful(Option(testBusinessCustomer)))
      when(mockMainStoreSave4LaterConnector.fetchData4Later[BusinessType](any(), ArgumentMatchers.eq("legalEntity"))(any(), any()))
        .thenReturn(Future.successful(Option(BusinessType(Some("INVALID"), None, None))))
      when(mockBusinessMatchingConnector.lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(matchSuccessResponseJson))

      val result = businessMatchingServiceTest.isValidMatchedGroupUtr(testUtr, TestUtil.defaultAuthRetrieval)
      intercept[Exception](await(result) mustBe true)
    }
  }
}
