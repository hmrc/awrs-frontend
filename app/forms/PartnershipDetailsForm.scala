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

import forms.AWRSEnums.{BooleanRadioEnum, EntityTypeEnum}
import forms.prevalidation._
import forms.submapping.CompanyNamesMapping._
import forms.submapping.FieldNameUtil
import forms.validation.util.ConstraintUtil._
import forms.validation.util.ErrorMessagesUtilAPI._
import forms.validation.util.MappingUtilAPI._
import forms.validation.util._
import models.{CompanyNames, Partner}
import play.api.data.{Form, Mapping}
import play.api.data.Forms._

import scala.util.{Failure, Success, Try}

object PartnershipDetailsForm {

  import EntityTypeEnum._
  import forms.validation.util.NamedMappingAndUtil._
  import submapping.AddressMapping._
  import submapping.CompanyRegMapping._

  val partnerTypes = Set(Individual, CorporateBody, SoleTrader)

  val doYouHaveNino = "doYouHaveNino"
  val doYouHaveVrn = "doYouHaveVRN"
  val doYouHaveCrn = "isBusinessIncorporated"
  val doYouHaveUtr = "doYouHaveUTR"
  val nino = "nino"
  val utr = "utr"
  val vrn = "vrn"
  val crnMapping = "companyRegDetails"
  val companyNames = "companyNames"


  @inline def answeredYesToDoYouHaveCRN = whenAnswerToFieldIs(doYouHaveCrn, BooleanRadioEnum.YesString)(_)

  @inline def answeredYesToDoYouHaveNino = whenAnswerToFieldIs(doYouHaveNino, BooleanRadioEnum.YesString)(_)

  @inline def answeredYesToDoYouHaveVRN = whenAnswerToFieldIs(doYouHaveVrn, BooleanRadioEnum.YesString)(_)

  @inline def answeredYesToDoYouHaveUTR = whenAnswerToFieldIs(doYouHaveUtr, BooleanRadioEnum.YesString)(_)

  private val getPartner = (data: FormData) =>
    data.getOrElse("entityType", "")

  private val whenPartnerTypeIsAnswered = (data: FormData) =>
    !"".equals(getPartner(data))

  // cross field validation condition for when the partner is a corporate body
  private val whenPartnerIsCorporateBody = (data: FormData) =>
    CorporateBody.toString.equals(getPartner(data))

  private val getIds = (data: FormData) =>
    Try(AWRSEnums.EntityTypeEnum.withName(getPartner(data)) match {
      case Individual => Seq[String](doYouHaveNino)
      case CorporateBody => Seq[String](doYouHaveUtr, doYouHaveCrn, doYouHaveVrn)
      case SoleTrader => Seq[String](doYouHaveUtr, doYouHaveNino, doYouHaveVrn)
      case _ => Seq[String]()
    }) match {
      case Success(result) => result
      case Failure(ex) => Seq[String]()
    }

  // cross field validation condition for when the partner is not a corporate body
  private val whenPartnerTypeIsAnsweredAndNotCorporateBody =
  whenPartnerTypeIsAnswered &&& !whenPartnerIsCorporateBody

  private val whenPartnerIsIndividual = (data: FormData) =>
    Individual.toString.equals(getPartner(data))

  private val whenPartnerTypeIsAnsweredButNotIndividual =
    whenPartnerTypeIsAnswered &&& !whenPartnerIsIndividual

  // cross field validation condition for when the partner is a sole trader
  private val whenPartnerIsSoleTrader = (data: FormData) =>
    SoleTrader.toString.equals(getPartner(data))


  private val otherPartners_compulsory = yesNoQuestion_compulsory("otherPartners", "awrs.business-partner.error.add_more_partner")

  private val partnerEntityType_compulsory = {
    val question = CompulsoryEnumMappingParameter(
      simpleFieldIsEmptyConstraintParameter("entityType", "awrs.business-partner.error.entityType_empty"),
      EntityTypeEnum
    )
    compulsoryEnum(question)
  }

  private val inferBasedOn = (dependentField: Option[String]) => dependentField.map(x => x.trim) match {
    case None | Some("") => BooleanRadioEnum.NoString
    case _ => BooleanRadioEnum.YesString
  }

  private val inferDoYouHave = (data: Map[String, String]) => {

    val doYouHaveCrnInferred = inferBasedOn(data.get(crnMapping attach crn))

    val doYouHaveUtrInferred = inferBasedOn(data.get(utr))

    data.+((doYouHaveCrn, doYouHaveCrnInferred), (doYouHaveUtr, doYouHaveUtrInferred))
  }

