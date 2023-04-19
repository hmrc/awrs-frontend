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

import connectors.{AWRSConnector, Save4LaterConnector}
import models._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import utils.AccountUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckEtmpServiceTest extends PlaySpec with MockitoSugar with BeforeAndAfterEach {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  implicit val req: Request[AnyContent] = FakeRequest()

  val mockAwrsConnector: AWRSConnector = mock[AWRSConnector]
  val mockSave4LaterService: Save4LaterService = mock[Save4LaterService]
  val mockEnrolService: EnrolService = mock[EnrolService]
  val mockSave4LaterConnector: Save4LaterConnector = mock[Save4LaterConnector]
  val mockAccountUtils: AccountUtils = mock[AccountUtils]
  val testBusinessCustomerDetails: BusinessCustomerDetails = BusinessCustomerDetails("ACME", Some("SOP"), BCAddress("line1", "line2", Option("line3"), Option("line4"), Option("postcode"), Option("country")), "sap123", "safe123", false, Some("agent123"))
  val testBusinessRegistrationDetails: BusinessRegistrationDetails = BusinessRegistrationDetails(Some("SOP"), None, Some("1234"))
  val checkEtmpTest = new CheckEtmpService(mockAwrsConnector, mockEnrolService)

  override def beforeEach(): Unit = {
    reset(mockAwrsConnector,
      mockSave4LaterConnector,
      mockAccountUtils,
      mockSave4LaterService)

    super.beforeEach()
  }

  "validateBusinessDetails" must {
    val enrolSuccessResponse = EnrolResponse("serviceName", "state", identifiers = List(Identifier("AWRS", "AWRS_Ref_No")))

    "return true if all details are provided" in {
      when(mockAwrsConnector.checkEtmp(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(SelfHealSubscriptionResponse("123456"))))
      when(mockEnrolService.enrolAWRS(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(enrolSuccessResponse)))

      val result = checkEtmpTest.validateBusinessDetails(testBusinessCustomerDetails, "SOP")

      await(result) mustBe true
    }

    "return false if enrol AWRS ES8 fails" in {
      when(mockAwrsConnector.checkEtmp(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(SelfHealSubscriptionResponse("123456"))))
      when(mockEnrolService.enrolAWRS(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      val result = checkEtmpTest.validateBusinessDetails(testBusinessCustomerDetails, "SOP")

      await(result) mustBe false
    }

    "return false if all details are provided but checkEtmp returns false" in {
      when(mockAwrsConnector.checkEtmp(any(), any())(any(), any()))
        .thenReturn(Future.successful(None))
      val result = checkEtmpTest.validateBusinessDetails(testBusinessCustomerDetails, "SOP")

      await(result) mustBe false
    }
  }
}
