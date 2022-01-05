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

package forms.test.util

import forms.AWRSEnums.BooleanRadioEnum._
import forms.AWRSEnums._
import forms.submapping.CompanyNamesMapping
import forms.validation.util.{FieldError, MessageArguments, SummaryError}
import models.TupleDate
import play.api.data.Form
import utils.AwrsFieldConfig

object NamedUnitTests extends AwrsFormTestUtils {

  import AddressVerifications._
  import IdentityVerifications._
  import ProofOfIdentiticationVerifications._

  def firstNameAndLastNameIsCompulsoryAndValid(preCondition: Map[String, String] = Map(),
                                               ignoreCondition: Set[Map[String, String]] = Set(),
                                               idPrefix: IdPrefix = None,
                                               firstNameId: String = "",
                                               lastNameId: String = "")(implicit form: Form[_]): Unit = {
    firstNameIsCompulsory(preCondition, ignoreCondition, idPrefix, firstNameId)
    lastNameIsCompulsory(preCondition, ignoreCondition, idPrefix, lastNameId)
  }

  def fieldIsCompulsoryAndValid(idPrefix: IdPrefix = None,
                                fieldId: String = "",
                                emptyErrorMsg: String,
                                invalidFormatErrorMsg: String)(implicit form: Form[_]): Unit = {
    val newFieldId = idPrefix attach fieldId

    val emptyError = ExpectedFieldIsEmpty(newFieldId, FieldError(emptyErrorMsg))
    val invalidFormats = List(ExpectedInvalidFieldFormat("α", newFieldId, FieldError(invalidFormatErrorMsg)))
    val formatError = ExpectedFieldFormat(invalidFormats)

    val expectations = CompulsoryFieldValidationExpectations(emptyError, MaxLengthIsHandledByTheRegEx(), formatError)

    newFieldId assertFieldIsCompulsory expectations
  }

  def eitherCompanyOrTradingNameIsAnsweredAndTheAnswersAreValid(companyNameDescString: String,
                                                                preCondition: Map[String, String] = Map(),
                                                                ignoreBothCondition: Set[Map[String, String]] = Set(),
                                                                ignoreCompanyNameCondition: Set[Map[String, String]] = Set(),
                                                                ignoreTradingCondition: Set[Map[String, String]] = Set(),
                                                                idPrefix: IdPrefix = None)(implicit form: Form[_]): Unit = {
    companyNameIsOptional(companyNameDescString, preCondition, ignoreCompanyNameCondition ++ ignoreCompanyNameCondition, idPrefix)
    IdentityVerifications.tradingNameIsOptional(preCondition, ignoreBothCondition ++ ignoreTradingCondition, idPrefix)
    companyOrTradingNameIsAnswered(preCondition, ignoreBothCondition, idPrefix)
  }

  @inline def tradingNameIsOptional(preCondition: Map[String, String] = Map(),
                                    ignoreCondition: Set[Map[String, String]] = Set(),
                                    idPrefix: IdPrefix = None)(implicit form: Form[_]): Unit = {
    IdentityVerifications.tradingNameIsOptional(preCondition, ignoreCondition, idPrefix)
  }

  @inline def companyNamesAreValid(preCondition: Map[String, String] = Map(),
                                   ignoreCondition: Set[Map[String, String]] = Set(),
                                   idPrefix: IdPrefix = None,
                                   isBusinessNameRequired: Boolean = true)(implicit form: Form[_]) =
    IdentityVerifications.companyNamesAreValid(preCondition, ignoreCondition, idPrefix, isBusinessNameRequired)

  def doYouHaveNinoIsAnsweredAndValidIfTheAnswerIsYes(preCondition: Map[String, String] = Map(),
                                                      ignoreCondition: Set[Map[String, String]] = Set(),
                                                      idPrefix: IdPrefix = None,
                                                      doYouHaveNinoNameString: String = "doYouHaveNino",
                                                      ninoNameString: String = "NINO")(implicit form: Form[_]): Unit = {
    doYouHaveNinoIsAnswered(preCondition, ignoreCondition, idPrefix, doYouHaveNinoNameString)
    ninoIsValidatedIfHaveNinoIsYes(preCondition, ignoreCondition, idPrefix, doYouHaveNinoNameString, ninoNameString)
  }

  def doYouHaveNinoIsAnsweredAndValidIfTheAnswerIsYes_orPassportOrNationalIdIfTheAnswerIsNo
  (preCondition: Map[String, String] = Map(),
   ignoreCondition: Set[Map[String, String]] = Set(),
   idPrefix: IdPrefix = None,
   doYouHaveNinoNameString: String = "doYouHaveNino",
   ninoNameString: String = "NINO"
  )(implicit form: Form[_]): Unit = {
    doYouHaveNinoIsAnsweredAndValidIfTheAnswerIsYes(preCondition, ignoreCondition, idPrefix, doYouHaveNinoNameString, ninoNameString)
    passportIsValidatedIfHaveNinoIsNo(preCondition, ignoreCondition, idPrefix, doYouHaveNinoNameString)
    nationalIDIsValidatedIfHaveNinoIsNo(preCondition, ignoreCondition, idPrefix, doYouHaveNinoNameString)
    eitherPassportOrNationalIDMustBeEnteredIfNinoIsNo(preCondition, ignoreCondition, idPrefix, doYouHaveNinoNameString)
  }

  def doYouHaveVRNIsAnsweredAndValidIfTheAnswerIsYes(preCondition: Map[String, String] = Map(),
                                                     ignoreCondition: Set[Map[String, String]] = Set(),
                                                     idPrefix: IdPrefix = None)(implicit form: Form[_]): Unit = {
    doYouHaveVRNIsAnswered(preCondition, ignoreCondition, idPrefix)
    vrnIsValidWhenDoYouHaveVRNIsAnsweredWithYes(preCondition, ignoreCondition, idPrefix)
  }

