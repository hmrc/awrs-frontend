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

package connectors

import java.util.concurrent.TimeUnit

import audit.TestAudit
import com.codahale.metrics.Timer
import metrics.AwrsMetrics
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.http.Status.{BAD_REQUEST => _, INTERNAL_SERVER_ERROR => _, NOT_FOUND => _, OK => _, SERVICE_UNAVAILABLE => _}
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.{WSGet, WSPost}
import utils.AwrsUnitTestTraits

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpPost, HttpResponse}

class GovernmentGatewayConnectorSpec extends AwrsUnitTestTraits {

  val mockAwrsMetrics = mock[AwrsMetrics]

  val MockAuditConnector = mock[AuditConnector]

  class MockHttp extends HttpGet with WSGet with HttpPost with WSPost with HttpAuditing {
    override val hooks = Seq(AuditingHook)

    override def auditConnector: AuditConnector = MockAuditConnector

    override def appName = "awrs-frontend"
  }

  val mockWSHttp = mock[MockHttp]

  val defaultTimeOut = FiniteDuration(10, TimeUnit.SECONDS)

  override def beforeEach = {
    reset(mockWSHttp)
  }

  object TestGovernmentGatewayConnector extends GovernmentGatewayConnector {
    override val http: HttpGet with HttpPost = mockWSHttp
    override val metrics = mockAwrsMetrics
    override val audit: Audit = new TestAudit
    override val retryWait = 1 // override the retryWait as the wait time is irrelevant to the meaning of the test and reducing it speeds up the tests
  }

  val failureGovGateResponse = Json.parse( """{"hostname":"government-gateway-admin", "reason": "Some other reason"}""")

  // verification value that equals the amount of gg calls made, i.e. the first failed call plus 7 failed retries
  val retries = testGGConnector.retryLimit + 1

  def testGGConnector(implicit headerCarrier: HeaderCarrier) = {
    when(mockAwrsMetrics.startTimer(Matchers.any())).thenReturn(new Timer().time)
    TestGovernmentGatewayConnector
  }

