/*
 * Copyright 2021 HM Revenue & Customs
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

package views.helpers

import models.TupleDate
import org.joda.time.LocalDate

import java.text.SimpleDateFormat
import java.util.Calendar


object DeRegistrationHelper {

  private val frontendKey = (name: String) => f"awrs.de_registration.reason.$name"
  private val backendKey = (name: String) => f"awrs.de_registration.reason.$name.schema_enum"

  def enumPair(enumName: String): (String, String) = backendKey(enumName) -> frontendKey(enumName)

  lazy val dateFormat = new SimpleDateFormat("dd MMMM yyyy")

  def stringifyDate(date: TupleDate): String = {
    val localDate = new LocalDate(date.year.trim.toInt, date.month.trim.toInt, date.day.trim.toInt).toDate
    dateFormat.format(localDate)
  }
  def today: String = {
    dateFormat.format(Calendar.getInstance().getTime())
  }
}
