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

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, stubFor, urlEqualTo}
import org.scalatest.matchers.must.Matchers
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.test.Injecting
import uk.gov.hmrc.helpers.IntegrationSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class LookupServiceISpec extends IntegrationSpec with Injecting with Matchers {

  val service: LookupService = inject[LookupService]
  override implicit val hc: HeaderCarrier = HeaderCarrier()
  val testUrn = "XXAW00000000054"

  "lookup" must {

    "return a SearchResult when the response is 200 OK" in {
      val validResponseJson = Json.parse(
        s"""
           {
             "results": [
               {
                 "class": "Business",
                 "data": {
                   "awrsRef": "$testUrn",
                   "status": "Approved",
                   "registrationDate": "01-01-2023",
                   "registrationEndDate": "01-01-2024",
                   "info": {
                     "businessName": "Test Business",
                      "tradingName": "Test Trading",
                      "fullName": "Test Full Name",
                     "address" : {
                        "addressLine1": "Test Street 1",
                        "addressLine2": "Test Line 2",
                        "addressLine3": "Test City",
                        "addressLine4": "Test County",
                        "postcode": "TE1 2ST",
                        "addressCountry": "United Kingdom",
                        "addressCountryCode": "GB"
                     }
                   }
                 }
               }
             ]
           }
         """
      )
      stubFor(WireMock.get(urlEqualTo(s"/awrs-lookup/query/urn/$testUrn"))
        .willReturn(aResponse().withStatus(OK).withBody(validResponseJson.toString())))

      val result = await(service.lookup(testUrn))
      result mustBe defined
      result.get.results.head.awrsRef mustBe "XXAW00000000054"
    }

    "return None when the response is 404 and contains the referenceNotFoundString - no match for urn" in {
      stubFor(WireMock.get(urlEqualTo(s"/awrs-lookup/query/urn/XXAW00000123456"))
        .willReturn(aResponse().withStatus(NOT_FOUND).withBody("AWRS reference not found")))

      val result = await(service.lookup("XXAW00000123456"))
      result mustBe None
    }
  }
}