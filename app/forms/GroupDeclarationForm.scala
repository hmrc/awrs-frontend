/*
 * Copyright 2022 HM Revenue & Customs
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
import forms.validation.util.ConstraintUtil.CompulsoryBooleanMappingParameter
import forms.validation.util.ErrorMessagesUtilAPI._
import forms.validation.util.MappingUtilAPI._
import models.GroupDeclaration
import play.api.data.Form
import play.api.data.Forms._

object GroupDeclarationForm {

  val bToOb = (bool: Boolean) => Some(bool)
  val obTob = (bool: Option[Boolean]) => bool.fold(false)(x => x)

  val groupRepConfirmation_compulsory = compulsoryBoolean(CompulsoryBooleanMappingParameter(
    empty = simpleFieldIsEmptyConstraintParameter("groupRepConfirmation", "awrs.group_declaration.error.declaration_confirmation_not_checked"),
    enumType = BooleanCheckboxEnum,
    invalidChoices = Set(BooleanCheckboxEnum.False)
  )).transform(obTob, bToOb)

  val groupDeclarationForm = Form(mapping(
    "groupRepConfirmation" -> groupRepConfirmation_compulsory
  )(GroupDeclaration.apply)(GroupDeclaration.unapply))

}
