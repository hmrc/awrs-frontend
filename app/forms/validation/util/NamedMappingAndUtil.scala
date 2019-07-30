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

package forms.validation.util

import forms.AWRSEnums.BooleanRadioEnum
import forms.validation.util.ConstraintUtil.FieldFormatConstraintParameter._
import forms.validation.util.ConstraintUtil._
import forms.validation.util.ErrorMessagesUtilAPI._
import forms.validation.util.MappingUtilAPI._
import play.api.data.{Forms, Mapping}
import play.api.data.validation.Valid
import utils.AwrsValidator._
import play.api.data.Mapping
import utils.AwrsFieldConfig

object NamedMappingAndUtil extends AwrsFieldConfig {

  def firstName_compulsory(fieldId: String = "firstName"): Mapping[Option[String]] = {
    val fieldNameInErrorMessage = "first name"
    val firstNameConstraintParameters =
      CompulsoryTextFieldMappingParameter(
        simpleFieldIsEmptyConstraintParameter(fieldId, "awrs.generic.error.first_name_empty"),
        genericFieldMaxLengthConstraintParameterForDifferentMessages(firstNameLen, fieldId, "First name", errorMsg = "awrs.generic.error.name.maximum_length"),
        genericInvalidFormatConstraintParameter(validText, fieldId, fieldNameInErrorMessage)
      )
    compulsoryText(firstNameConstraintParameters)
  }

  def getEntityMessage(entityType: String, messageKey: String) = {

    entityType match{
      case "LTD" => messageKey +"_LTD"
      case _ => messageKey

    }

  }

  def lastName_compulsory(fieldId: String = "lastName"): Mapping[Option[String]] = {
    val fieldNameInErrorMessage = "last name"
    val lastNameConstraintParameters =
      CompulsoryTextFieldMappingParameter(
        simpleFieldIsEmptyConstraintParameter(fieldId, "awrs.generic.error.last_name_empty"),
        genericFieldMaxLengthConstraintParameterForDifferentMessages(lastNameLen, fieldId, "Last name", errorMsg = "awrs.generic.error.name.maximum_length"),
        genericInvalidFormatConstraintParameter(validText, fieldId, fieldNameInErrorMessage)
      )
    compulsoryText(lastNameConstraintParameters)
  }

  def companyName_optional(fieldNameInErrorMessage: String = "business name"): Mapping[Option[String]] = {
    val fieldId = "companyName"
    val companyNameConstraintParameters =
      OptionalTextFieldMappingParameter(
        genericFieldMaxLengthConstraintParameter(companyNameLen, fieldId, fieldNameInErrorMessage),
        genericInvalidFormatConstraintParameter(validText, fieldId, fieldNameInErrorMessage)
      )
    optionalText(companyNameConstraintParameters)
  }

  def companyName_compulsory(fieldNameInErrorMessage: String = "business name"): Mapping[Option[String]] = {
    val fieldId = "companyName"
    val companyNameConstraintParameters =
      CompulsoryTextFieldMappingParameter(
        simpleFieldIsEmptyConstraintParameter(fieldId, "awrs.generic.error.businessName_empty"),
        genericFieldMaxLengthConstraintParameter(companyNameLen, fieldId, fieldNameInErrorMessage),
        genericInvalidFormatConstraintParameter(validText, fieldId, fieldNameInErrorMessage)
      )
    compulsoryText(companyNameConstraintParameters)
  }

  def doYouHaveTradingName_compulsory: Mapping[Option[String]] =
    yesNoQuestion_compulsory("doYouHaveTradingName", "awrs.generic.error.do_you_have_trading_name_empty")

  val whenDoYouHaveTradingNameIsAnsweredYes: FormData => Boolean = answerGivenInFieldIs("doYouHaveTradingName", "Yes")

  def tradingName_compulsory: Mapping[Option[String]] = {
    val fieldId = "tradingName"
    val fieldNameInErrorMessage = "trading name"
    val companyNameConstraintParameters =
      CompulsoryTextFieldMappingParameter(
        simpleFieldIsEmptyConstraintParameter(fieldId, "awrs.generic.error.tradingName_empty"),
        genericFieldMaxLengthConstraintParameter(tradingNameLen, fieldId, fieldNameInErrorMessage),
        genericInvalidFormatConstraintParameter(validText, fieldId, fieldNameInErrorMessage)
      )
    compulsoryText(companyNameConstraintParameters)
  }