  def doYouHaveCRNIsAnsweredAndValidIfTheAnswerIsYes_withoutDateOfIncorporation(preCondition: Map[String, String] = Map(),
                                                                                ignoreCondition: Set[Map[String, String]] = Set(),
                                                                                doYouHaveCRNPrefix: IdPrefix = None,
                                                                                doYouHaveCRNNameString: String,
                                                                                idPrefix: IdPrefix = None,
                                                                                CRNNameString: String,
                                                                                alsoTestWhenDoYouHaveCRNIsAnsweredWithNo: Boolean = true)(implicit form: Form[_]): Unit = {
    doYouHaveCRNIsAnswered(preCondition, ignoreCondition, doYouHaveCRNPrefix, doYouHaveCRNNameString)
    companyRegNumberIsValidWhenDoYouHaveCRNIsAnsweredWithYes(preCondition, ignoreCondition, doYouHaveCRNPrefix, doYouHaveCRNNameString, idPrefix, CRNNameString, alsoTestWhenDoYouHaveCRNIsAnsweredWithNo)
  }

  def doYouHaveCRNIsAnsweredAndValidIfTheAnswerIsYes_withDateOfIncorporation(preCondition: Map[String, String] = Map(),
                                                                             ignoreCondition: Set[Map[String, String]] = Set(),
                                                                             doYouHaveCRNPrefix: IdPrefix = None,
                                                                             doYouHaveCRNNameString: String,
                                                                             idPrefix: IdPrefix = None,
                                                                             CRNNameString: String = "companyRegistrationNumber"
                                                                            )(implicit form: Form[_]): Unit = {
    doYouHaveCRNIsAnswered(preCondition, ignoreCondition, doYouHaveCRNPrefix, doYouHaveCRNNameString)
    val dateOfIncorporation = idPrefix attach "dateOfIncorporation"
    val preConditionRegNum_test_safe = preCondition ++ Map(f"$dateOfIncorporation.day" -> "", f"$dateOfIncorporation.month" -> "", f"$dateOfIncorporation.year" -> "")
    companyRegNumberIsValidWhenDoYouHaveCRNIsAnsweredWithYes(preConditionRegNum_test_safe, ignoreCondition, doYouHaveCRNPrefix, doYouHaveCRNNameString, idPrefix, CRNNameString)
    val crn = idPrefix attach CRNNameString
    val preConditionDI_test_safe = preCondition ++ Map(crn -> "")
    dateOfIncorporationIsCompulsoryAndValidWhenDoYouHaveCRNIsAnsweredWithYes(preConditionDI_test_safe, ignoreCondition, idPrefix, doYouHaveCRNPrefix attach doYouHaveCRNNameString)
  }

  def doYouHaveUTRIsAnsweredAndValidIfTheAnswerIsYes(preCondition: Map[String, String] = Map(),
                                                     ignoreCondition: Set[Map[String, String]] = Set(),
                                                     idPrefix: IdPrefix = None,
                                                     alsoTestWhenDoYouHaveUtrIsAnsweredWithNo: Boolean = true)(implicit form: Form[_]): Unit = {
    doYouHaveUTRIsAnswered(preCondition, ignoreCondition, idPrefix)
    utrIsValidWhenDoYouHaveUTRIsAnsweredWithYes(preCondition, ignoreCondition, idPrefix, alsoTestWhenDoYouHaveUtrIsAnsweredWithNo)
  }

  def atLeastOneProofOfIdIsAnsweredWithYes(expectedError: ExpectedFieldIsEmpty,
                                           preCondition: Map[String, String] = Map(),
                                           fieldIds: Set[String],
                                           ignoreCondition: Set[Map[String, String]] = Set(),
                                           idPrefix: IdPrefix = None)(implicit form: Form[_]): Unit =
    ProofOfIdentiticationVerifications.AtLeastOneProofOfIdIsAnsweredWithYes(expectedError, preCondition, fieldIds, ignoreCondition, idPrefix)


  def doYouHaveOtherDirectorsIsAnswered(preCondition: Map[String, String] = Map(),
                                        ignoreCondition: Set[Map[String, String]] = Set(),
                                        idPrefix: IdPrefix = None)(implicit form: Form[_]): Unit = {
    val fieldId = idPrefix attach "otherDirectors"

    val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.business_directors.error.other_directors"))

    val expectations = CompulsoryEnumValidationExpectations(emptyError, BooleanRadioEnum)
    fieldId assertEnumFieldIsCompulsoryWhen(preCondition, expectations)
  }

  def ukAddressIsCompulsoryAndValid(preCondition: Map[String, String] = Map(),
                                    ignoreCondition: Set[Map[String, String]] = Set(),
                                    idPrefix: IdPrefix = None,
                                    nameInErrorMessage: String = "")(implicit form: Form[_]): Unit = {
    addressLine1(preCondition, ignoreCondition, idPrefix, nameInErrorMessage)
    addressLine2(preCondition, ignoreCondition, idPrefix, nameInErrorMessage)
    addressLine3(preCondition, ignoreCondition, idPrefix, nameInErrorMessage)
    addressLine4(preCondition, ignoreCondition, idPrefix, nameInErrorMessage)
    postcode(preCondition, ignoreCondition, idPrefix, nameInErrorMessage)
  }

