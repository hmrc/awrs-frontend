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

package forms

import forms.AWRSEnums.WithdrawalReasonEnum
import forms.prevalidation._
import forms.validation.util.ConstraintUtil._
import forms.validation.util.ErrorMessagesUtilAPI._
import forms.validation.util.MappingUtilAPI._
import models.WithdrawalReason
import play.api.data.Forms._
import play.api.data.validation.{Invalid, Valid}
import play.api.data.{FieldMapping, Form}
import utils.AwrsFieldConfig
import utils.AwrsValidator._

object WithdrawalReasonForm extends AwrsFieldConfig {

  val withdrawalReason = "reason"
  val withdrawalReasonOther = "reasonOther"

  val withdrawalReasons_compulsory: FieldMapping[Option[String]] = {
    val params = CompulsoryEnumMappingParameter(
      empty = simpleFieldIsEmptyConstraintParameter("withdrawalReason", "awrs.withdrawal.error.reason_empty"),
      enumType = WithdrawalReasonEnum
    )
    compulsoryEnum(params)
  }

  val withdrawalReasonOther_compulsory: FieldMapping[Option[String]] = {
    val fieldNameInErrorMessage = "other reasons"
    val fieldId = "withdrawalReason-other"
    val params = CompulsoryTextFieldMappingParameter(
    empty = simpleFieldIsEmptyConstraintParameter(fieldId, "awrs.withdrawal.error.other.reason_empty"),
      FieldMaxLengthConstraintParameter(withdrawalOtherReasonsLen, Invalid("awrs.generic.error.maximum_length",fieldNameInErrorMessage, withdrawalOtherReasonsLen)),
      FieldFormatConstraintParameter((name: String) => if (validText(name)) Valid else Invalid("awrs.generic.error.character_invalid.summary", fieldNameInErrorMessage))
    )
    compulsoryText(params)
  }

  val whenOtherReasonIsSelected: FormData => Boolean = (data: FormData) => data.getOrElse(withdrawalReason, "").equals(WithdrawalReasonEnum.Other.toString)

  val withdrawalReasonValidationForm = Form(mapping(
    withdrawalReason -> withdrawalReasons_compulsory,
    withdrawalReasonOther -> (withdrawalReasonOther_compulsory iff whenOtherReasonIsSelected)
  )(WithdrawalReason.apply)(WithdrawalReason.unapply))

  val withdrawalReasonForm: PrevalidationAPI[WithdrawalReason] = PreprocessedForm(withdrawalReasonValidationForm)

}
