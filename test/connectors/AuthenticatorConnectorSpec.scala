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

import config.AwrsFrontendAuditConnector
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.Play
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.{WSGet, WSPost}
import utils.AwrsUnitTestTraits

import scala.concurrent.Future


class AuthenticatorConnectorSpec extends AwrsUnitTestTraits {

  class MockHttp extends WSGet with WSPost with HttpAuditing{
    override val hooks = Seq(AuditingHook)
    override def auditConnector: AuditConnector = AwrsFrontendAuditConnector

    override def appName = Play.configuration.getString("appName").getOrElse("awrs-frontend")
  }

  val mockWSHttp = mock[MockHttp]

  object TestAuthenticatorConnector extends AuthenticatorConnector {
    override val http: HttpGet with HttpPost = mockWSHttp
  }

  override def beforeEach = {
    reset(mockWSHttp)
  }

  "AuthenticatorConnector" must {
    val subscribeFailureResponseJson = Json.parse( """{"reason" : "Error happened"}""")

    "refresh user" must {
      "works for a user" in {

        when(mockWSHttp.POSTEmpty[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any())).
          thenReturn(Future.successful(HttpResponse(OK, responseJson = None)))

        val result = TestAuthenticatorConnector.refreshProfile
        val enrolResponse = await(result)
        enrolResponse.status shouldBe OK
      }
    }
  }
}
