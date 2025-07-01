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

package uk.gov.hmrc.helpers

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalToJson, post, stubFor, urlMatching}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.helpers.AddressLookupStub.stubPost

object EnrolmentsLookupStub {

  private val enrolmentLookupSuccessResponse: String =
    s"""
       |{
       |  "service": "IR-SA",
       |  "enrolments": [
       |    {
       |      "identifiers": [
       |        {
       |          "key": "UTR",
       |          "value": "1234567890"
       |        }
       |      ],
       |      "verifiers": [
       |        {
       |          "key": "NINO",
       |          "value": "AB112233D"
       |        },
       |        {
       |          "key": "Postcode",
       |          "value": "SW1A 2AA"
       |        }
       |      ]
       |    }
       |  ]
       |}
       |""".stripMargin

  private def enrolmentRequest(utr: String, postcode: String): String =
    s"""
       |{
       |  "service": "IR-SA",
       |  "knownFacts": [
       |    {
       |      "key": "UTR",
       |      "value": "$utr"
       |    },
       |    {
       |      "key": "Postcode",
       |      "value": "$postcode"
       |    }
       |  ]
       |}
       |""".stripMargin

  private def stubPost(url: String, requestBody: Option[String] = None, status: Integer, responseBody: String): StubMapping =
    stubFor(
      post(urlMatching(url))
        .withRequestBody(equalToJson(requestBody.getOrElse("")))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(responseBody)
        ))

  def stubEnrolmentSuccessResponse(utr: String, postcode: String)(status: Int, responseBody: String = enrolmentLookupSuccessResponse): Unit = {
    stubPost("/enrolments", Some(enrolmentRequest(utr, postcode)), status, responseBody)
  }

}
