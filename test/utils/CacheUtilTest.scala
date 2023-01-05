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

package utils

import models.BusinessDetailsEntityTypes
import models.BusinessDetailsEntityTypes._
import services.DataCacheKeys._
import services.JourneyConstants
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import utils.CacheUtil.cacheUtil
import utils.TestUtil._

class CacheUtilTest extends PlaySpec with GuiceOneAppPerSuite {

  lazy val allEntities = List("SOP", "LTD", "Partnership", "LLP", "LTD_GRP", "LLP_GRP")

  "copyOfSave4Later" must {
    allEntities.foreach {
      legalEntity =>
        s"Return the correct kind of cache map for business type $legalEntity " in {
          val cache = createCacheMap(legalEntity = legalEntity, businessType = toBusinessType(legalEntity)).copyOfSave4Later
          val shouldAllBeTrue =
          JourneyConstants.getJourney(legalEntity) map {
            case `businessDetailsName` => cache.getTradingStartDetails.isDefined
            case `businessRegistrationDetailsName` => cache.getBusinessRegistrationDetails.isDefined
            case `businessContactsName` => cache.getBusinessContacts.isDefined
            case `placeOfBusinessName` => cache.getPlaceOfBusiness.isDefined
            case `groupMembersName` => cache.getGroupDeclaration.isDefined && cache.getGroupMembers.isDefined
            case `partnersName` => cache.getPartners.isDefined
            case `additionalBusinessPremisesName` => cache.getAdditionalBusinessPremises.isDefined
            case `businessDirectorsName` => cache.getBusinessDirectors.isDefined
            case `tradingActivityName` => cache.getTradingActivity.isDefined
            case `productsName` => cache.getProducts.isDefined
            case `suppliersName` => cache.getSuppliers.isDefined
          }
          shouldAllBeTrue.contains(false) mustBe false
        }
    }
  }

  def toBusinessType(legalEntity: String): BusinessDetailsEntityTypes.Value = legalEntity match {
    case "SOP" => SoleTrader
    case "LTD" => CorporateBody
    case "LTD_GRP" | "LLP_GRP" => GroupRep
    case "LLP" | "LP" => Llp
    case "Partnership" => Partnership
  }
}
