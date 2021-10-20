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
import forms.AWRSEnums.{BooleanRadioEnum, OperatingDurationEnum}
import forms.test.util._
import forms.validation.util.FieldError
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import utils.AwrsFieldConfig

class PlaceOfBusinessFormTest extends PlaySpec with MockitoSugar  with AwrsFormTestUtils with AwrsFieldConfig {
  implicit val mockConfig: ApplicationConfig = mockAppConfig
  implicit lazy val form = PlaceOfBusinessForm.placeOfBusinessForm.form

  "Business contacts form" must {

    "check validations for mainAddress" when {
      val preCondition: Map[String, String] = Map("mainPlaceOfBusiness" -> BooleanRadioEnum.No.toString)
      val prefix = "mainAddress"
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
    }

    "check validations for previous principal place of business address" when {
      val preCondition: Map[String, String] = Map("placeOfBusinessLast3Years" -> BooleanRadioEnum.No.toString)
      val prefix = "placeOfBusinessAddressLast3Years"
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
    }

    "check validations for principal place of business radio button " when {
      val fieldId = "placeOfBusinessLast3Years"

      "the radio button is not selected" in {
        form.bind(Map(fieldId -> "")).fold(
          formWithErrors => {
            formWithErrors(fieldId).errors.size mustBe 1
            formWithErrors(fieldId).errors.head.message mustBe "awrs.business_contacts.error.place_of_business_changed_last_3_years_empty"
          },
          _ => fail("Field should contain errors")
        )
      }
    }
    "check the operating duration is selected" in {
      val fieldId = "operatingDuration"

      val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.business_contacts.error.operating_duration_empty"))

      val expectations = CompulsoryEnumValidationExpectations(emptyError, OperatingDurationEnum)
      fieldId assertEnumFieldIsCompulsory expectations
    }
  }
}
