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

import forms.AWRSEnums.BooleanRadioEnum
import forms.prevalidation._
import forms.submapping.CompanyRegMapping._
import forms.submapping.FieldNameUtil
import forms.validation.util.ConstraintUtil._
import forms.validation.util.MappingUtilAPI._
import forms.validation.util.NamedMappingAndUtil.{doYouHaveCRN_compulsory => _, _}
import models._
import play.api.data.{Form, Mapping}
import play.api.data.Forms._

object BusinessRegistrationDetailsForm {

  val doYouHaveNino = "doYouHaveNino"
  val doYouHaveVrn = "doYouHaveVRN"
  val doYouHaveCrn = "isBusinessIncorporated"
  val doYouHaveUtr = "doYouHaveUTR"
  val nino = "NINO"
  val utr = "utr"
  val vrn = "vrn"
  val crnMapping = "companyRegDetails"

  @inline def doYouHaveCRN_compulsory(): Mapping[Option[String]] = yesNoQuestion_compulsory(doYouHaveCrn, "awrs.generic.error.do_you_have_company_reg_empty")

  @inline def answeredYesToDoYouHaveCRN: FormData => Boolean = whenAnswerToFieldIs(doYouHaveCrn, BooleanRadioEnum.YesString)(_)

  @inline def answeredYesToDoYouHaveNino: FormData => Boolean = whenAnswerToFieldIs(doYouHaveNino, BooleanRadioEnum.YesString)(_)

  @inline def answeredYesToDoYouHaveVRN: FormData => Boolean = whenAnswerToFieldIs(doYouHaveVrn, BooleanRadioEnum.YesString)(_)

  @inline def answeredYesToDoYouHaveUTR: FormData => Boolean = whenAnswerToFieldIs(doYouHaveUtr, BooleanRadioEnum.YesString)(_)

  private val soleTraderIds = Seq[String](doYouHaveUtr, doYouHaveNino, doYouHaveVrn)
  private val limitedIds = Seq[String](doYouHaveUtr, doYouHaveCrn, doYouHaveVrn)
  private val partnership = Seq[String](doYouHaveUtr, doYouHaveVrn)

  private val entityIds = (entityType: String) => entityType match {
    case "SOP" => soleTraderIds
    case "Partnership" => partnership
    case _ => limitedIds
  }

  private val inferBasedOn = (dependentField: Option[String]) => dependentField.map(x => x.trim) match {
    case None | Some("") => BooleanRadioEnum.NoString
    case _ => BooleanRadioEnum.YesString
  }

  private val inferDoYouHave = (data: Map[String, String]) => {
    val doYouhaveNinoInferred = inferBasedOn(data.get(nino))

    val doYouHaveCrnInferred = inferBasedOn(data.get(crnMapping attach crn))

    val doYouHaveUtrInferred = inferBasedOn(data.get(utr))

    data. +((doYouHaveNino, doYouhaveNinoInferred), (doYouHaveCrn, doYouHaveCrnInferred), (doYouHaveUtr, doYouHaveUtrInferred))
  }

  /*
  *  1 filter out vrn since its do you have answer is not inferred
  *  2 map ids to if they are answered with yes
  *  3 combine the answers to get the boolean for if any id is answered with yes
  *  4 invert the aforementioned boolean
  *  5 check if vrn is answered
  */
  @inline def noIdsHaveBeenSupplied(ids: Seq[String])(associatedQuestion: String)(data: FormData): Boolean = {
    ids.contains(associatedQuestion) match {
      case false => false // validate iff the associatedQuestion is in the id list
      case true =>
        val yesToDoYouHaveVRN = answeredYesToDoYouHaveVRN(data)
        val vrnIsNotAnswered = data.getOrElse(vrn, "").equals("")
        !ids.filter(!_.equals(doYouHaveVrn)).map(id => whenAnswerToFieldIs(id, BooleanRadioEnum.YesString)(data)).reduce(_ || _) &&
          (!yesToDoYouHaveVRN || yesToDoYouHaveVRN && vrnIsNotAnswered) // if doYouhaveVRN is not answered or if it is answered with yes but no VRN is supplied
    }
  }

  val businessRegistrationDetailsValidationForm: String => Form[BusinessRegistrationDetails] = (entityType: String) => {
    val ids = entityIds(entityType)
    val noIdIsSupplied = (associatedQuestion: String) => noIdsHaveBeenSupplied(ids)(associatedQuestion)(_)
    Form(
      mapping(
        "legalEntity" -> ignored(Option(entityType)),
        doYouHaveUtr -> (doYouHaveUTR_compulsory(doYouHaveUtr) iff ids.contains(doYouHaveUtr)),
        utr -> (utr_compulsory(utr, entityType) iff noIdIsSupplied(doYouHaveUtr) ||| answeredYesToDoYouHaveUTR),
        doYouHaveNino -> (doYouHaveNino_compulsory(doYouHaveNino) iff ids.contains(doYouHaveNino)),
        nino -> (nino_compulsory(nino) iff noIdIsSupplied(doYouHaveNino) ||| answeredYesToDoYouHaveNino),
        doYouHaveCrn -> (doYouHaveCRN_compulsory iff ids.contains(doYouHaveCrn)),
        crnMapping -> (companyReg_compulsory(crnMapping).toOptionalCompanyRegMapping iff noIdIsSupplied(doYouHaveCrn) ||| answeredYesToDoYouHaveCRN),
        doYouHaveVrn -> (doYouHaveVRN_compulsory(doYouHaveVrn) iff ids.contains(doYouHaveVrn)),
        vrn -> (vrn_compulsory(vrn) iff answeredYesToDoYouHaveVRN ||| answeredYesToDoYouHaveVRN)
      )(BusinessRegistrationDetails.apply)(BusinessRegistrationDetails.unapply))
  }

  val businessRegistrationDetailsForm: String => PrevalidationAPI[BusinessRegistrationDetails] = (entityType: String) => PreprocessedForm(businessRegistrationDetailsValidationForm(entityType)).addNewPreprocessFunction(inferDoYouHave)
}
