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
import forms.AWRSEnums.BooleanRadioEnum.{No, NoString, Yes, YesString}
import forms.AWRSEnums._
import forms.PartnershipDetailsForm._
import forms.test.util._
import forms.validation.util.FieldError
import models.Partner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.Form
import utils.AwrsFieldConfig
import utils.TestConstants._
import views.html.helpers.frontendDefaultLen

class PartnershipDetailsFormTest extends PlaySpec with MockitoSugar with AwrsFormTestUtils with AwrsFieldConfig {

  implicit val mockConfig: ApplicationConfig = mockAppConfig

  implicit lazy val form: Form[Partner] = PartnershipDetailsForm.partnershipDetailsForm.form

  import EntityTypeEnum._

  val partnerTypes = Set(Individual, CorporateBody, SoleTrader)
  lazy val crnField = s"$crnMapping.companyRegistrationNumber"

  "Form validations" must {
    "Partners," must {

      "check the entity type is selected" in {
        val fieldId = "entityType"

        val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.business-partner.error.entityType_empty"))

        val expectations = CompulsoryEnumValidationExpectations(emptyError, EntityTypeEnum)
        fieldId assertEnumFieldIsCompulsory expectations
      }

      for (partnerType <- partnerTypes - CorporateBody) {
        s"check 'first name' and 'last name' validation for $partnerType" when {
          val conditionPartnerIsOfType: Map[String, String] = Map("entityType" -> partnerType.toString)

          val fieldIdFirstName = "firstName"
          val fieldIdSurname = "lastName"
          val fieldNameInErrorMessageFN = "First name"
          val fieldNameInErrorMessageLN = "Last name"

          "the first name field is left empty" in {
            form.bind(Map(fieldIdFirstName -> "") ++ conditionPartnerIsOfType).fold(
              formWithErrors => {
                formWithErrors(fieldIdFirstName).errors.size mustBe 1
                formWithErrors(fieldIdFirstName).errors.head.message mustBe "awrs.generic.error.first_name_empty"
              },
              _ => fail("Field should contain errors")
            )
          }

          "the last name field is left empty" in {
            form.bind(Map(fieldIdSurname -> "") ++ conditionPartnerIsOfType).fold(
              formWithErrors => {
                formWithErrors(fieldIdSurname).errors.size mustBe 1
                formWithErrors(fieldIdSurname).errors.head.message mustBe "awrs.generic.error.last_name_empty"
              },
              _ => fail("Field should contain errors")
            )
          }

          "the first name field maxLength is exceeded" in {
            form.bind(Map(fieldIdFirstName -> "a" * 36) ++ conditionPartnerIsOfType).fold(
              formWithErrors => {
                formWithErrors(fieldIdFirstName).errors.size mustBe 1
                messages(formWithErrors(fieldIdFirstName).errors.head.message) mustBe messages("awrs.generic.error.name.maximum_length", fieldNameInErrorMessageFN, firstNameLen)
              },
              _ => fail("Field should contain errors")
            )
          }

          "the last name field maxLength is exceeded" in {
            form.bind(Map(fieldIdSurname -> "a" * 36) ++ conditionPartnerIsOfType).fold(
              formWithErrors => {
                formWithErrors(fieldIdSurname).errors.size mustBe 1
                messages(formWithErrors(fieldIdSurname).errors.head.message) mustBe messages("awrs.generic.error.name.maximum_length", fieldNameInErrorMessageLN, lastNameLen)
              },
              _ => fail("Field should contain errors")
            )
          }

          "invalid characters are entered in the first name field" in {
            form.bind(Map(fieldIdFirstName -> "α") ++ conditionPartnerIsOfType).fold(
              formWithErrors => {
                formWithErrors(fieldIdFirstName).errors.size mustBe 1
                messages(formWithErrors(fieldIdFirstName).errors.head.message) mustBe messages("awrs.generic.error.character_invalid.summary", fieldNameInErrorMessageFN)
              },
              _ => fail("Field should contain errors")
            )
          }

          "invalid characters are entered in the last name field" in {
            form.bind(Map(fieldIdSurname -> "α") ++ conditionPartnerIsOfType).fold(
              formWithErrors => {
                formWithErrors(fieldIdSurname).errors.size mustBe 1
                messages(formWithErrors(fieldIdSurname).errors.head.message) mustBe messages("awrs.generic.error.character_invalid.summary", fieldNameInErrorMessageLN)
              },
              _ => fail("Field should contain errors")
            )
          }
        }


        s"check National Insurance number validation for $partnerType" when {
          val preCondition: Map[String, String] = Map("doYouHaveNino" -> BooleanRadioEnum.Yes.toString, "entityType" -> partnerType.toString)
          val fieldIdNino = "nino"
          val fieldNameInErrorMessageNino = "National Insurance number"


          "the National Insurance number field is left empty" in {
            form.bind(Map(fieldIdNino -> "") ++ preCondition).fold(
              formWithErrors => {
                formWithErrors(fieldIdNino).errors.size mustBe 1
                formWithErrors(fieldIdNino).errors.head.message mustBe "awrs.generic.error.nino_empty"
              },
              _ => fail("Field should contain errors")
            )
          }

          "the National Insurance number field maxLength is exceeded" in {
            form.bind(Map(fieldIdNino -> "a" * 41) ++ preCondition).fold(
              formWithErrors => {
                formWithErrors(fieldIdNino).errors.size mustBe 1
                messages(formWithErrors(fieldIdNino).errors.head.message) mustBe messages("awrs.generic.error.nino_invalid", fieldNameInErrorMessageNino, frontendDefaultLen)
              },
              _ => fail("Field should contain errors")
            )
          }

          "invalid characters are entered in the National Insurance number field field" in {
            form.bind(Map(fieldIdNino -> "α") ++ preCondition).fold(
              formWithErrors => {
                formWithErrors(fieldIdNino).errors.size mustBe 1
                messages(formWithErrors(fieldIdNino).errors.head.message) mustBe messages("awrs.generic.error.nino_invalid", fieldNameInErrorMessageNino)
              },
              _ => fail("Field should contain errors")
            )
          }
        }

        s"check National Insurance number radio button for $partnerType" when {
          val fieldId = "doYouHaveNino"
          val preCondition: Map[String, String] = Map("entityType" -> partnerType.toString)

          "the radio button is not selected" in {
            form.bind(Map(fieldId -> "") ++ preCondition).fold(
              formWithErrors => {
                formWithErrors(fieldId).errors.size mustBe 1
                formWithErrors(fieldId).errors.head.message mustBe "awrs.generic.error.do_you_have_nino_empty"
              },
              _ => fail("Field should contain errors")
            )
          }
        }
      }
      for (partnerType <- partnerTypes) {

        s"check validations for Address Lines for $partnerType" when {
          val prefix = "partnerAddress"
          val fieldNameInErrorMessage = "address line"
          val fieldNameInErrorMessagePostcode = "postcode"
          val preCondition: Map[String, String] = Map("entityType" -> partnerType.toString)

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
      }

      for (partnerType <- partnerTypes - Individual) {

        s"check validations for trading name for $partnerType" when {
          val preCondition: Map[String, String] = Map("companyNames.doYouHaveTradingName" -> BooleanRadioEnum.Yes.toString, "entityType" -> partnerType.toString)
          val fieldId = "companyNames.tradingName"
          val fieldNameInErrorMessage = "trading name"


          "the trading name field is left empty" in {
            form.bind(Map(fieldId -> "") ++ preCondition).fold(
              formWithErrors => {
                formWithErrors(fieldId).errors.size mustBe 1
                formWithErrors(fieldId).errors.head.message mustBe "awrs.generic.enter_trading"
              },
              _ => fail("Field should contain errors")
            )
          }

          "the trading name field maxLength is exceeded" in {
            form.bind(Map(fieldId -> "a" * 121) ++ preCondition).fold(
              formWithErrors => {
                formWithErrors(fieldId).errors.size mustBe 1
                messages(formWithErrors(fieldId).errors.head.message) mustBe messages("awrs.generic.error.maximum_length", fieldNameInErrorMessage, tradingNameLen)
              },
              _ => fail("Field should contain errors")
            )
          }

          "invalid characters are entered in the trading name field field" in {
            form.bind(Map(fieldId -> "α") ++ preCondition).fold(
              formWithErrors => {
                formWithErrors(fieldId).errors.size mustBe 1
                messages(formWithErrors(fieldId).errors.head.message) mustBe messages("awrs.generic.error.character_invalid.summary", fieldNameInErrorMessage)
              },
              _ => fail("Field should contain errors")
            )
          }
        }

        s"check do you have trading name radio button for $partnerType" when {
          val fieldId = "companyNames.doYouHaveTradingName"
          val preCondition: Map[String, String] = Map("entityType" -> partnerType.toString)

          "the radio button is not selected" in {
            form.bind(Map(fieldId -> "") ++ preCondition).fold(
              formWithErrors => {
                formWithErrors(fieldId).errors.size mustBe 1
                formWithErrors(fieldId).errors.head.message mustBe "awrs.generic.error.do_you_have_trading_name_empty"
              },
              _ => fail("Field should contain errors")
            )
          }
        }

        s"check validations for utr for $partnerType" when {
          val preCondition: Map[String, String] = Map("entityType" -> partnerType.toString)
          val fieldId = "utr"
          val fieldNameInErrorMessage = "Unique Taxpayer Reference number"


          "the utr field is left empty" in {
            form.bind(Map(fieldId -> "") ++ preCondition).fold(
              formWithErrors => {
                formWithErrors(fieldId).errors.size mustBe 1
                formWithErrors(fieldId).errors.head.message mustBe "awrs.generic.error.utr_empty"
              },
              _ => fail("Field should contain errors")
            )
          }

          "the utr field maxLength is exceeded" in {
            form.bind(Map(fieldId -> "a" * 41) ++ preCondition).fold(
              formWithErrors => {
                formWithErrors(fieldId).errors.size mustBe 1
                messages(formWithErrors(fieldId).errors.head.message) mustBe messages("awrs.generic.error.utr_invalid", fieldNameInErrorMessage, frontendDefaultLen)
              },
              _ => fail("Field should contain errors")
            )
          }

          "invalid characters are entered in the utr field field" in {
            form.bind(Map(fieldId -> "α") ++ preCondition).fold(
              formWithErrors => {
                formWithErrors(fieldId).errors.size mustBe 1
                messages(formWithErrors(fieldId).errors.head.message) mustBe messages("awrs.generic.error.utr_invalid", fieldNameInErrorMessage)
              },
              _ => fail("Field should contain errors")
            )
          }
        }


        s"check VAT registration number validation for $partnerType" when {
          val preCondition: Map[String, String] = Map("doYouHaveVRN" -> BooleanRadioEnum.Yes.toString, "entityType" -> partnerType.toString)
          val fieldId = "vrn"
          val fieldNameInErrorMessageNino = "VAT registration number"


          "the VAT registration number field is left empty" in {
            form.bind(Map(fieldId -> "") ++ preCondition).fold(
              formWithErrors => {
                formWithErrors(fieldId).errors.size mustBe 1
                formWithErrors(fieldId).errors.head.message mustBe "awrs.generic.error.vrn_empty"
              },
              _ => fail("Field should contain errors")
            )
          }

          "the VAT registration number field maxLength is exceeded" in {
            form.bind(Map(fieldId -> "a" * 41) ++ preCondition).fold(
              formWithErrors => {
                formWithErrors(fieldId).errors.size mustBe 1
                messages(formWithErrors(fieldId).errors.head.message) mustBe messages("awrs.generic.error.vrn_invalid", fieldNameInErrorMessageNino, frontendDefaultLen)
              },
              _ => fail("Field should contain errors")
            )
          }

          "invalid characters are entered in the VAT registration number field field" in {
            form.bind(Map(fieldId -> "α") ++ preCondition).fold(
              formWithErrors => {
                formWithErrors(fieldId).errors.size mustBe 1
                messages(formWithErrors(fieldId).errors.head.message) mustBe messages("awrs.generic.error.vrn_invalid", fieldNameInErrorMessageNino)
              },
              _ => fail("Field should contain errors")
            )
          }
        }

        s"check VAT registration number radio button for $partnerType" when {
          val fieldId = "doYouHaveVRN"
          val preCondition: Map[String, String] = Map("entityType" -> partnerType.toString)

          "the radio button is not selected" in {
            form.bind(Map(fieldId -> "") ++ preCondition).fold(
              formWithErrors => {
                formWithErrors(fieldId).errors.size mustBe 1
                formWithErrors(fieldId).errors.head.message mustBe "awrs.generic.error.do_you_have_vat_reg_empty"
              },
              _ => fail("Field should contain errors")
            )
          }
        }

      }

        "check validations for Business name for CorporateBody " when {
          val preCondition: Map[String, String] = Map("entityType" -> CorporateBody.toString)
          val fieldId = "companyNames.businessName"
          val fieldNameInErrorMessage = "business name"


          "the Business name field is left empty" in {
            form.bind(Map(fieldId -> "") ++ preCondition).fold(
              formWithErrors => {
                formWithErrors(fieldId).errors.size mustBe 1
                formWithErrors(fieldId).errors.head.message mustBe "awrs.generic.error.businessName_empty"
              },
              _ => fail("Field should contain errors")
            )
          }

          "the Business name field maxLength is exceeded" in {
            form.bind(Map(fieldId -> "a" * 141) ++ preCondition).fold(
              formWithErrors => {
                formWithErrors(fieldId).errors.size mustBe 1
                messages(formWithErrors(fieldId).errors.head.message) mustBe messages("awrs.generic.error.maximum_length", fieldNameInErrorMessage, companyNameLen)
              },
              _ => fail("Field should contain errors")
            )
          }

          "invalid characters are entered in the Business name field field" in {
            form.bind(Map(fieldId -> "α") ++ preCondition).fold(
              formWithErrors => {
                formWithErrors(fieldId).errors.size mustBe 1
                messages(formWithErrors(fieldId).errors.head.message) mustBe messages("awrs.generic.error.character_invalid.summary", fieldNameInErrorMessage)
              },
              _ => fail("Field should contain errors")
            )
          }
        }

      "check validations for company registration number CRN for CorporateBody " when {
        val preCondition: Map[String, String] = Map("entityType" -> CorporateBody.toString)
        val fieldId = "companyRegDetails.companyRegistrationNumber"
        val fieldNameInErrorMessage = "CRN"


        "the company registration number CRN field is left empty" in {
          form.bind(Map(fieldId -> "") ++ preCondition).fold(
            formWithErrors => {
              formWithErrors(fieldId).errors.size mustBe 1
              formWithErrors(fieldId).errors.head.message mustBe "awrs.generic.error.companyRegNumber_empty"
            },
            _ => fail("Field should contain errors")
          )
        }

        "the company registration number CRN field maxLength is exceeded" in {
          form.bind(Map(fieldId -> "a" * 41) ++ preCondition).fold(
            formWithErrors => {
              formWithErrors(fieldId).errors.size mustBe 1
              messages(formWithErrors(fieldId).errors.head.message) mustBe messages("awrs.generic.error.companyRegNumber_invalid", fieldNameInErrorMessage, frontendDefaultLen)
            },
            _ => fail("Field should contain errors")
          )
        }

        "invalid characters are entered in the company registration number CRN field field" in {
          form.bind(Map(fieldId -> "α") ++ preCondition).fold(
            formWithErrors => {
              formWithErrors(fieldId).errors.size mustBe 1
              messages(formWithErrors(fieldId).errors.head.message) mustBe messages("awrs.generic.error.companyRegNumber_invalid", fieldNameInErrorMessage)
            },
            _ => fail("Field should contain errors")
          )
        }
      }

      "check validations for date of incorporation for CorporateBody " when {
        val preCondition: Map[String, String] = Map("entityType" -> CorporateBody.toString)
        val fieldId = "companyRegDetails.dateOfIncorporation"

        "empty date fields are entered" in {

          val inputDate = Map(
            s"$fieldId.day" -> "",
            s"$fieldId.month" -> "",
            s"$fieldId.year" -> "")

          form.bind(inputDate ++ preCondition).fold(
            formWithErrors => {
              formWithErrors(fieldId).errors.size mustBe 1
              formWithErrors(fieldId).errors.head.message mustBe messages("awrs.generic.error.companyRegDate_empty")
            },
            _ => {
              fail("Field should contain errors")
            }
          )
        }

        "valid characters but invalid dates are entered" in {

          val inputDate = Map(
            s"$fieldId.day" -> "0",
            s"$fieldId.month" -> "0",
            s"$fieldId.year" -> "0000")

          form.bind(inputDate ++ preCondition).fold(
            formWithErrors => {
              formWithErrors(fieldId).errors.size mustBe 1
              formWithErrors(fieldId).errors.head.message mustBe messages("awrs.generic.error.companyRegDate_invalid")
            },
            _ => {
              fail("Field should contain errors")
            }
          )
        }

        "invalid characters are entered" in {

          val inputDate = Map(
            s"$fieldId.day" -> "α",
            s"$fieldId.month" -> "αα",
            s"$fieldId.year" -> "αααα")

          form.bind(inputDate ++ preCondition).fold(
            formWithErrors => {
              formWithErrors(fieldId).errors.size mustBe 1
              formWithErrors(fieldId).errors.head.message mustBe messages("awrs.generic.error.companyRegDate_invalid")
            },
            _ => {
              fail("Field should contain errors")
            }
          )
        }

        "invalid date is entered" in {

          val inputDate = Map(
            s"$fieldId.day" -> "31",
            s"$fieldId.month" -> "02",
            s"$fieldId.year" -> "2019")

          form.bind(inputDate ++ preCondition).fold(
            formWithErrors => {
              formWithErrors(fieldId).errors.size mustBe 1
              formWithErrors(fieldId).errors.head.message mustBe messages("awrs.generic.error.companyRegDate_invalid")
            },
            _ => {
              fail("Field should contain errors")
            }
          )
        }

        "invalid year less than 4 digits are entered" in {

          val inputDate = Map(
            s"$fieldId.day" -> "11",
            s"$fieldId.month" -> "11",
            s"$fieldId.year" -> "111")

          form.bind(inputDate ++ preCondition).fold(
            formWithErrors => {
              formWithErrors(fieldId).errors.size mustBe 1
              formWithErrors(fieldId).errors.head.message mustBe messages("awrs.business_details.error.year_toSmall")
            },
            _ => {
              fail("Field should contain errors")
            }
          )
        }
      }
    }

  "check that if 'companyRegNumber' is entered, it is correctly validated, when partner is Corporate Body" in {
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
      "otherPartners" -> Seq("No"),
      "entityType" -> Seq("Corporate Body"),
      "partnerAddress.addressLine1" -> Seq("1 Testing Test Road"),
      "partnerAddress.addressLine2" -> Seq("Testton"),
      "partnerAddress.postcode" -> Seq("NE98 1ZZ")
    )

    val testForm = form.bindFromRequest(data)
    testForm.errors mustBe Seq()
  }

  "check do you have other partner is selected when adding more than one partner" in {
    val fieldId = "otherPartners"

    val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.business-partner.error.add_more_partner"))

    val expectations = CompulsoryEnumValidationExpectations(emptyError, BooleanRadioEnum)
    fieldId assertEnumFieldIsCompulsory expectations
  }

  "Valid journeys for when partner is Individual must pass" in {
    val conditionEntityIsIndividual = Map("entityType" -> Individual.toString)

    val testDataScen1: Map[String, String] =
      conditionEntityIsIndividual +
        ("firstName" -> "firstName",
          "lastName" -> "lastName",
          "partnerAddress.postcode" -> testPostcode,
          "partnerAddress.addressLine1" -> "addressLine1",
          "partnerAddress.addressLine2" -> "addressLine2",
          "doYouHaveNino" -> Yes.toString,
          "nino" -> testNino,
          "otherPartners" -> Yes.toString
          )
    assertFormIsValid(form, testDataScen1)

    val testDataScen2: Map[String, String] =
      conditionEntityIsIndividual +
        ("firstName" -> "firstName",
          "lastName" -> "lastName",
          "partnerAddress.postcode" -> testPostcode,
          "partnerAddress.addressLine1" -> "addressLine1",
          "partnerAddress.addressLine2" -> "addressLine2",
          "partnerAddress.addressLine3" -> "addressLine3",
          "partnerAddress.addressLine4" -> "addressLine4",
          "doYouHaveNino" -> No.toString,
          "otherPartners" -> No.toString
          )
    assertFormIsValid(form, testDataScen2)
  }

  "check when partner is Individual and uses Welsh characters" in {
    val conditionEntityIsIndividual = Map("entityType" -> Individual.toString)

    val testDataScen1: Map[String, String] =
      conditionEntityIsIndividual +
        ("firstName" -> testWelshChars,
          "lastName" -> testWelshChars,
          "partnerAddress.postcode" -> testPostcode,
          "partnerAddress.addressLine1" -> testWelshChars,
          "partnerAddress.addressLine2" -> testWelshChars,
          "partnerAddress.addressLine3" -> testWelshChars,
          "partnerAddress.addressLine4" -> testWelshChars,
          "doYouHaveNino" -> Yes.toString,
          "nino" -> testNino,
          "otherPartners" -> Yes.toString
        )
    assertFormIsValid(form, testDataScen1)
  }

    "Valid journeys for when partner is Corporate Body must pass" in {
    val conditionEntityIsCompany = Map("entityType" -> CorporateBody.toString)

    val testDataScen1: Map[String, String] =
      conditionEntityIsCompany +
        (
          "companyNames.businessName" -> "businessName",
          "companyNames.doYouHaveTradingName" -> NoString,
          "partnerAddress.postcode" -> testPostcode,
          "partnerAddress.addressLine1" -> "addressLine1",
          "partnerAddress.addressLine2" -> "addressLine2",
          "doYouHaveVRN" -> Yes.toString,
          "vrn" -> generateFieldTestDataInThisFormat(DataFormat("1", 9)),
          "isBusinessIncorporated" -> Yes.toString,
          "companyRegDetails.companyRegistrationNumber" ->
            generateFieldTestDataInThisFormat(
              DataFormat("0", 1),
              DataFormat("1", 7)),
          "companyRegDetails.dateOfIncorporation.day" -> "10",
          "companyRegDetails.dateOfIncorporation.month" -> "10",
          "companyRegDetails.dateOfIncorporation.year" -> "1000",
          "doYouHaveUTR" -> Yes.toString,
          "utr" -> generateFieldTestDataInThisFormat(DataFormat("1", 10)),
          "otherPartners" -> Yes.toString
          )
    assertFormIsValid(form, testDataScen1)

    val testDataScen2: Map[String, String] =
      conditionEntityIsCompany +
        ("companyNames.businessName" -> "businessName",
          "companyNames.doYouHaveTradingName" -> YesString,
          "companyNames.tradingName" -> "Trading",
          "partnerAddress.postcode" -> testPostcode,
          "partnerAddress.addressLine1" -> "addressLine1",
          "partnerAddress.addressLine2" -> "addressLine2",
          "doYouHaveVRN" -> No.toString,
          "isBusinessIncorporated" -> No.toString,
          "doYouHaveUTR" -> Yes.toString,
          "utr" -> generateFieldTestDataInThisFormat(DataFormat("1", 10)),
          "otherPartners" -> No.toString
          )
    assertFormIsValid(form, testDataScen2)

    val testDataScen3: Map[String, String] =
      conditionEntityIsCompany +
        ("companyNames.businessName" -> "businessName",
          "companyNames.doYouHaveTradingName" -> YesString,
          "companyNames.tradingName" -> "tradingName",
          "partnerAddress.postcode" -> testPostcode,
          "partnerAddress.addressLine1" -> "addressLine1",
          "partnerAddress.addressLine2" -> "addressLine2",
          "doYouHaveVRN" -> Yes.toString,
          "vrn" -> generateFieldTestDataInThisFormat(DataFormat("1", 9)),
          "isBusinessIncorporated" -> No.toString,
          "doYouHaveUTR" -> No.toString,
          "otherPartners" -> Yes.toString
          )
    assertFormIsValid(form, testDataScen3)

  }

  "check when partner is Corporate Body and uses Welsh characters" in {
    val conditionEntityIsCompany = Map("entityType" -> CorporateBody.toString)

    val data: Map[String, String] =
      conditionEntityIsCompany +
        ("companyNames.businessName" -> testWelshChars,
          "companyNames.doYouHaveTradingName" -> YesString,
          "companyNames.tradingName" -> testWelshChars,
          "partnerAddress.postcode" -> testPostcode,
          "partnerAddress.addressLine1" -> testWelshChars,
          "partnerAddress.addressLine2" -> testWelshChars,
          "doYouHaveVRN" -> No.toString,
          "isBusinessIncorporated" -> No.toString,
          "doYouHaveUTR" -> Yes.toString,
          "utr" -> generateFieldTestDataInThisFormat(DataFormat("1", 10)),
          "otherPartners" -> No.toString
        )
    assertFormIsValid(form, data)
  }

  "Valid journeys for when partner is Sole Trader must pass" in {
    val conditionEntityIsSoleTrader = Map("entityType" -> SoleTrader.toString)

    val testDataScen1: Map[String, String] =
      conditionEntityIsSoleTrader +
        ("firstName" -> "firstName",
          "lastName" -> "lastName",
          "companyNames.doYouHaveTradingName" -> YesString,
          "companyNames.tradingName" -> "tradingName",
          "partnerAddress.postcode" -> testPostcode,
          "partnerAddress.addressLine1" -> "addressLine1",
          "partnerAddress.addressLine2" -> "addressLine2",
          "doYouHaveNino" -> Yes.toString,
          "nino" -> testNino,
          "doYouHaveVRN" -> Yes.toString,
          "vrn" -> generateFieldTestDataInThisFormat(DataFormat("1", 9)),
          "doYouHaveUTR" -> Yes.toString,
          "utr" -> generateFieldTestDataInThisFormat(DataFormat("1", 10)),
          "otherPartners" -> Yes.toString
          )
    assertFormIsValid(form, testDataScen1)

    val testDataScen2: Map[String, String] =
      conditionEntityIsSoleTrader +
        ("firstName" -> "firstName",
          "lastName" -> "lastName",
          "companyNames.doYouHaveTradingName" -> NoString,
          "partnerAddress.postcode" -> testPostcode,
          "partnerAddress.addressLine1" -> "addressLine1",
          "partnerAddress.addressLine2" -> "addressLine2",
          "doYouHaveNino" -> No.toString,
          "doYouHaveVRN" -> No.toString,
          "doYouHaveUTR" -> Yes.toString,
          "utr" -> generateFieldTestDataInThisFormat(DataFormat("1", 10)),
          "otherPartners" -> Yes.toString
          )
    assertFormIsValid(form, testDataScen2)

    val testDataScen3: Map[String, String] =
      conditionEntityIsSoleTrader +
        ("firstName" -> "firstName",
          "lastName" -> "lastName",
          "companyNames.doYouHaveTradingName" -> YesString,
          "companyNames.tradingName" -> "tradingName",
          "partnerAddress.postcode" -> testPostcode,
          "partnerAddress.addressLine1" -> "addressLine1",
          "partnerAddress.addressLine2" -> "addressLine2",
          "doYouHaveNino" -> Yes.toString,
          "nino" -> testNino,
          "doYouHaveVRN" -> No.toString,
          "doYouHaveUTR" -> No.toString,
          "otherPartners" -> No.toString
          )
    assertFormIsValid(form, testDataScen3)

  }

  "check when partner is Sole Trader and uses Welsh characters" in {
    val conditionEntityIsSoleTrader = Map("entityType" -> SoleTrader.toString)

    val data: Map[String, String] =
      conditionEntityIsSoleTrader +
        ("firstName" -> testWelshChars,
          "lastName" -> testWelshChars,
          "companyNames.doYouHaveTradingName" -> YesString,
          "companyNames.tradingName" -> testWelshChars,
          "partnerAddress.postcode" -> testPostcode,
          "partnerAddress.addressLine1" -> testWelshChars,
          "partnerAddress.addressLine2" -> testWelshChars,
          "doYouHaveNino" -> Yes.toString,
          "nino" -> testNino,
          "doYouHaveVRN" -> Yes.toString,
          "vrn" -> generateFieldTestDataInThisFormat(DataFormat("1", 9)),
          "doYouHaveUTR" -> Yes.toString,
          "utr" -> generateFieldTestDataInThisFormat(DataFormat("1", 10)),
          "otherPartners" -> Yes.toString
        )
    assertFormIsValid(form, data)
  }
}
}