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

import akka.actor.ActorSystem
import audit.TestAudit
import com.typesafe.config.Config
import metrics.AwrsMetrics
import org.mockito.Matchers
import org.mockito.Matchers.{any, endsWith}
import org.mockito.Mockito._
import play.api.{Configuration, Play}
import play.api.Mode.Mode
import play.api.http.Status.{BAD_REQUEST => _, INTERNAL_SERVER_ERROR => _, NOT_FOUND => _, OK => _, SERVICE_UNAVAILABLE => _}
import play.api.libs.json.{JsObject, JsValue}
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.{WSDelete, WSGet, WSPost, WSPut}
import utils.AwrsUnitTestTraits
import utils.TestConstants._

import scala.concurrent.Future
import uk.gov.hmrc.http._

class EmailVerificationConnectorTest extends AwrsUnitTestTraits {
  val MockAuditConnector = mock[AuditConnector]

  val dummyAppName = "awrs-frontend"

  class MockHttp extends HttpGet with WSGet with HttpPost with WSPost with HttpPut with WSPut with HttpAuditing with HttpDelete with WSDelete {
    override val hooks = Seq(AuditingHook)

    override def auditConnector: AuditConnector = MockAuditConnector

    override def appName = dummyAppName

    override protected def actorSystem: ActorSystem = Play.current.actorSystem

    override protected def configuration: Option[Config] = Option(Play.current.configuration.underlying)
}

  val mockWSHttp = mock[MockHttp]

  override def beforeEach = {
    reset(mockWSHttp)
    reset(MockAuditConnector)
  }

  object TestEmailVerificationConnector extends EmailVerificationConnector {
    override val httpGet: MockHttp = mockWSHttp
    override val httpPost: MockHttp = mockWSHttp
    override val appName = dummyAppName
    override val metrics = mock[AwrsMetrics]
    override val audit: Audit = new TestAudit

    override protected def mode: Mode = Play.current.mode

    override protected def runModeConfiguration: Configuration = Play.current.configuration
  }


  // these values doesn't really matter since the call itself is mocked
  implicit val request = FakeRequest()

  lazy val sendEmailURI = TestEmailVerificationConnector.sendEmail
  lazy val verifiyEmailURI = TestEmailVerificationConnector.verifyEmail

  "sendVerificationEmail" should {

    def mockPostResponse(responseStatus: Int, responseData: Option[JsValue] = None): Unit =
      when(mockWSHttp.POST[Unit, HttpResponse](Matchers.endsWith(sendEmailURI), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(responseStatus, responseData)))

    def testPostCall(implicit user: AuthContext, hc: HeaderCarrier, request: Request[AnyContent]) = TestEmailVerificationConnector.sendVerificationEmail(testEmail)

    "return success equals true, for successful created email sending" in {
      mockPostResponse(responseStatus = CREATED)
      val result = testPostCall
      await(result) shouldBe true
    }

    "return success equals true, for successful ok email sending" in {
      mockPostResponse(responseStatus = OK)
      val result = testPostCall
      await(result) shouldBe true
    }

    "return success equals true, when email has already been verified" in {
      mockPostResponse(responseStatus = CONFLICT)
      val result = testPostCall
      await(result) shouldBe true
    }

    "return success equals false, when an error occurs" in {
      mockPostResponse(responseStatus = INTERNAL_SERVER_ERROR)
      val result = testPostCall
      await(result) shouldBe false
    }

  }

  "isEmailAddressVerified" should {

    def testGetCall(implicit user: AuthContext, hc: HeaderCarrier, request: Request[AnyContent]) = TestEmailVerificationConnector.isEmailAddressVerified(testEmail)

    "return success equals true, for successful verification of email" in {
      when(mockWSHttp.POST[JsObject, HttpResponse](endsWith(verifiyEmailURI), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK)))
      val result = testGetCall
      await(result) shouldBe true
    }

    "return success equals false, if email has not been verified" in {
      when(mockWSHttp.POST[JsObject, HttpResponse](endsWith(verifiyEmailURI), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.failed(new NotFoundException("404")))
      val result = testGetCall
      await(result) shouldBe false
    }

    "return success equals true, when an error occurs as we have chosen not to block" in {
      when(mockWSHttp.POST[JsObject, HttpResponse](endsWith(verifiyEmailURI), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.failed(Upstream5xxResponse("503", 503, 503)))
      val result = testGetCall
      await(result) shouldBe true
    }

  }

}