  "Government Gateway connector enrolling AWRS" should {

    val subscribeSuccessResponse = Some(EnrolResponse("AWRS", "not finished", identifiers = List(Identifier("AWRS", "AWRS_Ref_No"))))
    val subscribeFailureResponseJson = Json.parse( """{"status" : "Error occurred"}""")
    val subscribeFailureDupSubsResponse = Json.parse( """{"reason": "Business Partner already has an active AWRS subscription"}""")
    val subscribeSuccessResponseJson = Json.toJson(subscribeSuccessResponse)
    val enrolRequest = EnrolRequest(portalId = "MDTP", serviceName = "AWRS", friendlyName = "AWRS", knownFacts = Seq("AWRS-120394955", "", "", "SAFE-ID"))
    val testBusinessCustomerDetails = BusinessCustomerDetails("ACME", Some("SOP"), BCAddress("line1", "line2", Option("line3"), Option("line4"), Option("postcode"), Option("country")), "sap123", "safe123", false, Some("agent123"))
    val businessType = "LTD"

    "return status as OK, for successful enrolment" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(subscribeSuccessResponseJson))))
      val result = testGGConnector.enrol(enrolRequest, testBusinessCustomerDetails, businessType)
      await(result) shouldBe subscribeSuccessResponse
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
    }
    "return status as OK, if the first failed GG Enrol call is followed by A successful GG enrolment" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(subscribeFailureResponseJson))))
        .thenReturn(Future.successful(HttpResponse(OK, Some(subscribeSuccessResponseJson))))
      val result = testGGConnector.enrol(enrolRequest, testBusinessCustomerDetails, businessType)
      await(result) shouldBe subscribeSuccessResponse
      // verify that the call is made 2 times, i.e. the first failed call plus 1 successful retry
      verify(mockWSHttp, times(2)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
    }
    "return None and catch all, for bad data sent for subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(subscribeFailureResponseJson))))
      val result = testGGConnector.enrol(enrolRequest, testBusinessCustomerDetails, businessType)
      await(result)(defaultTimeOut) shouldBe None
      // verify that the correct amount of retry calls are made, i.e. the first failed call plus the specified amount of failed retries
      verify(mockWSHttp, times(retries)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
    }
    "return None when GovernmentGatewayException thrown, for bad data sent for subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(failureGovGateResponse))))
      val result = testGGConnector.enrol(enrolRequest, testBusinessCustomerDetails, businessType)
      await(result)(defaultTimeOut) shouldBe None
      // verify that the correct amount of retry calls are made, i.e. the first failed call plus the specified amount of failed retries
      verify(mockWSHttp, times(retries)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
    }
    "return None when DuplicateSubscriptionException thrown, for bad data sent for subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(subscribeFailureDupSubsResponse))))
      val result = testGGConnector.enrol(enrolRequest, testBusinessCustomerDetails, businessType)
      await(result)(defaultTimeOut) shouldBe None
      // verify that the correct amount of retry calls are made, i.e. the first failed call plus the specified amount of failed retries
      verify(mockWSHttp, times(retries)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
    }
    "return None when NotFoundException thrown, for bad data sent for subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(subscribeFailureResponseJson))))
      val result = testGGConnector.enrol(enrolRequest, testBusinessCustomerDetails, businessType)
      await(result)(defaultTimeOut) shouldBe None
      // verify that the correct amount of retry calls are made, i.e. the first failed call plus the specified amount of failed retries
      verify(mockWSHttp, times(retries)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
    }
    "return None when SERVICE_UNAVAILABLE, for bad data sent for subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(subscribeFailureResponseJson))))
      val result = testGGConnector.enrol(enrolRequest, testBusinessCustomerDetails, businessType)
      await(result)(defaultTimeOut) shouldBe None
      // verify that the correct amount of retry calls are made, i.e. the first failed call plus the specified amount of failed retries
      verify(mockWSHttp, times(retries)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
    }
    "return None when INTERNAL SERVER ERROR" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(subscribeFailureResponseJson))))
      val result = testGGConnector.enrol(enrolRequest, testBusinessCustomerDetails, businessType)
      await(result)(defaultTimeOut) shouldBe None
      // verify that the correct amount of retry calls are made, i.e. the first failed call plus the specified amount of failed retries
      verify(mockWSHttp, times(retries)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
    }
    "return None when INTERNAL SERVER ERROR and GovernmentGatewayException thrown, for bad data sent for subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(failureGovGateResponse))))
      val result = testGGConnector.enrol(enrolRequest, testBusinessCustomerDetails, businessType)
      await(result)(defaultTimeOut) shouldBe None
      // verify that the correct amount of retry calls are made, i.e. the first failed call plus the specified amount of failed retries
      verify(mockWSHttp, times(retries)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
    }
    "return None when INTERNAL SERVER ERROR and DuplicateSubscriptionException thrown, for bad data sent for subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(subscribeFailureDupSubsResponse))))
      val result = testGGConnector.enrol(enrolRequest, testBusinessCustomerDetails, businessType)
      await(result)(defaultTimeOut) shouldBe None
      // verify that the correct amount of retry calls are made, i.e. the first failed call plus the specified amount of failed retries
      verify(mockWSHttp, times(retries)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
    }
    "return None for any other exceptions" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(777, Some(subscribeFailureResponseJson))))
      val result = testGGConnector.enrol(enrolRequest, testBusinessCustomerDetails, businessType)
      await(result)(defaultTimeOut) shouldBe None
      // verify that the correct amount of retry calls are made, i.e. the first failed call plus the specified amount of failed retries
      verify(mockWSHttp, times(retries)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
    }
    "return None for anything else and when GovernmentGatewayException thrown, for bad data sent for subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(777, Some(failureGovGateResponse))))
      val result = testGGConnector.enrol(enrolRequest, testBusinessCustomerDetails, businessType)
      await(result)(defaultTimeOut) shouldBe None
      // verify that the correct amount of retry calls are made, i.e. the first failed call plus the specified amount of failed retries
      verify(mockWSHttp, times(retries)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
    }
    "return None for anything else and when DuplicateSubscriptionException thrown, for bad data sent for subscription" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(777, Some(subscribeFailureDupSubsResponse))))
      val result = testGGConnector.enrol(enrolRequest, testBusinessCustomerDetails, businessType)
      await(result)(defaultTimeOut) shouldBe None
      // verify that the correct amount of retry calls are made, i.e. the first failed call plus the specified amount of failed retries
      verify(mockWSHttp, times(retries)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
    }
  }
}