  def foreignAddressIsCompulsoryAndValid(preCondition: Map[String, String] = Map(),
                                         ignoreCondition: Set[Map[String, String]] = Set(),
                                         idPrefix: IdPrefix = None,
                                         nameInErrorMessage: String = "")(implicit form: Form[_]): Unit = {
    addressLine1(preCondition, ignoreCondition, idPrefix, nameInErrorMessage)
    addressLine2(preCondition, ignoreCondition, idPrefix, nameInErrorMessage)
    addressLine3(preCondition, ignoreCondition, idPrefix, nameInErrorMessage)
    addressLine4(preCondition, ignoreCondition, idPrefix, nameInErrorMessage)
    addressCountry(preCondition, ignoreCondition, idPrefix, nameInErrorMessage)
  }
}

private object AddressVerifications extends AwrsFormTestUtils with AwrsFieldConfig {
  private def addressLinex(lineNumber: Int,
                           preCondition: Map[String, String],
                           ignoreCondition: Set[Map[String, String]],
                           idPrefix: IdPrefix,
                           nameInErrorMessage: String)(implicit form: Form[_]): Unit = {
    require(Set(1, 2, 3, 4) contains lineNumber)
    val preCondition_safe = preCondition ++
      Map((idPrefix attach "addressLine1") -> "") ++
      Map((idPrefix attach "addressLine2") -> "")

    val fieldId: String = idPrefix attach f"addressLine$lineNumber"
    val maxLenError = ExpectedFieldExceedsMaxLength(fieldId, f"$nameInErrorMessage address line $lineNumber", addressLineLen)
    val invalidFormats = List(ExpectedInvalidFieldFormat("α", fieldId, f"$nameInErrorMessage address line $lineNumber"))
    val formatError = ExpectedFieldFormat(invalidFormats)

    if (lineNumber == 1 || lineNumber == 2) {
      val emptyFieldErr = FieldError(f"awrs.generic.error.addressLine${lineNumber}_empty", MessageArguments(nameInErrorMessage))
      val emptyError = ExpectedFieldIsEmpty(emptyFieldErr, SummaryError(emptyFieldErr, MessageArguments(nameInErrorMessage), fieldId))

      val expectations = CompulsoryFieldValidationExpectations(emptyError, maxLenError, formatError)

      fieldId assertFieldIsCompulsoryWhen(preCondition_safe, expectations)
      if (ignoreCondition.nonEmpty)
        fieldId assertFieldIsIgnoredWhen(ignoreCondition, expectations.toFieldToIgnore)
    }

    if (lineNumber == 3 || lineNumber == 4) {
      val expectations = OptionalFieldValidationExpectations(maxLenError, formatError)

      fieldId assertFieldIsOptionalWhen(preCondition_safe, expectations)
      if (ignoreCondition.nonEmpty)
        fieldId assertFieldIsIgnoredWhen(ignoreCondition, expectations.toFieldToIgnore)
    }
  }

  def addressLine1(preCondition: Map[String, String] = Map(),
                   ignoreCondition: Set[Map[String, String]] = Set(),
                   idPrefix: IdPrefix = None,
                   nameInErrorMessage: String)(implicit form: Form[_]): Unit ={
    addressLinex(1, preCondition, ignoreCondition, idPrefix, nameInErrorMessage)
  }


  def addressLine2(preCondition: Map[String, String] = Map(),
                   ignoreCondition: Set[Map[String, String]] = Set(),
                   idPrefix: IdPrefix = None,
                   nameInErrorMessage: String)(implicit form: Form[_]): Unit =
    addressLinex(2, preCondition, ignoreCondition, idPrefix, nameInErrorMessage)

  def addressLine3(preCondition: Map[String, String] = Map(),
                   ignoreCondition: Set[Map[String, String]] = Set(),
                   idPrefix: IdPrefix = None,
                   nameInErrorMessage: String)(implicit form: Form[_]): Unit =
    addressLinex(3, preCondition, ignoreCondition, idPrefix, nameInErrorMessage)

  def addressLine4(preCondition: Map[String, String] = Map(),
                   ignoreCondition: Set[Map[String, String]] = Set(),
                   idPrefix: IdPrefix = None,
                   nameInErrorMessage: String)(implicit form: Form[_]): Unit =
    addressLinex(4, preCondition, ignoreCondition, idPrefix, nameInErrorMessage)

  def postcode(preCondition: Map[String, String] = Map(),
               ignoreCondition: Set[Map[String, String]] = Set(),
               idPrefix: IdPrefix = None,
               nameInErrorMessage: String)(implicit form: Form[_]): Unit = {
    val preCondition_safe = preCondition ++
      Map((idPrefix attach "addressLine1") -> "") ++
      Map((idPrefix attach "addressLine2") -> "")

    val fieldId: String = idPrefix attach "postcode"
    val postCodeEmptyFieldErr = FieldError("awrs.generic.error.postcode_empty", MessageArguments(nameInErrorMessage))
    val emptyError = ExpectedFieldIsEmpty(postCodeEmptyFieldErr, SummaryError(postCodeEmptyFieldErr, MessageArguments(nameInErrorMessage), fieldId))
    val postCodeInvalidFieldErr = FieldError("awrs.generic.error.postcode_invalid", MessageArguments(nameInErrorMessage))
    val invalidFormats = List(ExpectedInvalidFieldFormat("α", postCodeInvalidFieldErr, SummaryError(postCodeInvalidFieldErr, MessageArguments(nameInErrorMessage), fieldId)))

    val formatError = ExpectedFieldFormat(invalidFormats)

    val expectations = CompulsoryFieldValidationExpectations(emptyError, MaxLengthIsHandledByTheRegEx(), formatError)

    fieldId assertFieldIsCompulsoryWhen(preCondition_safe, expectations)
    if (ignoreCondition.nonEmpty)
      fieldId assertFieldIsIgnoredWhen(ignoreCondition, expectations.toFieldToIgnore)

  }

