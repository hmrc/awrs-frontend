/*
 * Copyright 2020 HM Revenue & Customs
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

package services

import DataCacheKeys._
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

object JourneyConstants {

  val getJourney: String => Seq[String] = {
    case "SOP" => Seq(businessDetailsName, businessRegistrationDetailsName, placeOfBusinessName, businessContactsName, additionalBusinessPremisesName, tradingActivityName, productsName, suppliersName)
    case "Partnership" => Seq(businessDetailsName, businessRegistrationDetailsName, placeOfBusinessName, businessContactsName, partnersName, additionalBusinessPremisesName, tradingActivityName, productsName, suppliersName)
    case "LLP" | "LP" => Seq(businessDetailsName, businessRegistrationDetailsName, placeOfBusinessName, businessContactsName, partnersName, additionalBusinessPremisesName, tradingActivityName, productsName, suppliersName)
    case "LTD_GRP" => Seq(businessDetailsName, businessRegistrationDetailsName, placeOfBusinessName, businessContactsName, groupMembersName, additionalBusinessPremisesName, businessDirectorsName, tradingActivityName, productsName, suppliersName)
    case "LLP_GRP" => Seq(businessDetailsName, businessRegistrationDetailsName, placeOfBusinessName, businessContactsName, groupMembersName, partnersName, additionalBusinessPremisesName, tradingActivityName, productsName, suppliersName)
    case _ => Seq(businessDetailsName, businessRegistrationDetailsName, placeOfBusinessName, businessContactsName, additionalBusinessPremisesName, businessDirectorsName, tradingActivityName, productsName, suppliersName)
  }

  val getJourneyProgress: (Option[String], String) => (Int, Int, String) = (businessType: Option[String], sectionName: String) => {
    val journey = getJourney(businessType.fold("")(x => x))
    val totalSections = journey.size
    val sectionIndex = journey.indexOf(sectionName) + 1
    (businessType, sectionName) match {
      case (Some("LTD_GRP" | "LLP_GRP"), `businessDetailsName`) => (sectionIndex, totalSections, "awrs.index_page.group_business_details_text")
      case (Some("Partnership" | "LP" | "LLP"), `businessDetailsName`) => (sectionIndex, totalSections,"awrs.index_page.partnership_details_text")
      case (_, `businessDetailsName`) => (sectionIndex, totalSections,"awrs.index_page.business_details_text")
      case (Some("LTD_GRP" | "LLP_GRP"), `businessRegistrationDetailsName`) => (sectionIndex, totalSections,"awrs.index_page.group_business_registration_details_text")
      case (Some("Partnership" | "LP" | "LLP"), `businessRegistrationDetailsName`) => (sectionIndex, totalSections,"awrs.index_page.partnership_registration_details_text")
      case (_, `businessRegistrationDetailsName`) => (sectionIndex, totalSections,"awrs.index_page.business_registration_details_text")
      case (Some("LTD_GRP" | "LLP_GRP"), `placeOfBusinessName`) => (sectionIndex, totalSections,"awrs.index_page.group_business_place_of_business_text")
      case (Some("Partnership" | "LP" | "LLP"), `placeOfBusinessName`) => (sectionIndex, totalSections,"awrs.index_page.partnership_place_of_business_text")
      case (_, `placeOfBusinessName`) => (sectionIndex, totalSections,"awrs.index_page.business_place_of_business_text")
      case (Some("LTD_GRP" | "LLP_GRP"), `businessContactsName`) => (sectionIndex, totalSections,"awrs.index_page.group_business_contacts_text")
      case (Some("Partnership" | "LP" | "LLP"), `businessContactsName`) => (sectionIndex, totalSections,"awrs.index_page.partnership_contacts_text")
      case (_, `businessContactsName`) => (sectionIndex, totalSections,"awrs.index_page.business_contacts_text")
      case (_, `partnersName`) => (sectionIndex, totalSections,"awrs.index_page.business_partners_text")
      case (_, `groupMembersName`) => (sectionIndex, totalSections,"awrs.index_page.group_member_details_text")
      case (_, `businessDirectorsName`) => (sectionIndex, totalSections,"awrs.index_page.business_directors.index_text")
      case (_, `additionalBusinessPremisesName`) => (sectionIndex, totalSections,"awrs.index_page.additional_premises_text")
      case (_, `tradingActivityName`) => (sectionIndex, totalSections,"awrs.index_page.trading_activity_text")
      case (_, `productsName`) => (sectionIndex, totalSections,"awrs.index_page.products_text")
      case (_, `suppliersName`) => (sectionIndex, totalSections,"awrs.index_page.suppliers_text")
    }
  }
}