  /*
  *  1 filter out vrn and nino since the do you have answer is not inferred
  *  2 map ids to if they are answered with yes
  *  3 combine the answers to get the boolean for if any id is answered with yes
  *  4 invert the aforementioned boolean
  *  5 check if vrn and nino is answered
  */
  @inline def noIdsHaveBeenSupplied(ids: FormData => Seq[String])(associatedQuestion: String)(data: FormData): Boolean = {
    ids(data).contains(associatedQuestion) match {
      case false => false // validate iff the associatedQuestion is in the id list
      case true =>
        val yesToDoYouHaveVRN = answeredYesToDoYouHaveVRN(data)
        val yesToDoYouHaveNino = answeredYesToDoYouHaveNino(data)
        val vrnIsNotAnswered = data.getOrElse(vrn, "").equals("")
        val ninoIsNotAnswered = data.getOrElse(nino, "").equals("")
        val vrnIncomplete = !yesToDoYouHaveVRN || yesToDoYouHaveVRN && vrnIsNotAnswered // if doYouhaveVRN is not answered or if it is answered with yes but no VRN is supplied
      val ninoIncomplete = !yesToDoYouHaveNino || yesToDoYouHaveNino && ninoIsNotAnswered // if doYouhaveNino is not answered or if it is answered with yes but no Nino is supplied

        !ids(data).filter(!_.equals(doYouHaveVrn)).filter(!_.equals(doYouHaveNino)).map(id => whenAnswerToFieldIs(id, BooleanRadioEnum.YesString)(data)).reduce(_ || _) &&
          (vrnIncomplete && ninoIncomplete) // if either vrn and nino are incomplete
    }
  }

  @inline def idExistsForQuestion(ids: FormData => Seq[String])(associatedQuestion: String)(data: FormData): Boolean = ids(data).contains(associatedQuestion)

  val mustHaveVRNorCRNorUTR = mustHaveAtLeastOneId(TargetFieldIds("doYouHaveVRN", "isBusinessIncorporated", "doYouHaveUTR"), "awrs.generic.error.identification_provided")
  val mustHaveNinoOrVRNorUTR = mustHaveAtLeastOneId(TargetFieldIds("doYouHaveNino", "doYouHaveVRN", "doYouHaveUTR"), "awrs.generic.error.identification_provided")

  val whenAnsweredYesToCRN =
    (data: FormData) => whenAnswerToIdTypeIs("isBusinessIncorporated", BooleanRadioEnum.Yes)(data)

  // this mapping only run validations if partnerType is already answered
  // and this mapping will only validate the business name filed if the partner type is not a sole trader
  val companyNamesMapping_rules: Mapping[Option[CompanyNames]] =
    companyNamesMapping(companyNames, validateBusinessName = whenPartnerIsCorporateBody).toOptional iff whenPartnerTypeIsAnsweredButNotIndividual

  val partnershipDetailsValidationForm = {
    val noIdIsSupplied = (associatedQuestion: String) => noIdsHaveBeenSupplied(getIds)(associatedQuestion)(_)
    val idExists = (associatedQuestion: String) => idExistsForQuestion(getIds)(associatedQuestion)(_)
    Form(
      mapping(
        "entityType" -> partnerEntityType_compulsory,
        "firstName" -> (firstName_compulsory() iff whenPartnerTypeIsAnsweredAndNotCorporateBody),
        "lastName" -> (lastName_compulsory() iff whenPartnerTypeIsAnsweredAndNotCorporateBody),
        //        "companyName" -> ((companyName_optional("company name") + companyNameAndTradingNameCannotBothBeEmpty) iff whenPartnerIsCorporateBody),
        //        "tradingName" -> (tradingName_optional iff whenPartnerTypeIsAnsweredButNotIndividual),
        companyNames -> companyNamesMapping_rules,
        "partnerAddress" -> (ukAddress_compulsory(prefix = "partnerAddress").toOptionalAddressMapping iff whenPartnerTypeIsAnswered),
        doYouHaveNino -> ((doYouHaveNino_compulsory() iff whenPartnerTypeIsAnsweredAndNotCorporateBody) + (mustHaveNinoOrVRNorUTR xiff whenPartnerIsSoleTrader)),
        nino -> (nino_compulsory(nino) iff (answeredYesToNino(doYouHaveNinoFieldId = doYouHaveNino) &&& whenPartnerTypeIsAnsweredAndNotCorporateBody)),
        doYouHaveUtr -> (doYouHaveUTR_compulsory(doYouHaveUtr) iff idExists(doYouHaveUtr)),
        utr -> (utr_compulsory(utr) iff noIdIsSupplied(doYouHaveUtr) ||| answeredYesToDoYouHaveUTR),
        doYouHaveCrn -> (doYouHaveCRN_compulsory(doYouHaveCrn) iff idExists(doYouHaveCrn)),
        crnMapping -> (companyReg_compulsory(crnMapping).toOptionalCompanyRegMapping iff noIdIsSupplied(doYouHaveCrn) ||| answeredYesToDoYouHaveCRN),
        doYouHaveVrn -> ((doYouHaveVRN_compulsory() iff whenPartnerTypeIsAnsweredButNotIndividual) + (mustHaveVRNorCRNorUTR xiff whenPartnerIsCorporateBody)),
        vrn -> (vrn_compulsory() iff (whenAnsweredYesToVRN &&& whenPartnerTypeIsAnsweredButNotIndividual)),
        "otherPartners" -> otherPartners_compulsory
      )(Partner.apply)(Partner.unapply))
  }

  val partnershipDetailsForm = PreprocessedForm(partnershipDetailsValidationForm).addNewPreprocessFunction(inferDoYouHave)

}