  def addressCountry(preCondition: Map[String, String] = Map(),
                     ignoreCondition: Set[Map[String, String]] = Set(),
                     idPrefix: IdPrefix = None,
                     nameInErrorMessage: String)(implicit form: Form[_]): Unit = {
    val preCondition_safe = preCondition ++
      Map((idPrefix attach "addressLine1") -> "") ++
      Map((idPrefix attach "addressLine2") -> "")

    val fieldId: String = idPrefix attach "addressCountry"
    val addressCountryEmptyFieldErr = FieldError("awrs.supplier-addresses.error.supplier_address_country_blank")
    val emptyError = ExpectedFieldIsEmpty(addressCountryEmptyFieldErr, SummaryError(addressCountryEmptyFieldErr, fieldId))
    val invalidFormats = List(ExpectedInvalidFieldFormat("α", fieldId, FieldError("awrs.supplier-addresses.error.supplier_address_country_invalid")))

    val formatError = ExpectedFieldFormat(invalidFormats)

    val expectations = CompulsoryFieldValidationExpectations(emptyError, MaxLengthIsHandledByTheRegEx(), formatError)

    fieldId assertFieldIsCompulsoryWhen(preCondition_safe, expectations)
    if (ignoreCondition.nonEmpty)
      fieldId assertFieldIsIgnoredWhen(ignoreCondition, expectations.toFieldToIgnore)

  }
}

private object IdentityVerifications extends AwrsFormTestUtils with AwrsFieldConfig {

  def firstNameIsCompulsory(preCondition: Map[String, String],
                            ignoreCondition: Set[Map[String, String]],
                            idPrefix: IdPrefix = None,
                            firstNameId: String)(implicit form: Form[_]): Unit = {
    val fieldId: String = idPrefix attach (firstNameId match {
      case "" => "firstName"
      case id => id
    })
    val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.generic.error.first_name_empty"))
    val maxLenError = ExpectedFieldExceedsMaxLength(fieldId, "First name", firstNameLen, "awrs.generic.error.name.maximum_length")
    val invalidFormats = List(ExpectedInvalidFieldFormat("α", fieldId, "first name"))
    val formatError = ExpectedFieldFormat(invalidFormats)

    val expectations = CompulsoryFieldValidationExpectations(emptyError, maxLenError, formatError)

    fieldId assertFieldIsCompulsoryWhen(preCondition, expectations)
    if (ignoreCondition.nonEmpty)
      fieldId assertFieldIsIgnoredWhen(ignoreCondition, expectations.toFieldToIgnore)
  }

  def lastNameIsCompulsory(preCondition: Map[String, String],
                           ignoreCondition: Set[Map[String, String]],
                           idPrefix: IdPrefix = None,
                           lastNameId: String)(implicit form: Form[_]): Unit = {
    val fieldId: String = idPrefix attach (lastNameId match {
      case "" => "lastName"
      case id => id
    })

    val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.generic.error.last_name_empty"))
    val maxLenError = ExpectedFieldExceedsMaxLength(fieldId, "Last name", lastNameLen, "awrs.generic.error.name.maximum_length")
    val invalidFormats = List(ExpectedInvalidFieldFormat("α", fieldId, "last name"))
    val formatError = ExpectedFieldFormat(invalidFormats)

    val expectations = CompulsoryFieldValidationExpectations(emptyError, maxLenError, formatError)

    fieldId assertFieldIsCompulsoryWhen(preCondition, expectations)
    if (ignoreCondition.nonEmpty)
      fieldId assertFieldIsIgnoredWhen(ignoreCondition, expectations.toFieldToIgnore)
  }


  def companyNamesAreValid(preCondition: Map[String, String],
                           ignoreCondition: Set[Map[String, String]],
                           idPrefix: IdPrefix,
                           isBusinessNameRequired: Boolean = true
                          )(implicit form: Form[_]) = {

    val businessName = idPrefix attach CompanyNamesMapping.businessName
    val doYouHaveTradingName = idPrefix attach CompanyNamesMapping.doYouHaveTradingName
    val tradingName = idPrefix attach CompanyNamesMapping.tradingName


    val businessNameExpectations = (fieldId: String) => {
      val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.generic.error.businessName_empty"))
      val maxLenError = ExpectedFieldExceedsMaxLength(fieldId, "business name", companyNameLen)
      val invalidFormats = List(ExpectedInvalidFieldFormat("α", fieldId, "business name"))
      val formatError = ExpectedFieldFormat(invalidFormats)

      CompulsoryFieldValidationExpectations(emptyError, maxLenError, formatError)
    }

    val doYouHaveTradingNameExpectations = (fieldId: String) => {
      val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.generic.error.do_you_have_trading_name_empty"))

      CompulsoryEnumValidationExpectations(emptyError, BooleanRadioEnum)
    }

    val tradingNameExpectations = (fieldId: String) => {
      val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.generic.error.tradingName_empty"))
      val maxLenError = ExpectedFieldExceedsMaxLength(fieldId, "trading name", tradingNameLen)
      val invalidFormats = List(ExpectedInvalidFieldFormat("α", fieldId, "trading name"))
      val formatError = ExpectedFieldFormat(invalidFormats)

      CompulsoryFieldValidationExpectations(emptyError, maxLenError, formatError)
    }

    val asnsweredYesToTradingName: Map[String, String] = Map(doYouHaveTradingName -> BooleanRadioEnum.Yes.toString)

    def assertBusinessNameCompulsory() = {
      val fieldId = businessName

      fieldId assertFieldIsCompulsoryWhen(preCondition, businessNameExpectations(fieldId))
      if (ignoreCondition.nonEmpty)
        fieldId assertFieldIsIgnoredWhen(ignoreCondition, businessNameExpectations(fieldId).toFieldToIgnore)
    }

    def assertBusinessNameIsIgnored() = {
      val ignoreCondition = Map[String, String]()

      val fieldId = businessName

      fieldId assertFieldIsIgnoredWhen(ignoreCondition, businessNameExpectations(fieldId).toFieldToIgnore)
    }

    def assertDoYouHaveTradingNameCompulsory() = {
      val fieldId = doYouHaveTradingName

      fieldId assertEnumFieldIsCompulsoryWhen(preCondition, doYouHaveTradingNameExpectations(fieldId))

      if (ignoreCondition.nonEmpty)
        fieldId assertEnumFieldIsIgnoredWhen(ignoreCondition, doYouHaveTradingNameExpectations(fieldId).toIgnoreEnumFieldExpectation)
    }


    def assertTradingNameCompulsoryYesToDoYouHaveTradingName() = {
      val fieldId = tradingName


      fieldId assertFieldIsCompulsoryWhen(preCondition ++ asnsweredYesToTradingName, tradingNameExpectations(tradingName))

      if (ignoreCondition.nonEmpty)
        fieldId assertFieldIsIgnoredWhen(asnsweredYesToTradingName, tradingNameExpectations(tradingName).toFieldToIgnore)
    }

    isBusinessNameRequired match {
      case true => assertBusinessNameCompulsory
      case false => assertBusinessNameIsIgnored
    }
    assertDoYouHaveTradingNameCompulsory
    assertTradingNameCompulsoryYesToDoYouHaveTradingName
  }


