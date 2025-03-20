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

package connectors

import models.{EnrolResponse, RequestPayload}
import org.scalatest.matchers.must.Matchers
import play.api.http.Status.{CREATED, BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, OK, SERVICE_UNAVAILABLE}
import play.api.test.Helpers._
import play.api.test.Injecting
import services.GGConstants._
import uk.gov.hmrc.helpers.IntegrationSpec
import uk.gov.hmrc.http.{BadGatewayException, BadRequestException, HeaderCarrier, InternalServerException, NotFoundException, ServiceUnavailableException}
import utils.TestUtil

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaxEnrolmentsConnectorISpec extends IntegrationSpec with Injecting with Matchers {

  val connector: TaxEnrolmentsConnector = inject[TaxEnrolmentsConnector]

  "Tax enrolments connector de-enrolling AWRS" must {
    // used in the mock to check the destination of the connector calls
    lazy val deEnrolURI = s"/${connector.deEnrolURI}/$service"

    // these values doesn't really matter since the call itself is mocked
    val awrsRef = ""
    val businessName = ""
    val businessType = ""
    val deEnrolResponseSuccess = true
    val deEnrolResponseFailure = false

    def mockResponse(responseStatus: Int, responseString: Option[String] = None): Unit = {
      stubbedPost(deEnrolURI, responseStatus, responseString.getOrElse(""))
    }

    def testCall(implicit headerCarrier: HeaderCarrier): Future[Boolean] = {

      connector.deEnrol(awrsRef, businessName, businessType)(headerCarrier, implicitly)
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

    def mockResponse(responseStatus: Int, responseString: Option[String] = None): Unit = {
      stubbedPost(s"/tax-enrolments/groups/groupId/enrolments/$service~AWRSRefNumber~1234567", responseStatus, responseString.getOrElse(""))
    }

    def testCall(implicit headerCarrier: HeaderCarrier): Option[EnrolResponse] = {
      val requestPayload = RequestPayload.apply("", "", "", List.empty)
      val groupId = "groupId"
      val awrsRef = "1234567"
      val businessType = ""
      val businessPartnerDetails = TestUtil.testBusinessCustomerDetails("LP")
      await(connector.enrol(requestPayload, groupId, awrsRef, businessPartnerDetails, businessType)(headerCarrier, implicitly))
    }

    def testCallEnrolWithAuditMap(implicit headerCarrier: HeaderCarrier): Option[EnrolResponse] = {
      val requestPayload = RequestPayload.apply("", "", "", List.empty)
      val groupId = "groupId"
      val awrsRef = "1234567"

      await(connector.enrol(requestPayload, groupId, awrsRef, auditMap = Map.empty)(headerCarrier, implicitly))
    }

    "return enrol response for successful enrolment" in {
      mockResponse(CREATED)
      testCall match {
        case Some(response) => response mustBe EnrolResponse("", "", Seq.empty)
        case _              => fail("Unexpected response from enrolment")
      }
    }

    "return enrol response for successful enrolment with audit map" in {
      mockResponse(CREATED)
      testCallEnrolWithAuditMap match {
        case Some(response) => response mustBe EnrolResponse("", "", Seq.empty)
        case _              => fail("Unexpected response from enrolment")
      }
    }

    "return enrol response for unsuccessful enrolment" in {
      mockResponse(BAD_REQUEST)
      intercept[BadRequestException](testCall)
    }

    "return enrol response for not found response" in {
      mockResponse(NOT_FOUND)
      intercept[NotFoundException](testCall)
    }

    "return enrol response for service unavailable response" in {
      mockResponse(SERVICE_UNAVAILABLE)
      intercept[ServiceUnavailableException](testCall)
    }

    "return enrol response for bad gateway response" in {
      mockResponse(BAD_GATEWAY)
      intercept[BadGatewayException](testCall)
    }

    "return enrol response for any other status" in {
      val otherStatus = 421

      mockResponse(otherStatus)
      intercept[InternalServerException](testCall)
    }
  }
}
