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

import forms.AWRSEnums.BooleanCheckboxEnum
import forms.prevalidation._
import forms.validation.util.ConstraintUtil.{CompulsoryBooleanMappingParameter, CompulsoryTextFieldMappingParameter, FieldFormatConstraintParameter, FieldMaxLengthConstraintParameter, castSingleToSet}
import forms.validation.util.ErrorMessagesUtilAPI._
import forms.validation.util.MappingUtilAPI._
import models.ApplicationDeclaration
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Invalid, Valid}
import utils.AwrsFieldConfig
import utils.AwrsValidator._

object ApplicationDeclarationForm extends AwrsFieldConfig {
  val declarationNameConstraintParameters =
    CompulsoryTextFieldMappingParameter(
      simpleFieldIsEmptyConstraintParameter(fieldId = "declarationName", "awrs.application_declaration.error.declaration_name_empty"),
      FieldMaxLengthConstraintParameter(applicationDeclarationNameLen, Invalid("awrs.generic.error.name.maximum_length", "Your name", applicationDeclarationNameLen)),
      FieldFormatConstraintParameter((name: String) => if (validText(name)) Valid else Invalid("awrs.generic.error.character_invalid.summary_declaration", "Your name"))
    )

  val declarationRoleConstraintParameters =
    CompulsoryTextFieldMappingParameter(
      simpleFieldIsEmptyConstraintParameter(fieldId = "declarationRole", "awrs.application_declaration.error.declaration_role_empty"),
      FieldMaxLengthConstraintParameter(applicationDeclarationRoleLen, Invalid("awrs.generic.error.name.maximum_length", "Your role", applicationDeclarationRoleLen)),
      FieldFormatConstraintParameter((name: String) => if (validText(name)) Valid else Invalid("awrs.generic.error.character_invalid.summary_declaration", "Your role"))
    )

  val confirmationConstraintParameters =
    CompulsoryBooleanMappingParameter(simpleFieldIsEmptyConstraintParameter("confirmation", "awrs.application_declaration.error.confirmation_empty"), BooleanCheckboxEnum, BooleanCheckboxEnum.False)

  val applicationDeclarationValidationForm = Form(mapping(
    "declarationName" -> compulsoryText(declarationNameConstraintParameters),
    "declarationRole" -> compulsoryText(declarationRoleConstraintParameters),
    "confirmation" -> compulsoryBoolean(confirmationConstraintParameters)
  )(ApplicationDeclaration.apply)(ApplicationDeclaration.unapply))

  val applicationDeclarationForm: PrevalidationAPI[ApplicationDeclaration] = PreprocessedForm(applicationDeclarationValidationForm)
}
