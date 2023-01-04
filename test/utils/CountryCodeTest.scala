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

package utils

import models.{Address, Supplier}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class CountryCodeTest extends PlaySpec with MockitoSugar with GuiceOneAppPerSuite {

  trait Setup {
    val countryCodes = new CountryCodes(app.environment)
  }

  "CountryCode countries" must {
    "return a string of countries" in new Setup {
      val countries: String = countryCodes.countries
      countries must include("Andorra")
      countries must include("France")
      countries must include("Germany")
      countries must include("Åland Islands")
      countries must include("Curaçao")
      countries must include("Türkiye")
      countries must include("British Indian Ocean Territory (the)")
    }
  }

  "CountryCode getCountry" must {
    "return a country from a country code" in new Setup {
      countryCodes.getCountry("AD") must be(Some("Andorra"))
      countryCodes.getCountry("FR") must be(Some("France"))
      countryCodes.getCountry("DE") must be(Some("Germany"))
      countryCodes.getCountry("AX") must be(Some("Åland Islands"))
      countryCodes.getCountry("CW") must be(Some("Curaçao"))
      countryCodes.getCountry("TR") must be(Some("Türkiye"))
      countryCodes.getCountry("IO") must be(Some("British Indian Ocean Territory (the)"))

      countryCodes.getCountry("ZZ") must be(None)
    }
  }

  "CountryCode getCountryCode" must {
    "return a country code from a country" in new Setup {
      countryCodes.getCountryCode("Andorra") must be(Some("AD"))
      countryCodes.getCountryCode("Germany") must be(Some("DE"))
      countryCodes.getCountryCode("France") must be(Some("FR"))
      countryCodes.getCountryCode("Åland Islands") must be(Some("AX"))
      countryCodes.getCountryCode("Curaçao") must be(Some("CW"))
      countryCodes.getCountryCode("Türkiye") must be(Some("TR"))
      countryCodes.getCountryCode("British Indian Ocean Territory (the)") must be(Some("IO"))

      countryCodes.getCountryCode("ZZ") must be(None)
    }
  }

  "CountryCode getSupplierAddressWithCountryCode" must {
    "get an address with a country code" in new Setup {
      val supplierAddress: Address = Address("Supplier Address 1", "Supplier Address 2", None, None, None, Some("Andorra"))
      val supplier: Supplier = Supplier(None, None, None, Some(supplierAddress), None, None, None)

      countryCodes.getSupplierAddressWithCountryCode(supplier).get.addressCountryCode must be(Some("AD"))

      val supplierAddressNone: Address = Address("Supplier Address 1", "Supplier Address 2", None, None, None, None)
      val supplierNone: Supplier = Supplier(None, None, None, Some(supplierAddressNone), None, None, None)

      countryCodes.getSupplierAddressWithCountryCode(supplierNone).get.addressCountryCode must be(Some("GB"))

      val supplierEmpty: Supplier = Supplier(None, None, None, None, None, None, None)

      countryCodes.getSupplierAddressWithCountryCode(supplierEmpty).isEmpty mustBe true
    }
  }

  "CountryCode getAddressWithCountryCode" must {
    "get an address with a country code" in new Setup {
      val supplierAddress: Address = Address("Supplier Address 1", "Supplier Address 2", None, None, None, Some("Andorra"))

      countryCodes.getAddressWithCountryCode(Some(supplierAddress)).get.addressCountryCode must be(Some("AD"))

      val supplierAddressNone: Address = Address("Supplier Address 1", "Supplier Address 2", None, None, None, None)

      countryCodes.getAddressWithCountryCode(Some(supplierAddressNone)).get.addressCountryCode must be(Some("GB"))
      countryCodes.getAddressWithCountryCode(None).isEmpty mustBe true
    }
  }

  "CountryCode getSupplierAddressWithCountry" must {
    "get an address with a country" in new Setup {
      val supplierAddress: Address = Address("Supplier Address 1", "Supplier Address 2", None, None, None, None, Some("AD"))
      val supplier: Supplier = Supplier(None, None, None, Some(supplierAddress), None, None, None)

      countryCodes.getSupplierAddressWithCountry(supplier).get.addressCountry must be(Some("Andorra"))

      val supplierAddressNone: Address = Address("Supplier Address 1", "Supplier Address 2", None, None, None, None)
      val supplierNone: Supplier = Supplier(None, None, None, Some(supplierAddressNone), None, None, None)

      countryCodes.getSupplierAddressWithCountry(supplierNone).get.addressCountry must be(Some("United Kingdom"))

      val supplierEmpty: Supplier = Supplier(None, None, None, None, None, None, None)

      countryCodes.getSupplierAddressWithCountry(supplierEmpty).isEmpty mustBe true
    }
  }

  "CountryCode getAddressWithCountry" must {
    "get an address with a country" in new Setup {
      val supplierAddress: Address = Address("Supplier Address 1", "Supplier Address 2", None, None, None, None, Some("AD"))

      countryCodes.getAddressWithCountry(Some(supplierAddress)).get.addressCountry must be(Some("Andorra"))

      val supplierAddressNone: Address = Address("Supplier Address 1", "Supplier Address 2", None, None, None, None)

      countryCodes.getAddressWithCountry(Some(supplierAddressNone)).get.addressCountry must be(Some("United Kingdom"))
      countryCodes.getAddressWithCountry(None).isEmpty mustBe true
    }
  }
}