  def tradingName_optional: Mapping[Option[String]] = {
    val fieldId = "tradingName"
    val fieldNameInErrorMessage = "trading name"
    val companyNameConstraintParameters =
      OptionalTextFieldMappingParameter(
        genericFieldMaxLengthConstraintParameter(tradingNameLen, fieldId, fieldNameInErrorMessage),
        genericInvalidFormatConstraintParameter(validText, fieldId, fieldNameInErrorMessage)
      )
    optionalText(companyNameConstraintParameters)
  }

  val companyNameIsEmpty: FormData => Boolean = noAnswerGivenInField("companyName")
  val tradingNameIsEmpty: FormData => Boolean = noAnswerGivenInField("tradingName")

  val companyNameAndTradingNameCannotBothBeEmpty =
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
    data.filterKeys((key: String) => key.contains(listId)).values.exists(answer)

  @inline def whenAnswerToIdTypeIs(doYouHaveIdTypeFieldId: String, answer: BooleanRadioEnum.Value)(data: FormData): Boolean =
    whenAnswerToFieldIs(doYouHaveIdTypeFieldId, answer.toString)(data)

  @inline def answeredYesToNino(doYouHaveNinoFieldId: String): (FormData) => Boolean =
    whenAnswerToIdTypeIs(doYouHaveNinoFieldId, BooleanRadioEnum.Yes)

  @inline def noAnswerGivenInField(fieldId: String) = (data: FormData) => whenAnswerToFieldIs(fieldId, "")(data)

  @inline def answerGivenInFieldIs(fieldId: String, answer: String) = (data: FormData) => whenAnswerToFieldIs(fieldId, answer)(data)

  val whenAnsweredYesToVRN: FormData => Boolean = (data: FormData) =>
    whenAnswerToIdTypeIs("doYouHaveVRN", BooleanRadioEnum.Yes)(data)

  val whenAnsweredYesToCRN: FormData => Boolean = (data: FormData) =>
    whenAnswerToIdTypeIs("doYouHaveCRN", BooleanRadioEnum.Yes)(data)

  val whenAnsweredYesToUTR: FormData => Boolean = (data: FormData) =>
    whenAnswerToIdTypeIs("doYouHaveUTR", BooleanRadioEnum.Yes)(data)

  @inline def doYouHaveNino_compulsory(fieldId: String = "doYouHaveNino") = yesNoQuestion_compulsory(fieldId, "awrs.generic.error.do_you_have_nino_empty")

  @inline def doYouHaveVRN_compulsory(fieldId: String = "doYouHaveVRN") = yesNoQuestion_compulsory(fieldId, "awrs.generic.error.do_you_have_vat_reg_empty")

  // TODO refactor
  @inline def doYouHaveCRN_compulsory(fieldId: String = "doYouHaveCRN") = yesNoQuestion_compulsory(fieldId, "awrs.generic.error.do_you_have_company_reg_empty")

  @inline def doYouHaveUTR_compulsory(fieldId: String = "doYouHaveUTR") = yesNoQuestion_compulsory(fieldId, "awrs.generic.error.do_you_have_utr_empty")

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
        genericFieldMaxLengthConstraintParameter(emailLen, fieldId, fieldNameInErrorMessage),
        genericInvalidFormatConstraintParameter(x => x.matches(emailRegex), fieldId, fieldNameInErrorMessage, "awrs.generic.error.email_invalid")
      )
    compulsoryText(firstNameConstraintParameters)
  }

  def confirmEmail_compulsory(fieldId: String = "confirmEmail"): Mapping[Option[String]] = {
    val fieldNameInErrorMessage = "confirm email"
    val firstNameConstraintParameters =
      CompulsoryTextFieldMappingParameter(
        simpleFieldIsEmptyConstraintParameter(fieldId, "awrs.generic.error.confirm_email_empty"),
        genericFieldMaxLengthConstraintParameter(emailLen, fieldId, fieldNameInErrorMessage),
        genericInvalidFormatConstraintParameter(x => x.matches(emailRegex), fieldId, fieldNameInErrorMessage, "awrs.generic.error.confirm_email_invalid")
      )
    compulsoryText(firstNameConstraintParameters)
  }


  def telephone_compulsory(fieldId: String = "telephone"): Mapping[Option[String]] =
    compulsoryText(
      CompulsoryTextFieldMappingParameter(
        simpleFieldIsEmptyConstraintParameter(fieldId, "awrs.generic.error.telephone_empty"),
        genericFieldMaxLengthConstraintParameter(telephoneLen, fieldId, "telephone"),
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
