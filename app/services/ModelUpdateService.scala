/*
 * Copyright 2019 HM Revenue & Customs
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

import models._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.AccountUtils
import utils.CacheUtil._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

trait ModelUpdateService {
  def ensureAllModelsAreUpToDate(implicit user: AuthContext, hc: HeaderCarrier, save4LaterService: Save4LaterService): Future[Boolean]
}


object UpdateRequired extends ModelUpdateService {

def updateTradingActivity(tradingActivity_old: TradingActivity_old): TradingActivity = {
  TradingActivity(
    wholesalerType = tradingActivity_old.wholesalerType,
    otherWholesaler = tradingActivity_old.otherWholesaler,
    typeOfAlcoholOrders = tradingActivity_old.typeOfAlcoholOrders,
    otherTypeOfAlcoholOrders = tradingActivity_old.otherTypeOfAlcoholOrders,
    doesBusinessImportAlcohol = tradingActivity_old.doesBusinessImportAlcohol,
    doYouExportAlcohol = tradingActivity_old.doYouExportAlcohol.contains("no") match {
      case true => Some("No")
      case _ => Some("Yes")
    },
    exportLocation = tradingActivity_old.doYouExportAlcohol.contains("no") match {
      case true => None
      case _ => Some(tradingActivity_old.doYouExportAlcohol)
    },
    thirdPartyStorage = tradingActivity_old.thirdPartyStorage
  )
}

  private def handleSubscriptionFrontEnd(implicit user: AuthContext, hc: HeaderCarrier, save4LaterService: Save4LaterService) = {
    def modelUpdate = // convert from old models
      save4LaterService.api.fetchSubscriptionTypeFrontEnd_old flatMap {
        case Some(subscriptionTypeFrontEnd) =>
          val newTradingActivity = subscriptionTypeFrontEnd.tradingActivity.fold(None: Option[TradingActivity])(x => Some(updateTradingActivity(x)))

          val subscriptionTypeFrontEndUpdated: SubscriptionTypeFrontEnd = SubscriptionTypeFrontEnd(
            legalEntity = subscriptionTypeFrontEnd.legalEntity,
            businessPartnerName = subscriptionTypeFrontEnd.businessPartnerName,
            groupDeclaration = subscriptionTypeFrontEnd.groupDeclaration,
            businessCustomerDetails = subscriptionTypeFrontEnd.businessCustomerDetails,
            businessDetails = subscriptionTypeFrontEnd.businessDetails,
            businessRegistrationDetails = subscriptionTypeFrontEnd.businessRegistrationDetails,
            businessContacts = subscriptionTypeFrontEnd.businessContacts,
            placeOfBusiness = subscriptionTypeFrontEnd.placeOfBusiness,
            partnership = subscriptionTypeFrontEnd.partnership,
            groupMembers = subscriptionTypeFrontEnd.groupMembers,
            additionalPremises = subscriptionTypeFrontEnd.additionalPremises,
            businessDirectors = subscriptionTypeFrontEnd.businessDirectors,
            tradingActivity = newTradingActivity,
            products = subscriptionTypeFrontEnd.products,
            suppliers = subscriptionTypeFrontEnd.suppliers,
            applicationDeclaration = subscriptionTypeFrontEnd.applicationDeclaration,
            changeIndicators = subscriptionTypeFrontEnd.changeIndicators
          )
          save4LaterService.api.saveSubscriptionTypeFrontEnd(subscriptionTypeFrontEndUpdated).map(Some(_))
        case None => Future.successful()
      }

    AccountUtils.hasAwrs match {
      case false => Future.successful(None)
      case true =>
        // first read it as the new model
        save4LaterService.api.fetchSubscriptionTypeFrontEnd flatMap {
          case updateToDate@Some(subscriptionTypeFrontEnd) =>
            subscriptionTypeFrontEnd.modelVersion match {
              case "1.1" => Future.successful(updateToDate)
              case _ => modelUpdate
            }
          case None => modelUpdate
        }

    }
  }

  private def patchTradingActivity(cacheMap: CacheMap)(implicit user: AuthContext, hc: HeaderCarrier, save4LaterService: Save4LaterService) = cacheMap.getTradingActivity match {
    case None =>
      cacheMap.getTradingActivity_old match {
        case Some(oldTradingActivity: TradingActivity_old) =>
          val ty = updateTradingActivity(oldTradingActivity)
          save4LaterService.mainStore.saveTradingActivity(updateTradingActivity(oldTradingActivity)) map (_ => true)
        case None => Future.successful(true)
      }
    case Some(ta) => ta.doYouExportAlcohol match {
      case None => cacheMap.getTradingActivity_old match {
        case Some(oldTradingActivity: TradingActivity_old) =>
          save4LaterService.mainStore.saveTradingActivity(updateTradingActivity(oldTradingActivity)) map (_ => true)
        case None => Future.successful(true)
      }
      case Some(_) => Future.successful(true)
    }
  }

  override def ensureAllModelsAreUpToDate(implicit user: AuthContext, hc: HeaderCarrier, save4LaterService: Save4LaterService): Future[Boolean] =
    save4LaterService.mainStore.fetchAll.flatMap {
      case Some(cache) =>
        for {
          _ <- handleSubscriptionFrontEnd // this function will look at the api store internally
          bool <- patchTradingActivity(cache)
        } yield bool
      case None => Future.successful(true)
    }

}
