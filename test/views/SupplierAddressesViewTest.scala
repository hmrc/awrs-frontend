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

package views

import forms.SupplierAddressesForm.supplierAddressesForm
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import views.view_application.helpers.LinearViewMode

class SupplierAddressesViewTest extends ViewTestFixture {

  val templateSupplierAddresses: views.html.awrs_supplier_addresses = app.injector.instanceOf[views.html.awrs_supplier_addresses]

  val htmlContent: HtmlFormat.Appendable = templateSupplierAddresses.apply(supplierAddressesForm(mockAppConfig).form, 1, isNewRecord = true, countryList = Seq(("String", "String"))) (fakeRequest, viewApplicationType = LinearViewMode, messages, mockAppConfig)

  "the supplier addresses page" must {

    "display the correct header" in {
      document.getElementById("supplier-addresses-title").text() mustBe Messages("awrs.supplier-addresses.heading.first")
    }

    "display the correct sub header" in {
      document.getElementById("supplier-address-description").text() mustBe Messages("awrs.supplier-addresses.description")
    }

    "display the correct radio button options" in {
      val radioOptions = document.getElementsByClass("govuk-radios__input")

      radioOptions.get(0).attr("value") mustBe "Yes"
      radioOptions.get(1).attr("value") mustBe "No"
    }

    "display the continue button" in {
      document.getElementById("save-and-continue").text() mustBe Messages("awrs.generic.save_continue")
    }

    "display the back link" in {
      back_link mustBe Messages("awrs.generic.back")
      back_link_href mustBe "/alcohol-wholesale-scheme/back?section=suppliers"
    }
  }
}
