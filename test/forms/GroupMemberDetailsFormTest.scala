/*
 * Copyright 2022 HM Revenue & Customs
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
import forms.test.util._
import forms.validation.util.FieldError
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import utils.AwrsFieldConfig

class GroupMemberDetailsFormTest extends PlaySpec with MockitoSugar with AwrsFormTestUtils with AwrsFieldConfig {
  implicit val mockConfig: ApplicationConfig = mockAppConfig
  implicit lazy val form = GroupMemberDetailsForm.groupMemberForm.form

  "Form validation" must {

    "check validations for BusinessName" when {
      val fieldId = "companyNames.businessName"
      val fieldNameInErrorMessage = "business name"

      "the field is left empty" in {
        form.bind(Map(fieldId -> "")).fold(
          formWithErrors => {
            formWithErrors(fieldId).errors.size mustBe 1
            formWithErrors(fieldId).errors.head.message mustBe "awrs.generic.error.businessName_empty"
          },
          _ => fail("Field should contain errors")
        )
      }

      "the field maxLength is exceeded" in {
        form.bind(Map(fieldId -> "a" * 141)).fold(
          formWithErrors => {
            formWithErrors(fieldId).errors.size mustBe 1
            messages(formWithErrors(fieldId).errors.head.message) mustBe messages("awrs.generic.error.maximum_length", fieldNameInErrorMessage, companyNameLen)
          },
          _ => fail("Field should contain errors")
        )
      }

      "invalid characters are entered in the field" in {
        form.bind(Map(fieldId -> "α")).fold(
          formWithErrors => {
            formWithErrors(fieldId).errors.size mustBe 1
            messages(formWithErrors(fieldId).errors.head.message) mustBe messages("awrs.generic.error.character_invalid.summary", fieldNameInErrorMessage)
          },
          _ => fail("Field should contain errors")
        )
      }
    }

    "check validations for TradingName" when {
      val fieldId = "companyNames.tradingName"
      val fieldNameInErrorMessage = "trading name"
      val preCondition: Map[String, String] = Map("companyNames.doYouHaveTradingName" -> BooleanRadioEnum.Yes.toString)

      "the field is left empty" in {
        form.bind(Map(fieldId -> "") ++ preCondition).fold(
          formWithErrors => {
            formWithErrors(fieldId).errors.size mustBe 1
            formWithErrors(fieldId).errors.head.message mustBe "awrs.generic.enter_trading"
          },
          _ => fail("Field should contain errors")
        )
      }

      "the field maxLength is exceeded" in {
        form.bind(Map(fieldId -> "a" * 121) ++ preCondition).fold(
          formWithErrors => {
            formWithErrors(fieldId).errors.size mustBe 1
            messages(formWithErrors(fieldId).errors.head.message) mustBe messages("awrs.generic.error.maximum_length", fieldNameInErrorMessage, tradingNameLen)
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

    "check validations for TradingName radio button " when {
      val fieldId = "companyNames.doYouHaveTradingName"

      "the radio button is not selected" in {
        form.bind(Map(fieldId -> "")).fold(
          formWithErrors => {
            formWithErrors(fieldId).errors.size mustBe 1
            formWithErrors(fieldId).errors.head.message mustBe "awrs.generic.error.do_you_have_trading_name_empty"
          },
          _ => fail("Field should contain errors")
        )
      }
    }

    "check validations for AddressLine" when {
      val prefix = "address"
      val fieldNameInErrorMessage = "address line"
      val fieldNameInErrorMessagePostcode = "postcode"

      "the fields are left empty" in {
        form.bind(Map(
          s"$prefix.addressLine1" -> "",
          s"$prefix.addressLine2" -> "",
          s"$prefix.addressLine3" -> "",
          s"$prefix.addressLine4" -> "",
          s"$prefix.postcode" -> ""
        )).fold(
          formWithErrors => {
            formWithErrors("address.addressLine1").errors.head.message mustBe "awrs.generic.error.addressLine1_empty"
            formWithErrors("address.addressLine2").errors.head.message mustBe "awrs.generic.error.addressLine2_empty"
            formWithErrors("address.postcode").errors.head.message mustBe "awrs.generic.error.postcode_empty"
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
        )).fold(
          formWithErrors => {
            formWithErrors("address.addressLine1").errors.head.message mustBe messages("awrs.generic.error.maximum_length", s"$fieldNameInErrorMessage 1", addressLineLen)
            formWithErrors("address.addressLine2").errors.head.message mustBe messages("awrs.generic.error.maximum_length", s"$fieldNameInErrorMessage 2", addressLineLen)
            formWithErrors("address.addressLine3").errors.head.message mustBe messages("awrs.generic.error.maximum_length", s"$fieldNameInErrorMessage 3", addressLineLen)
            formWithErrors("address.addressLine4").errors.head.message mustBe messages("awrs.generic.error.maximum_length", s"$fieldNameInErrorMessage 4", addressLineLen)
            formWithErrors("address.postcode").errors.head.message mustBe messages("awrs.generic.error.postcode_invalid", fieldNameInErrorMessagePostcode)
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
        )).fold(
          formWithErrors => {
            formWithErrors("address.addressLine1").errors.head.message mustBe messages("awrs.generic.error.character_invalid.summary", s"$fieldNameInErrorMessage 1", addressLineLen)
            formWithErrors("address.addressLine2").errors.head.message mustBe messages("awrs.generic.error.character_invalid.summary", s"$fieldNameInErrorMessage 2", addressLineLen)
            formWithErrors("address.addressLine3").errors.head.message mustBe messages("awrs.generic.error.character_invalid.summary", s"$fieldNameInErrorMessage 3", addressLineLen)
            formWithErrors("address.addressLine4").errors.head.message mustBe messages("awrs.generic.error.character_invalid.summary", s"$fieldNameInErrorMessage 4", addressLineLen)
            formWithErrors("address.postcode").errors.head.message mustBe messages("awrs.generic.error.postcode_invalid", fieldNameInErrorMessagePostcode)
          },
          _ => fail("Field should contain errors")
        )
      }
    }

    "display correct validation for group member VRN" in {
      NamedUnitTests.doYouHaveVRNIsAnsweredAndValidIfTheAnswerIsYes()
    }

    "display correct validation for group member CRN" in {
      val data = Map(
        "companyNames.businessName" -> Seq("Business Name"),
        "companyNames.doYouHaveTradingName" -> Seq("No"),
        "address.addressLine1" -> Seq("1 Testing Test Road"),
        "address.addressLine2" -> Seq("Testton"),
        "address.postcode" -> Seq("NE98 1ZZ"),
        "doYouHaveUTR" -> Seq("No"),
        "isBusinessIncorporated" -> Seq("Yes"),
        "companyRegDetails.companyRegistrationNumber" -> Seq("10101010"),
        "companyRegDetails.dateOfIncorporation.day" -> Seq("20"),
        "companyRegDetails.dateOfIncorporation.month" -> Seq("5"),
        "companyRegDetails.dateOfIncorporation.year" -> Seq("2015"),
        "doYouHaveVRN" -> Seq("No"),
        "addAnotherGrpMember" -> Seq("No")
      )

      val testForm = form.bindFromRequest(data)
      testForm.errors mustBe Seq()
    }

    "display correct validation for group member UTR" in {
      val preCondition = Map[String, String]()
      val ignoreCondition = Set[Map[String, String]]()
      val idPrefix = None
      ProofOfIdentiticationVerifications.utrIsValidWhenDoYouHaveUTRIsAnsweredWithYes(
        preCondition,
        ignoreCondition,
        idPrefix,
        alsoTestWhenDoYouHaveUtrIsAnsweredWithNo = false)
    }

    "display correct validation for for at least one form of identification" in {
      val expectedError = ExpectedFieldIsEmpty("doYouHaveVRN", FieldError("awrs.generic.error.identification_provided"))
      val fieldIds = Set[String]("doYouHaveVRN", "isBusinessIncorporated", "doYouHaveUTR")
      NamedUnitTests.atLeastOneProofOfIdIsAnsweredWithYes(expectedError = expectedError, fieldIds = fieldIds)
    }

    "display correct validation for other group members question" in {
      val fieldId = "addAnotherGrpMember"

      val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.group_member.addAnother.empty"))

      val expectations = CompulsoryEnumValidationExpectations(emptyError, BooleanRadioEnum)
      fieldId assertEnumFieldIsCompulsory expectations
    }
  }
}
