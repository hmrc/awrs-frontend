/*
 * Copyright 2018 HM Revenue & Customs
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

import _root_.models._
import connectors.{AwrsKeyStoreConnector, KeyStoreConnector, Save4LaterConnector}
import play.api.libs.json
import play.api.libs.json.Json
import services.DataCacheKeys._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.AccountUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import utils.CacheUtil.cacheUtil
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }

trait KeyStoreService {

  val keyStoreConnector: KeyStoreConnector

  @inline def saveDeRegistrationDate(deRegistration: DeRegistrationDate)(implicit hc: HeaderCarrier): Future[CacheMap] =
    saveDeRegistrationDate(Some(deRegistration))

  @inline def saveDeRegistrationDate(deRegistration: Option[DeRegistrationDate])(implicit hc: HeaderCarrier): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[Option[DeRegistrationDate]](deRegistrationDateName, deRegistration)

  @inline def fetchDeRegistrationDate(implicit hc: HeaderCarrier): Future[Option[DeRegistrationDate]] =
    keyStoreConnector.fetchDataFromKeystore[Option[DeRegistrationDate]](deRegistrationDateName) flatMap {
      case Some(someData@Some(data)) => Future.successful(someData)
      case _ => Future.successful(None)
    }

  @inline def deleteDeRegistrationDate(implicit hc: HeaderCarrier): Future[CacheMap] =
    saveDeRegistrationDate(None)

  @inline def saveWithdrawalReason(withdrawal: Option[WithdrawalReason])(implicit hc: HeaderCarrier): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[Option[WithdrawalReason]](withdrawalReasonName, withdrawal)

  @inline def saveWithdrawalReason(withdrawal: WithdrawalReason)(implicit hc: HeaderCarrier): Future[CacheMap] =
    saveWithdrawalReason(Some(withdrawal))

  @inline def fetchWithdrawalReason()(implicit hc: HeaderCarrier): Future[Option[WithdrawalReason]] =
  keyStoreConnector.fetchDataFromKeystore[Option[WithdrawalReason]](withdrawalReasonName) flatMap {
      case Some(someData@Some(data)) => Future.successful(someData)
      case _ => Future.successful(None)
    }

  @inline def deleteWithdrawalReason()(implicit hc: HeaderCarrier): Future[CacheMap] =
    saveWithdrawalReason(None)

  @inline def saveDeRegistrationReason(deRegistration: DeRegistrationReason)(implicit hc: HeaderCarrier): Future[CacheMap] =
    saveDeRegistrationReason(Some(deRegistration))

  @inline def saveDeRegistrationReason(deRegistration: Option[DeRegistrationReason])(implicit hc: HeaderCarrier): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[Option[DeRegistrationReason]](deRegistrationReasonName, deRegistration)

  @inline def fetchDeRegistrationReason(implicit hc: HeaderCarrier): Future[Option[DeRegistrationReason]] =
    keyStoreConnector.fetchDataFromKeystore[Option[DeRegistrationReason]](deRegistrationReasonName) flatMap {
      case Some(someData@Some(data)) if data.deregistrationReason.isDefined => Future.successful(someData)
      case _ => Future.successful(None)
    }

  @inline def deleteDeRegistrationReason(implicit hc: HeaderCarrier): Future[CacheMap] =
    saveDeRegistrationReason(None)

  @inline def saveSubscriptionStatus(subscriptionStatusType: SubscriptionStatusType)(implicit user: AuthContext, hc: HeaderCarrier): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[SubscriptionStatusType](subscriptionStatusTypeName, subscriptionStatusType)

  @inline def fetchSubscriptionStatus(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[SubscriptionStatusType]] =
    keyStoreConnector.fetchDataFromKeystore[SubscriptionStatusType](subscriptionStatusTypeName) flatMap {
      successData: Option[SubscriptionStatusType] => Future.successful(successData)
    }

  @inline def saveStatusInfo(statusInfoType: StatusInfoType)(implicit user: AuthContext, hc: HeaderCarrier): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[StatusInfoType](statusInfoTypeName, statusInfoType)

  @inline def fetchStatusInfo(implicit hc: HeaderCarrier): Future[Option[StatusInfoType]] =
    keyStoreConnector.fetchDataFromKeystore[StatusInfoType](statusInfoTypeName) flatMap {
      successData: Option[StatusInfoType] => Future.successful(successData)
    }

  @inline def saveStatusNotification(notificationStatus: StatusNotification)(implicit user: AuthContext, hc: HeaderCarrier): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[StatusNotification](statusNotificationName, notificationStatus)

  @inline def fetchStatusNotification(implicit hc: HeaderCarrier): Future[Option[StatusNotification]] =
    keyStoreConnector.fetchDataFromKeystore[StatusNotification](statusNotificationName)

  @inline def saveIsNewBusiness(isNewBusiness: Boolean)(implicit user: AuthContext, hc: HeaderCarrier): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[Boolean](isNewBusinessName, isNewBusiness)

  @inline def fetchIsNewBusiness(implicit hc: HeaderCarrier): Future[Option[Boolean]] =
    keyStoreConnector.fetchDataFromKeystore[Boolean](isNewBusinessName)

  @inline def saveViewedStatus(wasViewed: Boolean)(implicit user: AuthContext, hc: HeaderCarrier): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[Boolean](viewedStatusName, wasViewed)

  @inline def fetchViewedStatus(implicit hc: HeaderCarrier): Future[Option[Boolean]] =
    keyStoreConnector.fetchDataFromKeystore[Boolean](viewedStatusName)

  @inline def saveExtendedBusinessDetails(extendedBusinessDetails: ExtendedBusinessDetails)(implicit user: AuthContext, hc: HeaderCarrier): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[ExtendedBusinessDetails](extendedBusinessDetailsName, extendedBusinessDetails)

  @inline def fetchExtendedBusinessDetails(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[ExtendedBusinessDetails]] =
    keyStoreConnector.fetchDataFromKeystore[ExtendedBusinessDetails](extendedBusinessDetailsName)

  @inline def saveBusinessCustomerAddress(businessCustomerAddress: BCAddressApi3)(implicit user: AuthContext, hc: HeaderCarrier): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[BCAddressApi3](businessCustomerAddressName, businessCustomerAddress)

  @inline def fetchBusinessCustomerAddresss(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[BCAddressApi3]] =
    keyStoreConnector.fetchDataFromKeystore[BCAddressApi3](businessCustomerAddressName)


  @inline def saveSave4LaterBackup(save4LaterConnector: Save4LaterConnector)(implicit user: AuthContext, hc: HeaderCarrier): Future[CacheMap] = {
    save4LaterConnector.fetchAll(AccountUtils.getUtrOrName()).flatMap {
      case Some(cacheMap) => keyStoreConnector.saveDataToKeystore(save4LaterBackupName, cacheMap.copyOfSave4Later)
      case None => throw new RuntimeException("Backup save4Later to keystore failed, not data found in save4later")
    }
  }

  @inline def fetchSave4LaterBackup(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[CacheMap]] =
    keyStoreConnector.fetchDataFromKeystore[CacheMap](save4LaterBackupName)

  @inline def removeAll(implicit hc: HeaderCarrier): Future[HttpResponse] = keyStoreConnector.removeAll()
}

object KeyStoreService extends KeyStoreService {
  override val keyStoreConnector: KeyStoreConnector = AwrsKeyStoreConnector
}
