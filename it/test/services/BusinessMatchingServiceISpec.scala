/*
 * Copyright 2025 HM Revenue & Customs
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

package services

import models.reenrolment.AwrsRegisteredPostcode
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.helpers.IntegrationSpec
import utils.TestUtil
import connectors.BusinessMatchingConnectorImpl
import com.github.tomakehurst.wiremock.client.WireMock.{postRequestedFor, urlMatching}
import controllers.auth.StandardAuthRetrievals
import org.scalatest.matchers.must.Matchers
import play.api.test.Injecting
import scala.concurrent.ExecutionContext.Implicits.global


class BusinessMatchingServiceISpec extends IntegrationSpec with Matchers with Injecting{

  val businessMatchingService: BusinessMatchingService = inject[BusinessMatchingService]
  val connector: BusinessMatchingConnectorImpl = inject[BusinessMatchingConnectorImpl]

  val enrolmentUtr = "1111111112"
  val testPostcode = "NE98 1ZZ"
  val awrsRegisteredPostcode: AwrsRegisteredPostcode = AwrsRegisteredPostcode(testPostcode)
  val authRetrievals: StandardAuthRetrievals = TestUtil.defaultAuthRetrieval
  val url = s"/org/UNUSED/${connector.baseUri}/${connector.lookupUri}/$enrolmentUtr/org"

  "verifyUTRandPostCode" must {
    "return true when the postcode matches the returned data" in {
      val responseJson = Json.obj(
        "address" -> Json.obj(
          "addressLine1" -> "Test Address Line 1",
          "addressLine2" -> "Test Address Line 2",
          "postalCode" -> testPostcode
        )
      )
      stubbedPost(url, OK, responseJson.toString())

      val result = await(businessMatchingService.verifyUTRandPostCode(enrolmentUtr, awrsRegisteredPostcode, authRetrievals, isSA = false))
      result mustBe true
      wireMockServer.verify(1, postRequestedFor(urlMatching(url)))
    }

    "return false when the postcode does not match the returned data" in {
      val responseJson = Json.obj(
        "address" -> Json.obj(
          "addressLine1" -> "Test Address Line 1",
          "addressLine2" -> "Test Address Line 2",
          "postalCode" -> "WRONGPOSTCODE"
        )
      )
      stubbedPost(url, OK, responseJson.toString())

      val result = await(businessMatchingService.verifyUTRandPostCode(enrolmentUtr, awrsRegisteredPostcode, authRetrievals, isSA = false))
      result mustBe false
      wireMockServer.verify(1, postRequestedFor(urlMatching(url)))
    }

    "return false when the address is missing in the returned data" in {
      val responseJson = Json.obj(
        "address" -> Json.obj(
          "addressLine1" -> "Test Address Line 1",
          "addressLine2" -> "Test Address Line 2",
          "postalCode" -> ""
        )
      )
      stubbedPost(url, OK, responseJson.toString())

      val result = await(businessMatchingService.verifyUTRandPostCode(enrolmentUtr, awrsRegisteredPostcode, authRetrievals, isSA = false))
      result mustBe false
      wireMockServer.verify(1, postRequestedFor(urlMatching(url)))
    }
  }
}