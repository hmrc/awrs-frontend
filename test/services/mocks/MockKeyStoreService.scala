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

package services.mocks

import connectors.mock.MockKeyStoreConnector
import models.{StatusNotification, _}
import org.mockito.Mockito._
import org.mockito.{AdditionalMatchers, ArgumentMatchers}
import services.DataCacheKeys._
import services.KeyStoreService
import utils.TestUtil._
import utils.{AwrsUnitTestTraits, TestUtil, WithdrawalTestUtils}

import scala.concurrent.Future

trait MockKeyStoreService extends AwrsUnitTestTraits
  with MockKeyStoreConnector {

  import MockKeyStoreService._

  val testKeyStoreService: KeyStoreService = new KeyStoreService(mockKeyStoreConnector)

  object FetchNoneType extends Enumeration {
    val NoData = Value("NoData")
    val DeletedData = Value("Deleted")
  }

  import FetchNoneType._

  // children must not override this method, update here when KeyStoreService changes
  protected final def mockFetchFromKeyStore[T](key: String, config: MockConfiguration[Future[Option[T]]]): Unit =
  config ifConfiguredThen (dataToReturn => when(mockKeyStoreConnector.fetchDataFromKeystore[T](ArgumentMatchers.eq(key))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(dataToReturn))

  // children must not override this method, update here when KeyStoreService changes
  final def setupMockKeyStoreServiceOnlySaveFunctions(): Unit = {
    when(mockKeyStoreConnector.saveDataToKeystore(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(returnedCacheMap))
  }

  // children can override in order to customise their default settings
  def setupMockKeyStoreServiceForDeRegistrationOrWithdrawal(
                                                             haveDeRegDate: Boolean = true,
                                                             haveDeRegReason: Boolean = true,
                                                             haveWithdrawalReason: Boolean = true,
                                                             fetchNoneType: FetchNoneType.Value = NoData
                                                           ): Unit = {

    def setupFetchNone[T]: Option[Option[T]] =
      (fetchNoneType: @unchecked) match {
        case NoData => None
        case DeletedData => Some(None)
      }

    val deRegDate: Option[Option[DeRegistrationDate]] = if (haveDeRegDate) {
      Some(defaultDeRegistrationDate)
    } else {
      setupFetchNone
    }

    val deRegReason: Option[Option[DeRegistrationReason]] = if (haveDeRegReason) {
      Some(defaultDeRegistrationReason)
    } else {
      setupFetchNone
    }

    val withdrawalReason: Option[Option[WithdrawalReason]] = if (haveWithdrawalReason) {
      Some(defaultWithdrawalReason)
    } else {
      setupFetchNone
    }

    mockFetchFromKeyStore[Option[DeRegistrationDate]](deRegistrationDateName, deRegDate)
    mockFetchFromKeyStore[Option[DeRegistrationReason]](deRegistrationReasonName, deRegReason)
    mockFetchFromKeyStore[Option[WithdrawalReason]](withdrawalReasonName, withdrawalReason)

    setupMockKeyStoreServiceOnlySaveFunctions()
  }

  def setupMockKeystoreServiceForAwrsUrn(urn:Option[AwrsEnrollmentUrn] = None): Unit = {
    mockFetchFromKeyStore[AwrsEnrollmentUrn](awrsEnrollmentUrnKeyName, urn)
    setupMockKeyStoreServiceOnlySaveFunctions()

  }
  def setupMockKeystoreServiceForRegisteredPostcode(postcode: Option[AwrsRegisteredPostcode] = None ): Unit = {
    mockFetchFromKeyStore[AwrsRegisteredPostcode](registeredPostcodeKeyName, postcode)
    setupMockKeyStoreServiceOnlySaveFunctions()
  }

  def setupMockKeyStoreServiceForBusinessCustomerAddress(noAddress: Boolean = false): Unit =
    if (noAddress) {
      setupMockKeyStoreServiceWithOnly(fetchBusinessCustomerAddress = None)
    } else {
      setupMockKeyStoreServiceWithOnly(fetchBusinessCustomerAddress = Some(defaultBCAddressApi3))
    }

  // children can override in order to customise their default settings
  def setupMockKeyStoreService(
                                subscriptionStatusType: Future[Option[SubscriptionStatusType]] = Future.successful(Some(defaultSubscriptionStatusType)),
                                statusInfoType: Future[Option[StatusInfoType]] = Future.successful(Some(defaultStatusTypeInfo)),
                                statusNotification: Future[Option[StatusNotification]] = Future.successful(defaultStatusNotification),
                                fetchIsNewBusiness: Future[Option[Boolean]] = defaultIsNewBusiness,
                                fetchViewedStatus: Future[Option[Boolean]] = defaultViewedStatus,
                                fetchBusinessNameChange: Future[Option[BusinessNameDetails]] = defaultBusinessNameChange,
                                fetchAlreadyTrading: Future[Option[Boolean]] = Future.successful(None)
  ): Unit =
  setupMockKeyStoreServiceWithOnly(
    subscriptionStatusType = subscriptionStatusType,
    statusInfoType = statusInfoType,
    statusNotification = statusNotification,
    fetchIsNewBusiness = fetchIsNewBusiness,
    fetchViewedStatus = fetchViewedStatus,
    fetchBusinessNameChange = fetchBusinessNameChange,
    fetchAlreadyTrading = fetchAlreadyTrading
  )

  // children must not override this method, update here when KeyStoreService changes
  protected final def setupMockKeyStoreServiceWithOnly(
                                                        subscriptionStatusType: MockConfiguration[Future[Option[SubscriptionStatusType]]] = DoNotConfigure,
                                                        statusInfoType: MockConfiguration[Future[Option[StatusInfoType]]] = DoNotConfigure,
                                                        statusNotification: MockConfiguration[Future[Option[StatusNotification]]] = DoNotConfigure,
                                                        fetchIsNewBusiness: MockConfiguration[Future[Option[Boolean]]] = DoNotConfigure,
                                                        fetchViewedStatus: MockConfiguration[Future[Option[Boolean]]] = DoNotConfigure,
                                                        fetchBusinessNameChange: MockConfiguration[Future[Option[BusinessNameDetails]]] = DoNotConfigure,
                                                        fetchBusinessCustomerAddress: MockConfiguration[Future[Option[BCAddressApi3]]] = DoNotConfigure,
                                                        fetchAlreadyTrading: MockConfiguration[Future[Option[Boolean]]] = DoNotConfigure
                                                      ): Unit = {
    mockFetchFromKeyStore[SubscriptionStatusType](subscriptionStatusTypeName, subscriptionStatusType)
    mockFetchFromKeyStore[StatusInfoType](statusInfoTypeName, statusInfoType)
    mockFetchFromKeyStore[StatusNotification](statusNotificationName, statusNotification)
    mockFetchFromKeyStore[Boolean](isNewBusinessName, fetchIsNewBusiness)
    mockFetchFromKeyStore[Boolean](viewedStatusName, fetchViewedStatus)
    mockFetchFromKeyStore[Boolean](alreadyTradingName, fetchAlreadyTrading)
    mockFetchFromKeyStore[BusinessNameDetails](businessNameChangeName, fetchBusinessNameChange)
    mockFetchFromKeyStore[BCAddressApi3](businessCustomerAddressName, fetchBusinessCustomerAddress)

    setupMockKeyStoreServiceOnlySaveFunctions()
  }

  // children must not override this method, update here when KeyStoreService changes
  protected final def verifyKeyStoreService(
                                             fetchDeRegistrationDate: Option[Int] = None,
                                             saveDeRegistrationDate: Option[Int] = None,
                                             deleteDeRegistrationDate: Option[Int] = None,
                                             fetchDeRegistrationReason: Option[Int] = None,
                                             saveDeRegistrationReason: Option[Int] = None,
                                             deleteDeRegistrationReason: Option[Int] = None,
                                             fetchSubscriptionStatusType: Option[Int] = None,
                                             saveSubscriptionStatusType: Option[Int] = None,
                                             fetchStatusInfoType: Option[Int] = None,
                                             saveStatusInfoType: Option[Int] = None,
                                             fetchStatusNotification: Option[Int] = None,
                                             saveStatusNotification: Option[Int] = None,
                                             fetchWithdrawalReason: Option[Int] = None,
                                             saveWithdrawalReason: Option[Int] = None,
                                             fetchViewedStatus: Option[Int] = None,
                                             saveViewedStatus: Option[Int] = None,
                                             fetchBusinessCustomerAddress: Option[Int] = None,
                                             saveBusinessCustomerAddress: Option[Int] = None,
                                             saveSearchResults: Option[Int] = None,
                                             saveAwrsUrn: Option[Int] = None,
                                             saveBusinessCustomerAddress: Option[Int] = None,
                                             saveRegisteredPostcode:  Option[Int] = None,
                                           ): Unit = {
    def verifyDeleteSupportFetch[T](key: String, someCount: Option[Int]): Unit =
      someCount ifDefinedThen (count => verify(mockKeyStoreConnector, times(count)).fetchDataFromKeystore[Option[T]](ArgumentMatchers.eq(key))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))

    def verifyDeleteSupportSave[T](key: String, someCount: Option[Int]): Unit =
      someCount ifDefinedThen (count => verify(mockKeyStoreConnector, times(count)).saveDataToKeystore[Option[T]](ArgumentMatchers.eq(key), AdditionalMatchers.not(ArgumentMatchers.eq(None)))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))

    def verifyDelete[T](key: String, someCount: Option[Int]): Unit =
      someCount ifDefinedThen (count => verify(mockKeyStoreConnector, times(count)).saveDataToKeystore[Option[T]](ArgumentMatchers.eq(key), ArgumentMatchers.eq(None))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))

    def verifyFetch[T](key: String, someCount: Option[Int]): Unit =
      someCount ifDefinedThen (count => verify(mockKeyStoreConnector, times(count)).fetchDataFromKeystore[T](ArgumentMatchers.eq(key))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))

    def verifySave[T](key: String, someCount: Option[Int]): Unit =
      someCount ifDefinedThen (count => verify(mockKeyStoreConnector, times(count)).saveDataToKeystore[T](ArgumentMatchers.eq(key), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))

    verifyDeleteSupportFetch(deRegistrationDateName, fetchDeRegistrationDate)
    verifyDeleteSupportSave(deRegistrationDateName, saveDeRegistrationDate)
    verifyDelete(deRegistrationDateName, deleteDeRegistrationDate)

    verifyDeleteSupportFetch(deRegistrationReasonName, fetchDeRegistrationReason)
    verifyDeleteSupportSave(deRegistrationReasonName, saveDeRegistrationReason)
    verifyDelete(deRegistrationReasonName, deleteDeRegistrationReason)
    verifyFetch(subscriptionStatusTypeName, fetchSubscriptionStatusType)
    verifySave(subscriptionStatusTypeName, saveSubscriptionStatusType)

    verifyFetch(statusInfoTypeName, fetchStatusInfoType)
    verifySave(statusInfoTypeName, saveStatusInfoType)

    verifyFetch(statusNotificationName, fetchStatusNotification)
    verifySave(statusNotificationName, saveStatusNotification)

    verifyFetch(withdrawalReasonName, fetchWithdrawalReason)
    verifySave(withdrawalReasonName, saveWithdrawalReason)

    verifyFetch(viewedStatusName, fetchViewedStatus)
    verifySave(viewedStatusName, saveViewedStatus)

    verifyFetch(businessCustomerAddressName, fetchBusinessCustomerAddress)
    verifySave(businessCustomerAddressName, saveBusinessCustomerAddress)

    verifySave(awrsEnrollmentUrnKeyName, saveAwrsUrn)

    verifySave(awrsEnrollmentSearchResultKeyName, saveSearchResults)

    verifySave(registeredPostcodeKeyName, saveRegisteredPostcode)
  }

}

