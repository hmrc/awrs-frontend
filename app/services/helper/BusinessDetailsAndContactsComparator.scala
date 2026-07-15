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

package services.helper

import models._
import models.CacheMap
import utils.CacheUtil._

// change flag categories based on JIRA story AWRS-385
object BusinessDetailsAndContactsComparator {

  def compare(data: SubscriptionTypeFrontEnd, cached: CacheMap): (Boolean, Boolean, Boolean, Boolean) = {

    val businessNameDetails = cached.getBusinessNameDetails
    val tradingNameDetails = cached.getTradingStartDetails
    val businessRegistrationDetails = cached.getBusinessRegistrationDetails
    val businessContacts = cached.getBusinessContacts
    val placeOfBusiness = cached.getPlaceOfBusiness

    val corporateSessionCacheBusinessDetChangeData = data.businessDetails.map(corBusDetails => (corBusDetails.tradingName, corBusDetails.newAWBusiness))

    val corporateTempCacheBusinessDetChangeData = businessNameDetails.flatMap { businessNameDet =>
      tradingNameDetails.map {
        tradingNameDet => (businessNameDet.tradingName, Some(tradingNameDet))
      }
    }

    val corporateSessionCacheBusinessRegDetChangeData = data.businessRegistrationDetails.map(corBusRegDetails =>
      (corBusRegDetails.legalEntity, corBusRegDetails.nino, corBusRegDetails.utr, corBusRegDetails.vrn, corBusRegDetails.companyRegDetails))

    val corporateTempCacheBusinessRegDetChangeData = businessRegistrationDetails.map(corBusRegDetails =>
      (corBusRegDetails.legalEntity, corBusRegDetails.nino, corBusRegDetails.utr, corBusRegDetails.vrn, corBusRegDetails.companyRegDetails))

    val corporateSessionCacheBusinessAddChangeData = data.placeOfBusiness.map(corBusDetails => (corBusDetails.mainAddress,
      corBusDetails.operatingDuration, corBusDetails.placeOfBusinessAddressLast3Years))

    val corporateTempCacheBusinessAddChangeData = placeOfBusiness.map(corBusDetails => (corBusDetails.mainAddress,
      corBusDetails.operatingDuration, corBusDetails.placeOfBusinessAddressLast3Years))

    val corporateSessionCacheContactDetChangeData = data.businessContacts.map(corBusDetails => (corBusDetails.contactFirstName,
      corBusDetails.contactLastName, corBusDetails.telephone, corBusDetails.email, corBusDetails.contactAddress))

    val corporateTempCacheContactDetChangeData = businessContacts.map(corBusDetails => (corBusDetails.contactFirstName,
      corBusDetails.contactLastName, corBusDetails.telephone, corBusDetails.email, corBusDetails.contactAddress))

    val businessName = cached.getBusinessCustomerDetails.map(_.businessName).getOrElse("")
    val businessType = cached.getBusinessType.map(_.legalEntity).getOrElse("")
    val businessNameChanged: Boolean = if (data.businessPartnerName.isDefined) !data.businessPartnerName.get.equals(businessName) else false
    val BusinessDetChangeData: Boolean = (corporateSessionCacheBusinessDetChangeData.equals(corporateTempCacheBusinessDetChangeData), businessNameChanged) match {
      case (true, false) => false
      case (false, false) => true
      case _ => true
    }

    businessType match {
      case Some("LLP_GRP") | Some("LTD_GRP") =>
      (BusinessDetChangeData,
        !corporateSessionCacheBusinessRegDetChangeData.equals(corporateTempCacheBusinessRegDetChangeData),
        !corporateSessionCacheBusinessAddChangeData.equals(corporateTempCacheBusinessAddChangeData),
        !corporateSessionCacheContactDetChangeData.equals(corporateTempCacheContactDetChangeData)
      )
      case _ =>
      (!corporateSessionCacheBusinessDetChangeData.equals(corporateTempCacheBusinessDetChangeData),
        !corporateSessionCacheBusinessRegDetChangeData.equals(corporateTempCacheBusinessRegDetChangeData),
        !corporateSessionCacheBusinessAddChangeData.equals(corporateTempCacheBusinessAddChangeData),
        !corporateSessionCacheContactDetChangeData.equals(corporateTempCacheContactDetChangeData))
    }

  }
}
