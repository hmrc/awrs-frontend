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

import metrics.AwrsMetrics
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, endsWith}
import org.mockito.Mockito._
import play.api.http.Status.{BAD_REQUEST => _, INTERNAL_SERVER_ERROR => _, NOT_FOUND => _, OK => _, SERVICE_UNAVAILABLE => _}
import play.api.libs.json.{JsObject, JsValue}
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.AwrsUnitTestTraits
import utils.TestConstants._

import scala.concurrent.Future
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.concurrent.ExecutionContext.Implicits.global

class EmailVerificationConnectorTest extends AwrsUnitTestTraits {
  val MockAuditConnector: AuditConnector = mock[AuditConnector]
  val mockWSHttp: DefaultHttpClient = mock[DefaultHttpClient]

  override def beforeEach: Unit = {
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

  "sendVerificationEmail" should {

    def mockPostResponse(responseStatus: Int, responseData: Option[JsValue] = None): Unit =
      when(mockWSHttp.POST[Unit, HttpResponse](ArgumentMatchers.endsWith(sendEmailURI), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(responseStatus, responseData)))

    def testPostCall(implicit  hc: HeaderCarrier, request: Request[AnyContent]) = testEmailVerificationConnector.sendVerificationEmail(testEmail)

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

    def testGetCall: Future[Boolean] = testEmailVerificationConnector.isEmailAddressVerified(testEmail)

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
