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

import java.text.SimpleDateFormat

import org.joda.time.format.DateTimeFormat

object WithdrawalHelper {

  private val frontendKey = (name: String, messageKey: String) => f"awrs.$messageKey.reason.$name"
  private val backendKey = (name: String, messageKey: String) => f"awrs.$messageKey.reason.$name.schema_enum"

  def enumPair(enumName: String, messageKey: String): (String, String) = backendKey(enumName, messageKey) -> frontendKey(enumName, messageKey)

  private lazy val dateFormat = new SimpleDateFormat("dd MMMM yyyy")
  private lazy val responseDatePattern = "yyyy-MM-dd'T'HH:mm:ss'Z'"

  def stringifyDate(date: String): String = {
    val formatter = DateTimeFormat.forPattern(responseDatePattern)
    val localDate = formatter.parseLocalDate(date).toDate
    dateFormat.format(localDate)
  }

}
