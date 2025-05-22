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
import forms.validation.util.UTRValidator
import models.AwrsEnrolmentUtr
import play.api.data.Form
import play.api.data.Forms._

object RegisteredUtrForm {

  val utr = "utr"

  lazy val awrsEnrolmentUtrValidationForm: Form[AwrsEnrolmentUtr] = Form(mapping(
    utr ->  text
      .verifying("awrs.reenrolment.registered_utr.error", x => {
        val trimmedString = x.replaceAll(" ", "")
        (trimmedString.matches("""^[0-9]{10}$""") ||
          trimmedString.matches("""^[0-9]{13}$""")) &&
          UTRValidator.validateUTR(trimmedString)
      })
  )(AwrsEnrolmentUtr.apply)(AwrsEnrolmentUtr.unapply))

  lazy val awrsEnrolmentUtrForm: PrevalidationAPI[AwrsEnrolmentUtr] = PreprocessedForm(awrsEnrolmentUtrValidationForm)
}
