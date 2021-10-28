package uk.gov.hmrc.helpers.connectors

import connectors.{AddressLookupConnector, AddressLookupSuccessResponse}
import org.scalatest.MustMatchers
import play.api.test.Injecting
import uk.gov.hmrc.address.client.v1.{Address, AddressRecord, Country, RecordSet}
import uk.gov.hmrc.helpers.{AddressLookupStub, IntegrationSpec}

import scala.concurrent.ExecutionContext.Implicits.global

class AddressLookupConnectorISpec extends IntegrationSpec with Injecting with MustMatchers {

  val connector = inject[AddressLookupConnector]

  "AddressLookupConnector.lookup" when {
    "address-lookup returns a non empty list of addresses" must {
      "return a AddressLookupSuccessResponse containing a seq of addresses" in {
        AddressLookupStub.postPostcodePartialSuccessResponse
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
  }
}