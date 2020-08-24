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

package connectors

import com.codahale.metrics.Timer
import metrics.AwrsMetrics
import models.{EnrolResponse, RequestPayload}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status.{CREATED, BAD_REQUEST => _, INTERNAL_SERVER_ERROR => _, NOT_FOUND => _, OK => _, SERVICE_UNAVAILABLE => _}
import play.api.libs.json.JsValue
import play.api.test.Helpers._
import services.GGConstants._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import utils.{AwrsUnitTestTraits, TestUtil}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaxEnrolmentsConnectorSpec extends AwrsUnitTestTraits {

  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val mockAwrsMetrics: AwrsMetrics = mock[AwrsMetrics]
  val mockWSHttp: DefaultHttpClient = mock[DefaultHttpClient]

  override def beforeEach: Unit = {
    reset(mockWSHttp)
  }

  val testTaxEnrolmentsConnector: TaxEnrolmentsConnector = new TaxEnrolmentsConnector(mockServicesConfig, mockWSHttp, mockAwrsMetrics, mockAuditable) {
    override val retryWait: Int = 50
  }

  "Tax enrolments connector de-enrolling AWRS" must {
    // used in the mock to check the destination of the connector calls
    lazy val deEnrolURI = testTaxEnrolmentsConnector.deEnrolURI + "/" + service

    // these values doesn't really matter since the call itself is mocked
    val awrsRef = ""
    val businessName = ""
    val businessType = ""
    val deEnrolResponseSuccess = true
    val deEnrolResponseFailure = false

    def mockResponse(responseStatus: Int, responseString: Option[String] = None): Unit =
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.endsWith(deEnrolURI), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse.apply(responseStatus, responseString.toString())))

    def testCall(implicit headerCarrier: HeaderCarrier): Future[Boolean] = {
      when(mockAwrsMetrics.startTimer(ArgumentMatchers.any())).thenReturn(new Timer().time)
      testTaxEnrolmentsConnector.deEnrol(awrsRef, businessName, businessType)(headerCarrier, implicitly)
    }

    "return status as OK, for successful de-enrolment" in {
      mockResponse(OK)
      val result = testCall
      await(result) mustBe deEnrolResponseSuccess
    }

    "return status as BAD_REQUEST, for unsuccessful de-enrolment" in {
      mockResponse(BAD_REQUEST)
      val result = testCall
      await(result) mustBe deEnrolResponseFailure
    }

    "return status as NOT_FOUND, for unsuccessful de-enrolment" in {
      mockResponse(NOT_FOUND)
      val result = testCall
      await(result) mustBe deEnrolResponseFailure
    }

    "return status as SERVICE_UNAVAILABLE, for unsuccessful de-enrolment" in {
      mockResponse(SERVICE_UNAVAILABLE)
      val result = testCall
      await(result) mustBe deEnrolResponseFailure
    }

    "return status as INTERNAL_SERVER_ERROR, for unsuccessful de-enrolment" in {
      mockResponse(INTERNAL_SERVER_ERROR, Some("error in de-enrol service end point error"))
      val result = testCall
      await(result) mustBe deEnrolResponseFailure
    }

    "return status as unexpected status, for unsuccessful de-enrolment" in {
      val otherStatus = 999
      mockResponse(otherStatus)
      val result = testCall
      await(result) mustBe deEnrolResponseFailure
    }


  }

  "Tax enrolments connector enrolling AWRS" must {

    def mockResponse(responseStatus: Int, responseString: Option[String] = None): Unit =
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(),
        ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(),
        ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse.apply(responseStatus, responseString.toString())))

    def testCall(implicit headerCarrier: HeaderCarrier): Option[EnrolResponse] = {
      val requestPayload = RequestPayload.apply("", "", "", List.empty)
      val groupId = ""
      val awrsRef = ""
      val businessType = ""
      val businessPartnerDetails = TestUtil.testBusinessCustomerDetails("LP")
      when(mockAwrsMetrics.startTimer(ArgumentMatchers.any())).thenReturn(new Timer().time)
      await(testTaxEnrolmentsConnector.enrol(requestPayload, groupId, awrsRef, businessPartnerDetails, businessType)(headerCarrier, implicitly))
    }

    "return enrol response for successful enrolment" in {
      mockResponse(OK)
      testCall.isDefined mustBe true
    }

    "return enrol response for unsuccessful enrolment" in {
      mockResponse(BAD_REQUEST)
      testCall.isDefined mustBe true
    }

    "return enrol response for created response" in {
      mockResponse(CREATED)
      testCall.isDefined mustBe true
    }

    "return enrol response for not found response" in {
      mockResponse(NOT_FOUND)
      testCall.isDefined mustBe true
    }

    "return enrol response for service unavailable response" in {
      mockResponse(SERVICE_UNAVAILABLE)
      testCall.isDefined mustBe true
    }

    "return enrol response for bad gateway response" in {
      mockResponse(BAD_GATEWAY)
      testCall.isDefined mustBe true
    }

    "return enrol response for any other status" in {
      val otherStatus = 421

      mockResponse(otherStatus)
      testCall.isDefined mustBe true
    }
  }
}