  def companyNameIsOptional(companyNameDescString: String,
                            preCondition: Map[String, String],
                            ignoreCondition: Set[Map[String, String]],
                            idPrefix: IdPrefix = None
                           )(implicit form: Form[_]): Unit = {
    val fieldId = idPrefix attach "companyName"

    val maxLenError = ExpectedFieldExceedsMaxLength(fieldId, companyNameDescString, companyNameLen)
    val invalidFormats = List(ExpectedInvalidFieldFormat("α", fieldId, companyNameDescString))
    val formatError = ExpectedFieldFormat(invalidFormats)

    val expectations = OptionalFieldValidationExpectations(maxLenError, formatError)
    fieldId assertFieldIsOptionalWhen(preCondition, expectations)

    fieldId assertFieldIsIgnoredWhen(ignoreCondition, expectations.toFieldToIgnore)
  }

  def tradingNameIsOptional(preCondition: Map[String, String],
                            ignoreCondition: Set[Map[String, String]],
                            idPrefix: IdPrefix = None)(implicit form: Form[_]): Unit = {
    val fieldId = idPrefix attach "tradingName"

    val maxLenError = ExpectedFieldExceedsMaxLength(fieldId, "trading name", tradingNameLen)
    val invalidFormats = List(ExpectedInvalidFieldFormat("α", fieldId, "trading name"))
    val formatError = ExpectedFieldFormat(invalidFormats)

    val expectations = OptionalFieldValidationExpectations(maxLenError, formatError)
    fieldId assertFieldIsOptionalWhen(preCondition, expectations)

    fieldId assertFieldIsIgnoredWhen(ignoreCondition, expectations.toFieldToIgnore)
  }

  def companyOrTradingNameIsAnswered(preCondition: Map[String, String],
                                     ignoreCondition: Set[Map[String, String]],
                                     idPrefix: IdPrefix = None)(implicit form: Form[_]): Unit = {
    val companyNameId = idPrefix attach "companyName"
    val fieldIds = Set(companyNameId, idPrefix attach "tradingName")

    val emptyError = ExpectedFieldIsEmpty(companyNameId, FieldError("awrs.generic.error.company_trading_name"))

    val expectations = CrossFieldValidationExpectations(companyNameId, emptyError)
    fieldIds assertAtLeastOneFieldMustNotBeEmptyWhen(preCondition, expectations)

    fieldIds assertAtLeastOneFieldMustNotBeEmptyIsIgnoredWhen(ignoreCondition, expectations)
  }

}

object ProofOfIdentiticationVerifications extends AwrsFormTestUtils {

  object DoYouHaveIdentificationQuestionTypes extends Enumeration {
    val Nino = Value("doYouHaveNino")
    val Nino_altr = Value("doTheyHaveNationalInsurance")
    val VRN = Value("doYouHaveVRN")
    val CompanyReg = Value("doYouHaveCRN")
    val CompanyReg_alt = Value("isBusinessIncorporated")
    val UTR = Value("doYouHaveUTR")
  }

  lazy val validIds = DoYouHaveIdentificationQuestionTypes.values.toSeq.map(_.toString)

  def doYouHaveNinoIsAnswered(preCondition: Map[String, String],
                              ignoreCondition: Set[Map[String, String]],
                              idPrefix: IdPrefix = None,
                              doYouHaveNinoNameString: String = "doYouHaveNino",
                              ninoNameString: String = "NINO")(implicit form: Form[_]): Unit = {
    withClue(f"check $doYouHaveNinoNameString' validations\n") {
      val fieldId = idPrefix attach doYouHaveNinoNameString

      //TODO if the question is made generic then this can be refactored
      val emptyError = if (doYouHaveNinoNameString.equals("doYouHaveNino")) {
        // currently do you have nino
        ExpectedFieldIsEmpty(fieldId, FieldError("awrs.generic.error.do_you_have_nino_empty"))
      }
      else {
        // currently do they have nino
        ExpectedFieldIsEmpty(fieldId, FieldError("awrs.generic.error.do_you_have_nino_empty"))
      }

      val expectations =
        CompulsoryEnumValidationExpectations(emptyError, BooleanRadioEnum)

      fieldId assertEnumFieldIsCompulsoryWhen(preCondition, expectations)
      if (ignoreCondition.nonEmpty)
        fieldId assertEnumFieldIsIgnoredWhen(ignoreCondition, expectations.toIgnoreEnumFieldExpectation)
    }
  }

