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

import forms.prevalidation._
import forms.validation.util.ConstraintUtil.{CompulsoryTextFieldMappingParameter, FieldFormatConstraintParameter, FieldMaxLengthConstraintParameter}
import forms.validation.util.ErrorMessagesUtilAPI.simpleFieldIsEmptyConstraintParameter
import forms.validation.util.MappingUtilAPI.{MappingUtil, compulsoryText}
import models.AwrsRegisteredPostcode
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Invalid, Valid}
import utils.AwrsValidator

object AwrsRegisteredPostcodeForm extends AwrsValidator{

  val registeredPostcode = "registeredPostcode"
  val maxQueryLength = 140

  private lazy val formatRules =
    FieldFormatConstraintParameter(
      (name: String) => {
        trimAllFunc(name) match {
          case trimmedName@_ if trimmedName.matches(postcodeRegex) => Valid
          case _ => {
            Invalid("awrs.generic.error.postcode_invalid")
          }
        }
      }
    )

  private lazy val compulsoryQueryField = compulsoryText(
    CompulsoryTextFieldMappingParameter(
      empty = simpleFieldIsEmptyConstraintParameter(registeredPostcode, "awrs.register_postcode.error.empty"),
      maxLengthValidation = FieldMaxLengthConstraintParameter(maxQueryLength, Invalid("awrs.generic.error.awrsUrn.maximum_length", "awrsUrn field", maxQueryLength)),
      formatValidations = Seq(formatRules)
    ))

  lazy val awrsRegisteredPostcodeValidationForm: Form[AwrsRegisteredPostcode] = Form(mapping(
    registeredPostcode -> compulsoryQueryField.toStringFormatter
  )(AwrsRegisteredPostcode.apply)(AwrsRegisteredPostcode.unapply))


  lazy val awrsRegisteredPostcodeForm: PrevalidationAPI[AwrsRegisteredPostcode] = PreprocessedForm(
    awrsRegisteredPostcodeValidationForm,
    trimRules = Map(registeredPostcode -> TrimOption.bothAndCompress),
    caseRules = Map())
}
