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

import forms.AWRSEnums.PersonOrCompanyEnum._
import forms.AWRSEnums.{BooleanRadioEnum, DirectorAndSecretaryEnum, PersonOrCompanyEnum}
import forms.prevalidation._
import forms.submapping.CompanyNamesMapping._
import forms.validation.util.ConstraintUtil._
import forms.validation.util.ErrorMessagesUtilAPI._
import forms.validation.util.MappingUtilAPI._
import forms.validation.util.NamedMappingAndUtil._
import forms.validation.util.TargetFieldIds
import models.BusinessDirector
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Valid
import utils.AwrsValidator._

object BusinessDirectorsForm {

  val doYouHaveNino = "doTheyHaveNationalInsurance"
  val doYouHaveVrn = "doYouHaveVRN"
  val doYouHaveCrn = "doYouHaveCRN"
  val doYouHaveUtr = "doYouHaveUTR"
  val nino = "NINO"
  val utr = "utr"
  val vrn = "vrn"
  val crn = "companyRegNumber"
  val companyNames = "companyNames"

  // this question is only required for this form
  private val directorsAndCompanySecretaries_compulsory = {
    val question = CompulsoryEnumMappingParameter(
      simpleFieldIsEmptyConstraintParameter("directorsAndCompanySecretaries", "awrs.business_directors.error.directorsAndCompanySecretaries_empty"),
      DirectorAndSecretaryEnum
    )
    compulsoryEnum(question)
  }

  private val personOrCompany_compulsory = {
    val question = CompulsoryEnumMappingParameter(
      simpleFieldIsEmptyConstraintParameter("personOrCompany", "awrs.business_directors.error.personOrCompany_empty"),
      PersonOrCompanyEnum
    )
    compulsoryEnum(question)
  }

  private val getPersonOrCompany = (data: FormData) =>
    data.getOrElse("personOrCompany", "")

  // cross field validation condition for when the director is an individual
  private val whenDirectorIsAPerson = (data: FormData) =>
    getPersonOrCompany(data).equals(Person.toString)
  // cross field validation condition for when the director is a company
  private val whenDirectorIsCompany = (data: FormData) =>
    getPersonOrCompany(data).equals(Company.toString)

  // nino error messages can be different between forms
  private val doTheyHaveNino_compulsory = yesNoQuestion_compulsory(doYouHaveNino, "awrs.generic.error.do_you_have_nino_empty")

  val answeredNoToNino = answerGivenInFieldIs(doYouHaveNino, BooleanRadioEnum.No.toString)

  private val passportNumberIsEmpty = noAnswerGivenInField("passportNumber")

  private val passportNumber_optional = {
    val fieldId = "passportNumber"
    val passportConstraintParameters =
      OptionalTextFieldMappingParameter(
        MaxLengthConstraintIsHandledByTheRegEx(),
        FieldFormatConstraintParameter(
          (str: String) =>
            str.matches(passportRegex) match {
              case true => Valid
              case false => simpleErrorMessage(fieldId, "awrs.business_directors.error.passport_no_invalid")
            }
        )
      )
    optionalText(passportConstraintParameters)
  }

  private val nationalIDIsEmpty = noAnswerGivenInField("nationalID")

  private val nationalID_optional = {
    val fieldId = "nationalID"
    val passportConstraintParameters =
      OptionalTextFieldMappingParameter(
        MaxLengthConstraintIsHandledByTheRegEx(),
        FieldFormatConstraintParameter(
          (str: String) =>
            str.matches(nationalIDRegex) match {
              case true => Valid
              case false => simpleErrorMessage(fieldId, "awrs.business_directors.error.national_id_numeric")
            }
        )
      )
    optionalText(passportConstraintParameters)
  }

  private val passportAndNationalIDCannotBothBeEmpty =
    CrossFieldConstraint(whenDirectorIsAPerson &&& nationalIDIsEmpty &&& passportNumberIsEmpty,
      simpleCrossFieldErrorMessage(TargetFieldIds("passportNumber", "nationalID"),
        "awrs.business_directors.error.non_resident.passport_no_and_nationalId_empty"))

