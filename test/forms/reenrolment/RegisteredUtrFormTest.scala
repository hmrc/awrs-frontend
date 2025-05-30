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

import forms.reenrolment.RegisteredUtrForm.{awrsEnrolmentUtrForm, utr}
import forms.test.util.AwrsFormTestUtils
import models.AwrsEnrolmentUtr
import org.scalatestplus.play.PlaySpec
import play.api.data.Form

class RegisteredUtrFormTest extends PlaySpec with AwrsFormTestUtils {

  "AwrsEnrolmentUtrForm" should {
    implicit val form: Form[AwrsEnrolmentUtr] = awrsEnrolmentUtrForm.form
    val fieldId = "utr"
    val fieldNameInErrorMessage = "utr field"

    "field is left blank" in {
      form.bind(Map(fieldId -> "")).fold(
        formWithErrors => {
          formWithErrors(fieldId).errors.size mustBe 1
          formWithErrors(fieldId).errors.head.message mustBe "awrs.reenrolment.registered_utr.error"
        },
        _ => fail("Field should contain errors")
      )
    }

    "field is more than length mismatch" in {
      form.bind(Map(fieldId -> "623211381456")).fold(
        formWithErrors => {
          formWithErrors(fieldId).errors.size mustBe 1
          messages(formWithErrors(fieldId).errors.head.message) mustBe messages("awrs.reenrolment.registered_utr.error", fieldNameInErrorMessage)
        },
        _ => fail("Field should contain errors")
      )
    }

    "field has utr with lots of spaces" in {
      form.bind(Map(fieldId -> "       ")).fold(
        formWithErrors => {
          formWithErrors(fieldId).errors.size mustBe 1
          messages(formWithErrors(fieldId).errors.head.message) mustBe messages("awrs.reenrolment.registered_utr.error", fieldNameInErrorMessage)
        },
        _ => fail("Field should contain errors")
      )
    }

    "field is more than length is less" in {
      form.bind(Map(fieldId -> "623211")).fold(
        formWithErrors => {
          formWithErrors(fieldId).errors.size mustBe 1
          messages(formWithErrors(fieldId).errors.head.message) mustBe messages("awrs.reenrolment.registered_utr.error", fieldNameInErrorMessage)
        },
        _ => fail("Field should contain errors")
      )
    }


    "field has invalid utr" in {
      form.bind(Map(fieldId -> "6232113818073")).fold(
        formWithErrors => {
          formWithErrors(fieldId).errors.size mustBe 1
          messages(formWithErrors(fieldId).errors.head.message) mustBe messages("awrs.reenrolment.registered_utr.error", fieldNameInErrorMessage)
        },
        _ => fail("Field should contain errors")
      )
    }

    "allow valid submissions" in {
      assertFormIsValid(form, Map(utr -> "8951309411"))
      assertFormIsValid(form, Map(utr -> "6238951309411"))
      assertFormIsValid(form, Map(utr -> "6232113818078"))
      assertFormIsValid(form, Map(utr -> "623211381 8078"))
      assertFormIsValid(form, Map(utr -> "62321  1381 8078"))
    }
  }
}
