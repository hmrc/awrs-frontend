/*
 * Copyright 2019 HM Revenue & Customs
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
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import uk.gov.hmrc.play.test.UnitSpec
import forms.PartnershipDetailsForm._
import utils.TestConstants._

class PartnershipDetailsFormTest extends UnitSpec with MockitoSugar with OneServerPerSuite {

  implicit lazy val form = PartnershipDetailsForm.partnershipDetailsForm.form

  import EntityTypeEnum._
  import BooleanRadioEnum._

  val partnerTypes = Set(Individual, CorporateBody, SoleTrader)
  lazy val crnField = s"$crnMapping.companyRegistrationNumber"

  "Form validations" should {
    "Partners," should {

      "check the entity type is selected" in {
        val fieldId = "entityType"

        val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.business-partner.error.entityType_empty"))

        val expectations = CompulsoryEnumValidationExpectations(emptyError, EntityTypeEnum)
        fieldId assertEnumFieldIsCompulsory expectations
      }

      for (partnerType <- partnerTypes - CorporateBody) {
        s"check 'first name' and 'last name' validation for $partnerType" in {
          val conditionPartnerIsOfType: Map[String, String] = Map("entityType" -> partnerType.toString)
          val ignoreConstraintsWhenThePartnerIsCompany: Set[Map[String, String]] = Set(Map("entityType" -> CorporateBody.toString))
          NamedUnitTests.firstNameAndLastNameIsCompulsoryAndValid(conditionPartnerIsOfType, ignoreConstraintsWhenThePartnerIsCompany)
        }

        s"check nino validation for $partnerType" in {
          val conditionPartnerIsOfType: Map[String, String] = Map("entityType" -> partnerType.toString)
          val ignoreConstraintsWhenThePartnerIsCompany: Set[Map[String, String]] = Set(Map("entityType" -> CorporateBody.toString))
          NamedUnitTests.doYouHaveNinoIsAnsweredAndValidIfTheAnswerIsYes(conditionPartnerIsOfType, ignoreConstraintsWhenThePartnerIsCompany, ninoNameString = "nino")
        }
      }

      "check company name and trading name validation" in {
        val partnerIsUnanswered: Map[String, String] = Map()
        val partnerIsIndividual: Map[String, String] = Map("entityType" -> Individual.toString)
        val partnerIsCorporateBody: Map[String, String] = Map("entityType" -> CorporateBody.toString)
        val partnerIsSoleTrader: Map[String, String] = Map("entityType" -> SoleTrader.toString)

        NamedUnitTests.companyNamesAreValid(
          preCondition = partnerIsCorporateBody,
          ignoreCondition = Set(partnerIsUnanswered, partnerIsIndividual),
          idPrefix = companyNames,
          isBusinessNameRequired = true
        )
        NamedUnitTests.companyNamesAreValid(
          preCondition = partnerIsSoleTrader,
          idPrefix = companyNames,
          isBusinessNameRequired = false
        )
      }

      for (partnerType <- partnerTypes) {

        val conditionPartnerIsOfType: Map[String, String] = Map("entityType" -> partnerType.toString)

        "check 'addressLine1', 'addressLine2, 'addressLine3', 'addressLine4','postcode' validations, when partner is %s".format(partnerType) in
          NamedUnitTests.ukAddressIsCompulsoryAndValid(conditionPartnerIsOfType, idPrefix = "partnerAddress")
      }

      for (partnerType <- partnerTypes - Individual) {

        f"check 'doYouHaveVRN' is answered and if the answer is yes then 'vrn' is correctly validated, when partner is $partnerType" in {
          val conditionPartnerIsOfType: Map[String, String] = Map("entityType" -> partnerType.toString)
          val ignoreConstraintsWhenThePartnerIsIndividual: Set[Map[String, String]] = Set(Map("entityType" -> Individual.toString))
          NamedUnitTests.doYouHaveVRNIsAnsweredAndValidIfTheAnswerIsYes(
            conditionPartnerIsOfType, ignoreConstraintsWhenThePartnerIsIndividual)
        }

        f"check that if 'utr' is entered, it is correctly validated, when partner is $partnerType" in {
          val preCondition = Map[String, String]()
          val ignoreCondition = Set[Map[String, String]]()
          val idPrefix = None
          ProofOfIdentiticationVerifications.utrIsValidWhenDoYouHaveUTRIsAnsweredWithYes(
            preCondition,
            ignoreCondition,
            idPrefix,
            alsoTestWhenDoYouHaveUtrIsAnsweredWithNo = false)
        }

      }

      "check that if 'companyRegNumber' is entered, it is correctly validated, when partner is Corporate Body" in {
        ProofOfIdentiticationVerifications.companyRegNumberIsValidWhenDoYouHaveCRNIsAnsweredWithYes(
          preCondition = Map(),
          ignoreCondition = Set(),
          doYouHaveCRNNameString = doYouHaveCrn,
          CRNNameString = crnField,
          alsoTestWhenDoYouHaveCRNIsAnsweredWithNo = false
        )
        ProofOfIdentiticationVerifications.dateOfIncorporationIsCompulsoryAndValidWhenDoYouHaveCRNIsAnsweredWithYes(
          preCondition = Map(),
          ignoreCondition = Set(),
          idPrefix = "companyRegDetails",
          doYouHaveCRNNameString = doYouHaveCrn
        )
      }

      "check validations for at least one in ('doYouHaveVRN' or 'isBusinessIncorporated' or 'doYouHaveUTR'), when partner is Corporate Body" in {
        val conditionPartnerIsOfType: Map[String, String] = Map("entityType" -> CorporateBody.toString)
        val ignoreConstraintsWhenThePartnerIsCompany: Set[Map[String, String]] = Set(Map("entityType" -> Individual.toString), Map("entityType" -> SoleTrader.toString))

        val expectedError = ExpectedFieldIsEmpty("doYouHaveVRN", FieldError("awrs.generic.error.identification_provided"))
        NamedUnitTests.atLeastOneProofOfIdIsAnsweredWithYes(
          expectedError,
          conditionPartnerIsOfType,
          Set("doYouHaveVRN", "isBusinessIncorporated", "doYouHaveUTR"),
          ignoreConstraintsWhenThePartnerIsCompany)
      }

      "check validations for at least one in ('doYouHaveNino' or 'doYouHaveVRN' or 'doYouHaveUTR'), when partner is Sole Trader" in {
        val conditionPartnerIsOfType: Map[String, String] = Map("entityType" -> SoleTrader.toString)
        val ignoreConstraintsWhenThePartnerIsCompany: Set[Map[String, String]] = Set(Map("entityType" -> Individual.toString), Map("entityType" -> CorporateBody.toString))

        val expectedError = ExpectedFieldIsEmpty("doYouHaveNino", FieldError("awrs.generic.error.identification_provided"))
        NamedUnitTests.atLeastOneProofOfIdIsAnsweredWithYes(
          expectedError,
          conditionPartnerIsOfType,
          Set("doYouHaveNino", "doYouHaveVRN", "doYouHaveUTR"),
          ignoreConstraintsWhenThePartnerIsCompany)
      }
      //


      "check do you have other partner is selected" in {
        val fieldId = "otherPartners"

        val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.business-partner.error.add_more_partner"))

        val expectations = CompulsoryEnumValidationExpectations(emptyError, BooleanRadioEnum)
        fieldId assertEnumFieldIsCompulsory expectations
      }


      "Valid journeys for when partner is Individual should pass" in {
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

      "Valid journeys for when partner is Corporate Body should pass" in {
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

      "Valid journeys for when partner is Sole Trader should pass" in {
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

    }

  }


}
