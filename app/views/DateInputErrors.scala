/*
 * Copyright 2024 HM Revenue & Customs
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

package views

import play.api.data.Form
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.dateinput.DateInput
import uk.gov.hmrc.hmrcfrontend.views.html.components.implicits._

object DateInputErrors {

  private val ErrorClass = "govuk-input--error"

  private def highlightErrors(dateInput: DateInput, form: Form[_]): DateInput = {

    dateInput.copy(items = dateInput.items.map { item =>
      val dateFieldIsMissing = if (form.hasErrors) form.errors.head.args.contains(item.id) else false
      val retained = item.classes.split("\\s+").filter(c => c.nonEmpty && c != ErrorClass)
      val classes =retained :+ ErrorClass
      if (dateFieldIsMissing) item.copy(classes = classes.mkString(" ")) else item
    })
  }

  def withDayMonthYear(dateInput: DateInput, form: Form[_], dateField: String)(implicit messages: Messages): DateInput =
    highlightErrors(dateInput.withDayMonthYearFormField(form(dateField)), form)
}
