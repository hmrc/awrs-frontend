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

package services

import _root_.models._
import connectors.{AwrsKeyStoreConnector, Save4LaterConnector}
import controllers.auth.StandardAuthRetrievals
import services.DataCacheKeys._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AccountUtils
import utils.CacheUtil.cacheUtil

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class KeyStoreService @Inject()(keyStoreConnector: AwrsKeyStoreConnector) {

  @inline def saveDeRegistrationDate(deRegistration: DeRegistrationDate)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    saveDeRegistrationDate(Some(deRegistration))

  @inline def saveDeRegistrationDate(deRegistration: Option[DeRegistrationDate])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[Option[DeRegistrationDate]](deRegistrationDateName, deRegistration)

  @inline def fetchDeRegistrationDate(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[DeRegistrationDate]] =
    keyStoreConnector.fetchDataFromKeystore[Option[DeRegistrationDate]](deRegistrationDateName) flatMap {
      case Some(someData@Some(data)) => Future.successful(someData)
      case _ => Future.successful(None)
    }

  @inline def deleteDeRegistrationDate(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    saveDeRegistrationDate(None)

  @inline def saveWithdrawalReason(withdrawal: Option[WithdrawalReason])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[Option[WithdrawalReason]](withdrawalReasonName, withdrawal)

  @inline def saveWithdrawalReason(withdrawal: WithdrawalReason)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    saveWithdrawalReason(Some(withdrawal))

  @inline def fetchWithdrawalReason()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[WithdrawalReason]] =
    keyStoreConnector.fetchDataFromKeystore[Option[WithdrawalReason]](withdrawalReasonName) flatMap {
      case Some(someData@Some(data)) => Future.successful(someData)
      case _ => Future.successful(None)
    }

  @inline def deleteWithdrawalReason()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    saveWithdrawalReason(None)

  @inline def saveDeRegistrationReason(deRegistration: DeRegistrationReason)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    saveDeRegistrationReason(Some(deRegistration))

  @inline def saveDeRegistrationReason(deRegistration: Option[DeRegistrationReason])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[Option[DeRegistrationReason]](deRegistrationReasonName, deRegistration)

  @inline def fetchDeRegistrationReason(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[DeRegistrationReason]] =
    keyStoreConnector.fetchDataFromKeystore[Option[DeRegistrationReason]](deRegistrationReasonName) flatMap {
      case Some(someData@Some(data)) if data.deregistrationReason.isDefined => Future.successful(someData)
      case _ => Future.successful(None)
    }

  @inline def deleteDeRegistrationReason(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    saveDeRegistrationReason(None)

  @inline def saveSubscriptionStatus(subscriptionStatusType: SubscriptionStatusType)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[SubscriptionStatusType](subscriptionStatusTypeName, subscriptionStatusType)

  @inline def fetchSubscriptionStatus(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SubscriptionStatusType]] =
    keyStoreConnector.fetchDataFromKeystore[SubscriptionStatusType](subscriptionStatusTypeName) flatMap {
      successData: Option[SubscriptionStatusType] => Future.successful(successData)
    }

  @inline def saveStatusInfo(statusInfoType: StatusInfoType)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[StatusInfoType](statusInfoTypeName, statusInfoType)

  @inline def fetchStatusInfo(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[StatusInfoType]] =
    keyStoreConnector.fetchDataFromKeystore[StatusInfoType](statusInfoTypeName) flatMap {
      successData: Option[StatusInfoType] => Future.successful(successData)
    }

  @inline def saveStatusNotification(notificationStatus: StatusNotification)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[StatusNotification](statusNotificationName, notificationStatus)

  @inline def fetchStatusNotification(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[StatusNotification]] =
    keyStoreConnector.fetchDataFromKeystore[StatusNotification](statusNotificationName)

  @inline def saveIsNewBusiness(isNewBusiness: Boolean)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[Boolean](isNewBusinessName, isNewBusiness)

  @inline def fetchIsNewBusiness(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Boolean]] =
    keyStoreConnector.fetchDataFromKeystore[Boolean](isNewBusinessName)

  @inline def saveViewedStatus(wasViewed: Boolean)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[Boolean](viewedStatusName, wasViewed)

  @inline def fetchViewedStatus(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Boolean]] =
    keyStoreConnector.fetchDataFromKeystore[Boolean](viewedStatusName)

  @inline def saveBusinessNameChange(newName: BusinessNameDetails)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[BusinessNameDetails](businessNameChangeName, newName)

  @inline def fetchBusinessNameChange(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[BusinessNameDetails]] =
    keyStoreConnector.fetchDataFromKeystore[BusinessNameDetails](businessNameChangeName)

  @inline def saveAlreadyTrading(already: Boolean)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[Boolean](alreadyTradingName, already)

  @inline def fetchAlreadyTrading(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Boolean]] =
    keyStoreConnector.fetchDataFromKeystore[Boolean](alreadyTradingName)

  @inline def saveBusinessCustomerAddress(businessCustomerAddress: BCAddressApi3)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[BCAddressApi3](businessCustomerAddressName, businessCustomerAddress)

  @inline def fetchBusinessCustomerAddresss(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[BCAddressApi3]] =
    keyStoreConnector.fetchDataFromKeystore[BCAddressApi3](businessCustomerAddressName)


  @inline def saveSave4LaterBackup(save4LaterConnector: Save4LaterConnector, authRetrievals: StandardAuthRetrievals, accountUtils: AccountUtils)
                                  (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] = {
    save4LaterConnector
      .fetchAll(accountUtils.getUtr(authRetrievals)).flatMap {
      case Some(cacheMap) => keyStoreConnector.saveDataToKeystore(save4LaterBackupName, cacheMap.copyOfSave4Later)
      case None => throw new RuntimeException("Backup save4Later to keystore failed, not data found in save4later")
    }
  }

  @inline def fetchSave4LaterBackup(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[CacheMap]] =
    keyStoreConnector.fetchDataFromKeystore[CacheMap](save4LaterBackupName)

  @inline def removeAll(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = keyStoreConnector.removeAll()

  @inline def fetchAwrsEnrolmentUrn(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[AwrsEnrollmentUrn]] =
    keyStoreConnector.fetchDataFromKeystore[AwrsEnrollmentUrn](awrsEnrollmentUrnKeyName)

  @inline def saveAwrsEnrolmentUrn(awrsEnrollmentUrn: AwrsEnrollmentUrn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[AwrsEnrollmentUrn](awrsEnrollmentUrnKeyName, awrsEnrollmentUrn)

  @inline def fetchAwrsUrnSearchResult(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SearchResult]] =
    keyStoreConnector.fetchDataFromKeystore[SearchResult](awrsEnrollmentSearchResultKeyName)

  @inline def saveAwrsUrnSearchResult(searchResult: SearchResult)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[SearchResult](awrsEnrollmentSearchResultKeyName, searchResult)
}
