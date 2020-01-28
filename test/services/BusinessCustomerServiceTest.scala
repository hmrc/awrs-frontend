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

import _root_.models.{BCAddress, BusinessCustomerDetails}
import connectors._
import connectors.mock.MockKeyStoreConnector
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import utils.AwrsUnitTestTraits

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class BusinessCustomerServiceTest extends AwrsUnitTestTraits
  with MockKeyStoreConnector {

  val testAddress = BCAddress("line_1", "line_2", None, None, None, Some("U.K."))
  val testReviewBusinessDetails = BusinessCustomerDetails("ACME", Some("SOP"), BCAddress("line1", "line2", Option("line3"), Option("line4"), Option("postcode"), Option("country")), "sap123", "safe123", false, Some("agent123"))
  val mockBusCusCacheConnector: BusinessCustomerDataCacheConnector = mock[BusinessCustomerDataCacheConnector]
  val businessCustomerTest = new BusinessCustomerService(mockBusCusCacheConnector)

  "fetch review details, if found from Business Customer Keystore" in {
    when(mockBusCusCacheConnector.fetchDataFromKeystore[BusinessCustomerDetails](any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(testReviewBusinessDetails)))

    val result = businessCustomerTest.getReviewBusinessDetails[BusinessCustomerDetails]
    await(result).get shouldBe testReviewBusinessDetails
  }

  "return None, if no details are found in BC keystore" in {
    when(mockBusCusCacheConnector.fetchDataFromKeystore[BusinessCustomerDetails](any())(any(), any(), any()))
      .thenReturn(Future.successful(None))

    val result = businessCustomerTest.getReviewBusinessDetails[BusinessCustomerDetails]
    await(result) shouldBe None
  }
}
