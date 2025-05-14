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

package forms.reenrolment

import forms.reenrolment.RegisteredUrnForm.{awrsEnrolmentUrnForm, awrsUrn, maxQueryLength}
import forms.test.util.AwrsFormTestUtils
import models.AwrsEnrolmentUrn
import org.scalatestplus.play.PlaySpec
import play.api.data.Form

class RegisteredUrnFormTest extends PlaySpec  with AwrsFormTestUtils {

  "AwrsEnrolmentUrnForm" should {
    implicit val form: Form[AwrsEnrolmentUrn] = awrsEnrolmentUrnForm.form
    val fieldId = "awrsUrn"
    val fieldNameInErrorMessage = "awrsUrn field"


    "field is left blank" in {
      form.bind(Map(fieldId -> "")).fold(
        formWithErrors => {
          formWithErrors(fieldId).errors.size mustBe 1
          formWithErrors(fieldId).errors.head.message mustBe "awrs.awrsUrn.generic.error"
        },
        _ => fail("Field should contain errors")
      )
    }

    "field is more than max length" in {
      form.bind(Map(fieldId -> "a" * 141)).fold(
        formWithErrors => {
          formWithErrors(fieldId).errors.size mustBe 1
          messages(formWithErrors(fieldId).errors.head.message) mustBe messages("awrs.awrsUrn.generic.error", fieldNameInErrorMessage, maxQueryLength)
        },
        _ => fail("Field should contain errors")
      )
    }

    "field is more than length mismatch" in {
      form.bind(Map(fieldId -> "XAAW000001234567")).fold(
        formWithErrors => {
          formWithErrors(fieldId).errors.size mustBe 1
          messages(formWithErrors(fieldId).errors.head.message) mustBe messages("awrs.awrsUrn.generic.error", fieldNameInErrorMessage, maxQueryLength)
        },
        _ => fail("Field should contain errors")
      )
    }

    "field is more than length is less" in {
      form.bind(Map(fieldId -> "XAAW000001234")).fold(
        formWithErrors => {
          formWithErrors(fieldId).errors.size mustBe 1
          messages(formWithErrors(fieldId).errors.head.message) mustBe messages("awrs.awrsUrn.generic.error", fieldNameInErrorMessage, maxQueryLength)
        },
        _ => fail("Field should contain errors")
      )
    }

    "field has zero mismatches" in {
      form.bind(Map(fieldId -> "XAAW00001123456")).fold(
        formWithErrors => {
          formWithErrors(fieldId).errors.size mustBe 1
          messages(formWithErrors(fieldId).errors.head.message) mustBe messages("awrs.awrsUrn.generic.error", fieldNameInErrorMessage, maxQueryLength)
        },
        _ => fail("Field should contain errors")
      )
    }

    "field has invalid urns" in {
      form.bind(Map(fieldId -> "X0AW00000123456")).fold(
        formWithErrors => {
          formWithErrors(fieldId).errors.size mustBe 1
          messages(formWithErrors(fieldId).errors.head.message) mustBe messages("awrs.awrsUrn.generic.error", fieldNameInErrorMessage, maxQueryLength)
        },
        _ => fail("Field should contain errors")
      )
    }

    "allow valid submissions" in {
      assertFormIsValid(form, Map(awrsUrn -> "XAAW00000123456"))
      assertFormIsValid(form, Map(awrsUrn -> "XSAW00000123456"))
      assertFormIsValid(form, Map(awrsUrn -> "XZAW00000999999"))
      assertFormIsValid(form, Map(awrsUrn -> "XFAW00000000000"))
    }

  }


}
