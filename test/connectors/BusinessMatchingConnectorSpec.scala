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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlEqualTo}
import config.ApplicationConfig
import models._
import play.api.test.Helpers._
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.AwrsTestJson.matchSuccessResponseJson
import utils.TestConstants._
import utils.{AwrsUnitTestTraits, TestUtil}

import scala.concurrent.ExecutionContext.Implicits.global

class BusinessMatchingConnectorSpec extends AwrsUnitTestTraits {

  val MockAuditConnector: AuditConnector = mock[AuditConnector]
  val httpClient: HttpClientV2 = httpClientV2
  val appConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

  val testBusinessMatchingConnector = new BusinessMatchingConnectorImpl(appConfig.servicesConfig, mockAuditable, mockAccountUtils, httpClient)
  val matchBusinessData: MatchBusinessData = MatchBusinessData("sessionId", testUtr, false, false, None, None)
  val userType = "org"

  val url = s"${testBusinessMatchingConnector.serviceUrl}/org/UNUSED/${testBusinessMatchingConnector.baseUri}/${testBusinessMatchingConnector.lookupUri}/${matchBusinessData.utr}/$userType"

  "Business Matching connector" must {

    "return status as OK, for successful call" in {

//
//      when(mockWSHttp.post(ArgumentMatchers.any())(ArgumentMatchers.any()).withBody(Json.toJson(matchBusinessData)).execute[HttpResponse])
//        .thenReturn(Future.successful(HttpResponse.apply(OK, matchSuccessResponseJson.toString())))

      println(url)
      stubFor(
        post(urlEqualTo(url))
        willReturn aResponse()
          .withStatus(OK)
          .withBody(matchSuccessResponseJson.toString())

      )
      val result = testBusinessMatchingConnector.lookup(matchBusinessData, userType, TestUtil.defaultAuthRetrieval)
      await(result) mustBe matchSuccessResponseJson
    }

//    "for unsuccessful match, return error message" in {
//      when(mockWSHttp.post(ArgumentMatchers.any())(ArgumentMatchers.any()).execute[HttpResponse])
//        .thenReturn(Future.successful(HttpResponse.apply(NOT_FOUND, matchFailureResponseJson.toString())))
//      val result = testBusinessMatchingConnector.lookup(matchBusinessData, userType, TestUtil.defaultAuthRetrieval)
//      await(result) mustBe matchFailureResponseJson
//      verify(mockWSHttp, times(1)).post(ArgumentMatchers.any())(ArgumentMatchers.any()).execute[HttpResponse]
//    }
//
//    "throw service unavailable exception, if service is unavailable" in {
//      when(mockWSHttp.post(ArgumentMatchers.any())(ArgumentMatchers.any()).execute[HttpResponse])
//        .thenReturn(Future.successful(HttpResponse.apply(SERVICE_UNAVAILABLE, "")))
//      val result = testBusinessMatchingConnector.lookup(matchBusinessData, userType, TestUtil.defaultAuthRetrieval)
//      val thrown = the[ServiceUnavailableException] thrownBy await(result)
//      thrown.getMessage must include("Service unavailable")
//      verify(mockWSHttp, times(1)).post(ArgumentMatchers.any())(ArgumentMatchers.any()).execute[HttpResponse]
//    }
//
//    "throw bad request exception, if bad request is passed" in {
//      when(mockWSHttp.post(ArgumentMatchers.any())(ArgumentMatchers.any()).execute[HttpResponse])
//        .thenReturn(Future.successful(HttpResponse.apply(BAD_REQUEST, "")))
//      val result = testBusinessMatchingConnector.lookup(matchBusinessData, userType, TestUtil.defaultAuthRetrieval)
//      val thrown = the[BadRequestException] thrownBy await(result)
//      thrown.getMessage must include("Bad Request")
//      verify(mockWSHttp, times(1)).post(ArgumentMatchers.any())(ArgumentMatchers.any()).execute[HttpResponse]
//    }
//
//    "throw internal server error, if Internal server error status is returned" in {
//      when(mockWSHttp.post(ArgumentMatchers.any())(ArgumentMatchers.any()).execute[HttpResponse])
//        .thenReturn(Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, "")))
//      val result = testBusinessMatchingConnector.lookup(matchBusinessData, userType, TestUtil.defaultAuthRetrieval)
//      val thrown = the[InternalServerException] thrownBy await(result)
//      thrown.getMessage must include("Internal server error")
//      verify(mockWSHttp, times(1)).post(ArgumentMatchers.any())(ArgumentMatchers.any()).execute[HttpResponse]
//    }
//
//   "throw runtime exception, unknown status is returned" in {
//      when(mockWSHttp.post(ArgumentMatchers.any())(ArgumentMatchers.any()).execute[HttpResponse])
//        .thenReturn(Future.successful(HttpResponse.apply(BAD_GATEWAY, "")))
//      val result = testBusinessMatchingConnector.lookup(matchBusinessData, userType, TestUtil.defaultAuthRetrieval)
//      val thrown = the[RuntimeException] thrownBy await(result)
//      thrown.getMessage must include("Unknown response")
//      verify(mockWSHttp, times(1)).post(ArgumentMatchers.any())(ArgumentMatchers.any()).execute[HttpResponse]
//    }
  }
}
