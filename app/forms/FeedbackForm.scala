/*
 * Copyright 2017 HM Revenue & Customs
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

import forms.validation.util.ConstraintUtil.OptionalTextFieldMappingParameter
import forms.validation.util.ErrorMessagesUtilAPI._
import forms.validation.util.MappingUtilAPI._
import models.Feedback
import play.api.data.Forms._
import play.api.data.{Form, Mapping}
import utils.AwrsValidator._
import forms.prevalidation._

object FeedbackForm {

  private def comments_optional: Mapping[Option[String]] = {
    val fieldId = "comments"
    val fieldNameInErrorMessage = "comments"
    val companyNameConstraintParameters =
      OptionalTextFieldMappingParameter(
        genericFieldMaxLengthConstraintParameter(2500, fieldId, fieldNameInErrorMessage),
        genericInvalidFormatConstraintParameter(validText, fieldId, fieldNameInErrorMessage)
      )
    optionalText(companyNameConstraintParameters)
  }

  private def visit_reason_optional: Mapping[Option[String]] = {
    val fieldId = "visitReason"
    val fieldNameInErrorMessage = "visit reason"
    val companyNameConstraintParameters =
      OptionalTextFieldMappingParameter(
        genericFieldMaxLengthConstraintParameter(2500, fieldId, fieldNameInErrorMessage),
        genericInvalidFormatConstraintParameter(validText, fieldId, fieldNameInErrorMessage)
      )
    optionalText(companyNameConstraintParameters)
  }

  val feedbackValidationForm = Form[Feedback](
    mapping(
      "visitReason" -> visit_reason_optional,
      "satisfactionRating" -> optional(text(maxLength = 40)),
      "comments" -> comments_optional
    )(Feedback.apply)(Feedback.unapply)
  )

  val feedbackForm = PreprocessedForm(feedbackValidationForm)

}
