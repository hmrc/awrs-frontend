/*
 * Copyright 2017 HM Revenue & Customs
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

import audit.TestAudit
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.http.Status.{BAD_REQUEST => _, INTERNAL_SERVER_ERROR => _, NOT_FOUND => _, OK => _, SERVICE_UNAVAILABLE => _}
import play.api.libs.json.JsValue
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.{WSGet, WSPost}
import utils.AwrsTestJson.{matchFailureResponseJson, matchSuccessResponseJson}
import utils.AwrsUnitTestTraits
import utils.TestConstants._

import scala.concurrent.Future

class BusinessMatchingConnectorSpec extends AwrsUnitTestTraits {

  val MockAuditConnector = mock[AuditConnector]

  class MockHttp extends WSGet with WSPost with HttpAuditing {
    override val hooks = Seq(AuditingHook)

    override def auditConnector: AuditConnector = MockAuditConnector

    override def appName = "awrs-frontend"
  }

  val mockWSHttp = mock[MockHttp]

  override def beforeEach = {
    reset(mockWSHttp)
  }

  object TestBusinessMatchingConnector extends BusinessMatchingConnector {
    override val http: HttpGet with HttpPost = mockWSHttp
    override val audit: Audit = new TestAudit
    override def serviceUrl: String = ""
    override def baseUri: String = ""
    override def lookupUri: String = ""
  }

  val matchBusinessData = MatchBusinessData(SessionKeys.sessionId, testUtr, false, false, None, None)

  val userType = "org"

  "Business Matching connector" should {

    "return status as OK, for successful call" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(matchSuccessResponseJson))))
      val result = TestBusinessMatchingConnector.lookup(matchBusinessData, userType)
      await(result) shouldBe matchSuccessResponseJson
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "for unsuccessful match, return error message" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(matchFailureResponseJson))))
      val result = TestBusinessMatchingConnector.lookup(matchBusinessData, userType)
      await(result) shouldBe matchFailureResponseJson
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "throw service unavailable exception, if service is unavailable" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, None)))
      val result = TestBusinessMatchingConnector.lookup(matchBusinessData, userType)
      val thrown = the[ServiceUnavailableException] thrownBy await(result)
      thrown.getMessage should include("Service unavailable")
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "throw bad request exception, if bad request is passed" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))
      val result = TestBusinessMatchingConnector.lookup(matchBusinessData, userType)
      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage should include("Bad Request")
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "throw internal server error, if Internal server error status is returned" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, None)))
      val result = TestBusinessMatchingConnector.lookup(matchBusinessData, userType)
      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage should include("Internal server error")
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

   "throw runtime exception, unknown status is returned" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_GATEWAY, None)))
      val result = TestBusinessMatchingConnector.lookup(matchBusinessData, userType)
      val thrown = the[RuntimeException] thrownBy await(result)
      thrown.getMessage should include("Unknown response")
      verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }
  }
}