object MockKeyStoreService {

  val defaultWithdrawalReason = Some(WithdrawalTestUtils.withdrawalReason())

  val defaultDeRegistrationDateData = Some(TestUtil.deRegistrationDate())
  val defaultDeRegistrationReasonData = Some(TestUtil.deRegistrationReason())

  val defaultFetchNoData = None
  val defaultFetchDeletedData = Some(None)
  val defaultDeRegistrationDate: Option[DeRegistrationDate] = defaultDeRegistrationDateData
  val defaultDeRegistrationReason: Option[DeRegistrationReason] = defaultDeRegistrationReasonData

  val deRegistrationSuccessData = Some(TestUtil.deRegistrationType(true))
  val deRegistrationFailureData = Some(TestUtil.deRegistrationType(false))

  val defaultDeEnrollResponseSuccessData = true

  val defaultSubscriptionStatusType = testSubscriptionStatusTypeApprovedWithConditions
  val defaultStatusTypeInfo = testStatusInfoTypeApprovedWithConditions
  val defaultStatusNotification = testStatusNotificationNoAlert

  val defaultExtendedBusinessDetails = testExtendedBusinessDetails()

  val defaultBusinessNameChange = testBusinessNameDetails()

  val defaultIsNewBusiness = true

  val defaultViewedStatus = true

  val defaultBCAddressApi3 = testBCAddressApi3()
}
