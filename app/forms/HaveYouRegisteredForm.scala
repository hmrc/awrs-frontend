/*
 * Copyright 2025 HM Revenue & Customs
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

import forms.AWRSEnums.{BooleanCheckboxEnum, BooleanRadioEnum}
import forms.validation.util.MappingUtilAPI.compulsoryBoolean
import forms.validation.util.ConstraintUtil.CompulsoryBooleanMappingParameter
import forms.validation.util.ErrorMessagesUtilAPI.simpleFieldIsEmptyConstraintParameter
import play.api.data.Form
import models.HaveYouRegisteredModel
import play.api.data.Forms.mapping


object HaveYouRegisteredForm {

  val bToOb = (bool: Boolean) => Some(bool)
  val obTob = (bool: Option[Boolean]) => bool.fold(false)(x => x)

  val haveYouRegisteredFormCompulsoryBooleanMappingParam = compulsoryBoolean(CompulsoryBooleanMappingParameter(
    empty = simpleFieldIsEmptyConstraintParameter("hasUserRegistered", "awrs.enrolment.have_you_registered.error"),
    enumType = BooleanRadioEnum,
    invalidChoices = Set(BooleanCheckboxEnum.False)
  )).transform(obTob, bToOb)

  val haveYouRegisteredForm: Form[HaveYouRegisteredModel] =
    Form(mapping(
      "hasUserRegistered" -> haveYouRegisteredFormCompulsoryBooleanMappingParam
    )(HaveYouRegisteredModel.apply)(HaveYouRegisteredModel.unapply))
}
