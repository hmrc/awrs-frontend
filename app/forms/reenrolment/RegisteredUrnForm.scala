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
import forms.validation.util.ConstraintUtil.{CompulsoryTextFieldMappingParameter, FieldFormatConstraintParameter, FieldMaxLengthConstraintParameter}
import forms.validation.util.ErrorMessagesUtilAPI.simpleFieldIsEmptyConstraintParameter
import forms.validation.util.MappingUtilAPI.{MappingUtil, compulsoryText}
import models.AwrsEnrolmentUrn
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Invalid, Valid}

object RegisteredUrnForm {

  val awrsUrn = "awrsUrn"

  private lazy val leading4CharRegex = "^[xX][a-zA-Z][aA][wW][0-9]{1,}$"//Just match the 1st 4 characters rule
  private lazy val zerosRegex = "^[a-zA-Z]{4}[0]{5}.{1,}$"//check number of zeros

  val awrsRefRegEx = "^[xX][a-zA-Z][aA][wW][0]{5}[0-9]{6}$"//check for all chars in AWRS URN

  val maxQueryLength = 140

  def validText(input: String): Boolean = {
    val inputList: List[Char] = input.toList
    inputList.forall { c =>
      (c >= asciiChar32 && c <= asciiChar126) || (c >= asciiChar160 && c <= asciiChar255)
    }
  }

  private lazy val formatRules =
    FieldFormatConstraintParameter(
      (name: String) => {
        trimAllFunc(name) match {
          case trimmedName@_ if !validText(trimmedName) => Invalid("awrs.generic.error.character_invalid")
          case trimmedName@_ if trimmedName.matches(awrsRefRegEx) => Valid
          case trimmedName@_ if trimmedName.matches(leading4CharRegex) => {
            trimmedName match {
              case trimmedName if (trimmedName.length != 15) => Invalid("awrs.awrsUrn.string_length_mismatch")
              case trimmedName if (!trimmedName.matches(zerosRegex)) => Invalid("awrs.awrsUrn.zeros_mismatch")

              case _ => Invalid("awrs.awrsUrn.default_invalid_urn")
            }
          }
          case _ => {
            Invalid("awrs.awrsUrn.default_invalid_urn")
          }
        }
      }
    )

  val asciiChar32 = 32
  val asciiChar126 = 126
  val asciiChar160 = 160
  val asciiChar255 = 255



  private lazy val compulsoryQueryField = compulsoryText(
    CompulsoryTextFieldMappingParameter(
      empty = simpleFieldIsEmptyConstraintParameter(awrsUrn, "awrs.awrsUrn.empty"),
      maxLengthValidation = FieldMaxLengthConstraintParameter(maxQueryLength, Invalid("awrs.generic.error.awrsUrn.maximum_length", "awrsUrn field", maxQueryLength)),
      formatValidations = Seq(formatRules)
    ))

  lazy val awrsEnrolmentUrnValidationForm: Form[AwrsEnrolmentUrn] = Form(mapping(
    awrsUrn -> compulsoryQueryField.toStringFormatter
  )(AwrsEnrolmentUrn.apply)(AwrsEnrolmentUrn.unapply))

  lazy val awrsEnrolmentUrnForm: PrevalidationAPI[AwrsEnrolmentUrn] = PreprocessedForm(
    awrsEnrolmentUrnValidationForm,
    trimRules = Map(awrsUrn -> TrimOption.bothAndCompress),
    caseRules = Map())


}
