/*
 * Copyright 2022 HM Revenue & Customs
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

import models.{ApplicationDeclaration, Products, Suppliers, _}
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import services.DataCacheKeys._
import services.JourneyConstants
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

object CacheUtil extends Logging {

  implicit class CacheHelper(cache: CacheMap){

    private def tryOrReturnNone[T](f: => Option[T])(implicit tag: ClassTag[T]): Option[T] = Try(f) match {
      case Success(result) => result
      case Failure(e) =>
        logger.debug(s"\nCacheUtil.CacheHelper failure on retrieving ${tag.runtimeClass}:\n$e")
        None
    }

    def getBusinessType: Option[BusinessType] = cache.getEntry[BusinessType](businessTypeName)

    def getPartners: Option[Partners] = tryOrReturnNone(cache.getEntry[Partners](partnersName))

    def getGroupDeclaration: Option[GroupDeclaration] = cache.getEntry[GroupDeclaration](groupDeclarationName)

    def getBusinessCustomerDetails: Option[BusinessCustomerDetails] = cache.getEntry[BusinessCustomerDetails](businessCustomerDetailsName)

    def getBusinessDetails: Option[BusinessDetails] = cache.getEntry[BusinessDetails](businessDetailsName)

    def getBusinessNameDetails: Option[BusinessNameDetails] = cache.getEntry[BusinessNameDetails](businessNameDetailsName)

    def getTradingStartDetails: Option[NewAWBusiness] = cache.getEntry[NewAWBusiness](tradingStartDetailsName)

    def getBusinessRegistrationDetails: Option[BusinessRegistrationDetails] = cache.getEntry[BusinessRegistrationDetails](businessRegistrationDetailsName)

    def getBusinessContacts: Option[BusinessContacts] = cache.getEntry[BusinessContacts](businessContactsName)

    def getPlaceOfBusiness: Option[PlaceOfBusiness] = cache.getEntry[PlaceOfBusiness](placeOfBusinessName)

    def getGroupMembers: Option[GroupMembers] = tryOrReturnNone(cache.getEntry[GroupMembers](groupMembersName))

    def getBusinessDirectors: Option[BusinessDirectors] = tryOrReturnNone(cache.getEntry[BusinessDirectors](businessDirectorsName))

    def getAdditionalBusinessPremises: Option[AdditionalBusinessPremisesList] = cache.getEntry[AdditionalBusinessPremisesList](additionalBusinessPremisesName)

    def getTradingActivity: Option[TradingActivity] = cache.getEntry[TradingActivity](tradingActivityName)
    /* TODO AWRS-1800 old code to remove after 28 days */
    def getTradingActivity_old: Option[TradingActivity_old] = cache.getEntry[TradingActivity_old](tradingActivityName)

    def getProducts: Option[Products] = cache.getEntry[Products](productsName)

    def getSuppliers: Option[Suppliers] = cache.getEntry[Suppliers](suppliersName)

    def getApplicationDeclaration: Option[ApplicationDeclaration] = cache.getEntry[ApplicationDeclaration](applicationDeclarationName)

    def copyOfSave4Later: CacheMap = {
      val jsonMap: Map[String, JsValue] = {
        JourneyConstants.getJourney(cache.getBusinessRegistrationDetails.get.legalEntity.get) map {
          case `businessDetailsName` => Map(
            businessNameDetailsName -> Json.toJson(cache.getBusinessNameDetails),
            tradingStartDetailsName -> Json.toJson(cache.getTradingStartDetails)
          )
          case `businessRegistrationDetailsName` => Map(businessRegistrationDetailsName -> Json.toJson(cache.getBusinessRegistrationDetails))
          case `businessContactsName` => Map(businessContactsName -> Json.toJson(cache.getBusinessContacts))
          case `placeOfBusinessName` => Map(placeOfBusinessName -> Json.toJson(cache.getPlaceOfBusiness))
          case `groupMembersName` => Map(groupDeclarationName -> Json.toJson(cache.getGroupDeclaration), groupMembersName -> Json.toJson(cache.getGroupMembers))
          case `partnersName` => Map(partnersName -> Json.toJson(cache.getPartners))
          case `additionalBusinessPremisesName` => Map(additionalBusinessPremisesName -> Json.toJson(cache.getAdditionalBusinessPremises))
          case `businessDirectorsName` => Map(businessDirectorsName -> Json.toJson(cache.getBusinessDirectors))
          case `tradingActivityName` => Map(tradingActivityName -> Json.toJson(cache.getTradingActivity))
          case `productsName` => Map(productsName -> Json.toJson(cache.getProducts))
          case `suppliersName` => Map(suppliersName -> Json.toJson(cache.getSuppliers))
        }
      }.reduce(_ ++ _) +
        (businessTypeName -> Json.toJson(cache.getBusinessType),
          businessCustomerDetailsName -> Json.toJson(cache.getBusinessCustomerDetails),
          applicationDeclarationName -> Json.toJson(cache.getApplicationDeclaration))

      CacheMap(cache.id, jsonMap)
    }

  }

  implicit val cacheUtil = (cache: CacheMap) => new CacheUtil.CacheHelper(cache)
}
