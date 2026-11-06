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

import _root_.models.{AwrsPostcodeModel, _}
import connectors.{AwrsKeyStoreConnector, Save4LaterConnector}
import controllers.auth.StandardAuthRetrievals
import models.reenrolment.KnownFactsResponse
import services.DataCacheKeys.*
import uk.gov.hmrc.http.HeaderCarrier
import models.CacheMap
import utils.AccountUtils
import utils.CacheUtil.cacheUtil

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class KeyStoreService @Inject()(keyStoreConnector: AwrsKeyStoreConnector) {

  @inline def saveDeRegistrationDate(deRegistration: DeRegistrationDate)(using hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    saveDeRegistrationDate(Some(deRegistration))

  @inline def saveDeRegistrationDate(deRegistration: Option[DeRegistrationDate])(using hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[Option[DeRegistrationDate]](deRegistrationDateName, deRegistration)

  @inline def fetchDeRegistrationDate(using hc: HeaderCarrier, ec: ExecutionContext): Future[Option[DeRegistrationDate]] =
    keyStoreConnector.fetchDataFromKeystore[Option[DeRegistrationDate]](deRegistrationDateName) flatMap {
      case Some(someData@Some(_)) => Future.successful(someData)
      case _ => Future.successful(None)
    }

  @inline def deleteDeRegistrationDate(using hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    saveDeRegistrationDate(None)

  @inline def saveWithdrawalReason(withdrawal: Option[WithdrawalReason])(using hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[Option[WithdrawalReason]](withdrawalReasonName, withdrawal)

  @inline def saveWithdrawalReason(withdrawal: WithdrawalReason)(using hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    saveWithdrawalReason(Some(withdrawal))

  @inline def fetchWithdrawalReason()(using hc: HeaderCarrier, ec: ExecutionContext): Future[Option[WithdrawalReason]] =
    keyStoreConnector.fetchDataFromKeystore[Option[WithdrawalReason]](withdrawalReasonName) flatMap {
      case Some(someData@Some(_)) => Future.successful(someData)
      case _ => Future.successful(None)
    }

  @inline def deleteWithdrawalReason()(using hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    saveWithdrawalReason(None)

  @inline def saveDeRegistrationReason(deRegistration: DeRegistrationReason)(using hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    saveDeRegistrationReason(Some(deRegistration))

  @inline def saveDeRegistrationReason(deRegistration: Option[DeRegistrationReason])(using hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[Option[DeRegistrationReason]](deRegistrationReasonName, deRegistration)

  @inline def fetchDeRegistrationReason(using hc: HeaderCarrier, ec: ExecutionContext): Future[Option[DeRegistrationReason]] =
    keyStoreConnector.fetchDataFromKeystore[Option[DeRegistrationReason]](deRegistrationReasonName) flatMap {
      case Some(someData@Some(data)) if data.deregistrationReason.isDefined => Future.successful(someData)
      case _ => Future.successful(None)
    }

  @inline def deleteDeRegistrationReason(using hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    saveDeRegistrationReason(None)

  @inline def saveSubscriptionStatus(subscriptionStatusType: SubscriptionStatusType)(using hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[SubscriptionStatusType](subscriptionStatusTypeName, subscriptionStatusType)

  @inline def fetchSubscriptionStatus(using hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SubscriptionStatusType]] =
    keyStoreConnector.fetchDataFromKeystore[SubscriptionStatusType](subscriptionStatusTypeName) flatMap {
      (successData: Option[SubscriptionStatusType]) => Future.successful(successData)
    }

  @inline def saveStatusInfo(statusInfoType: StatusInfoType)(using hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[StatusInfoType](statusInfoTypeName, statusInfoType)

  @inline def fetchStatusInfo(using hc: HeaderCarrier, ec: ExecutionContext): Future[Option[StatusInfoType]] =
    keyStoreConnector.fetchDataFromKeystore[StatusInfoType](statusInfoTypeName) flatMap {
      (successData: Option[StatusInfoType]) => Future.successful(successData)
    }

  @inline def saveStatusNotification(notificationStatus: StatusNotification)(using hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[StatusNotification](statusNotificationName, notificationStatus)

  @inline def fetchStatusNotification(using hc: HeaderCarrier, ec: ExecutionContext): Future[Option[StatusNotification]] =
    keyStoreConnector.fetchDataFromKeystore[StatusNotification](statusNotificationName)

  @inline def saveIsNewBusiness(isNewBusiness: Boolean)(using hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[Boolean](isNewBusinessName, isNewBusiness)

  @inline def fetchIsNewBusiness(using hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Boolean]] =
    keyStoreConnector.fetchDataFromKeystore[Boolean](isNewBusinessName)

  @inline def saveViewedStatus(wasViewed: Boolean)(using hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[Boolean](viewedStatusName, wasViewed)

  @inline def fetchViewedStatus(using hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Boolean]] =
    keyStoreConnector.fetchDataFromKeystore[Boolean](viewedStatusName)

  @inline def saveBusinessNameChange(newName: BusinessNameDetails)(using hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[BusinessNameDetails](businessNameChangeName, newName)

  @inline def fetchBusinessNameChange(using hc: HeaderCarrier, ec: ExecutionContext): Future[Option[BusinessNameDetails]] =
    keyStoreConnector.fetchDataFromKeystore[BusinessNameDetails](businessNameChangeName)

  @inline def saveAlreadyTrading(already: Boolean)(using hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[Boolean](alreadyTradingName, already)

  @inline def fetchAlreadyTrading(using hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Boolean]] =
    keyStoreConnector.fetchDataFromKeystore[Boolean](alreadyTradingName)

  @inline def saveBusinessCustomerAddress(businessCustomerAddress: BCAddressApi3)(using hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[BCAddressApi3](businessCustomerAddressName, businessCustomerAddress)

  @inline def fetchBusinessCustomerAddresss(using hc: HeaderCarrier, ec: ExecutionContext): Future[Option[BCAddressApi3]] =
    keyStoreConnector.fetchDataFromKeystore[BCAddressApi3](businessCustomerAddressName)


  @inline def saveSave4LaterBackup(save4LaterConnector: Save4LaterConnector, authRetrievals: StandardAuthRetrievals, accountUtils: AccountUtils)
                                  (using hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] = {
    save4LaterConnector
      .fetchAll(accountUtils.getUtr(authRetrievals)).flatMap {
      case Some(cacheMap) => keyStoreConnector.saveDataToKeystore(save4LaterBackupName, cacheMap.copyOfSave4Later)
      case None => throw new RuntimeException("Backup save4Later to keystore failed, not data found in save4later")
    }
  }

  @inline def fetchSave4LaterBackup(using hc: HeaderCarrier, ec: ExecutionContext): Future[Option[CacheMap]] =
    keyStoreConnector.fetchDataFromKeystore[CacheMap](save4LaterBackupName)

  @inline def removeAll(using hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = keyStoreConnector.removeAll()

  @inline def fetchAwrsEnrolmentUrn(using hc: HeaderCarrier, ec: ExecutionContext): Future[Option[AwrsEnrolmentUrn]] =
    keyStoreConnector.fetchDataFromKeystore[AwrsEnrolmentUrn](awrsEnrolmentUrnKeyName)

  @inline def saveAwrsEnrolmentUrn(awrsEnrolmentUrn: AwrsEnrolmentUrn)(using hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[AwrsEnrolmentUrn](awrsEnrolmentUrnKeyName, awrsEnrolmentUrn)

  @inline def fetchAwrsEnrolmentUtr(using hc: HeaderCarrier, ec: ExecutionContext): Future[Option[AwrsEnrolmentUtr]] =
    keyStoreConnector.fetchDataFromKeystore[AwrsEnrolmentUtr](awrsEnrolmentUtrKeyName)

  @inline def saveAwrsEnrolmentUtr(awrsEnrolmentUtr: AwrsEnrolmentUtr)(using hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[AwrsEnrolmentUtr](awrsEnrolmentUtrKeyName, awrsEnrolmentUtr)

  @inline def fetchKnownFacts(using hc: HeaderCarrier, ec: ExecutionContext): Future[Option[KnownFactsResponse]] =
    keyStoreConnector.fetchDataFromKeystore[KnownFactsResponse](awrsKnownFactsResponseKeyName)

  @inline def saveKnownFacts(knownFactsResponse: KnownFactsResponse)(using hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[KnownFactsResponse](awrsKnownFactsResponseKeyName, knownFactsResponse)

  @inline def saveAwrsRegisteredPostcode(postcode: AwrsPostcodeModel)(using hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[AwrsPostcodeModel](registeredPostcodeKeyName, postcode)

  @inline def fetchAwrsRegisteredPostcode(using hc: HeaderCarrier, ec: ExecutionContext): Future[Option[AwrsPostcodeModel]] =
    keyStoreConnector.fetchDataFromKeystore[AwrsPostcodeModel](registeredPostcodeKeyName)

  @inline def fetchHaveYouRegistered(using hc: HeaderCarrier, ec: ExecutionContext): Future[Option[HaveYouRegisteredModel]] = {
    keyStoreConnector.fetchDataFromKeystore[HaveYouRegisteredModel](enrolmentJourneyHaveYouRegisteredKeyName)
  }

  @inline def saveHaveYouRegistered(haveYouRegisteredModel: HaveYouRegisteredModel)(using hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    keyStoreConnector.saveDataToKeystore[HaveYouRegisteredModel](enrolmentJourneyHaveYouRegisteredKeyName, haveYouRegisteredModel)
}
