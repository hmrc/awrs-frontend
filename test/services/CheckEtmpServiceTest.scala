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

import connectors.{AWRSConnector, BusinessCustomerDataCacheConnector, Save4LaterConnector}
import models.{BCAddress, BusinessCustomerDetails, BusinessRegistrationDetails}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import utils.{AccountUtils, TestUtil}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckEtmpServiceTest extends UnitSpec with MockitoSugar with BeforeAndAfterEach with GuiceOneAppPerSuite {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockAwrsConnector: AWRSConnector = mock[AWRSConnector]
  val mockSave4LaterService: Save4LaterService = mock[Save4LaterService]
  val mockSave4LaterConnector: Save4LaterConnector = mock[Save4LaterConnector]
  val mockAccountUtils: AccountUtils = mock[AccountUtils]
  val testBusinessCustomerDetails = BusinessCustomerDetails("ACME", Some("SOP"), BCAddress("line1", "line2", Option("line3"), Option("line4"), Option("postcode"), Option("country")), "sap123", "safe123", false, Some("agent123"))
  val testBusinessRegistrationDetails = BusinessRegistrationDetails(Some("SOP"), None, Some("1234"))
  val checkEtmpTest = new CheckEtmpService(mockAwrsConnector, mockSave4LaterService)

  override def beforeEach(): Unit = {
    reset(mockAwrsConnector,
      mockSave4LaterConnector,
      mockAccountUtils,
      mockSave4LaterService)

    super.beforeEach()
  }

  "getRegistrationDetails" should {

    "fetch registration details, if found from Business Registration Keystore" in {
      when(mockSave4LaterConnector.fetchData4Later[BusinessRegistrationDetails](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(testBusinessRegistrationDetails)))
      val mockMainStore: MainStore = new MainStore(mockAccountUtils, mockSave4LaterConnector)
      when(mockSave4LaterService.mainStore)
        .thenReturn(mockMainStore)
      val result = checkEtmpTest.getRegistrationDetails(TestUtil.defaultAuthRetrieval)(hc, implicitly)

      await(result).get shouldBe testBusinessRegistrationDetails
    }
  }

  "validateBusinessDetails" should {
    val mockMainStore: MainStore = new MainStore(mockAccountUtils, mockSave4LaterConnector)

    "return true if all details are provided" in {

      when(mockSave4LaterConnector.fetchData4Later[BusinessRegistrationDetails](any(), ArgumentMatchers.eq("businessRegistrationDetails"))(any(), any(), any()))
        .thenReturn(Future.successful(Some(testBusinessRegistrationDetails)))
      when(mockAwrsConnector.checkEtmp(any(), any())(any(), any()))
        .thenReturn(Future.successful(true))
      when(mockSave4LaterService.mainStore).thenReturn(mockMainStore)
      val result = checkEtmpTest.validateBusinessDetails(TestUtil.defaultAuthRetrieval, testBusinessCustomerDetails)

      await(result) shouldBe true
    }

    "return false if business customer details only are provided" in {
      when(mockSave4LaterConnector.fetchData4Later[BusinessRegistrationDetails](any(), ArgumentMatchers.eq("businessRegistrationDetails"))(any(), any(), any()))
        .thenReturn(Future.successful(None))
      when(mockSave4LaterService.mainStore).thenReturn(mockMainStore)
      val result = checkEtmpTest.validateBusinessDetails(TestUtil.defaultAuthRetrieval, testBusinessCustomerDetails)

      await(result) shouldBe false
    }

    "return false if all details are provided but checkEtmp returns false" in {
      when(mockSave4LaterConnector.fetchData4Later[BusinessRegistrationDetails](any(), ArgumentMatchers.eq("businessRegistrationDetails"))(any(), any(), any()))
        .thenReturn(Future.successful(Some(testBusinessRegistrationDetails)))
      when(mockSave4LaterService.mainStore).thenReturn(mockMainStore)
      when(mockAwrsConnector.checkEtmp(any(), any())(any(), any()))
        .thenReturn(Future.successful(false))
      val result = checkEtmpTest.validateBusinessDetails(TestUtil.defaultAuthRetrieval, testBusinessCustomerDetails)

      await(result) shouldBe false
    }
  }
}
