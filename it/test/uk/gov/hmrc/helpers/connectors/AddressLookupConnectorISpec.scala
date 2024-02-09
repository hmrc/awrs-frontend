/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.helpers.connectors

import connectors.{AddressLookupConnector, AddressLookupErrorResponse, AddressLookupSuccessResponse}
import org.scalatest.matchers.must.Matchers
import play.api.test.Injecting
import uk.gov.hmrc.address.client.v1.{Address, AddressRecord, Country, RecordSet}
import uk.gov.hmrc.helpers.{AddressLookupStub, IntegrationSpec}

import scala.concurrent.ExecutionContext.Implicits.global

class AddressLookupConnectorISpec extends IntegrationSpec with Injecting with Matchers {

  val connector = inject[AddressLookupConnector]

  "AddressLookupConnector.lookup" when {
    "address-lookup returns a non empty list of addresses" must {
      "return a AddressLookupSuccessResponse containing a seq of addresses" in {
        AddressLookupStub.postPostcodePartialSuccessResponse()
        await(connector.lookup("BB000BB")) mustBe
          AddressLookupSuccessResponse(
            RecordSet(Seq(
              AddressRecord(
                id = "GB200000698110",
                address = Address(
                  lines = Seq( "2 The Test Close"),
                  town = Some("Test Town"),
                  postcode = "BB00 0BB",
                  country = Country("GB", "United Kingdom")),
                language = "en"),
              AddressRecord(
                id = "GB200000708497",
                address = Address(
                  lines = Seq( "4 Test Close"),
                  town = Some("Test Town"),
                  postcode = "BB00 0BB",
                  country = Country("GB", "United Kingdom")),
                language = "en")
            ))
          )
      }
    }
    "an exception is encountered when calling Address Lookup" must {
      "return and AddressLookupErrorResponse" in {
        lazy val res = {
          AddressLookupStub.errorResponsePostPostcode("BB000BB")(400, """{"Reason":"Your submission contains one or more errors."}""")
          await(connector.lookup("BB000BB"))
        }
        try {
          res
        }
        catch {
          case e: Exception => res mustBe AddressLookupErrorResponse(e)
        }
      }
    }
  }
}