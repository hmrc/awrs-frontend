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
import forms.validation.util.ErrorMessageFactory.createErrorMessage
import forms.validation.util.ErrorMessagesUtilAPI.simpleFieldIsEmptyConstraintParameter
import forms.validation.util.MappingUtilAPI.{MappingUtil, compulsoryText}
import forms.validation.util.{FieldErrorConfig, MessageArguments, SummaryErrorConfig, TargetFieldIds}
import models.AwrsEnrollmentUrn
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Invalid, Valid}

object AwrsEnrollmentUrnForm {

  val awrsUrn = "awrsUrn"

  private lazy val leading4CharRegex = "^[xX][a-zA-Z][aA][wW][0-9]{1,}$"//Just match the 1st 4 characters rule
  private lazy val zerosRegex = "^[a-zA-Z]{4}[0]{5}.{1,}$"//check number of zeros

  val awrsRefRegEx = "^[xX][a-zA-Z][aA][wW][0]{5}[0-9]{6}$"//check for all chars in AWRS URN

  val maxQueryLength = 140

  private lazy val awrsUrnTargetId = TargetFieldIds(awrsUrn)

  private lazy val invalidFormatSummaryError =
    (fieldErr: String) => SummaryErrorConfig(fieldErr + ".summary", MessageArguments("urn field"))

  private lazy val invalidQueryFieldError =
    (fieldErr: String) => createErrorMessage(
      awrsUrnTargetId,
      FieldErrorConfig(fieldErr),
      invalidFormatSummaryError(fieldErr))

  private lazy val formatRules =
    FieldFormatConstraintParameter(
      (name: String) => {
        trimAllFunc(name) match {
          case trimmedName@_ if !validText(trimmedName) => invalidQueryFieldError("awrs.generic.error.character_invalid")
          case trimmedName@_ if trimmedName.matches(awrsRefRegEx) => Valid
          case trimmedName@_ if trimmedName.matches(leading4CharRegex) => {
            trimmedName match {
              case trimmedName if (trimmedName.length != 15) => invalidQueryFieldError("awrs.search.query.string_length_mismatch")
              case trimmedName if (!trimmedName.matches(zerosRegex)) => invalidQueryFieldError("awrs.search.query.zeros_mismatch")

              case _ => invalidQueryFieldError("awrs.search.query.default_invalid_urn")
            }
          }
          case _ => {
            invalidQueryFieldError("awrs.search.query.default_invalid_urn")
          }
        }
      }
    )

  val asciiChar32 = 32
  val asciiChar126 = 126
  val asciiChar160 = 160
  val asciiChar255 = 255

  def validText(input: String): Boolean = {
    val inputList: List[Char] = input.toList
    inputList.forall { c =>
      (c >= asciiChar32 && c <= asciiChar126) || (c >= asciiChar160 && c <= asciiChar255)
    }
  }

  private lazy val compulsoryQueryField = compulsoryText(
    CompulsoryTextFieldMappingParameter(
      empty = simpleFieldIsEmptyConstraintParameter(awrsUrn, "awrs.search.query.empty"),
      maxLengthValidation = FieldMaxLengthConstraintParameter(maxQueryLength, Invalid("awrs.generic.error.urn.maximum_length", "urn field", maxQueryLength)),
      formatValidations = Seq(formatRules)
    ))

  lazy val awrsEnrolmentUrnValidationForm: Form[AwrsEnrollmentUrn] = Form(mapping(
    awrsUrn -> compulsoryQueryField.toStringFormatter
  )(AwrsEnrollmentUrn.apply)(AwrsEnrollmentUrn.unapply))

  lazy val awrsEnrolmentUrnForm: PrevalidationAPI[AwrsEnrollmentUrn] = PreprocessedForm(awrsEnrolmentUrnValidationForm)

}
