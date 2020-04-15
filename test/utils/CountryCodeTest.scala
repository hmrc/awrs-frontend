/*
 * Copyright 2020 HM Revenue & Customs
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

package utils

import models.{Address, Supplier}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import uk.gov.hmrc.play.test.UnitSpec

class CountryCodeTest extends UnitSpec with MockitoSugar with OneServerPerSuite {

  trait Setup {
    val countryCodes = new CountryCodesImpl(app.environment)
  }

  "CountryCode countries" should {
    "return a string of countries" in new Setup {
      val countries: String = countryCodes.countries
      countries should include("Andorra")
      countries should include("Germany")
      countries should include("France")
    }
  }

  "CountryCode getCountry" should {
    "return a country from a country code" in new Setup {
      countryCodes.getCountry("AD") should be(Some("Andorra"))
      countryCodes.getCountry("DE") should be(Some("Germany"))
      countryCodes.getCountry("FR") should be(Some("France"))

      countryCodes.getCountry("ZZ") should be(None)
    }
  }

  "CountryCode getCountryCode" should {
    "return a country code from a country" in new Setup {
      countryCodes.getCountryCode("Andorra") should be(Some("AD"))
      countryCodes.getCountryCode("Germany") should be(Some("DE"))
      countryCodes.getCountryCode("France") should be(Some("FR"))

      countryCodes.getCountryCode("ZZ") should be(None)
    }
  }

  "CountryCode getSupplierAddressWithCountryCode" should {
    "get an address with a country code" in new Setup {
      val supplierAddress = Address("Supplier Address 1", "Supplier Address 2", None, None, None, Some("Andorra"))
      val supplier = Supplier(None, None, None, Some(supplierAddress), None, None, None)

      countryCodes.getSupplierAddressWithCountryCode(supplier).get.addressCountryCode should be(Some("AD"))

      val supplierAddressNone = Address("Supplier Address 1", "Supplier Address 2", None, None, None, None)
      val supplierNone = Supplier(None, None, None, Some(supplierAddressNone), None, None, None)

      countryCodes.getSupplierAddressWithCountryCode(supplierNone).get.addressCountryCode should be(Some("GB"))

      val supplierEmpty = Supplier(None, None, None, None, None, None, None)

      countryCodes.getSupplierAddressWithCountryCode(supplierEmpty).isEmpty shouldBe true
    }
  }

  "CountryCode getAddressWithCountryCode" should {
    "get an address with a country code" in new Setup {
      val supplierAddress = Address("Supplier Address 1", "Supplier Address 2", None, None, None, Some("Andorra"))

      countryCodes.getAddressWithCountryCode(Some(supplierAddress)).get.addressCountryCode should be(Some("AD"))

      val supplierAddressNone = Address("Supplier Address 1", "Supplier Address 2", None, None, None, None)

      countryCodes.getAddressWithCountryCode(Some(supplierAddressNone)).get.addressCountryCode should be(Some("GB"))
      countryCodes.getAddressWithCountryCode(None).isEmpty shouldBe true
    }
  }

  "CountryCode getSupplierAddressWithCountry" should {
    "get an address with a country" in new Setup {
      val supplierAddress = Address("Supplier Address 1", "Supplier Address 2", None, None, None, None, Some("AD"))
      val supplier = Supplier(None, None, None, Some(supplierAddress), None, None, None)

      countryCodes.getSupplierAddressWithCountry(supplier).get.addressCountry should be(Some("Andorra"))

      val supplierAddressNone = Address("Supplier Address 1", "Supplier Address 2", None, None, None, None)
      val supplierNone = Supplier(None, None, None, Some(supplierAddressNone), None, None, None)

      countryCodes.getSupplierAddressWithCountry(supplierNone).get.addressCountry should be(Some("United Kingdom"))

      val supplierEmpty = Supplier(None, None, None, None, None, None, None)

      countryCodes.getSupplierAddressWithCountry(supplierEmpty).isEmpty shouldBe true
    }
  }

  "CountryCode getAddressWithCountry" should {
    "get an address with a country" in new Setup {
      val supplierAddress = Address("Supplier Address 1", "Supplier Address 2", None, None, None, None, Some("AD"))

      countryCodes.getAddressWithCountry(Some(supplierAddress)).get.addressCountry should be(Some("Andorra"))

      val supplierAddressNone = Address("Supplier Address 1", "Supplier Address 2", None, None, None, None)

      countryCodes.getAddressWithCountry(Some(supplierAddressNone)).get.addressCountry should be(Some("United Kingdom"))
      countryCodes.getAddressWithCountry(None).isEmpty shouldBe true
    }
  }
}
