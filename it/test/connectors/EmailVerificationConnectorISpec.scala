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

import org.scalatest.matchers.must.Matchers
import play.api.http.Status.{BAD_REQUEST => _, INTERNAL_SERVER_ERROR => _, NOT_FOUND => _, OK => _, SERVICE_UNAVAILABLE => _}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.helpers.IntegrationSpec
import uk.gov.hmrc.http._
import utils.TestConstants._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailVerificationConnectorISpec extends IntegrationSpec with Injecting with Matchers {

  val connector: EmailVerificationConnector = inject[EmailVerificationConnector]

  // these values doesn't really matter since the call itself is mocked
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  lazy val sendEmailURI: String = s"${connector.baseURI}${connector.sendEmail}"
  lazy val verifiyEmailURI: String = s"${connector.baseURI}${connector.verifyEmail}"

  def mockPostResponse(url: String, responseStatus: Int): Unit = stubbedPost(url, responseStatus, "")

  "sendVerificationEmail" must {

    def testPostCall(implicit  hc: HeaderCarrier): Future[Boolean] = connector.sendVerificationEmail(testEmail)

    "return success equals true, for successful created email sending" in {
      mockPostResponse(sendEmailURI, CREATED)
      val result = testPostCall
      await(result) mustBe true
    }

    "return success equals true, when email has already been verified" in {
      mockPostResponse(sendEmailURI, CONFLICT)
      val result = testPostCall
      await(result) mustBe true
    }

    "return success equals false, when an error occurs" in {
      mockPostResponse(sendEmailURI, INTERNAL_SERVER_ERROR)
      val result = testPostCall
      await(result) mustBe false
    }

  }

  "isEmailAddressVerified" must {

    def testGetCall: Future[Boolean] = connector.isEmailAddressVerified(Some(testEmail))

    "return success equals true, for successful verification of email" in {
      mockPostResponse(verifiyEmailURI, OK)
      val result = testGetCall
      await(result) mustBe true
    }

    "return success equals false, if email has not been verified" in {
      mockPostResponse(verifiyEmailURI, NOT_FOUND)
      val result = testGetCall
      await(result) mustBe false
    }

    "return success equals true, when an error occurs as we have chosen not to block" in {
      mockPostResponse(verifiyEmailURI, SERVICE_UNAVAILABLE)
      val result = testGetCall
      await(result) mustBe true
    }

  }

}
