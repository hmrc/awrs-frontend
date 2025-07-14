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

import models.reenrolment.{AwrsKnownFacts, EnrolledUserIds, Enrolment, Identifier, KnownFactsResponse, Verifier}
import org.scalatest.matchers.must.Matchers
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.Injecting
import uk.gov.hmrc.helpers.IntegrationSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import models.reenrolment.KnownFactsResponse._

class EnrolmentStoreProxyConnectorISpec extends IntegrationSpec with Injecting with Matchers {

  val connector: EnrolmentStoreProxyConnector   = inject[EnrolmentStoreProxyConnector]
  val awrsRef                                   = "TestAwrsRef"
  val es1ResponseWithGroupId: Option[String]    = Some("""{"principalGroupIds": ["TestPrincipalGroupId"]}""")
  val es1ResponseWithoutGroupId: Option[String] = Some("""{"principalGroupIds": []}""")

  "Enrolment Store Connector" must {
    // used in the mock to check the destination of the connector calls
    lazy val es1Url =
      s"/enrolment-store-proxy/enrolment-store/enrolments/${connector.awrsServiceName}~${connector.enrolmentIdentifierName}~$awrsRef/groups"
    lazy val es20Url = s"/enrolment-store-proxy/enrolment-store/enrolments"
    lazy val es0Url = s"/enrolment-store-proxy/enrolment-store/enrolments/${connector.awrsServiceName}~${connector.enrolmentIdentifierName}~$awrsRef/users"

    // these values doesn't really matter since the call itself is mocked e

    def mockResponseES1(responseStatus: Int, responseString: Option[String] = None): Unit = {
      stubbedGet(es1Url, responseStatus, responseString.getOrElse(""))
    }

    def mockPostResponseES20(responseStatus: Int, responseString: Option[String]): Unit = {
      stubbedPost(es20Url, responseStatus, responseString.getOrElse(""))
    }

    def mockPostResponseES0(responseStatus: Int, responseString: Option[String]): Unit = {
      stubbedGet(es0Url, responseStatus, responseString.getOrElse(""))
    }

    def testCall(implicit headerCarrier: HeaderCarrier): Future[Option[String]] = {
      connector.queryForPrincipalGroupIdOfAWRSEnrolment(awrsRef)(headerCarrier, implicitly)
    }

    "return EnrolmentSuccessResponse when ES20 returns successful response" in {
      val urn                          = "XKAW00000200130"
      val postcode                     = "SW1A 2AA"
      val response: KnownFactsResponse = es20SuccessResponse(urn, postcode)
      mockPostResponseES20(OK, Some(Json.toJson(response).toString()))
      val knownFacts = AwrsKnownFacts(urn)
      await(connector.lookupEnrolments(knownFacts)) mustBe Some(response)
    }

    "return EnrolmentSuccessResponse when ES0 returns successful response" in {
      val response: EnrolledUserIds = es0SuccessResponse(awrsRef)
      mockPostResponseES0(OK, Some(Json.toJson(response).toString()))
      await(connector.queryForEnrolments(awrsRef)) mustBe Some(response)
    }

    "return None when ES20 returns NO_CONTENT" in {
      mockPostResponseES20(NO_CONTENT, None)
      val knownFacts = AwrsKnownFacts("XKAW00000200130")
      await(connector.lookupEnrolments(knownFacts)) mustBe None
    }

    "return None when ES20 returns BAD_REQUEST" in {
      mockPostResponseES20(BAD_REQUEST, None)
      val knownFacts = AwrsKnownFacts("XKAW00000200130")
      await(connector.lookupEnrolments(knownFacts)) mustBe None
    }

    "return None when ES20 returns unknown status" in {
      mockPostResponseES20(999, None)
      val knownFacts = AwrsKnownFacts("XKAW00000200130")
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

    "return None if response status is NO_CONTENT  " in {
      mockResponseES1(NO_CONTENT)
      val result = testCall
      await(result) mustBe None
    }

    "return None if response status is BAD_REQUEST  " in {
      mockResponseES1(BAD_REQUEST)
      val result = testCall
      await(result) mustBe None
    }

    "return None if response status is BAD_GATEWAY  " in {
      mockResponseES1(BAD_REQUEST)
      val result = testCall
      await(result) mustBe None
    }

    "return None if response status is NOT_FOUND  " in {
      mockResponseES1(NOT_FOUND)
      val result = testCall
      await(result) mustBe None
    }
  }

  private def es20SuccessResponse(urn: String, postcode: String) = {
    KnownFactsResponse(
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

  private def es0SuccessResponse(urn: String) =
    EnrolledUserIds(
      principalUserIds = Seq(urn), delegatedUserIds = Seq.empty
    )

}
