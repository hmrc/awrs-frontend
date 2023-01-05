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

object AwrsFormFields extends AwrsFormFields

trait AwrsFormFields {
  lazy val wholesaler: Seq[(String, String)] = Seq(
    "05"-> "Broker",
    "01"-> "Cash and Carry",
    "04"-> "Producer",
    "02"-> "Supplying to off trade, like retailers and off-licences",
    "03"-> "Supplying to on trade, like pubs and hotels",
    "99"-> "Other"
  )

  lazy val orders: Seq[(String, String)] = Seq(
    "04"-> "Face to face",
    "02"-> "Internet, email or social media",
    "03"-> "Telephone or fax",
    "99"-> "Other"
  )

  lazy val mainCustomerOptions: Seq[(String, String)] = Seq(
    "05" -> "Hospitality and catering",
    "04" -> "Hotels",
    "07" -> "Independent retailers",
    "08" -> "National retailers",
    "02" -> "Night clubs",
    "10" -> "Other wholesalers",
    "03" -> "Private members clubs",
    "09" -> "Public",
    "01" -> "Pubs",
    "06" -> "Restaurants",
    "99"-> "Other"
  )

  lazy val products: Seq[(String, String)] = Seq(
    "05"-> "Beer",
    "04"-> "Cider",
    "06"-> "Perry",
    "03"-> "Spirits",
    "02"-> "Wine",
    "99"-> "Other"
  )

  lazy val exportAlcohol: Seq[(String, String)] = Seq(
    "euDispatches" -> "Within the EU",
    "outsideEU" -> "Outside the EU"
  )
}