  def ninoIsValidatedIfHaveNinoIsYes(preCondition: Map[String, String],
                                     ignoreCondition: Set[Map[String, String]],
                                     idPrefix: IdPrefix = None,
                                     doYouHaveNinoNameString: String = "doYouHaveNino",
                                     ninoNameString: String = "NINO",
                                     alsoTestWhenDoYouHaveNinoIsAnsweredWithNo: Boolean = true)(implicit form: Form[_]): Unit = {
    withClue(f"check $ninoNameString validations\n") {
      val fieldId = idPrefix attach ninoNameString
      val questionId = idPrefix attach doYouHaveNinoNameString
      val theyHaveNINO = generateFormTestData(preCondition, questionId, Yes.toString)

      val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.generic.error.nino_empty"))
      val invalidFormats = List(ExpectedInvalidFieldFormat("α", fieldId, FieldError("awrs.generic.error.nino_invalid")))
      val formatError = ExpectedFieldFormat(invalidFormats)

      val expectations = CompulsoryFieldValidationExpectations(emptyError, MaxLengthIsHandledByTheRegEx(), formatError)
      fieldId assertFieldIsCompulsoryWhen(theyHaveNINO, expectations)

      alsoTestWhenDoYouHaveNinoIsAnsweredWithNo match {
        case false =>
        case true =>
          val theyDoNotHaveNINO = generateFormTestData(preCondition, questionId, No.toString)
          fieldId assertFieldIsIgnoredWhen(theyDoNotHaveNINO, expectations.toFieldToIgnore)

          if (ignoreCondition.nonEmpty)
            fieldId assertFieldIsIgnoredWhen(ignoreCondition, expectations.toFieldToIgnore)
      }
    }
  }

  def passportIsValidatedIfHaveNinoIsNo(preCondition: Map[String, String],
                                        ignoreCondition: Set[Map[String, String]],
                                        idPrefix: IdPrefix = None,
                                        doYouHaveNinoNameString: String = "doYouHaveNino")(implicit form: Form[_]): Unit = {
    withClue("check 'passportNumber' validations\n") {
      val fieldId = idPrefix attach "passportNumber"

      val theyDoNotHaveNINO = generateFormTestData(preCondition, doYouHaveNinoNameString, No.toString)

      val invalidFormats = List(ExpectedInvalidFieldFormat("α", fieldId, FieldError("awrs.business_directors.error.passport_no_invalid")))
      val formatError = ExpectedFieldFormat(invalidFormats)

      val expectations = OptionalFieldValidationExpectations(MaxLengthIsHandledByTheRegEx(), formatError)
      fieldId assertFieldIsOptionalWhen(theyDoNotHaveNINO, expectations)

      // if they answered that they have nino then we must not validate the passport field even if it is populated
      val theyHaveNINO = generateFormTestData(preCondition, doYouHaveNinoNameString, Yes.toString)
      fieldId assertFieldIsIgnoredWhen(theyHaveNINO, expectations.toFieldToIgnore)

      if (ignoreCondition.nonEmpty)
        fieldId assertFieldIsIgnoredWhen(ignoreCondition, expectations.toFieldToIgnore)
    }
  }

  def nationalIDIsValidatedIfHaveNinoIsNo(preCondition: Map[String, String],
                                          ignoreCondition: Set[Map[String, String]],
                                          idPrefix: IdPrefix = None,
                                          doYouHaveNinoNameString: String = "doYouHaveNino")(implicit form: Form[_]): Unit = {
    withClue("check 'nationalID' validations\n") {
      val fieldId = idPrefix attach "nationalID"

      val theyDoNotHaveNINO = generateFormTestData(preCondition, doYouHaveNinoNameString, No.toString)

      val invalidFormats = List(ExpectedInvalidFieldFormat("α", fieldId, FieldError("awrs.business_directors.error.national_id_numeric")))
      val formatError = ExpectedFieldFormat(invalidFormats)

      val expectations = OptionalFieldValidationExpectations(MaxLengthIsHandledByTheRegEx(), formatError)
      fieldId assertFieldIsOptionalWhen(theyDoNotHaveNINO, expectations)

      // if they answered that they have nino then we must not validate the national field even if it is populated
      val theyHaveNINO = generateFormTestData(preCondition, doYouHaveNinoNameString, Yes.toString)
      fieldId assertFieldIsIgnoredWhen(theyHaveNINO, expectations.toFieldToIgnore)

      if (ignoreCondition.nonEmpty)
        fieldId assertFieldIsIgnoredWhen(ignoreCondition, expectations.toFieldToIgnore)
    }
  }

