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

import forms.AWRSEnums.BooleanCheckboxEnum
import forms.prevalidation.{PreprocessedForm, PrevalidationAPI, TrimOption}
import forms.validation.util.ConstraintUtil.CompulsoryBooleanMappingParameter
import forms.validation.util.ErrorMessagesUtilAPI.simpleFieldIsEmptyConstraintParameter
import forms.validation.util.MappingUtilAPI.compulsoryBoolean
import models.HaveYouRegisteredModel
import play.api.data.Form
import play.api.data.Forms.mapping

object HaveYouRegisteredForm {

  val hasUserRegistered = "hasUserRegistered"

  val haveYouRegisteredFormCompulsoryBooleanMappingParam= compulsoryBoolean(CompulsoryBooleanMappingParameter(
    empty = simpleFieldIsEmptyConstraintParameter(hasUserRegistered, "awrs.enrolment.have_you_registered.error"),
    enumType = BooleanCheckboxEnum
  ))

  val haveYouRegisteredFormValidation: Form[HaveYouRegisteredModel] =
    Form(mapping(
      "hasUserRegistered" -> haveYouRegisteredFormCompulsoryBooleanMappingParam
    )(HaveYouRegisteredModel.apply)(HaveYouRegisteredModel.unapply))

  lazy val haveYouRegisteredForm: PrevalidationAPI[HaveYouRegisteredModel] = PreprocessedForm(
    haveYouRegisteredFormValidation,
    trimRules = Map(hasUserRegistered -> TrimOption.bothAndCompress),
    caseRules = Map())
}
