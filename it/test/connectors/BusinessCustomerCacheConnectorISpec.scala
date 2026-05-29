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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, urlEqualTo}
import org.scalatest.matchers.must.Matchers
import play.api.http.Status.{NOT_FOUND, OK, SERVICE_UNAVAILABLE}
import play.api.libs.json.{Format, Json}
import play.api.test.Injecting
import uk.gov.hmrc.helpers.IntegrationSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class BusinessCustomerCacheConnectorISpec extends IntegrationSpec with Injecting with Matchers {

  private val businessCustomerCacheConnector: BusinessCustomerCacheConnector = inject[BusinessCustomerCacheConnector]

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  final case class TestReviewDetails(name: String, utr: String)

  object TestReviewDetails {
    implicit val format: Format[TestReviewDetails] = Json.format[TestReviewDetails]
  }

  private val externalDataPath = "/business-customer/external-data/awrs"

  private def mockResponse(responseStatus: Int, responseBody: String = ""): Unit = {
    wireMockServer.stubFor(
      get(urlEqualTo(externalDataPath))
        .willReturn(
          aResponse()
            .withStatus(responseStatus)
            .withHeader("Content-Type", "application/json")
            .withBody(responseBody)
        )
    )
  }

  "BusinessCustomerCacheConnector.getReviewBusinessDetails" must {

    "return review details when the downstream responds with 200 and valid json" in {
      val expectedResponse = TestReviewDetails("agency", "1234567890")

      mockResponse(
        OK,
        Json.toJson(expectedResponse).toString()
      )

      val result = await(businessCustomerCacheConnector.getReviewBusinessDetails[TestReviewDetails])

      result mustBe Some(expectedResponse)
    }

    "return None when the downstream responds with 404" in {
      mockResponse(NOT_FOUND)

      val result = await(businessCustomerCacheConnector.getReviewBusinessDetails[TestReviewDetails])

      result mustBe None
    }

    "return None when the downstream responds with an unexpected status" in {
      mockResponse(SERVICE_UNAVAILABLE)

      val result = await(businessCustomerCacheConnector.getReviewBusinessDetails[TestReviewDetails])

      result mustBe None
    }

    "return None when the downstream responds with 200 and invalid json" in {
      mockResponse(
        OK,
        """this-is-not-json"""
      )

      val result = await(businessCustomerCacheConnector.getReviewBusinessDetails[TestReviewDetails])

      result mustBe None
    }

    "return None when the downstream responds with 200 and json that does not validate as T" in {
      mockResponse(
        OK,
        """{"foo":"bar"}"""
      )

      val result = await(businessCustomerCacheConnector.getReviewBusinessDetails[TestReviewDetails])

      result mustBe None
    }
  }

}
