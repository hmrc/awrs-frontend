/*
 * Copyright 2026 HM Revenue & Customs
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

import audit.Auditable
import models.BusinessCustomerDetails
import org.scalatest.matchers.must.Matchers
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.AwrsUnitTestTraits

class BusinessCustomerCacheConnectorTest extends AwrsUnitTestTraits with Matchers {

  private val mockHttpClientV2                    = mock[uk.gov.hmrc.http.client.HttpClientV2]
  override val mockServicesConfig: ServicesConfig = mock[uk.gov.hmrc.play.bootstrap.config.ServicesConfig]
  override val mockAuditable: Auditable           = mock[audit.Auditable]

  private val service = new BusinessCustomerCacheConnector(
    mockServicesConfig,
    mockHttpClientV2,
    mockAuditable
  )

  private val testBusinessCustomerDetails =
    BusinessCustomerDetails(
      businessName = "agency",
      businessType = Some("Corporate Body"),
      businessAddress = models.BCAddress(
        "23 High Street",
        "Park View",
        Some("Gloucester"),
        Some("Gloucestershire"),
        Some("NE98 1ZZ"),
        Some("GB")
      ),
      sapNumber = "1234567890",
      safeId = "XE0001234567890",
      isAGroup = false,
      agentReferenceNumber = Some("JARN1234567")
    )

  "handleResponse" must {

    "return Some(details) for 200 with valid json" in {
      val response = HttpResponse(
        status = Status.OK,
        body = Json.toJson(testBusinessCustomerDetails).toString()
      )

      service.handleResponse[BusinessCustomerDetails](response) mustBe Some(testBusinessCustomerDetails)
    }

    "return None for 404" in {
      val response = HttpResponse(status = Status.NOT_FOUND, body = "")

      service.handleResponse[BusinessCustomerDetails](response) mustBe None
    }

    "return None for unexpected status" in {
      val response = HttpResponse(status = Status.SERVICE_UNAVAILABLE, body = "")

      service.handleResponse[BusinessCustomerDetails](response) mustBe None
    }

    "return None for 200 with invalid json" in {
      val response = HttpResponse(status = Status.OK, body = "not-json")

      service.handleResponse[BusinessCustomerDetails](response) mustBe None
    }

    "return None for 200 with json that does not validate as T" in {
      val response = HttpResponse(status = Status.OK, body = """{"foo":"bar"}""")

      service.handleResponse[BusinessCustomerDetails](response) mustBe None
    }
  }

}