  def eitherPassportOrNationalIDMustBeEnteredIfNinoIsNo(preCondition: Map[String, String],
                                                        ignoreCondition: Set[Map[String, String]],
                                                        idPrefix: IdPrefix = None,
                                                        doYouHaveNinoNameString: String = "doYouHaveNino")(implicit form: Form[_]): Unit = {
    withClue("check validations for at least one in ('passportNumber' or 'nationalID')\n") {
      val fieldIds = Set(idPrefix attach "passportNumber", idPrefix attach "nationalID")

      val theyDoNotHaveNINO = generateFormTestData(preCondition, doYouHaveNinoNameString, No.toString)

      // This is used for cross field error link, only a none empty config for passport number is required because
      // that is where the anchor will be set to.
      // Although national ID will have a field error it will not have a summary error anchored to it
      val emptyError = ExpectedFieldIsEmpty("passportNumber", FieldError("awrs.business_directors.error.non_resident.passport_no_and_nationalId_empty"))
      val expectations = CrossFieldValidationExpectations("passportNumber", emptyError)
      fieldIds assertAtLeastOneFieldMustNotBeEmptyWhen(theyDoNotHaveNINO, expectations)

      // if they answered that they have nino then we must not validate the either passport orn ational id field even if it is populated
      val theyHaveNINO = generateFormTestData(preCondition, doYouHaveNinoNameString, Yes.toString)
      fieldIds assertAtLeastOneFieldMustNotBeEmptyIsIgnoredWhen(theyHaveNINO, expectations)

      if (ignoreCondition.nonEmpty)
        fieldIds assertAtLeastOneFieldMustNotBeEmptyIsIgnoredWhen(ignoreCondition, expectations)
    }
  }

  def doYouHaveVRNIsAnswered(preCondition: Map[String, String],
                             ignoreCondition: Set[Map[String, String]],
                             idPrefix: IdPrefix = None)(implicit form: Form[_]): Unit = {
    val fieldId = idPrefix attach "doYouHaveVRN"

    val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.generic.error.do_you_have_vat_reg_empty"))

    val expectations = CompulsoryEnumValidationExpectations(emptyError, BooleanRadioEnum)
    fieldId assertEnumFieldIsCompulsoryWhen(preCondition, expectations)
    if (ignoreCondition.nonEmpty)
      fieldId assertEnumFieldIsIgnoredWhen(ignoreCondition, expectations.toIgnoreEnumFieldExpectation)
  }

  def vrnIsValidWhenDoYouHaveVRNIsAnsweredWithYes(preCondition: Map[String, String],
                                                  ignoreCondition: Set[Map[String, String]],
                                                  idPrefix: IdPrefix = None)(implicit form: Form[_]): Unit = {
    val fieldId = idPrefix attach "vrn"

    val questionId = idPrefix attach "doYouHaveVRN"

    val theyHaveVRN = generateFormTestData(preCondition, questionId, Yes.toString)

    val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.generic.error.vrn_empty"))
    val invalidFormats = List(ExpectedInvalidFieldFormat("α", fieldId, FieldError("awrs.generic.error.vrn_invalid")))
    val formatError = ExpectedFieldFormat(invalidFormats)

    val expectations = CompulsoryFieldValidationExpectations(emptyError, MaxLengthIsHandledByTheRegEx(), formatError)

    fieldId assertFieldIsCompulsoryWhen(theyHaveVRN, expectations)

    val theyDoNotHaveVRN = generateFormTestData(preCondition, questionId, No.toString)
    fieldId assertFieldIsIgnoredWhen(theyDoNotHaveVRN, expectations.toFieldToIgnore)
    if (ignoreCondition.nonEmpty)
      fieldId assertFieldIsIgnoredWhen(ignoreCondition, expectations.toFieldToIgnore)
  }

  def doYouHaveCRNIsAnswered(preCondition: Map[String, String],
                             ignoreCondition: Set[Map[String, String]],
                             idPrefix: IdPrefix = None,
                             doYouHaveCRNNameString: String)(implicit form: Form[_]): Unit = {
    val fieldId = idPrefix attach doYouHaveCRNNameString

    val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.generic.error.do_you_have_company_reg_empty"))

    val expectations = CompulsoryEnumValidationExpectations(emptyError, BooleanRadioEnum)
    fieldId assertEnumFieldIsCompulsoryWhen(preCondition, expectations)
    if (ignoreCondition.nonEmpty)
      fieldId assertEnumFieldIsIgnoredWhen(ignoreCondition, expectations.toIgnoreEnumFieldExpectation)
  }

  def companyRegNumberIsValidWhenDoYouHaveCRNIsAnsweredWithYes(preCondition: Map[String, String],
                                                               ignoreCondition: Set[Map[String, String]],
                                                               doYouHaveCRNPrefix: IdPrefix = None,
                                                               doYouHaveCRNNameString: String,
                                                               crnPrefix: IdPrefix = None,
                                                               CRNNameString: String,
                                                               alsoTestWhenDoYouHaveCRNIsAnsweredWithNo: Boolean = true)(implicit form: Form[_]): Unit = {
    val fieldId = crnPrefix attach CRNNameString

    val theyHaveCRN = preCondition + ((doYouHaveCRNPrefix attach doYouHaveCRNNameString) -> Yes.toString)

    val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.generic.error.companyRegNumber_empty"))
    val invalidFormats =
      List(
        ExpectedInvalidFieldFormat("α", fieldId, FieldError("awrs.generic.error.companyRegNumber_invalid")),
        ExpectedInvalidFieldFormat("1234-678", fieldId, FieldError("awrs.generic.error.companyRegNumber_invalid")),
        ExpectedInvalidFieldFormat("aaaaaaaa", fieldId, FieldError("awrs.generic.error.companyRegNumber_atleastOneNumber")),
        ExpectedInvalidFieldFormat("", fieldId, FieldError("awrs.generic.error.companyRegNumber_empty")),
        ExpectedInvalidFieldFormat("123", fieldId, FieldError("awrs.generic.error.companyRegNumber_invalid"))
      )
    val formatError = ExpectedFieldFormat(invalidFormats)

    val expectations = CompulsoryFieldValidationExpectations(emptyError, MaxLengthIsHandledByTheRegEx(), formatError)

    fieldId assertFieldIsCompulsoryWhen(theyHaveCRN, expectations)

    alsoTestWhenDoYouHaveCRNIsAnsweredWithNo match {
      case false =>
      case true =>
        val theyDoNotHaveCRN = preCondition + ("doYouHaveCRN" -> No.toString)
        fieldId assertFieldIsIgnoredWhen(theyDoNotHaveCRN, expectations.toFieldToIgnore)
        if (ignoreCondition.nonEmpty)
          fieldId assertFieldIsIgnoredWhen(ignoreCondition, expectations.toFieldToIgnore)
    }
  }

