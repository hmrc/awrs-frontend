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

import models.reenrolment.{Enrolment, EnrolmentSuccessResponse, Identifier, KnownFacts, Verifier}
import org.scalatest.matchers.must.Matchers
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.Injecting
import uk.gov.hmrc.helpers.IntegrationSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import models.reenrolment.EnrolmentSuccessResponse._

class EnrolmentStoreProxyConnectorISpec extends IntegrationSpec with Injecting with Matchers {

  val connector: EnrolmentStoreProxyConnector = inject[EnrolmentStoreProxyConnector]
  val awrsRef = "TestAwrsRef"
  val es1ResponseWithGroupId = Some("""{"principalGroupIds": ["TestPrincipalGroupId"]}""")
  val es1ResponseWithoutGroupId = Some("""{"principalGroupIds": []}""")

  "Enrolment Store Connector" must {
    // used in the mock to check the destination of the connector calls
    lazy val es1Url= s"/enrolment-store-proxy/enrolment-store/enrolments/${connector.AWRS_SERVICE_NAME}~${connector.EnrolmentIdentifierName}~$awrsRef/groups"
    lazy val es20Url= s"/enrolment-store-proxy/enrolment-store/enrolments"

    // these values doesn't really matter since the call itself is mocked e

    def mockResponseES1(responseStatus: Int, responseString: Option[String] = None): Unit = {
      stubbedGet(es1Url, responseStatus, responseString.getOrElse(""))
    }

    def mockPostResponseES20(responseStatus: Int, responseString: Option[String]): Unit = {
      stubbedPost(es20Url, responseStatus, responseString.getOrElse(""))
    }

    def testCall(implicit headerCarrier: HeaderCarrier): Future[Option[String]] = {
      connector.queryForPrincipalGroupIdOfAWRSEnrolment(awrsRef)(headerCarrier, implicitly)
    }

    "return EnrolmentSuccessResponse when ES20 returns successful response" in {
      val urn = "XKAW00000200130"
      val postcode = "SW1A 2AA"
      val response: EnrolmentSuccessResponse = successResponse(urn, postcode)
      mockPostResponseES20(OK, Some(Json.toJson(response).toString()))
      val knownFacts = KnownFacts(urn)
      await(connector.lookupEnrolments(knownFacts)) mustBe Some(response)
    }

    "return None when ES20 returns NO_CONTENT" in {
      mockPostResponseES20(NO_CONTENT, None)
      val knownFacts = KnownFacts("XKAW00000200130")
      await(connector.lookupEnrolments(knownFacts)) mustBe None
    }

    "return None when ES20 returns BAD_REQUEST" in {
      mockPostResponseES20(BAD_REQUEST, None)
      val knownFacts = KnownFacts("XKAW00000200130")
      await(connector.lookupEnrolments(knownFacts)) mustBe None
    }

    "return None when ES20 returns unknown status" in {
      mockPostResponseES20(999, None)
      val knownFacts = KnownFacts("XKAW00000200130")
      await(connector.lookupEnrolments(knownFacts)) mustBe None
    }

    "return PrincipalGorupId if found in response" in {
      mockResponseES1(OK, es1ResponseWithGroupId)
      val result = testCall
      await(result) mustBe Some("TestPrincipalGroupId")
    }

    "return None if no PrincipalGroupId found in response" in {
      mockResponseES1(OK, es1ResponseWithoutGroupId)
      val result = testCall
      await(result) mustBe None
    }

    "return None  if response status is NO_CONTENT  " in {
      mockResponseES1(NO_CONTENT)
      val result = testCall
      await(result) mustBe None
    }

    "return None  if response status is BAD_REQUEST  " in {
      mockResponseES1(BAD_REQUEST)
      val result = testCall
      await(result) mustBe None
    }

    "return None  if response status is BAD_GATEWAY  " in {
      mockResponseES1(BAD_REQUEST)
      val result = testCall
      await(result) mustBe None
    }

    "return None  if response status is NOT_FOUND  " in {
      mockResponseES1(NOT_FOUND)
      val result = testCall
      await(result) mustBe None
    }
  }

  private def successResponse(urn: String, postcode: String) = {
    EnrolmentSuccessResponse(
      service = "IR-SA",
      enrolments = Seq(
        Enrolment(
          identifiers = Seq(
            Identifier(key = "AWRSRefNumber", value = urn)
          ),
          verifiers = Seq(
            Verifier(key = "CTUTR", value = "AB112233D"),
            Verifier(key = "Postcode", value = postcode)
          )
        )
      )
    )
  }
}
