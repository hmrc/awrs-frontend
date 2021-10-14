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
import forms.prevalidation._
import forms.submapping.AddressMapping._
import forms.submapping.CompanyNamesMapping._
import forms.submapping.CompanyRegMapping._
import forms.submapping._
import forms.validation.util.ConstraintUtil._
import forms.validation.util.MappingUtilAPI._
import forms.validation.util.NamedMappingAndUtil._
import forms.validation.util.TargetFieldIds
import models.GroupMember
import play.api.data.Form
import play.api.data.Forms._

object GroupMemberDetailsForm {
  @inline def answeredYesToDoYouHaveCRN: FormData => Boolean = whenAnswerToFieldIs(doYouHaveCrn, BooleanRadioEnum.YesString)(_)

  @inline def answeredYesToDoYouHaveUTR: FormData => Boolean = whenAnswerToFieldIs(doYouHaveUtr, BooleanRadioEnum.YesString)(_)

  private val otherMembers_compulsory = yesNoQuestion_compulsory("addAnotherGrpMember", "awrs.group_member.addAnother.empty")
  private val mustHaveVRNorCRNorUTR = mustHaveAtLeastOneId(TargetFieldIds("doYouHaveVRN", "isBusinessIncorporated", "doYouHaveUTR"), "awrs.generic.error.identification_provided")

  val doYouHaveVrn = "doYouHaveVRN"
  val doYouHaveCrn = "isBusinessIncorporated"
  val doYouHaveUtr = "doYouHaveUTR"
  val utr = "utr"
  val vrn = "vrn"
  val crnMapping = "companyRegDetails"
  val names = "companyNames"

  private val inferBasedOn = (dependentField: Option[String]) => dependentField.map(x => x.trim) match {
    case None | Some("") => BooleanRadioEnum.NoString
    case _ => BooleanRadioEnum.YesString
  }

  private val inferDoYouHave = (data: Map[String, String]) => {

    val doYouHaveCrnInferred = inferBasedOn(data.get(crnMapping attach crn))

    val doYouHaveUtrInferred = inferBasedOn(data.get(utr))

    data. +((doYouHaveCrn, doYouHaveCrnInferred), (doYouHaveUtr, doYouHaveUtrInferred))
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

  def groupMemberValidationForm(implicit applicationConfig: ApplicationConfig): Form[GroupMember] = {
    val noIdIsSupplied = noIdsHaveBeenSupplied _
    Form(
      mapping(
        names -> companyNamesMapping(names),
        "address" -> ukAddress_compulsory(prefix = "address", "", applicationConfig.countryCodes).toOptionalAddressMapping,
        "groupJoiningDate" -> optional(text),
        doYouHaveUtr -> doYouHaveUTR_compulsory(doYouHaveUtr),
        utr -> (utr_compulsory(utr) iff noIdIsSupplied ||| answeredYesToDoYouHaveUTR),
        doYouHaveCrn -> doYouHaveCRN_compulsory(doYouHaveCrn),
        crnMapping -> (companyReg_compulsory(crnMapping).toOptionalCompanyRegMapping iff noIdIsSupplied ||| answeredYesToDoYouHaveCRN),
        doYouHaveVrn -> (doYouHaveVRN_compulsory() + mustHaveVRNorCRNorUTR),
        vrn -> (vrn_compulsory() iff whenAnsweredYesToVRN),
        "addAnotherGrpMember" -> otherMembers_compulsory
      )
      (GroupMember.apply)(GroupMember.unapply)
    )
  }

  def groupMemberForm(implicit applicationConfig: ApplicationConfig): PrevalidationAPI[GroupMember] = PreprocessedForm(groupMemberValidationForm).addNewPreprocessFunction(inferDoYouHave)

}