  def dateOfIncorporationIsCompulsoryAndValidWhenDoYouHaveCRNIsAnsweredWithYes(preCondition: Map[String, String] = Map(),
                                                                               ignoreCondition: Set[Map[String, String]] = Set(),
                                                                               idPrefix: IdPrefix = None,
                                                                               doYouHaveCRNNameString: String)(implicit form: Form[_]): Unit = {
    val fieldId = idPrefix attach "dateOfIncorporation"

    val theyHaveCRN = preCondition + (doYouHaveCRNNameString -> Yes.toString)

    val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.generic.error.companyRegDate_empty"))
    val dateIsInvalidFieldErr = FieldError("awrs.generic.error.companyRegDate_invalid")
    val invalidFormats = List(ExpectedInvalidDateFormat(TupleDate("α", "α", "α"), dateIsInvalidFieldErr, SummaryError(dateIsInvalidFieldErr, MessageArguments(""), fieldId)))
    val formatError = ExpectedDateFormat(invalidFormats)

    val expectations = CompulsoryDateValidationExpectations(emptyError, formatError)

    fieldId assertDateFieldIsCompulsoryWhen(theyHaveCRN, expectations)
    if (ignoreCondition.nonEmpty)
      fieldId assertDateFieldIsIgnoredWhen(ignoreCondition, expectations.toDateToIgnore)
  }

  def doYouHaveUTRIsAnswered(preCondition: Map[String, String],
                             ignoreCondition: Set[Map[String, String]],
                             idPrefix: IdPrefix = None)(implicit form: Form[_]): Unit = {
    val fieldId = idPrefix attach "doYouHaveUTR"

    val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.generic.error.do_you_have_utr_empty"))

    val expectations = CompulsoryEnumValidationExpectations(emptyError, BooleanRadioEnum)
    fieldId assertEnumFieldIsCompulsoryWhen(preCondition, expectations)
    if (ignoreCondition.nonEmpty)
      fieldId assertEnumFieldIsIgnoredWhen(ignoreCondition, expectations.toIgnoreEnumFieldExpectation)
  }

  def utrIsValidWhenDoYouHaveUTRIsAnsweredWithYes(preCondition: Map[String, String],
                                                  ignoreCondition: Set[Map[String, String]],
                                                  idPrefix: IdPrefix = None,
                                                  alsoTestWhenDoYouHaveUtrIsAnsweredWithNo: Boolean = true,
                                                  isLTD: Boolean = false)(implicit form: Form[_]): Unit = {
    val fieldId = idPrefix attach "utr"

    val questionId = idPrefix attach "doYouHaveUTR"

    val theyHaveUTR = generateFormTestData(preCondition, questionId, Yes.toString)
    var emptyErrorMsg = "awrs.generic.error.utr_empty"
    var invalidErrorMsg = "awrs.generic.error.utr_invalid"

    isLTD match {
      case true => {
        emptyErrorMsg = "awrs.generic.error.utr_empty_LTD"
        invalidErrorMsg = "awrs.generic.error.utr_invalid_LTD"
      }
      case _ => {
        emptyErrorMsg = "awrs.generic.error.utr_empty"
        invalidErrorMsg = "awrs.generic.error.utr_invalid"
      }
    }

    val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError(emptyErrorMsg))
    val invalidFormats = List(ExpectedInvalidFieldFormat("α", fieldId, FieldError(invalidErrorMsg)))

    val formatError = ExpectedFieldFormat(invalidFormats)

    val expectations = CompulsoryFieldValidationExpectations(emptyError, MaxLengthIsHandledByTheRegEx(), formatError)

    fieldId assertFieldIsCompulsoryWhen(theyHaveUTR, expectations)

    alsoTestWhenDoYouHaveUtrIsAnsweredWithNo match {
      case false =>
      case true =>
        val theyDoNotHaveUTR = generateFormTestData(preCondition, questionId, No.toString)
        fieldId assertFieldIsIgnoredWhen(theyDoNotHaveUTR, expectations.toFieldToIgnore)
        if (ignoreCondition.nonEmpty)
          fieldId assertFieldIsIgnoredWhen(ignoreCondition, expectations.toFieldToIgnore)
    }
  }

  def AtLeastOneProofOfIdIsAnsweredWithYes(expectedError: ExpectedFieldIsEmpty,
                                           preCondition: Map[String, String],
                                           fieldIds: Set[String],
                                           ignoreCondition: Set[Map[String, String]],
                                           idPrefix: IdPrefix = None
                                          )(implicit form: Form[_]): Unit = {
    require(fieldIds.nonEmpty)

    for (id <- fieldIds)
      withClue(f"'$id' is not a supported identification type for this test\nPlease check the spelling or amended it in the enum DoYouHaveIdentificationQuestionTypes\n") {
        validIds must contain(id)
      }

    val fieldIdsWithPrefix: Set[String] = idPrefix attachToAll fieldIds

    val expectations = CrossFieldValidationExpectations("vrn", expectedError)
    fieldIdsWithPrefix assertAllFieldsCannotBeAnsweredWithInvalidWhen(preCondition, expectations, No.toString)
    if (ignoreCondition.nonEmpty)
      fieldIdsWithPrefix assertAllFieldsCannotBeAnsweredWithInvalidIsIgnoredWhen(ignoreCondition, expectations, No.toString)
  }
}
