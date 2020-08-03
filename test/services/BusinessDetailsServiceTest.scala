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

import connectors.{AWRSConnector, Save4LaterConnector}
import models._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import services.mocks.MockSave4LaterService
import uk.gov.hmrc.http.HeaderCarrier
import org.scalatestplus.play.PlaySpec
import utils.{AccountUtils, TestUtil}
import views.Configuration.{NewApplicationMode, ReturnedApplicationEditMode, ReturnedApplicationMode}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BusinessDetailsServiceTest extends PlaySpec with MockitoSugar with BeforeAndAfterEach with MockSave4LaterService {

  implicit val req: Request[AnyContent] = FakeRequest()

  val mockSave4LaterService: Save4LaterService = mock[Save4LaterService]
  val testBusinessCustomerDetails = BusinessCustomerDetails("ACME", Some("SOP"), BCAddress("line1", "line2", Option("line3"), Option("line4"), Option("postcode"), Option("country")), "sap123", "safe123", false, Some("agent123"))
  val testBusinessRegistrationDetails = BusinessRegistrationDetails(Some("SOP"), None, Some("1234"))
  val businessDetailsService = new BusinessDetailsService(testSave4LaterService)

  override def beforeEach(): Unit = {
    reset(
      mockMainStoreSave4LaterConnector,
      mockAccountUtils,
      mockSave4LaterService)

    super.beforeEach()
  }

  "validateBusinessDetails" must {
    "return NewApplicationMode when new app type is true" in {
      when(mockMainStoreSave4LaterConnector.fetchData4Later[NewApplicationType](any(), ArgumentMatchers.eq("newApplicationType"))(any(), any(), any()))
        .thenReturn(Future.successful(Option(NewApplicationType(Some(true)))))

      val result = businessDetailsService.businessDetailsPageRenderMode(TestUtil.defaultAuthRetrieval)

      await(result) mustBe NewApplicationMode
    }

    "return ReturnedApplicationEditMode if the new app type is empty but proposed start date is true" in {
      when(mockMainStoreSave4LaterConnector.fetchData4Later[NewApplicationType](any(), ArgumentMatchers.eq("newApplicationType"))(any(), any(), any()))
        .thenReturn(Future.successful(None))
      when(mockApiSave4LaterConnector.fetchData4Later[BusinessDetailsSupport](any(), ArgumentMatchers.eq("businessDetailsSupport"))(any(), any(), any()))
        .thenReturn(Future.successful(Option(BusinessDetailsSupport(true))))

      val result = businessDetailsService.businessDetailsPageRenderMode(TestUtil.defaultAuthRetrieval)

      await(result) mustBe ReturnedApplicationEditMode
    }

    "return ReturnedApplicationMode if the new app type is false and proposed start date is false" in {
      when(mockMainStoreSave4LaterConnector.fetchData4Later[NewApplicationType](any(), ArgumentMatchers.eq("newApplicationType"))(any(), any(), any()))
        .thenReturn(Future.successful(Option(NewApplicationType(Some(false)))))
      when(mockApiSave4LaterConnector.fetchData4Later[BusinessDetailsSupport](any(), ArgumentMatchers.eq("businessDetailsSupport"))(any(), any(), any()))
        .thenReturn(Future.successful(Option(BusinessDetailsSupport(false))))

      val result = businessDetailsService.businessDetailsPageRenderMode(TestUtil.defaultAuthRetrieval)

      await(result) mustBe ReturnedApplicationMode
    }
  }
}
