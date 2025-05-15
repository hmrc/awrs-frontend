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

package forms.reenrolment

import forms.prevalidation._
import forms.validation.util.ConstraintUtil.{CompulsoryTextFieldMappingParameter, FieldFormatConstraintParameter}
import forms.validation.util.ErrorMessagesUtilAPI.simpleFieldIsEmptyConstraintParameter
import forms.validation.util.MappingUtilAPI.{MappingUtil, compulsoryText}
import models.reenrolment.AwrsRegisteredPostcode
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Invalid, Valid}
import utils.AwrsValidator

object RegisteredPostcodeForm extends AwrsValidator{

  val registeredPostcode = "registeredPostcode"

  val invalidPostcodeErrorMessage = Seq[FieldFormatConstraintParameter](
    FieldFormatConstraintParameter(
      (registeredPostcode: String) => {
        if (AwrsRegisteredPostcode.sanitise(registeredPostcode).matches(postcodeRegex)) {
          Valid
        } else {
          Invalid("awrs.reenrolment.registered_postcode.error")
        }
      }
    )
  )

  private lazy val compulsoryQueryField = compulsoryText(
    CompulsoryTextFieldMappingParameter(
      empty = simpleFieldIsEmptyConstraintParameter(registeredPostcode, "awrs.reenrolment.registered_postcode.error"),
      maxLengthValidation = null,
      formatValidations = invalidPostcodeErrorMessage
    ))

  lazy val awrsRegisteredPostcodeValidationForm: Form[AwrsRegisteredPostcode] = Form(mapping(
    registeredPostcode -> compulsoryQueryField.toStringFormatter
  )(AwrsRegisteredPostcode.apply)(AwrsRegisteredPostcode.unapply))


  lazy val awrsRegisteredPostcodeForm: PrevalidationAPI[AwrsRegisteredPostcode] = PreprocessedForm(
    awrsRegisteredPostcodeValidationForm,
    trimRules = Map(),
    caseRules = Map())
}
