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

import com.github.tomakehurst.wiremock.client.WireMock.{postRequestedFor, urlMatching}
import models._
import org.scalatest.matchers.must.Matchers
import play.api.test.Helpers._
import play.api.test.Injecting
import uk.gov.hmrc.helpers.IntegrationSpec
import uk.gov.hmrc.http.{BadRequestException, InternalServerException, ServiceUnavailableException}
import utils.AwrsTestJson.{matchFailureResponseJson, matchSuccessResponseJson}
import utils.TestConstants._
import utils.TestUtil

import scala.concurrent.ExecutionContext.Implicits.global

class BusinessMatchingConnectorISpec extends IntegrationSpec with Injecting with Matchers {

  val connector = inject[BusinessMatchingConnectorImpl]
  val matchBusinessData: MatchBusinessData = MatchBusinessData("sessionId", testUtr, false, false, None, None)
  val userType = "org"


  val url = s"/org/UNUSED/${connector.baseUri}/${connector.lookupUri}/${matchBusinessData.utr}/$userType"

  "Business Matching connector" must {

    "return status as OK, for successful call" in {
      stubbedPost(url, OK, matchSuccessResponseJson.toString())
      val result = connector.lookup(matchBusinessData, userType, TestUtil.defaultAuthRetrieval)
      await(result) mustBe matchSuccessResponseJson
      wireMockServer.verify(1, postRequestedFor(urlMatching(url)))
    }

    "for unsuccessful match, return error message" in {
      stubbedPost(url, NOT_FOUND, matchFailureResponseJson.toString())
      val result = connector.lookup(matchBusinessData, userType, TestUtil.defaultAuthRetrieval)
      await(result) mustBe matchFailureResponseJson
      wireMockServer.verify(1, postRequestedFor(urlMatching(url)))
    }

    "throw service unavailable exception, if service is unavailable" in {
      stubbedPost(url, SERVICE_UNAVAILABLE, "")
      val result = connector.lookup(matchBusinessData, userType, TestUtil.defaultAuthRetrieval)
      val thrown = the[ServiceUnavailableException] thrownBy await(result)
      thrown.getMessage must include("Service unavailable")
      wireMockServer.verify(1, postRequestedFor(urlMatching(url)))
    }

    "throw bad request exception, if bad request is passed" in {
      stubbedPost(url, BAD_REQUEST, "")
      val result = connector.lookup(matchBusinessData, userType, TestUtil.defaultAuthRetrieval)
      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage must include("Bad Request")
      wireMockServer.verify(1, postRequestedFor(urlMatching(url)))
    }

    "throw internal server error, if Internal server error status is returned" in {
      stubbedPost(url, INTERNAL_SERVER_ERROR, "")
      val result = connector.lookup(matchBusinessData, userType, TestUtil.defaultAuthRetrieval)
      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage must include("Internal server error")
      wireMockServer.verify(1, postRequestedFor(urlMatching(url)))
    }

    "throw runtime exception, unknown status is returned" in {
      stubbedPost(url, BAD_GATEWAY, "")
      val result = connector.lookup(matchBusinessData, userType, TestUtil.defaultAuthRetrieval)
      val thrown = the[RuntimeException] thrownBy await(result)
      thrown.getMessage must include("Unknown response")
      wireMockServer.verify(1, postRequestedFor(urlMatching(url)))
    }
  }
}
