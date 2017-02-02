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

import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

object AwrsFormFields extends AwrsFormFields

trait AwrsFormFields {
  val wholesaler = Seq(
    "01"-> Messages("awrs.additional_information.cash_and_carry"),
    "02"-> Messages("awrs.additional_information.supply_off_trade"),
    "03"-> Messages("awrs.additional_information.supply_on_trade"),
    "04"-> Messages("awrs.additional_information.producer"),
    "05"-> Messages("awrs.additional_information.broker"),
    "99"-> Messages("awrs.additional_information.other")
  )

  val orders = Seq(
    "02"-> Messages("awrs.additional_information.orders.internet"),
    "03"-> Messages("awrs.additional_information.orders.telephonefax"),
    "04"-> Messages("awrs.additional_information.orders.facetoface"),
    "99"-> Messages("awrs.additional_information.other")
  )

  val mainCustomerOptions = Seq(
    "01" -> Messages("awrs.additional_information.mainCustomers.pubs"),
    "02" -> Messages("awrs.additional_information.mainCustomers.night_clubs"),
    "03" -> Messages("awrs.additional_information.mainCustomers.private_members_clubs"),
    "04" -> Messages("awrs.additional_information.mainCustomers.hotels"),
    "05" -> Messages("awrs.additional_information.mainCustomers.hospitality_catering"),
    "06" -> Messages("awrs.additional_information.mainCustomers.restaurants"),
    "07" -> Messages("awrs.additional_information.mainCustomers.independent_retailers"),
    "08" -> Messages("awrs.additional_information.mainCustomers.national_retailers"),
    "09" -> Messages("awrs.additional_information.mainCustomers.public"),
    "10" -> Messages("awrs.additional_information.mainCustomers.other_wholesalers"),
    "99" -> Messages("awrs.additional_information.other")
  )

  val products = Seq(
    "02"-> Messages("awrs.additional_information.wine"),
    "03"-> Messages("awrs.additional_information.spirits"),
    "04"-> Messages("awrs.additional_information.cider"),
    "05"-> Messages("awrs.additional_information.beer"),
    "06"-> Messages("awrs.additional_information.perry"),
    "99"-> Messages("awrs.additional_information.other")
  )

  val exportAlcohol = Seq(
    "euDispatches" -> Messages("awrs.additional_information.within_eu"),
    "outsideEU" -> Messages("awrs.additional_information.outside_eu")
  )
}
