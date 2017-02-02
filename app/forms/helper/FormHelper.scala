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

package forms.helper

import java.text.SimpleDateFormat
import java.util.Date

import forms.validation.util.ErrorMessageFactory._
import forms.validation.util.{FieldErrorConfig, MessageArguments, SummaryErrorConfig, TargetFieldIds}
import models.TupleDate
import org.joda.time.LocalDate
import play.api.data.validation.Valid

import scala.util.{Failure, Success, Try}


object FormHelper {

  def maxLengthValidation(fieldLen: Int, maxLen: Int, inputField: String, fieldLabel: String): play.api.data.validation.ValidationResult =
    fieldLen <= maxLen match {
      case true => Valid
      case false =>
        createErrorMessage(TargetFieldIds(inputField), FieldErrorConfig("awrs.generic.error.maximum_length", MessageArguments(fieldLabel, maxLen)), SummaryErrorConfig(MessageArguments(fieldLabel)))
    }

  def isInvalidDate(date: TupleDate) =
    Try(new LocalDate(date.year.trim.toInt, date.month.trim.toInt, date.day.trim.toInt).toDate) match {
      case Failure(_) => true
      case Success(_) => false
    }

  def isDateAfterOrEqual(baseDate: String, date2: Date): Boolean = {
    val ddMMyyyyFormat = new SimpleDateFormat("dd/MM/yyyy")
    val date1: Date = ddMMyyyyFormat.parse(baseDate)
    date2.compareTo(date1) match {
      case 0 | 1 => true
      case _ => false
    }
  }

}
