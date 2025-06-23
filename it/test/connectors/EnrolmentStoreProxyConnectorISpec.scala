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
import play.api.http.Status.OK
import play.api.test.Helpers._
import play.api.test.Injecting
import uk.gov.hmrc.helpers.IntegrationSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EnrolmentStoreProxyConnectorISpec extends IntegrationSpec with Injecting with Matchers {

  val connector: EnrolmentStoreProxyConnector = inject[EnrolmentStoreProxyConnector]
  val awrsRef = "TestAwrsRef"
  val es1ResponseWithGroupId = Some("""{"principalGroupIds": ["TestPrincipalGroupId"], "delegatedGroupIds": []}""")
  val es1ResponseWithoutGroupId = Some("""{"principalGroupIds": [], "delegatedGroupIds": []}""")

  "Enrolment Store Connector" must {
    // used in the mock to check the destination of the connector calls
    lazy val es1Url= s"/enrolment-store-proxy/enrolment-store/enrolments/${connector.AWRS_SERVICE_NAME}~${connector.EnrolmentIdentifierName}~$awrsRef/groups"

    // these values doesn't really matter since the call itself is mocked e

    def mockResponse(responseStatus: Int, responseString: Option[String] = None): Unit = {
      stubbedGet(es1Url, responseStatus, responseString.getOrElse(""))
    }

    def testCall(implicit headerCarrier: HeaderCarrier): Future[Option[String]] = {
      connector.queryGroupIdForEnrolment(awrsRef)(headerCarrier, implicitly)
    }

    "return status as OK, for successful query which returns group id" in {
      mockResponse(OK, es1ResponseWithGroupId)
      val result = testCall
      await(result) mustBe Some("TestPrincipalGroupId")
    }

    "return status as OK, but no group ids" in {
      mockResponse(OK, es1ResponseWithoutGroupId)
      val result = testCall
      await(result) mustBe None
    }

    "return status NO_CONTENT" in {
      mockResponse(NO_CONTENT)
      val result = testCall
      await(result) mustBe None
    }
  }

}
