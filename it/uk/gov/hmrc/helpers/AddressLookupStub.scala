package uk.gov.hmrc.helpers

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalToJson, post, stubFor, urlMatching}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.test.Helpers._

object AddressLookupStub {

  def stubPost(url: String, requestBody: Option[String] = None, status: Integer, responseBody: String): StubMapping =
    stubFor(post(urlMatching(url))
      .withRequestBody(equalToJson(requestBody.getOrElse("")))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withBody(responseBody)
      )
    )

  def responsePostUprn(uprn: String)(status: Int, body: String): Unit =
    stubPost("/lookup/by-uprn", Some(s"""{"uprn" : "$uprn"}"""), status, body)

  def responsePostPostcode(postcode: String)(status: Int, body: String): Unit = {
    stubPost("/lookup", Some(s"""{ "postcode": "$postcode" }"""), status, body)
  }

  def errorResponsePostPostcode(postcode: String)(status: Int, body: String): Unit = {
    stubPost("/lookup", Some(s"""{ "postcode": "$postcode" }"""), status, body)
  }

  val partialSuccessResponse: String =
    s"""
       |[
       |    {
       |        "id": "GB200000698110",
       |        "uprn": 200000698110,
       |        "address": {
       |            "lines": [
       |                "2 The Test Close"
       |            ],
       |            "town": "Test Town",
       |            "postcode": "BB00 0BB",
       |            "subdivision": {
       |                "code": "GB-ENG",
       |                "name": "England"
       |            },
       |            "country": {
       |                "code": "GB",
       |                "name": "United Kingdom"
       |            }
       |        },
       |        "localCustodian": {
       |            "code": 1760,
       |            "name": "Test Valley"
       |        },
       |        "language": "en"
       |    },
       |    {
       |        "id": "GB200000708497",
       |        "uprn": 200000708497,
       |        "address": {
       |            "lines": [
       |                "4 Test Close"
       |            ],
       |            "town": "Test Town",
       |            "postcode": "BB00 0BB",
       |            "subdivision": {
       |                "code": "GB-ENG",
       |                "name": "England"
       |            },
       |            "country": {
       |                "code": "GB",
       |                "name": "United Kingdom"
       |            }
       |        },
       |        "localCustodian": {
       |            "code": 1760,
       |            "name": "Test Valley"
       |        },
       |        "language": "en"
       |    }
       |]
       |""".stripMargin

  val fullSuccessResponseJson: String =
    s"""
       |[
       |    {
       |        "id": "GB200000698110",
       |        "uprn": 200000698110,
       |        "address": {
       |            "lines": [
       |                "2 The Test Close"
       |            ],
       |            "town": "Test Town",
       |            "postcode": "BB00 0BB",
       |            "subdivision": {
       |                "code": "GB-ENG",
       |                "name": "England"
       |            },
       |            "country": {
       |                "code": "GB",
       |                "name": "United Kingdom"
       |            }
       |        },
       |        "localCustodian": {
       |            "code": 1760,
       |            "name": "Test Valley"
       |        },
       |        "language": "en"
       |    },
       |    {
       |        "id": "GB200000708497",
       |        "uprn": 200000708497,
       |        "address": {
       |            "lines": [
       |                "4 Test Close"
       |            ],
       |            "town": "Test Town",
       |            "postcode": "BB00 0BB",
       |            "subdivision": {
       |                "code": "GB-ENG",
       |                "name": "England"
       |            },
       |            "country": {
       |                "code": "GB",
       |                "name": "United Kingdom"
       |            }
       |        },
       |        "localCustodian": {
       |            "code": 1760,
       |            "name": "Test Valley"
       |        },
       |        "language": "en"
       |    },
       |    {
       |        "id": "GB200000704710",
       |        "uprn": 200000704710,
       |        "address": {
       |            "lines": [
       |                "6 Test Close"
       |            ],
       |            "town": "Test Town",
       |            "postcode": "BB00 0BB",
       |            "subdivision": {
       |                "code": "GB-ENG",
       |                "name": "England"
       |            },
       |            "country": {
       |                "code": "GB",
       |                "name": "United Kingdom"
       |            }
       |        },
       |        "localCustodian": {
       |            "code": 1760,
       |            "name": "Test Valley"
       |        },
       |        "language": "en"
       |    },
       |    {
       |        "id": "GB200000700558",
       |        "uprn": 200000700558,
       |        "address": {
       |            "lines": [
       |                "8 Test Close"
       |            ],
       |            "town": "Test Town",
       |            "postcode": "BB00 0BB",
       |            "subdivision": {
       |                "code": "GB-ENG",
       |                "name": "England"
       |            },
       |            "country": {
       |                "code": "GB",
       |                "name": "United Kingdom"
       |            }
       |        },
       |        "localCustodian": {
       |            "code": 1760,
       |            "name": "Test Valley"
       |        },
       |        "language": "en"
       |    },
       |    {
       |        "id": "GB200010012154",
       |        "uprn": 200010012154,
       |        "address": {
       |            "lines": [
       |                "Test Lodge",
       |                "Test Close"
       |            ],
       |            "town": "Test Town",
       |            "postcode": "BB00 0BB",
       |            "subdivision": {
       |                "code": "GB-ENG",
       |                "name": "England"
       |            },
       |            "country": {
       |                "code": "GB",
       |                "name": "United Kingdom"
       |            }
       |        },
       |        "localCustodian": {
       |            "code": 1760,
       |            "name": "Test Valley"
       |        },
       |        "language": "en"
       |    },
       |    {
       |        "id": "GB200000706253",
       |        "uprn": 200000706253,
       |        "address": {
       |            "lines": [
       |                "Test House",
       |                "Test Close"
       |            ],
       |            "town": "Test Town",
       |            "postcode": "BB00 0BB",
       |            "subdivision": {
       |                "code": "GB-ENG",
       |                "name": "England"
       |            },
       |            "country": {
       |                "code": "GB",
       |                "name": "United Kingdom"
       |            }
       |        },
       |        "localCustodian": {
       |            "code": 1760,
       |            "name": "Test Valley"
       |        },
       |        "language": "en"
       |    }
       |]
       |""".stripMargin

  def postPostcodeFullSuccessResponse(): Unit = responsePostPostcode("BB000BB")(OK, fullSuccessResponseJson)

  def postPostcodePartialSuccessResponse(): Unit = responsePostPostcode("BB000BB")(OK, partialSuccessResponse)

}
