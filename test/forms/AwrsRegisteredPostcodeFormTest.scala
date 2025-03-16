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

  "Declaration form validation" must {

    val fieldId = "registeredPostcode"

    "the postcode field is left empty" in {
      form.bind(Map(fieldId -> "")).fold(
        formWithErrors => {
          formWithErrors(fieldId).errors.size mustBe 1
          formWithErrors(fieldId).errors.head.message mustBe "awrs.register_postcode.error.empty"
        },
        _ => fail("Field should contain errors")
      )
    }

    "the postcode field has invalid uk postcode format-test " in {
      form.bind(Map(fieldId -> "test")).fold(
        formWithErrors => {
          formWithErrors(fieldId).errors.size mustBe 1
          formWithErrors(fieldId).errors.head.message mustBe "awrs.register_postcode.error.invalid_postcode"
        },
        _ => fail("Field should contain errors")
      )
    }

    "the postcode field has invalid uk postcode format - 1111ne" in {
      form.bind(Map(fieldId -> "1111ne")).fold(
        formWithErrors => {
          formWithErrors(fieldId).errors.size mustBe 1
          formWithErrors(fieldId).errors.head.message mustBe "awrs.register_postcode.error.invalid_postcode"
        },
        _ => fail("Field should contain errors")
      )
    }

    "the postcode field has valid uk postcode format ne270jz" in {
      val data: Map[String, String] =
        Map("registeredPostcode" -> "ne270jz"
        )
      assertFormIsValid(form, data)
    }

    "the postcode field has valid uk postcode format n e2 7 0 jz with spaces" in {
      val data: Map[String, String] =
        Map("registeredPostcode" -> "n e2 7 0 jz"
        )
      assertFormIsValid(form, data)
    }
  }
}
