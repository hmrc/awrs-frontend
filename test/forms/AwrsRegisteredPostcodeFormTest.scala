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

package forms

import forms.test.util._
import models.AwrsRegisteredPostcode
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.Form
import utils.AwrsFieldConfig

class AwrsRegisteredPostcodeFormTest extends PlaySpec with MockitoSugar  with AwrsFieldConfig with AwrsFormTestUtils {

  implicit lazy val form: Form[AwrsRegisteredPostcode] = AwrsRegisteredPostcodeForm.awrsRegisteredPostcodeForm.form

  "Postcode form validation" must {

    val fieldId = "registeredPostcode"

    "contain an error if the postcode field is left empty" in {
      form.bind(Map(fieldId -> "")).fold(
        formWithErrors => {
          formWithErrors(fieldId).errors.size mustBe 1
          formWithErrors(fieldId).errors.head.message mustBe "awrs.register_postcode.error.empty"
        },
        _ => fail("Field should contain errors")
      )
    }

    "contain an error if the postcode field has invalid uk postcode - test" in {
      form.bind(Map(fieldId -> "test")).fold(
        formWithErrors => {
          formWithErrors(fieldId).errors.size mustBe 1
          formWithErrors(fieldId).errors.head.message mustBe "awrs.register_postcode.error.invalid_postcode"
        },
        _ => fail("Field should contain errors")
      )
    }

    "contain an error if the postcode field has invalid uk postcode - 1111ne" in {
      form.bind(Map(fieldId -> "1111ne")).fold(
        formWithErrors => {
          formWithErrors(fieldId).errors.size mustBe 1
          formWithErrors(fieldId).errors.head.message mustBe "awrs.register_postcode.error.invalid_postcode"
        },
        _ => fail("Field should contain errors")
      )
    }

    "be valid if the postcode field has valid uk postcode format ne270jz" in {
      val data: Map[String, String] =
        Map("registeredPostcode" -> "ne270jz"
        )
      assertFormIsValid(form, data)
    }

    "be valid if the postcode field has valid uk postcode with upper and lower cases" in {
      val data: Map[String, String] =
        Map("registeredPostcode" -> "Ne270jZ"
        )
      assertFormIsValid(form, data)
    }

    "be valid if the postcode field has valid uk postcode format n e2 7 0 jz with spaces" in {
      val data: Map[String, String] =
        Map(fieldId -> "n e2 7 0 jz"
        )
      assertFormIsValid(form, data)
    }

    "be valid if the postcode field contains a valid uk postcode and punctuation, brackets or blanks for NE270JZ" in {
      val data: Map[String, String] =
        Map(fieldId -> "(Ne 2_7) - [0 jZ{ *}]"
        )
      assertFormIsValid(form, data)
    }

    "contain an error if the postcode field contains invalid not allowed character &" in {
      form.bind(Map(fieldId -> "NE27&0JZ")).fold(
        formWithErrors => {
          formWithErrors(fieldId).errors.size mustBe 1
          formWithErrors(fieldId).errors.head.message mustBe "awrs.register_postcode.error.invalid_postcode"
        },
        _ => fail("Field should contain errors")
      )
    }
  }
}
