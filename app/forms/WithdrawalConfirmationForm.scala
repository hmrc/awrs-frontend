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
import forms.validation.util.ConstraintUtil.CompulsoryBooleanMappingParameter
import forms.validation.util.ErrorMessagesUtilAPI._
import forms.validation.util.NamedMappingAndUtil._
import models.WithdrawalConfirmation
import play.api.data.Form
import play.api.data.Forms._

object WithdrawalConfirmationForm {

  val fieldId= "confirmation"

  val confirmationConstraintParameters =
    CompulsoryBooleanMappingParameter(simpleFieldIsEmptyConstraintParameter(fieldId, "withdrawal-confirmation.key"), BooleanRadioEnum)

  val withdrawalConfirmation = Form(mapping(
    fieldId -> yesNoQuestion_compulsory(fieldId = fieldId, errorMessageId = "awrs.withdrawal.confirmation.error.empty")
    )(WithdrawalConfirmation.apply)(WithdrawalConfirmation.unapply))

}
