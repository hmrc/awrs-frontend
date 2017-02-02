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

import models.Survey
import play.api.data.Forms._
import play.api.data.{Form, Mapping}
import utils.AwrsValidator._
import forms.validation.util.ConstraintUtil.OptionalTextFieldMappingParameter
import forms.validation.util.MappingUtilAPI._
import forms.validation.util.ErrorMessagesUtilAPI._
import forms.prevalidation._

object SurveyForm {

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

  val surveyValidationForm = Form[Survey](
    mapping(
      "visitReason" -> optional(text(maxLength = 40)),
      "easeOfAccess" -> optional(text(maxLength = 40)),
      "easeOfUse" -> optional(text(maxLength = 40)),
      "overall" -> optional(text(maxLength = 40)),
      "helpNeeded" -> optional(text(maxLength = 40)),
      "howDidYouFindOut" -> optional(text(maxLength = 40)),
      "comments" -> comments_optional,
      "contactFullName" -> optional(text),
      "contactEmail" -> optional(text),
      "contactTelephone" -> optional(text)
    )(Survey.apply)(Survey.unapply)
  )

  val surveyForm = PreprocessedForm(surveyValidationForm)

}
