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

import forms.AWRSEnums.DeRegistrationReasonEnum
import forms.helper.FormHelper._
import forms.submapping.TupleDateMapping._
import forms.validation.util.ConstraintUtil.{CompulsoryEnumMappingParameter, CompulsoryTextFieldMappingParameter, FormData}
import forms.validation.util.ErrorMessagesUtilAPI._
import forms.validation.util.MappingUtilAPI._
import forms.validation.util.NamedMappingAndUtil._
import models.{DeRegistrationConfirmation, DeRegistrationDate, DeRegistrationReason, TupleDate}
import org.joda.time.LocalDate
import play.api.data.Forms._
import play.api.data.validation.Valid
import play.api.data.{FieldMapping, Form, Mapping}
import utils.AwrsFieldConfig
import utils.AwrsValidator._
import forms.prevalidation._

object DeRegistrationForm {

  private val isTooEarlyOrTooLate = (fieldKey: String) => (date: TupleDate) => {
    val today = LocalDate.now()
    val isTooLate = if (today.plusYears(100).isAfter(date.localDate)) {
      Valid
    } else {
      simpleErrorMessage(fieldKey, "awrs.de_registration.error.date_valid")
    }
    val isTooEarly = if (isDateAfterOrEqual(LocalDate.now().toString("dd/MM/yyyy"),
      new LocalDate(date.year.trim.toInt, date.month.trim.toInt, date.day.trim.toInt).toDate)) {
      isTooLate
    } else {
      simpleErrorMessage(fieldKey, "awrs.de_registration.error.proposedDate_toEarly")
    }
    isTooEarly
  }

  private def testDate_compulsory: Mapping[TupleDate] =
    tupleDate_compulsory(
      isEmptyErrMessage = simpleErrorMessage(_, "awrs.de_registration.error.date_empty"),
      isInvalidErrMessage = simpleErrorMessage(_, "awrs.de_registration.error.date_valid"),
      dateRangeCheck = Some(isTooEarlyOrTooLate(_)))

  val deRegistrationForm = Form(mapping(
    "proposedEndDate" -> testDate_compulsory
  )(DeRegistrationDate.apply)(DeRegistrationDate.unapply))

}

object DeRegistrationReasonForm extends AwrsFieldConfig {

  val deRegistrationReasonId = "deRegistrationReason"
  val deRegReasonOtherId = "deRegistrationReason-other"

  val deRegistrationReason_compulsory: FieldMapping[Option[String]] = {
    val params = CompulsoryEnumMappingParameter(
      empty = simpleFieldIsEmptyConstraintParameter(deRegistrationReasonId, "awrs.de_registration.error.reason_empty"),
      enumType = DeRegistrationReasonEnum
    )
    compulsoryEnum(params)
  }

  val otherReason_compulsory: FieldMapping[Option[String]] = {
    val fieldId = deRegReasonOtherId
    val fieldNameInErrorMessage = "other reasons"
    val params = CompulsoryTextFieldMappingParameter(
      empty = simpleFieldIsEmptyConstraintParameter(fieldId, "awrs.de_registration.error.other.reason_empty"),
      maxLengthValidation = genericFieldMaxLengthConstraintParameter(deRegistrationOtherReasonsLen, fieldId, fieldNameInErrorMessage),
      formatValidations = genericInvalidFormatConstraintParameter(validText, fieldId, fieldNameInErrorMessage)
    )
    compulsoryText(params)
  }

  val whenOtherReasonIsSelected: FormData => Boolean = (data: FormData) => data.getOrElse(deRegistrationReasonId, "").equals(DeRegistrationReasonEnum.Other.toString)

  val deRegistrationReasonValidationForm = Form(mapping(
    deRegistrationReasonId -> deRegistrationReason_compulsory,
    deRegReasonOtherId -> (otherReason_compulsory iff whenOtherReasonIsSelected)
  )(DeRegistrationReason.apply)(DeRegistrationReason.unapply))

  val deRegistrationReasonForm: PrevalidationAPI[DeRegistrationReason] = PreprocessedForm(deRegistrationReasonValidationForm)

}

object DeRegistrationConfirmationForm {

  val deRegistrationConfirmationForm = Form(mapping(
    "deRegistrationConfirmation" -> yesNoQuestion_compulsory(fieldId = "deRegistrationConfirmation", errorMessageId = "awrs.de_registration.confirmation.error.empty")
  )(DeRegistrationConfirmation.apply)(DeRegistrationConfirmation.unapply))

}