  private val otherDirectors_compulsory = yesNoQuestion_compulsory("otherDirectors", "awrs.business_directors.error.other_directors")

  private val inferBasedOn = (dependentField: Option[String]) => dependentField.map(x => x.trim) match {
    case None | Some("") => BooleanRadioEnum.NoString
    case _ => BooleanRadioEnum.YesString
  }

  private val inferDoYouHave = (data: Map[String, String]) => {

    val doYouHaveCrnInferred = inferBasedOn(data.get(crn))

    val doYouHaveUtrInferred = inferBasedOn(data.get(utr))

    data.+((doYouHaveCrn, doYouHaveCrnInferred), (doYouHaveUtr, doYouHaveUtrInferred))
  }

  /*
  *  1 map ids to if they are answered with yes
  *  2 combine the answers to get the boolean for if any id is answered with yes
  *  3 invert the aforementioned boolean
  *  4 check if vrn is answered
  */
  @inline def noIdsHaveBeenSupplied(data: FormData): Boolean = {
    val ids = Seq[String](doYouHaveUtr, doYouHaveCrn)
    val yesToDoYouHaveVRN = whenAnsweredYesToVRN(data)
    val vrnIsNotAnswered = data.getOrElse(vrn, "").equals("")
    !ids.filter(!_.equals(doYouHaveVrn)).map(id => whenAnswerToFieldIs(id, BooleanRadioEnum.YesString)(data)).reduce(_ || _) &&
      (!yesToDoYouHaveVRN || yesToDoYouHaveVRN && vrnIsNotAnswered) // if doYouhaveVRN is not answered or if it is answered with yes but no VRN is supplied
  }


  val businessDirectorsValidationForm = {
    val noIdIsSupplied = noIdsHaveBeenSupplied(_)
    Form(mapping(
      "personOrCompany" -> personOrCompany_compulsory,
      "firstName" -> (firstName_compulsory() iff whenDirectorIsAPerson),
      "lastName" -> (lastName_compulsory() iff whenDirectorIsAPerson),
      doYouHaveNino -> (doTheyHaveNino_compulsory iff whenDirectorIsAPerson),
      nino -> (nino_compulsory() iff (whenDirectorIsAPerson &&& answeredYesToNino(doYouHaveNino))), // the implicit `&&&` is defined in ConstraintUtil
      "passportNumber" -> ((passportNumber_optional + passportAndNationalIDCannotBothBeEmpty) iff (whenDirectorIsAPerson &&& answeredNoToNino)),
      "nationalID" -> (nationalID_optional iff (whenDirectorIsAPerson &&& answeredNoToNino)),
      companyNames -> (companyNamesMapping(companyNames).toOptional iff whenDirectorIsCompany),
      doYouHaveUtr -> (doYouHaveUTR_compulsory() iff whenDirectorIsCompany),
      utr -> (utr_compulsory() iff (whenDirectorIsCompany &&& (noIdIsSupplied ||| whenAnsweredYesToUTR))),
      doYouHaveCrn -> (doYouHaveCRN_compulsory() iff whenDirectorIsCompany),
      crn -> (crn_compulsory iff (whenDirectorIsCompany &&& (noIdIsSupplied ||| whenAnsweredYesToCRN))),
      doYouHaveVrn -> ((doYouHaveVRN_compulsory() + mustHaveVRNorCRNorUTR) iff whenDirectorIsCompany),
      vrn -> (vrn_compulsory() iff (whenDirectorIsCompany &&& whenAnsweredYesToVRN)),
      "directorsAndCompanySecretaries" -> directorsAndCompanySecretaries_compulsory,
      "otherDirectors" -> otherDirectors_compulsory
    )(BusinessDirector.apply)(BusinessDirector.unapply))
  }

  val businessDirectorsForm = PreprocessedForm(businessDirectorsValidationForm).addNewPreprocessFunction(inferDoYouHave)

}
