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

import config.ApplicationConfig
import forms.AWRSEnums.BooleanRadioEnum
import forms.AWRSEnums.BooleanRadioEnum._
import forms.test.util._
import forms.validation.util.FieldError
import models.Supplier
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.Form
import utils.AwrsFieldConfig
import utils.TestConstants._

class SupplierAddressesFormTest extends PlaySpec with MockitoSugar  with AwrsFieldConfig with AwrsFormTestUtils {
  implicit val mockConfig: ApplicationConfig = mockAppConfig
  implicit lazy val form: Form[Supplier] = SupplierAddressesForm.supplierAddressesForm.form

  "Form validations" must {

    "check validations for alcoholSupplier radio button " when {
      val fieldId = "alcoholSupplier"

      "the radio button is not selected" in {
        form.bind(Map(fieldId -> "")).fold(
          formWithErrors => {
            formWithErrors(fieldId).errors.size mustBe 1
            formWithErrors(fieldId).errors.head.message mustBe "awrs.supplier-addresses.alcohol_supplier_empty"
          },
          _ => fail("Field should contain errors")
        )
      }
    }

    "display the correct validation errors for alcoholSupplier" in {
      val fieldId = "alcoholSupplier"

      val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.supplier-addresses.alcohol_supplier_empty"))
      val expectations = CompulsoryEnumValidationExpectations(emptyError, BooleanRadioEnum)

      fieldId assertEnumFieldIsCompulsory expectations
    }

    "display the correct validation errors for supplierName" when {
      val fieldId = "supplierName"
      val fieldNameInErrorMessage = "name of supplier"
      val preCondition = Map("alcoholSupplier" -> "Yes")

      "the field is left empty" in {
        form.bind(Map(fieldId -> "") ++ preCondition).fold(
          formWithErrors => {
            formWithErrors(fieldId).errors.size mustBe 1
            formWithErrors(fieldId).errors.head.message mustBe "awrs.supplier-addresses.error.supplier_name_blank"
          },
          _ => fail("Field should contain errors")
        )
      }

      "the field maxLength is exceeded" in {
        form.bind(Map(fieldId -> "a" * 141) ++ preCondition).fold(
          formWithErrors => {
            formWithErrors(fieldId).errors.size mustBe 1
            messages(formWithErrors(fieldId).errors.head.message) mustBe messages("awrs.generic.error.maximum_length", fieldNameInErrorMessage, supplierNameLen)
          },
          _ => fail("Field should contain errors")
        )
      }

      "invalid characters are entered in the field" in {
        form.bind(Map(fieldId -> "α") ++ preCondition).fold(
          formWithErrors => {
            formWithErrors(fieldId).errors.size mustBe 1
            messages(formWithErrors(fieldId).errors.head.message) mustBe messages("awrs.generic.error.character_invalid.summary", fieldNameInErrorMessage)
          },
          _ => fail("Field should contain errors")
        )
      }
    }

    "display the correct validation errors for ukSupplier" when {
      val preCondition = Map("alcoholSupplier" -> "Yes", "ukSupplier" -> "Yes" )
      val prefix = "supplierAddress"
      val fieldNameInErrorMessage = "supplier address line"
      val fieldNameInErrorMessagePostcode = "postcode"

      "the fields are left empty" in {
        form.bind(Map(
          s"$prefix.addressLine1" -> "",
          s"$prefix.addressLine2" -> "",
          s"$prefix.addressLine3" -> "",
          s"$prefix.addressLine4" -> "",
          s"$prefix.postcode" -> ""
        ) ++preCondition ).fold(
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
        ) ++preCondition).fold(
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
        ) ++preCondition).fold(
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

    "display the correct validation errors for NOT ukSupplier" when {
      val preCondition = Map("alcoholSupplier" -> "Yes", "ukSupplier" -> "No" )
      val prefix = "supplierAddress"
      val fieldNameInErrorMessage = "supplier address line"

      "the fields are left empty" in {
        form.bind(Map(
          s"$prefix.addressLine1" -> "",
          s"$prefix.addressLine2" -> "",
          s"$prefix.addressLine3" -> "",
          s"$prefix.addressLine4" -> "",
          s"$prefix.addressCountry" -> ""
        ) ++preCondition ).fold(
          formWithErrors => {
            formWithErrors(s"$prefix.addressLine1").errors.head.message mustBe "awrs.generic.error.addressLine1_empty"
            formWithErrors(s"$prefix.addressLine2").errors.head.message mustBe "awrs.generic.error.addressLine2_empty"
            formWithErrors(s"$prefix.addressCountry").errors.head.message mustBe "awrs.supplier-addresses.error.supplier_address_country_blank"
          },
          _ => fail("Field should contain errors")
        )
      }

      "the field maxLength is exceeded" in {
        form.bind(Map(
          s"$prefix.addressLine1" -> "a" * 36,
          s"$prefix.addressLine2" -> "a" * 36,
          s"$prefix.addressLine3" -> "a" * 36,
          s"$prefix.addressLine4" -> "a" * 36
        ) ++preCondition).fold(
          formWithErrors => {
            formWithErrors(s"$prefix.addressLine1").errors.head.message mustBe messages("awrs.generic.error.maximum_length", s"$fieldNameInErrorMessage 1", addressLineLen)
            formWithErrors(s"$prefix.addressLine2").errors.head.message mustBe messages("awrs.generic.error.maximum_length", s"$fieldNameInErrorMessage 2", addressLineLen)
            formWithErrors(s"$prefix.addressLine3").errors.head.message mustBe messages("awrs.generic.error.maximum_length", s"$fieldNameInErrorMessage 3", addressLineLen)
            formWithErrors(s"$prefix.addressLine4").errors.head.message mustBe messages("awrs.generic.error.maximum_length", s"$fieldNameInErrorMessage 4", addressLineLen)
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
          s"$prefix.addressCountry" -> "α"
        ) ++preCondition).fold(
          formWithErrors => {
            formWithErrors(s"$prefix.addressLine1").errors.head.message mustBe messages("awrs.generic.error.character_invalid.summary", s"$fieldNameInErrorMessage 1", addressLineLen)
            formWithErrors(s"$prefix.addressLine2").errors.head.message mustBe messages("awrs.generic.error.character_invalid.summary", s"$fieldNameInErrorMessage 2", addressLineLen)
            formWithErrors(s"$prefix.addressLine3").errors.head.message mustBe messages("awrs.generic.error.character_invalid.summary", s"$fieldNameInErrorMessage 3", addressLineLen)
            formWithErrors(s"$prefix.addressLine4").errors.head.message mustBe messages("awrs.generic.error.character_invalid.summary", s"$fieldNameInErrorMessage 4", addressLineLen)
          },
          _ => fail("Field should contain errors")
        )
      }
    }

    "display the correct validation errors for vatRegistered" in {
      val fieldId = "vatRegistered"

      val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.generic.error.supplier.do_you_have_vat_reg_empty"))
      val expecations = CompulsoryEnumValidationExpectations(emptyError, BooleanRadioEnum)

      val preCondition = Map("ukSupplier" -> "Yes")
      fieldId assertEnumFieldIsCompulsoryWhen(preCondition, expecations)

      val ignoreCondition = Map("alcoholSupplier" -> "No")
      fieldId assertEnumFieldIsIgnoredWhen(ignoreCondition, expecations.toIgnoreEnumFieldExpectation)
    }

    "display the correct validation errors for vatNumber" in {
      val fieldId = "vatNumber"

      val theyHaveVRN = Map("vatRegistered" -> Yes.toString)

      val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.generic.their.error.vrn_empty"))
      val invalidFormats = List(ExpectedInvalidFieldFormat("α", fieldId, FieldError("awrs.generic.error.vrn_invalid")))
      val formatError = ExpectedFieldFormat(invalidFormats)

      val expectations = CompulsoryFieldValidationExpectations(emptyError, MaxLengthIsHandledByTheRegEx(), formatError)

      fieldId assertFieldIsCompulsoryWhen(theyHaveVRN, expectations)

      val theyDoNotHaveVRN = Map("vatNumber" -> No.toString)
      fieldId assertFieldIsIgnoredWhen(theyDoNotHaveVRN, expectations.toFieldToIgnore)

      val ignoreCondition = Map("alcoholSupplier" -> "No")
      fieldId assertFieldIsIgnoredWhen(ignoreCondition, expectations.toFieldToIgnore)
    }

    "display the correct validation errors for supplierAddress when it is a foreign address" in {
      val data = Map(
        "alcoholSupplier" -> Seq("Yes"),
        "supplierName" -> Seq("Supplier Name"),
        "ukSupplier" -> Seq("No"),
        "supplierAddress.addressLine1" -> Seq("1 Testing Ton"),
        "supplierAddress.addressLine2" -> Seq("Testton"),
        "supplierAddress.addressCountry" -> Seq("France"),
        "additionalSupplier" -> Seq("No")
      )

      val testForm = form.bindFromRequest(data)
      testForm.errors mustBe Seq()
    }

    "display the correct validation errors for additionalSupplier" in {
      val fieldId = "additionalSupplier"

      val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.supplier-addresses.add_supplier_empty"))
      val expecations = CompulsoryEnumValidationExpectations(emptyError, BooleanRadioEnum)

      val preConditions = Set(Map("alcoholSupplier" -> "Yes", "ukSupplier" -> "Yes"), Map("alcoholSupplier" -> "Yes", "ukSupplier" -> "No"))
      fieldId assertEnumFieldIsCompulsoryWhen(preConditions, expecations)

      val ignoreCondition = Map("alcoholSupplier" -> "No")
      fieldId assertEnumFieldIsIgnoredWhen(ignoreCondition, expecations.toIgnoreEnumFieldExpectation)
    }


    "Form validations" must {

      "Allow submission if alcoholSupplier is answered with 'No'" in {
        val data: Map[String, String] =
          Map("alcoholSupplier" -> BooleanRadioEnum.No.toString)
        assertFormIsValid(form, data)
      }

      "Allow submission if alcoholSupplier is answered with 'Yes for an UK address" in {
        val data: Map[String, String] =
          Map("alcoholSupplier" -> BooleanRadioEnum.Yes.toString,
            "ukSupplier" -> "Yes",
            "supplierName" -> "supplierName",
            "vatRegistered" -> "No",
            "supplierAddress.addressLine1" -> "addressLine1",
            "supplierAddress.addressLine2" -> "addressLine2",
            "supplierAddress.addressLine3" -> "addressLine3",
            "supplierAddress.addressLine4" -> "addressLine4",
            "supplierAddress.postcode" -> testPostcode,
            "additionalSupplier" -> BooleanRadioEnum.Yes.toString
          )
        assertFormIsValid(form, data)

        val data2: Map[String, String] =
          Map("alcoholSupplier" -> BooleanRadioEnum.Yes.toString,
            "ukSupplier" -> "Yes",
            "supplierName" -> "supplierName",
            "vatRegistered" -> "Yes",
            "vatNumber" -> testVrn,
            "supplierAddress.addressLine1" -> "addressLine1",
            "supplierAddress.addressLine2" -> "addressLine2",
            "supplierAddress.postcode" -> testPostcode,
            "additionalSupplier" -> BooleanRadioEnum.No.toString
          )
        assertFormIsValid(form, data2)
      }

      "Allow submission if alcoholSupplier is answered with 'Yes for a foreign address" in {
        val data: Map[String, String] =
          Map("alcoholSupplier" -> BooleanRadioEnum.Yes.toString,
            "ukSupplier" -> "No",
            "supplierName" -> "supplierName",
            "supplierAddress.addressLine1" -> "addressLine1",
            "supplierAddress.addressLine2" -> "addressLine2",
            "supplierAddress.addressCountry" -> "France",
            "additionalSupplier" -> BooleanRadioEnum.No.toString
          )
        assertFormIsValid(form, data)
      }

      "Allow submission if fields contain Welsh characters" in {
        val data: Map[String, String] =
          Map("alcoholSupplier" -> BooleanRadioEnum.Yes.toString,
            "ukSupplier" -> "Yes",
            "supplierName" -> testWelshChars,
            "vatRegistered" -> "No",
            "supplierAddress.addressLine1" -> testWelshChars,
            "supplierAddress.addressLine2" -> testWelshChars,
            "supplierAddress.addressLine3" -> testWelshChars,
            "supplierAddress.addressLine4" -> testWelshChars,
            "supplierAddress.postcode" -> testPostcode,
            "additionalSupplier" -> BooleanRadioEnum.Yes.toString
          )
        assertFormIsValid(form, data)
      }
    }
  }
}
