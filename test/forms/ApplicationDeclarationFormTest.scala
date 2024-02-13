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

import forms.AWRSEnums._
import forms.test.util._
import forms.validation.util.FieldError
import models.ApplicationDeclaration
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.Form
import utils.AwrsFieldConfig
import utils.TestConstants._

class ApplicationDeclarationFormTest extends PlaySpec with MockitoSugar  with AwrsFieldConfig with AwrsFormTestUtils {

  implicit lazy val form: Form[ApplicationDeclaration] = ApplicationDeclarationForm.applicationDeclarationForm.form

  "Declaration form validation" must {

    val fieldId = "declarationName"
    val fieldNameInErrorMessage = "full name"
    val fieldIdRole = "declarationRole"
    val fieldNameInErrorMessageRole = "job title or role"

    "the full name field is left empty" in {
      form.bind(Map(fieldId -> "")).fold(
        formWithErrors => {
          formWithErrors(fieldId).errors.size mustBe 1
          formWithErrors(fieldId).errors.head.message mustBe "awrs.application_declaration.error.declaration_name_empty"
        },
        _ => fail("Field should contain errors")
      )
    }

    "the full name field maxLength is exceeded" in {
      form.bind(Map(fieldId -> "a" * 141)).fold(
        formWithErrors => {
          formWithErrors(fieldId).errors.size mustBe 1
          messages(formWithErrors(fieldId).errors.head.message) mustBe messages("awrs.generic.error.name.maximum_length", fieldNameInErrorMessage, applicationDeclarationNameLen)
        },
        _ => fail("Field should contain errors")
      )
    }

    "invalid characters are entered in the full name field" in {
      form.bind(Map(fieldId -> "α")).fold(
        formWithErrors => {
          formWithErrors(fieldId).errors.size mustBe 1
          messages(formWithErrors(fieldId).errors.head.message) mustBe messages("awrs.generic.error.character_invalid.summary_declaration", fieldNameInErrorMessage)
        },
        _ => fail("Field should contain errors")
      )
    }

    "the job title or role field is left empty" in {
      form.bind(Map(fieldIdRole -> "")).fold(
        formWithErrors => {
          formWithErrors(fieldIdRole).errors.size mustBe 1
          formWithErrors(fieldIdRole).errors.head.message mustBe "awrs.application_declaration.error.declaration_role_empty"
        },
        _ => fail("Field should contain errors")
      )
    }

    "the job title or role field maxLength is exceeded" in {
      form.bind(Map(fieldIdRole -> "a" * 41)).fold(
        formWithErrors => {
          formWithErrors(fieldIdRole).errors.size mustBe 1
          messages(formWithErrors(fieldIdRole).errors.head.message) mustBe messages("awrs.generic.error.name.maximum_length", fieldNameInErrorMessageRole, applicationDeclarationRoleLen)
        },
        _ => fail("Field should contain errors")
      )
    }

    "invalid characters are entered in the job title or role field" in {
      form.bind(Map(fieldIdRole -> "α")).fold(
        formWithErrors => {
          formWithErrors(fieldIdRole).errors.size mustBe 1
          messages(formWithErrors(fieldIdRole).errors.head.message) mustBe messages("awrs.generic.error.character_invalid.summary_declaration", fieldNameInErrorMessageRole)
        },
        _ => fail("Field should contain errors")
      )
    }
  }

  "enforce the confirmation box must be checked" in {
    val fieldId = "confirmation"

    val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.application_declaration.error.confirmation_empty"))

    val expecations =
      CompulsoryEnumValidationExpectations(emptyError,
        Set(BooleanCheckboxEnum.True),
        Set(BooleanCheckboxEnum.False))

    fieldId assertEnumFieldIsCompulsory expecations
  }

  "Form validation" must {
    "Allow submission if both name and role are filled in and the confirmation box is checked" in {
      val data: Map[String, String] =
        Map("declarationName" -> testWelshChars,
          "declarationRole" -> "Director",
          "confirmation" -> "true"
        )
      assertFormIsValid(form, data)
    }
  }
}
