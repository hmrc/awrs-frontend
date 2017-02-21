/*
 * Copyright 2017 HM Revenue & Customs
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

import forms.AWRSEnums.BooleanRadioEnum
import forms.AWRSEnums.BooleanRadioEnum._
import forms.test.util._
import forms.validation.util.FieldError
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import uk.gov.hmrc.play.test.UnitSpec
import utils.AwrsFieldConfig
import utils.TestConstants._

class SupplierAddressesFormTest extends UnitSpec with MockitoSugar with OneServerPerSuite {
  implicit lazy val form = SupplierAddressesForm.supplierAddressesForm.form


  "Form validations" should {
    "display the correct validation errors for alcoholSupplier" in {
      val fieldId = "alcoholSupplier"

      val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.supplier-addresses.alcohol_supplier_empty"))
      val expectations = CompulsoryEnumValidationExpectations(emptyError, BooleanRadioEnum)

      fieldId assertEnumFieldIsCompulsory expectations
    }

    "display the correct validation errors for supplierName" in {
      val fieldId = "supplierName"

      val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.supplier-addresses.error.supplier_name_blank"))
      val maxLenError = ExpectedFieldExceedsMaxLength(fieldId, "supplier name", AwrsFieldConfig.supplierNameLen)
      val invalidFormats = List(ExpectedInvalidFieldFormat("α", fieldId, "supplier name"))
      val formatError = ExpectedFieldFormat(invalidFormats)

      val expecations = CompulsoryFieldValidationExpectations(emptyError, maxLenError, formatError)

      val preCondition = Map("alcoholSupplier" -> "Yes")
      fieldId assertFieldIsCompulsoryWhen(preCondition, expecations)

      val ignoreCondition = Map("alcoholSupplier" -> "No")
      fieldId assertFieldIsIgnoredWhen(ignoreCondition, expecations.toFieldToIgnore)
    }

    "display the correct validation errors for ukSupplier" in {
      val fieldId = "ukSupplier"

      val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.supplier-addresses.error.uk_supplier_blank"))
      val expecations = CompulsoryEnumValidationExpectations(emptyError, BooleanRadioEnum)

      val preCondition = Map("alcoholSupplier" -> "Yes")
      fieldId assertEnumFieldIsCompulsoryWhen(preCondition, expecations)

      val ignoreCondition = Map("alcoholSupplier" -> "No")
      fieldId assertEnumFieldIsIgnoredWhen(ignoreCondition, expecations.toIgnoreEnumFieldExpectation)
    }

    //TODO refactor
    "display the correct validation errors for vatRegistered" in {
      val fieldId = "vatRegistered"

      val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.generic.error.do_you_have_vat_reg_empty"))
      val expecations = CompulsoryEnumValidationExpectations(emptyError, BooleanRadioEnum)

      val preCondition = Map("ukSupplier" -> "Yes")
      fieldId assertEnumFieldIsCompulsoryWhen(preCondition, expecations)

      val ignoreCondition = Map("alcoholSupplier" -> "No")
      fieldId assertEnumFieldIsIgnoredWhen(ignoreCondition, expecations.toIgnoreEnumFieldExpectation)
    }

    //TODO refactor
    "display the correct validation errors for vatNumber" in {
      val fieldId = "vatNumber"

      val theyHaveVRN = Map("vatRegistered" -> Yes.toString)

      val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.generic.error.vrn_empty"))
      val invalidFormats = List(ExpectedInvalidFieldFormat("α", fieldId, FieldError("awrs.generic.error.vrn_invalid")))
      val formatError = ExpectedFieldFormat(invalidFormats)

      val expectations = CompulsoryFieldValidationExpectations(emptyError, MaxLengthIsHandledByTheRegEx(), formatError)

      fieldId assertFieldIsCompulsoryWhen(theyHaveVRN, expectations)

      val theyDoNotHaveVRN = Map("vatNumber" -> No.toString)
      fieldId assertFieldIsIgnoredWhen(theyDoNotHaveVRN, expectations.toFieldToIgnore)

      val ignoreCondition = Map("alcoholSupplier" -> "No")
      fieldId assertFieldIsIgnoredWhen(ignoreCondition, expectations.toFieldToIgnore)
    }

    "display the correct validation errors for supplierAddress when it is an UK address" in {
      val preCondition = Map("alcoholSupplier" -> "Yes", "ukSupplier" -> "Yes")
      val ignoreConditions = Set(Map("alcoholSupplier" -> "No"))
      NamedUnitTests.ukAddressIsCompulsoryAndValid(preCondition, ignoreCondition = ignoreConditions, idPrefix = "supplierAddress", nameInErrorMessage = "supplier")
    }

    "display the correct validation errors for supplierAddress when it is a foreign address" in {
      val preCondition = Map("alcoholSupplier" -> "Yes", "ukSupplier" -> "No")
      val ignoreConditions = Set(Map("alcoholSupplier" -> "No"))
      NamedUnitTests.foreignAddressIsCompulsoryAndValid(preCondition, ignoreCondition = ignoreConditions, idPrefix = "supplierAddress", nameInErrorMessage = "supplier")
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
  }

  "Form validations" should {

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
  }
}