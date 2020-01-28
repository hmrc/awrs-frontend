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

import forms.AWRSEnums._
import forms.test.util._
import forms.validation.util.FieldError
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestConstants._

class BusinessDirectorsFormTest extends UnitSpec with MockitoSugar with OneServerPerSuite {

  implicit lazy val form = BusinessDirectorsForm.businessDirectorsForm.form

  import BooleanRadioEnum._
  import DirectorAndSecretaryEnum._
  import PersonOrCompanyEnum._

  val directorTypes = Set(Director, CompanySecretary, DirectorAndSecretary)


  "Form validations" should {

    "check the director and company secretary type is selected" in {
      val fieldId = "directorsAndCompanySecretaries"

      val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.business_directors.error.directorsAndCompanySecretaries_empty"))

      val expectations = CompulsoryEnumValidationExpectations(emptyError, DirectorAndSecretaryEnum)
      fieldId assertEnumFieldIsCompulsory expectations
    }

    "check the person or company question is answered" in {
      val fieldId = "personOrCompany"

      val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.business_directors.error.personOrCompany_empty"))

      val expectations = CompulsoryEnumValidationExpectations(emptyError, PersonOrCompanyEnum)
      fieldId assertEnumFieldIsCompulsory expectations
    }

    "Directors or secretaries that are individuals," should {

      for (directorType <- directorTypes) {

        val conditionDirectorIsCompany_theseShouldIgnoreTheValidationOnThisField: Set[Map[String, String]] =
          Set(Map("directorsAndCompanySecretaries" -> directorType.toString, "personOrCompany" -> Company.toString))

        val conditionDirectorIsIndividual: Map[String, String] = Map("directorsAndCompanySecretaries" -> directorType.toString, "personOrCompany" -> Person.toString)

        "check 'first name' and 'last name' validations, when director is %s".format(directorType) in
          NamedUnitTests.firstNameAndLastNameIsCompulsoryAndValid(
            conditionDirectorIsIndividual, conditionDirectorIsCompany_theseShouldIgnoreTheValidationOnThisField)

        "check 'NINO' or ('passportNumber' or 'nationalID') validations, when director is %s".format(directorType) in
          NamedUnitTests.doYouHaveNinoIsAnsweredAndValidIfTheAnswerIsYes_orPassportOrNationalIdIfTheAnswerIsNo(
            conditionDirectorIsIndividual, conditionDirectorIsCompany_theseShouldIgnoreTheValidationOnThisField, doYouHaveNinoNameString = "doTheyHaveNationalInsurance")

        "check 'otherDirectors' validations, when director is %s".format(directorType) in
          NamedUnitTests.doYouHaveOtherDirectorsIsAnswered(conditionDirectorIsIndividual)

        "Valid journeys for when director is %s should pass".format(directorType) in {
          val testDataScen1: Map[String, String] =
            conditionDirectorIsIndividual +
              ("firstName" -> "firstName",
                "lastName" -> "lastName",
                "doTheyHaveNationalInsurance" -> Yes.toString,
                "NINO" -> testNino,
                "otherDirectors" -> Yes.toString
              )
          assertFormIsValid(form, testDataScen1)

          val testDataScen2: Map[String, String] =
            conditionDirectorIsIndividual +
              ("firstName" -> "firstName",
                "lastName" -> "lastName",
                "doTheyHaveNationalInsurance" -> No.toString,
                "passportNumber" -> generateFieldTestDataInThisFormat(DataFormat("1", 20)),
                "otherDirectors" -> Yes.toString
              )
          assertFormIsValid(form, testDataScen2)

          val testDataScen3: Map[String, String] =
            conditionDirectorIsIndividual +
              ("firstName" -> "firstName",
                "lastName" -> "lastName",
                "doTheyHaveNationalInsurance" -> No.toString,
                "nationalID" -> generateFieldTestDataInThisFormat(DataFormat("1", 20)),
                "otherDirectors" -> No.toString
              )
          assertFormIsValid(form, testDataScen3)
        }

        "check Welsh character validations for %s".format(directorType) in {
          val data: Map[String, String] =
            conditionDirectorIsIndividual +
              ("firstName" -> testWelshChars,
                "lastName" -> testWelshChars,
                "doTheyHaveNationalInsurance" -> Yes.toString,
                "NINO" -> testNino,
                "otherDirectors" -> Yes.toString
              )
          assertFormIsValid(form, data)
        }
      }
    }

    "Directors or secretaries that are companies," should {

      for (directorType <- directorTypes) {

        val conditionsDirectorIsIndividual_theseShouldIgnoreTheValidationOnThisField: Set[Map[String, String]] =
          Set(Map("directorsAndCompanySecretaries" -> directorType.toString, "personOrCompany" -> Person.toString))

        val conditionDirectorIsCompany: Map[String, String] = Map("directorsAndCompanySecretaries" -> directorType.toString, "personOrCompany" -> Company.toString)

        "company names validations are correct, when director is %s".format(directorType) in {
          NamedUnitTests.companyNamesAreValid(preCondition = conditionDirectorIsCompany, idPrefix = "companyNames")
        }

        "check 'doYouHaveVRN' is answered and if the answer is yes then 'vrn' is correctly validated, when director is %s".format(directorType) in
          NamedUnitTests.doYouHaveVRNIsAnsweredAndValidIfTheAnswerIsYes(
            conditionDirectorIsCompany, conditionsDirectorIsIndividual_theseShouldIgnoreTheValidationOnThisField)

        "check 'doYouHaveCRN' is answered and if the answer is yes then 'companyRegNumber' is correctly validated, when director is %s".format(directorType) in
          NamedUnitTests.doYouHaveCRNIsAnsweredAndValidIfTheAnswerIsYes_withoutDateOfIncorporation(
            conditionDirectorIsCompany, conditionsDirectorIsIndividual_theseShouldIgnoreTheValidationOnThisField,
            doYouHaveCRNNameString = "doYouHaveCRN", CRNNameString = "companyRegNumber", alsoTestWhenDoYouHaveCRNIsAnsweredWithNo = false)

        "check 'doYouHaveUTR' is answered and if the answer is yes then 'utr' is correctly validated, when director is %s".format(directorType) in
          NamedUnitTests.doYouHaveUTRIsAnsweredAndValidIfTheAnswerIsYes(
            conditionDirectorIsCompany, conditionsDirectorIsIndividual_theseShouldIgnoreTheValidationOnThisField, None, false)

        "check validations for at least one in ('doYouHaveVRN' or 'doYouHaveCRN' or 'doYouHaveUTR'), when director is %s".format(directorType) in
          NamedUnitTests.atLeastOneProofOfIdIsAnsweredWithYes(
            ExpectedFieldIsEmpty("doYouHaveVRN", FieldError("awrs.business_directors.error.do_you_have_id")),
            conditionDirectorIsCompany,
            Set("doYouHaveVRN", "doYouHaveCRN", "doYouHaveUTR"),
            conditionsDirectorIsIndividual_theseShouldIgnoreTheValidationOnThisField)

        "check 'otherDirectors' validations, when director is %s".format(directorType) in
          NamedUnitTests.doYouHaveOtherDirectorsIsAnswered(conditionDirectorIsCompany)

        "Valid journeys for when director is %s should pass".format(directorType) in {
          val testDataScen1: Map[String, String] =
            conditionDirectorIsCompany +
              ("companyNames.tradingName" -> "tradingName",
                "companyNames.doYouHaveTradingName" -> "Yes",
                "companyNames.businessName" -> "companyName",
                "doYouHaveVRN" -> Yes.toString,
                "vrn" -> generateFieldTestDataInThisFormat(DataFormat("1", 9)),
                "doYouHaveCRN" -> No.toString,
                "doYouHaveUTR" -> No.toString,
                "otherDirectors" -> Yes.toString
                )
          assertFormIsValid(form, testDataScen1)

          val testDataScen2: Map[String, String] =
            conditionDirectorIsCompany +
              ("companyNames.doYouHaveTradingName" -> "No",
                "companyNames.businessName" -> "companyName",
                "doYouHaveVRN" -> No.toString,
                "doYouHaveCRN" -> Yes.toString,
                "companyRegNumber" ->
                  generateFieldTestDataInThisFormat(
                    DataFormat("0", 1),
                    DataFormat("1", 7)),
                "doYouHaveUTR" -> No.toString,
                "otherDirectors" -> No.toString
                )
          assertFormIsValid(form, testDataScen2)

          val testDataScen3: Map[String, String] =
            conditionDirectorIsCompany +
              ("companyNames.tradingName" -> "tradingName",
                "companyNames.doYouHaveTradingName" -> "Yes",
                "companyNames.businessName" -> "companyName",
                "doYouHaveVRN" -> No.toString,
                "doYouHaveCRN" -> No.toString,
                "doYouHaveUTR" -> Yes.toString,
                "utr" -> generateFieldTestDataInThisFormat(DataFormat("1", 10)),
                "otherDirectors" -> Yes.toString
                )
          assertFormIsValid(form, testDataScen3)
        }

        "check Welsh character validations for %s".format(directorType) in {
          val data: Map[String, String] =
            conditionDirectorIsCompany +
              ("companyNames.tradingName" -> testWelshChars,
                "companyNames.doYouHaveTradingName" -> "Yes",
                "companyNames.businessName" -> testWelshChars,
                "doYouHaveVRN" -> Yes.toString,
                "vrn" -> generateFieldTestDataInThisFormat(DataFormat("1", 9)),
                "doYouHaveCRN" -> No.toString,
                "doYouHaveUTR" -> No.toString,
                "otherDirectors" -> Yes.toString
              )
          assertFormIsValid(form, data)
        }
      }
    }
  }
}
