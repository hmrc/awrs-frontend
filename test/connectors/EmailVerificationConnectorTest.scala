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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, endsWith}
import org.mockito.Mockito._
import play.api.http.Status.{BAD_REQUEST => _, INTERNAL_SERVER_ERROR => _, NOT_FOUND => _, OK => _, SERVICE_UNAVAILABLE => _}
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import utils.AwrsUnitTestTraits
import utils.TestConstants._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailVerificationConnectorTest extends AwrsUnitTestTraits {
  val MockAuditConnector: AuditConnector = mock[AuditConnector]
  val mockWSHttp: DefaultHttpClient = mock[DefaultHttpClient]

  override def beforeEach(): Unit = {
    reset(mockWSHttp)

    when(mockAppConfig.servicesConfig)
      .thenReturn(mockServicesConfig)
    when(mockServicesConfig.baseUrl(ArgumentMatchers.eq("awrs")))
      .thenReturn("testURL")
  }

  val testEmailVerificationConnector = new EmailVerificationConnector(mockWSHttp, mockAuditable, mockAppConfig)

  // these values doesn't really matter since the call itself is mocked
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  lazy val sendEmailURI: String = testEmailVerificationConnector.sendEmail
  lazy val verifiyEmailURI: String = testEmailVerificationConnector.verifyEmail

  "sendVerificationEmail" must {

    def mockPostResponse(responseStatus: Int, responseData: Option[JsValue] = None): Unit =
      when(mockWSHttp.POST[Unit, HttpResponse](ArgumentMatchers.endsWith(sendEmailURI), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse.apply(responseStatus, responseData.getOrElse(Json.obj()), Map.empty[String, Seq[String]])))

    def testPostCall(implicit  hc: HeaderCarrier) = testEmailVerificationConnector.sendVerificationEmail(testEmail)

    "return success equals true, for successful created email sending" in {
      mockPostResponse(responseStatus = CREATED)
      val result = testPostCall
      await(result) mustBe true
    }

    "return success equals true, for successful ok email sending" in {
      mockPostResponse(responseStatus = OK)
      val result = testPostCall
      await(result) mustBe true
    }

    "return success equals true, when email has already been verified" in {
      mockPostResponse(responseStatus = CONFLICT)
      val result = testPostCall
      await(result) mustBe true
    }

    "return success equals false, when an error occurs" in {
      mockPostResponse(responseStatus = INTERNAL_SERVER_ERROR)
      val result = testPostCall
      await(result) mustBe false
    }

  }

  "isEmailAddressVerified" must {

    def testGetCall: Future[Boolean] = testEmailVerificationConnector.isEmailAddressVerified(testEmail)

    "return success equals true, for successful verification of email" in {
      when(mockWSHttp.POST[JsObject, HttpResponse](endsWith(verifiyEmailURI), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(OK, "")))
      val result = testGetCall
      await(result) mustBe true
    }

    "return success equals false, if email has not been verified" in {
      when(mockWSHttp.POST[JsObject, HttpResponse](endsWith(verifiyEmailURI), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.failed(new NotFoundException("404")))
      val result = testGetCall
      await(result) mustBe false
    }

    "return success equals true, when an error occurs as we have chosen not to block" in {
      when(mockWSHttp.POST[JsObject, HttpResponse](endsWith(verifiyEmailURI), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("503", 503, 503)))
      val result = testGetCall
      await(result) mustBe true
    }

  }

}
