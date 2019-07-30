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

import models._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.libs.json.JsValue
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.AwrsTestJson.{matchFailureResponseJson, matchSuccessResponseJson}
import utils.{AwrsUnitTestTraits, TestUtil}
import utils.TestConstants._

import scala.concurrent.Future
import uk.gov.hmrc.http.{BadRequestException, HttpResponse, InternalServerException, ServiceUnavailableException, SessionKeys}
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import scala.concurrent.ExecutionContext.Implicits.global

class BusinessMatchingConnectorSpec extends AwrsUnitTestTraits {

  val MockAuditConnector: AuditConnector = mock[AuditConnector]
  val mockWSHttp: DefaultHttpClient = mock[DefaultHttpClient]

  override def beforeEach: Unit = {
    reset(mockWSHttp)
  }

  val testBusinessMatchingConnector = new BusinessMatchingConnectorImpl(mockServicesConfig, mockAuditable, mockAccountUtils, mockWSHttp)
  val matchBusinessData = MatchBusinessData(SessionKeys.sessionId, testUtr, false, false, None, None)
  val userType = "org"

  "Business Matching connector" should {

    "return status as OK, for successful call" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(matchSuccessResponseJson))))
      val result = testBusinessMatchingConnector.lookup(matchBusinessData, userType, TestUtil.defaultAuthRetrieval)
      await(result) shouldBe matchSuccessResponseJson
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    "for unsuccessful match, return error message" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(matchFailureResponseJson))))
      val result = testBusinessMatchingConnector.lookup(matchBusinessData, userType, TestUtil.defaultAuthRetrieval)
      await(result) shouldBe matchFailureResponseJson
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    "throw service unavailable exception, if service is unavailable" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, None)))
      val result = testBusinessMatchingConnector.lookup(matchBusinessData, userType, TestUtil.defaultAuthRetrieval)
      val thrown = the[ServiceUnavailableException] thrownBy await(result)
      thrown.getMessage should include("Service unavailable")
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    "throw bad request exception, if bad request is passed" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))
      val result = testBusinessMatchingConnector.lookup(matchBusinessData, userType, TestUtil.defaultAuthRetrieval)
      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage should include("Bad Request")
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    "throw internal server error, if Internal server error status is returned" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, None)))
      val result = testBusinessMatchingConnector.lookup(matchBusinessData, userType, TestUtil.defaultAuthRetrieval)
      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage should include("Internal server error")
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

   "throw runtime exception, unknown status is returned" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_GATEWAY, None)))
      val result = testBusinessMatchingConnector.lookup(matchBusinessData, userType, TestUtil.defaultAuthRetrieval)
      val thrown = the[RuntimeException] thrownBy await(result)
      thrown.getMessage should include("Unknown response")
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }
  }
}
