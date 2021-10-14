/*
 * Copyright 2021 HM Revenue & Customs
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

import config.ApplicationConfig
import forms.AWRSEnums.BooleanRadioEnum
import forms.ProductsForm.addressLineLen
import forms.test.util._
import forms.validation.util.FieldError
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import utils.TestConstants._

class BusinessPremisesFormTest extends PlaySpec with MockitoSugar  with AwrsFormTestUtils {
  implicit val mockConfig: ApplicationConfig = mockAppConfig
  implicit lazy val form = BusinessPremisesForm.businessPremisesForm.form

  "Form validation" must {
    "display the correct validation errors for additionalPremises" in {
      val fieldId = "additionalPremises"

      val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.additional-premises.error.do_you_have_additional_premises"))
      val expectations = CompulsoryEnumValidationExpectations(emptyError, BooleanRadioEnum)

      fieldId assertEnumFieldIsCompulsory expectations
    }

    "display correct validation for additionalAddress" when {
      val preCondition: Map[String, String] = Map("additionalPremises" -> BooleanRadioEnum.Yes.toString)
      val ignoreCondition: Map[String, String] = Map("additionalPremises" -> BooleanRadioEnum.No.toString)
      val prefix: String = "additionalAddress"
      val fieldNameInErrorMessage = "address line"
      val fieldNameInErrorMessagePostcode = "postcode"

      "the fields are left empty" in {
        form.bind(Map(
          s"$prefix.addressLine1" -> "",
          s"$prefix.addressLine2" -> "",
          s"$prefix.addressLine3" -> "",
          s"$prefix.addressLine4" -> "",
          s"$prefix.postcode" -> ""
        ) ++ preCondition).fold(
          formWithErrors => {
            formWithErrors(s"$prefix.addressLine1").errors.head.message mustBe "awrs.generic.error.addressLine1_empty"
            formWithErrors(s"$prefix.addressLine2").errors.head.message mustBe "awrs.generic.error.addressLine2_empty"
            formWithErrors(s"$prefix.postcode").errors.head.message mustBe "awrs.generic.error.postcode_empty"
          },
          _ => fail("Field should contain errors")
        )
      }

      "the field maxLength is exceeded" in {
        form.bind(Map(
          s"$prefix.addressLine1" -> "a" * 36,
          s"$prefix.addressLine2" -> "a" * 36,
          s"$prefix.addressLine3" -> "a" * 36,
          s"$prefix.addressLine4" -> "a" * 36,
          s"$prefix.postcode" -> "a" * 21
        ) ++ preCondition).fold(
          formWithErrors => {
            formWithErrors(s"$prefix.addressLine1").errors.head.message mustBe messages("awrs.generic.error.maximum_length", s"$fieldNameInErrorMessage 1", addressLineLen)
            formWithErrors(s"$prefix.addressLine2").errors.head.message mustBe messages("awrs.generic.error.maximum_length", s"$fieldNameInErrorMessage 2", addressLineLen)
            formWithErrors(s"$prefix.addressLine3").errors.head.message mustBe messages("awrs.generic.error.maximum_length", s"$fieldNameInErrorMessage 3", addressLineLen)
            formWithErrors(s"$prefix.addressLine4").errors.head.message mustBe messages("awrs.generic.error.maximum_length", s"$fieldNameInErrorMessage 4", addressLineLen)
            formWithErrors(s"$prefix.postcode").errors.head.message mustBe messages("awrs.generic.error.postcode_invalid", fieldNameInErrorMessagePostcode)
          },
          _ => fail("Field should contain errors")
        )
      }
      "invalid characters are entered in the field" in {
        form.bind(Map(
          s"$prefix.addressLine1" -> "α",
          s"$prefix.addressLine2" -> "α",
          s"$prefix.addressLine3" -> "α",
          s"$prefix.addressLine4" -> "α",
          s"$prefix.postcode" -> "α"
        ) ++ preCondition).fold(
          formWithErrors => {
            formWithErrors(s"$prefix.addressLine1").errors.head.message mustBe messages("awrs.generic.error.character_invalid.summary", s"$fieldNameInErrorMessage 1", addressLineLen)
            formWithErrors(s"$prefix.addressLine2").errors.head.message mustBe messages("awrs.generic.error.character_invalid.summary", s"$fieldNameInErrorMessage 2", addressLineLen)
            formWithErrors(s"$prefix.addressLine3").errors.head.message mustBe messages("awrs.generic.error.character_invalid.summary", s"$fieldNameInErrorMessage 3", addressLineLen)
            formWithErrors(s"$prefix.addressLine4").errors.head.message mustBe messages("awrs.generic.error.character_invalid.summary", s"$fieldNameInErrorMessage 4", addressLineLen)
            formWithErrors(s"$prefix.postcode").errors.head.message mustBe messages("awrs.generic.error.postcode_invalid", fieldNameInErrorMessagePostcode)
          },
          _ => fail("Field should contain errors")
        )
      }

      "no errors are thrown when NO is selected" in {
        val boundForm = form.bind(Map(
          s"$prefix.addressLine1" -> "",
          s"$prefix.addressLine2" -> "",
          s"$prefix.addressLine3" -> "α",
          s"$prefix.addressLine4" -> "α",
          s"$prefix.postcode" -> ""
        ) ++ ignoreCondition)

        assert(!boundForm.hasErrors, s"form contains errors: ${boundForm.errors}")
      }
    }

    "display correct validation for addAnother" in {
      val fieldId = "addAnother"

      val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.additional-premises.error.add_another"))
      val expectations = CompulsoryEnumValidationExpectations(emptyError, BooleanRadioEnum)
      val preCondition: Map[String, String] = Map("additionalPremises" -> BooleanRadioEnum.Yes.toString)
      val ignoreCondition: Set[Map[String, String]] = Set(Map("additionalPremises" -> BooleanRadioEnum.No.toString))

      fieldId assertEnumFieldIsCompulsoryWhen(preCondition, expectations)
      fieldId assertEnumFieldIsIgnoredWhen(ignoreCondition, expectations.toIgnoreEnumFieldExpectation)
    }
  }

  "Form validation" must {
    "Allow submission if additionalPremises is answered with 'No'" in {
      val data: Map[String, String] =
        Map("additionalPremises" -> BooleanRadioEnum.No.toString)
      assertFormIsValid(form, data)
    }
    "Allow submission if additionalPremises is answered with 'Yes" in {
      val data: Map[String, String] =
        Map("additionalPremises" -> BooleanRadioEnum.Yes.toString,
          "additionalAddress.postcode" -> testPostcode,
          "additionalAddress.addressLine1" -> "addressLine1",
          "additionalAddress.addressLine2" -> "addressLine2",
          "additionalAddress.addressLine3" -> "addressLine3",
          "additionalAddress.addressLine4" -> "addressLine4",
          "addAnother" -> BooleanRadioEnum.Yes.toString
        )
      assertFormIsValid(form, data)

      val data2: Map[String, String] =
        Map("additionalPremises" -> BooleanRadioEnum.Yes.toString,
          "additionalAddress.postcode" -> testPostcode,
          "additionalAddress.addressLine1" -> "addressLine1",
          "additionalAddress.addressLine2" -> "addressLine2",
          "addAnother" -> BooleanRadioEnum.No.toString
        )
      assertFormIsValid(form, data2)
    }
    "Allow submission if Welsh characters are input" in {
      val data: Map[String, String] =
        Map("additionalPremises" -> BooleanRadioEnum.Yes.toString,
          "additionalAddress.postcode" -> testPostcode,
          "additionalAddress.addressLine1" -> testWelshChars,
          "additionalAddress.addressLine2" -> testWelshChars,
          "additionalAddress.addressLine3" -> testWelshChars,
          "additionalAddress.addressLine4" -> testWelshChars,
          "addAnother" -> BooleanRadioEnum.Yes.toString
        )
      assertFormIsValid(form, data)
    }
  }
}
