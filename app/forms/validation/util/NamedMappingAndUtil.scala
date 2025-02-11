/*
 * Copyright 2023 HM Revenue & Customs
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

package forms.validation.util

import forms.AWRSEnums.BooleanRadioEnum
import forms.validation.util.ConstraintUtil.FieldFormatConstraintParameter._
import forms.validation.util.ConstraintUtil._
import forms.validation.util.ErrorMessagesUtilAPI._
import forms.validation.util.MappingUtilAPI._
import play.api.data.validation.{Invalid, Valid}
import play.api.data.{Forms, Mapping}
import utils.AwrsFieldConfig
import utils.AwrsValidator._

object NamedMappingAndUtil extends AwrsFieldConfig {

  def firstName_compulsory(fieldId: String = "firstName"): Mapping[Option[String]] = {
    val fieldNameInErrorMessage = "first name"
    val firstNameConstraintParameters =
      CompulsoryTextFieldMappingParameter(
        simpleFieldIsEmptyConstraintParameter(fieldId, "awrs.generic.error.first_name_empty"),
        FieldMaxLengthConstraintParameter(firstNameLen, Invalid("awrs.generic.error.name.maximum_length", "First name", firstNameLen)),
        FieldFormatConstraintParameter((name: String) => if (validText(name)) Valid else Invalid("awrs.generic.error.character_invalid.summary", fieldNameInErrorMessage))
      )
    compulsoryText(firstNameConstraintParameters)
  }

  def getEntityMessage(entityType: String, messageKey: String): String = {

    entityType match{
      case "LTD" => messageKey + "_LTD"
      case _ => messageKey

    }

  }

  def lastName_compulsory(fieldId: String = "lastName"): Mapping[Option[String]] = {
    val fieldNameInErrorMessage = "last name"
    val lastNameConstraintParameters =
      CompulsoryTextFieldMappingParameter(
        simpleFieldIsEmptyConstraintParameter(fieldId, "awrs.generic.error.last_name_empty"),
        FieldMaxLengthConstraintParameter(lastNameLen, Invalid("awrs.generic.error.name.maximum_length", "Last name", lastNameLen)),
        FieldFormatConstraintParameter((name: String) => if (validText(name)) Valid else Invalid("awrs.generic.error.character_invalid.summary", fieldNameInErrorMessage))
      )
    compulsoryText(lastNameConstraintParameters)
  }

  def companyName_compulsory(fieldNameInErrorMessage: String = "business name"): Mapping[Option[String]] = {
    val fieldId = "companyName"
    val companyNameConstraintParameters =
      CompulsoryTextFieldMappingParameter(
        simpleFieldIsEmptyConstraintParameter(fieldId, "awrs.generic.error.businessName_empty"),
        FieldMaxLengthConstraintParameter(companyNameLen, Invalid("awrs.generic.error.maximum_length", fieldNameInErrorMessage, companyNameLen)),
        FieldFormatConstraintParameter((name: String) => if (validText(name)) Valid else Invalid("awrs.generic.error.character_invalid.summary", fieldNameInErrorMessage))
      )
    compulsoryText(companyNameConstraintParameters)
  }

  def doYouHaveTradingName_compulsory(): Mapping[Option[String]] =
    yesNoQuestion_compulsory("doYouHaveTradingName", "awrs.generic.error.do_you_have_trading_name_empty")

  val whenDoYouHaveTradingNameIsAnsweredYes: FormData => Boolean = answerGivenInFieldIs("doYouHaveTradingName", "Yes")

  def tradingName_compulsory: Mapping[Option[String]] = {
    val fieldId = "tradingName"
    val fieldNameInErrorMessage = "trading name"
    val companyNameConstraintParameters =
      CompulsoryTextFieldMappingParameter(
        simpleFieldIsEmptyConstraintParameter(fieldId, "awrs.generic.error.tradingName_empty"),
        FieldMaxLengthConstraintParameter(tradingNameLen, Invalid("awrs.generic.error.maximum_length", fieldNameInErrorMessage, tradingNameLen)),
        FieldFormatConstraintParameter((name: String) => if (validText(name)) Valid else Invalid("awrs.generic.error.character_invalid.summary", fieldNameInErrorMessage))

      )
    compulsoryText(companyNameConstraintParameters)
  }

  val companyNameIsEmpty: FormData => Boolean = noAnswerGivenInField("companyName")
  val tradingNameIsEmpty: FormData => Boolean = noAnswerGivenInField("tradingName")

  val companyNameAndTradingNameCannotBothBeEmpty: CrossFieldConstraint =
    CrossFieldConstraint(
      companyNameIsEmpty &&& tradingNameIsEmpty,
      simpleCrossFieldErrorMessage(TargetFieldIds("companyName", "tradingName"),
        "awrs.generic.error.company_trading_name"))

  val mustHaveVRNorCRNorUTR: CrossFieldConstraint = mustHaveAtLeastOneId(TargetFieldIds("doYouHaveVRN", "doYouHaveCRN", "doYouHaveUTR"), "awrs.business_directors.error.do_you_have_id")

  /*
  * targetFieldIdRedirectOverrides is used to transform targetFieldIds in the error message to target a different field instead
  * this is introduced to direct the cross validation error messages to another field other than the question itself
  */
  def mustHaveAtLeastOneId(targetFieldIds: TargetFieldIds, errorMessageId: String, targetFieldIdRedirectOverrides: Option[Map[String, String]] = None): CrossFieldConstraint = {
    val anchor = targetFieldIds.anchor
    val ids = targetFieldIds.otherIds
    val condition =
      ids.map(id => answerGivenInFieldIs(id, BooleanRadioEnum.No.toString))
        .foldLeft(answerGivenInFieldIs(anchor, BooleanRadioEnum.No.toString))(_ &&& _)
    val targetFields = targetFieldIdRedirectOverrides match {
      case Some(overrideConfig) =>
        val applyOverride = (fieldId: String) => overrideConfig.getOrElse(fieldId, fieldId): String
        TargetFieldIds(applyOverride(anchor), ids.map(x => applyOverride(x)): _*)
      case _ => targetFieldIds
    }
    CrossFieldConstraint(
      condition,
      simpleCrossFieldErrorMessage(targetFields,
        errorMessageId))
  }

  val compulsoryStringList: (String, String) => Mapping[List[String]] = (fieldId: String, emptyErrorMsgId: String) =>
    compulsoryList(CompulsoryListMappingParameter[String](Forms.text, simpleErrorMessage(fieldId, emptyErrorMsgId)))

  @inline def whenAnswerToFieldIs(fieldId: String, answer: String)(data: FormData): Boolean =
    data.getOrElse(fieldId, "").equals(answer)

  @inline def whenListContainsAnswer(listId: String, answer: String): FormData => Boolean = (data: FormData) =>
    data.view.filterKeys((key: String) => key.contains(listId)).values.exists(answer)

  @inline def whenAnswerToIdTypeIs(doYouHaveIdTypeFieldId: String, answer: BooleanRadioEnum.Value)(data: FormData): Boolean =
    whenAnswerToFieldIs(doYouHaveIdTypeFieldId, answer.toString)(data)

  @inline def answeredYesToNino(doYouHaveNinoFieldId: String): FormData => Boolean =
    whenAnswerToIdTypeIs(doYouHaveNinoFieldId, BooleanRadioEnum.Yes)

  @inline def noAnswerGivenInField(fieldId: String): FormData => Boolean = (data: FormData) => whenAnswerToFieldIs(fieldId, "")(data)

  @inline def answerGivenInFieldIs(fieldId: String, answer: String): FormData => Boolean = (data: FormData) => whenAnswerToFieldIs(fieldId, answer)(data)

  val whenAnsweredYesToVRN: FormData => Boolean = (data: FormData) =>
    whenAnswerToIdTypeIs("doYouHaveVRN", BooleanRadioEnum.Yes)(data)

  val whenAnsweredYesToCRN: FormData => Boolean = (data: FormData) =>
    whenAnswerToIdTypeIs("doYouHaveCRN", BooleanRadioEnum.Yes)(data)

  val whenAnsweredYesToUTR: FormData => Boolean = (data: FormData) =>
    whenAnswerToIdTypeIs("doYouHaveUTR", BooleanRadioEnum.Yes)(data)

  @inline def doYouHaveNino_compulsory(fieldId: String = "doYouHaveNino"): Mapping[Option[String]] = yesNoQuestion_compulsory(fieldId, "awrs.generic.error.do_you_have_nino_empty")

  @inline def doYouHaveVRN_compulsory(fieldId: String = "doYouHaveVRN"): Mapping[Option[String]] = yesNoQuestion_compulsory(fieldId, "awrs.generic.error.do_you_have_vat_reg_empty")

  @inline def doYouHaveCRN_compulsory(fieldId: String = "doYouHaveCRN"): Mapping[Option[String]] = yesNoQuestion_compulsory(fieldId, "awrs.generic.error.do_you_have_company_reg_empty")

  @inline def doYouHaveUTR_compulsory(fieldId: String = "doYouHaveUTR"): Mapping[Option[String]] = yesNoQuestion_compulsory(fieldId, "awrs.generic.error.do_you_have_utr_empty")

  @inline def yesNoQuestion_compulsory(fieldId: String,
                                       errorMessageId: String): Mapping[Option[String]] = {
    val question = CompulsoryEnumMappingParameter(
      simpleFieldIsEmptyConstraintParameter(fieldId, errorMessageId),
      BooleanRadioEnum
    )
    compulsoryEnum(question)
  }

  def commonIdConstraints(fieldId: String,
                          isEmptyErrMessage: String,
                          regex: String,
                          isInvalidErrMessage: String): Mapping[Option[String]] =
    compulsoryText(
      CompulsoryTextFieldMappingParameter(
        simpleFieldIsEmptyConstraintParameter(fieldId, isEmptyErrMessage),
        MaxLengthConstraintIsHandledByTheRegEx(),
        FieldFormatConstraintParameter(
          (str: String) =>
            if (str.matches(regex)) {
              Valid
            } else {
              simpleErrorMessage(fieldId, isInvalidErrMessage)
            }
        )
      )
    )


  def nino_compulsory(fieldId: String = "NINO"): Mapping[Option[String]] =
    commonIdConstraints(
      fieldId,
      isEmptyErrMessage = "awrs.generic.error.nino_empty",
      regex = ninoRegex,
      isInvalidErrMessage = "awrs.generic.error.nino_invalid"
    )


  def vrn_compulsory(fieldId: String = "vrn"): Mapping[Option[String]] =
    commonIdConstraints(
      fieldId,
      isEmptyErrMessage = "awrs.generic.error.vrn_empty",
      regex = vatRegex,
      isInvalidErrMessage = "awrs.generic.error.vrn_invalid"
    )


  def utr_compulsory(fieldId: String = "utr", businessType :String = "None"): Mapping[Option[String]] ={
    commonIdConstraints(
      fieldId,
      isEmptyErrMessage = getEntityMessage(businessType,"awrs.generic.error.utr_empty"),
      regex = utrRegex,
      isInvalidErrMessage =  getEntityMessage(businessType,"awrs.generic.error.utr_invalid")
    )
  }

  def email_compulsory(fieldId: String = "email"): Mapping[Option[String]] = {
    val fieldNameInErrorMessage = "email"

    val firstNameConstraintParameters =
      CompulsoryTextFieldMappingParameter(
        simpleFieldIsEmptyConstraintParameter(fieldId, "awrs.generic.error.email_empty"),
        FieldMaxLengthConstraintParameter(emailLen, Invalid("awrs.generic.error.name.maximum_length", fieldNameInErrorMessage, emailLen)),
        FieldFormatConstraintParameter((name: String) => if (name.matches(emailRegex)) Valid else Invalid("awrs.generic.error.email_invalid", fieldNameInErrorMessage))

      )
    compulsoryText(firstNameConstraintParameters)
  }

  def telephone_compulsory(fieldId: String = "telephone"): Mapping[Option[String]] =
    compulsoryText(
      CompulsoryTextFieldMappingParameter(
        simpleFieldIsEmptyConstraintParameter(fieldId, "awrs.generic.error.telephone_empty"),
        FieldMaxLengthConstraintParameter(telephoneLen, Invalid("awrs.generic.error.name.maximum_length", "Telephone", telephoneLen)),
        FieldFormatConstraintParameter(
          (str: String) =>
            if (str.matches(telephoneRegex)) {
              Valid
            } else {
              simpleErrorMessage(fieldId, "awrs.generic.error.telephone_numeric")
            }
        )
      )
    )

  def crn_compulsory: Mapping[Option[String]] = crn_compulsory("companyRegNumber")

  def crn_compulsory(fieldId: String): Mapping[Option[String]] =
    compulsoryText(
      CompulsoryTextFieldMappingParameter(
        simpleFieldIsEmptyConstraintParameter(fieldId, "awrs.generic.error.companyRegNumber_empty"),
        MaxLengthConstraintIsHandledByTheRegEx(),
        Seq(
          FieldFormatConstraintParameter(
            (str: String) =>
              str.replace(" ", "").matches(crnRegex) match {
                case true => Valid
                case false if """[1-9]""".r.findFirstIn(str).isEmpty && str.length == 8
                => simpleErrorMessage(fieldId, "awrs.generic.error.companyRegNumber_atleastOneNumber")
                case false => simpleErrorMessage(fieldId, "awrs.generic.error.companyRegNumber_invalid")
              }
          )
        )
      )
    )


}
