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

  private[views] val AllFields: Set[String] = Set("day", "month", "year")

  private[views] val affectedFields: Map[String, Set[String]] = Map(
    "ated.error.date.day.missing" -> Set("day"),
    "ated.error.date.month.missing" -> Set("month"),
    "ated.error.date.year.missing" -> Set("year"),
    "ated.error.date.daymonth.missing" -> Set("day", "month"),
    "ated.error.date.dayyear.missing" -> Set("day", "year"),
    "ated.error.date.monthyear.missing" -> Set("month", "year"),
    "ated.error.date.invalid.day.month" -> Set("day", "month"),
    "ated.error.day.invalid" -> Set("day"),
    "ated.error.month.invalid" -> Set("month"),
    "ated.error.date.year.length" -> Set("year"),
    "ated.error.date.notInRange" -> Set("year")
  )

  def fieldsInError(form: Form[_], dateField: String): Set[String] =
    form.errors
      .filter(error => error.key == dateField || error.key.startsWith(s"$dateField."))
      .flatMap(error => affectedFields.getOrElse(error.message, AllFields))
      .toSet

  def highlightErrors(dateInput: DateInput, form: Form[_], dateField: String): DateInput = {
    val inError = fieldsInError(form, dateField)

    dateInput.copy(items = dateInput.items.map { item =>
      val key = item.name.split('.').last
      val retained = item.classes.split("\\s+").filter(c => c.nonEmpty && c != ErrorClass)
      val classes = if (inError.contains(key)) retained :+ ErrorClass else retained

      item.copy(classes = classes.mkString(" "))
    })
  }

  def withDayMonthYear(dateInput: DateInput, form: Form[_], dateField: String)(implicit messages: Messages): DateInput =
    highlightErrors(dateInput.withDayMonthYearFormField(form(dateField)), form, dateField)
}
