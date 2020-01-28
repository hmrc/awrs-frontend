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

package forms

import forms.test.util._
import forms.validation.util.FieldError
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import uk.gov.hmrc.play.test.UnitSpec
import utils.AwrsFieldConfig
import forms.AWRSEnums._
import utils.TestConstants._

class ApplicationDeclarationFormTest extends UnitSpec with MockitoSugar with OneServerPerSuite with AwrsFieldConfig {

  implicit lazy val form = ApplicationDeclarationForm.applicationDeclarationForm.form

  "Form validation" should {
    "display the correct validation errors for declarationName" in {
      val fieldId = "declarationName"

      val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.application_declaration.error.declaration_name_empty"))
      val maxLenError = ExpectedFieldExceedsMaxLength(fieldId, "your name", applicationDeclarationNameLen)
      val invalidFormats = List(ExpectedInvalidFieldFormat("α", fieldId, "your name"))
      //      val validFormats = List(ValidFormatConfig("a"))
      //      val formatError = FormatConfig(invalidFormats,validFormats)
      val formatError = ExpectedFieldFormat(invalidFormats)

      val expecations = CompulsoryFieldValidationExpectations(emptyError, maxLenError, formatError)

      fieldId assertFieldIsCompulsory expecations
    }

    "display the correct validation errors for declarationRole" in {
      val fieldId = "declarationRole"

      val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.application_declaration.error.declaration_role_empty"))
      val maxLenError = ExpectedFieldExceedsMaxLength(fieldId, "your role", applicationDeclarationRoleLen)
      val invalidFormats = List(ExpectedInvalidFieldFormat("α", fieldId, "your role"))
      val formatError = ExpectedFieldFormat(invalidFormats)

      val expecations = CompulsoryFieldValidationExpectations(emptyError, maxLenError, formatError)

      fieldId assertFieldIsCompulsory expecations
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
  }

  "Form validation" should {
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
