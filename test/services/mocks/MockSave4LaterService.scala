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

import connectors.Save4LaterConnector
import connectors.mock.MockSave4LaterConnector
import models.BusinessDetailsEntityTypes._
import models._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import play.api.http.Status._
import services.DataCacheKeys._
import services.Save4LaterService
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AwrsUnitTestTraits
import utils.TestUtil._

import scala.concurrent.Future

trait MockSave4LaterService extends AwrsUnitTestTraits
  with MockSave4LaterConnector {

  import MockSave4LaterService._

  val testSave4LaterService = new Save4LaterService(mockMainStoreSave4LaterConnector, mockApiSave4LaterConnector, mockAccountUtils)

  // children must not override this method, update here when Save4LaterService changes
  protected final def mockFetchFromSave4Later[T](key: String, config: MockConfiguration[Future[Option[T]]])(implicit connector: Save4LaterConnector): Unit =
  config ifConfiguredThen (dataToReturn => when(connector.fetchData4Later[T](ArgumentMatchers.any(), ArgumentMatchers.eq(key))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(dataToReturn))

  // children must not override this method, update here when Save4LaterService changes
  protected final def setupMockSave4LaterServiceOnlySaveFunctions(): Unit = {
    val defaultSaveMock = new Answer[Future[Option[Any]]] {
      def answer(invocation: InvocationOnMock) = {
        val firstArg: Any = invocation.getArguments.drop(2).head.asInstanceOf[Any]
        Future.successful(Some(firstArg))
      }
    }

    when(mockMainStoreSave4LaterConnector.saveData4Later(any(), any(), any())(any(), any(), any())).thenAnswer(defaultSaveMock)
    when(mockApiSave4LaterConnector.saveData4Later(any(), any(), any())(any(), any(), any())).thenAnswer(defaultSaveMock)
  }

  // internal function for this trait only, must not be made visable to children
  private final def verifySave4LaterFetch[T](key: String, someCount: Option[Int])(implicit connector: Save4LaterConnector): Unit =
  someCount ifDefinedThen (count => verify(connector, times(count)).fetchData4Later[T](ArgumentMatchers.any(), ArgumentMatchers.eq(key))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))

  // internal function for this trait only, must not be made visable to children
  private final def verifySave4LaterSave[T](key: String, someCount: Option[Int])(implicit connector: Save4LaterConnector): Unit =
  someCount ifDefinedThen (count => verify(connector, times(count)).saveData4Later[T](ArgumentMatchers.any(), ArgumentMatchers.eq(key), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))

  // children can override in order to customise their default settings
  def setupMockSave4LaterService(
                                  fetchApplicationStatus: Future[Option[ApplicationStatus]] = defaultApplicationStatus,
                                  fetchBusinessType: Future[Option[BusinessType]] = defaultBusinessType,
                                  fetchBusinessCustomerDetails: Future[Option[BusinessCustomerDetails]] = defaultBusinessCustomerDetails,
                                  fetchNewApplicationType: Future[Option[NewApplicationType]] = defaultNewApplicationType,
                                  fetchGroupMemberDetails: Future[Option[GroupMembers]] = defaultGroupMemberDetails,
                                  fetchGroupDeclaration: Future[Option[GroupDeclaration]] = defaultGroupDeclaration,
                                  fetchBusinessDirectors: Future[Option[BusinessDirectors]] = defaultBusinessDirectors,
                                  fetchPartnerDetails: Future[Option[Partners]] = defaultPartnerDetails,
                                  fetchAdditionalBusinessPremisesList: Future[Option[AdditionalBusinessPremisesList]] = defaultAdditionalBusinessPremisesList,
                                  fetchTradingActivity: Future[Option[TradingActivity]] = defaultTradingActivity,
                                  fetchProducts: Future[Option[Products]] = defaultProducts,
                                  fetchSuppliers: Future[Option[Suppliers]] = defaultSuppliers,
                                  fetchApplicationDeclaration: Future[Option[ApplicationDeclaration]] = defaultApplicationDeclaration,
                                  fetchBusinessDetails: Future[Option[BusinessDetails]] = defaultBusinessDetails,
                                  fetchBusinessContacts: Future[Option[BusinessContacts]] = defaultBusinessContacts,
                                  fetchPlaceOfBusiness: Future[Option[PlaceOfBusiness]] = defaultPlaceOfBusiness,
                                  fetchAll: Future[Option[CacheMap]] = defaultFetchAll,
                                  removeAll: Future[HttpResponse] = defaultRemoveAll
                                ): Unit =
  setupMockSave4LaterServiceWithOnly(
    fetchApplicationStatus = fetchApplicationStatus,
    fetchBusinessType = fetchBusinessType,
    fetchBusinessCustomerDetails = fetchBusinessCustomerDetails,
    fetchNewApplicationType = fetchNewApplicationType,
    fetchGroupMemberDetails = fetchGroupMemberDetails,
    fetchGroupDeclaration = fetchGroupDeclaration,
    fetchBusinessDirectors = fetchBusinessDirectors,
    fetchPartnerDetails = fetchPartnerDetails,
    fetchAdditionalBusinessPremisesList = fetchAdditionalBusinessPremisesList,
    fetchTradingActivity = fetchTradingActivity,
    fetchProducts = fetchProducts,
    fetchSuppliers = fetchSuppliers,
    fetchApplicationDeclaration = fetchApplicationDeclaration,
    fetchBusinessDetails = fetchBusinessDetails,
    fetchBusinessContacts = fetchBusinessContacts,
    fetchPlaceOfBusiness = fetchPlaceOfBusiness,
    fetchAll = fetchAll,
    removeAll = removeAll
  )

  // children must not override this method, update here when Save4LaterService changes
  protected final def setupMockSave4LaterServiceWithOnly(
                                                          fetchApplicationStatus: MockConfiguration[Future[Option[ApplicationStatus]]] = DoNotConfigure,
                                                          fetchBusinessType: MockConfiguration[Future[Option[BusinessType]]] = DoNotConfigure,
                                                          fetchBusinessCustomerDetails: MockConfiguration[Future[Option[BusinessCustomerDetails]]] = DoNotConfigure,
                                                          fetchNewApplicationType: MockConfiguration[Future[Option[NewApplicationType]]] = DoNotConfigure,
                                                          fetchGroupMemberDetails: MockConfiguration[Future[Option[GroupMembers]]] = DoNotConfigure,
                                                          fetchGroupDeclaration: MockConfiguration[Future[Option[GroupDeclaration]]] = DoNotConfigure,
                                                          fetchBusinessDirectors: MockConfiguration[Future[Option[BusinessDirectors]]] = DoNotConfigure,
                                                          fetchPartnerDetails: MockConfiguration[Future[Option[Partners]]] = DoNotConfigure,
                                                          fetchAdditionalBusinessPremisesList: MockConfiguration[Future[Option[AdditionalBusinessPremisesList]]] = DoNotConfigure,
                                                          fetchTradingActivity: MockConfiguration[Future[Option[TradingActivity]]] = DoNotConfigure,
                                                          fetchProducts: MockConfiguration[Future[Option[Products]]] = DoNotConfigure,
                                                          fetchSuppliers: MockConfiguration[Future[Option[Suppliers]]] = DoNotConfigure,
                                                          fetchApplicationDeclaration: MockConfiguration[Future[Option[ApplicationDeclaration]]] = DoNotConfigure,
                                                          fetchBusinessDetails: MockConfiguration[Future[Option[BusinessDetails]]] = DoNotConfigure,
                                                          fetchBusinessRegistrationDetails: MockConfiguration[Future[Option[BusinessRegistrationDetails]]] = DoNotConfigure,
                                                          fetchBusinessContacts: MockConfiguration[Future[Option[BusinessContacts]]] = DoNotConfigure,
                                                          fetchPlaceOfBusiness: MockConfiguration[Future[Option[PlaceOfBusiness]]] = DoNotConfigure,
                                                          fetchAll: MockConfiguration[Future[Option[CacheMap]]] = DoNotConfigure,
                                                          removeAll: MockConfiguration[Future[Unit]] = DoNotConfigure
                                                        ): Unit = {
    implicit val connector: Save4LaterConnector = mockMainStoreSave4LaterConnector

    mockFetchFromSave4Later(applicationStatusName, fetchApplicationStatus)
    mockFetchFromSave4Later(businessTypeName, fetchBusinessType)
    mockFetchFromSave4Later(businessCustomerDetailsName, fetchBusinessCustomerDetails)(connector)
    mockFetchFromSave4Later(newApplicationTypeName, fetchNewApplicationType)
    mockFetchFromSave4Later(groupMembersName, fetchGroupMemberDetails)
    mockFetchFromSave4Later(groupDeclarationName, fetchGroupDeclaration)
    mockFetchFromSave4Later(businessDirectorsName, fetchBusinessDirectors)(connector)
    mockFetchFromSave4Later(partnersName, fetchPartnerDetails)
    mockFetchFromSave4Later(additionalBusinessPremisesName, fetchAdditionalBusinessPremisesList)
    mockFetchFromSave4Later(tradingActivityName, fetchTradingActivity)
    mockFetchFromSave4Later(productsName, fetchProducts)
    mockFetchFromSave4Later(suppliersName, fetchSuppliers)
    mockFetchFromSave4Later(applicationDeclarationName, fetchApplicationDeclaration)
    mockFetchFromSave4Later(businessDetailsName, fetchBusinessDetails)
    mockFetchFromSave4Later(businessRegistrationDetailsName, fetchBusinessRegistrationDetails)
    mockFetchFromSave4Later(businessContactsName, fetchBusinessContacts)
    mockFetchFromSave4Later(placeOfBusinessName, fetchPlaceOfBusiness)

    fetchAll ifConfiguredThen (dataCache => when(connector.fetchAll(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(dataCache))
    removeAll ifConfiguredThen (response => when(connector.removeAll(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(response))

    setupMockSave4LaterServiceOnlySaveFunctions()
  }

  // children can override in order to customise their default settings
  def setupMockApiSave4LaterService(
                                     fetchSubscriptionTypeFrontEnd: Future[Option[SubscriptionTypeFrontEnd]] = defaultSubscriptionTypeFrontEnd,
                                     fetchBusinessDetailsSupport: Future[Option[BusinessDetailsSupport]] = defaultBusinessDetailsSupport,
                                     removeAll: Future[HttpResponse] = defaultRemoveAll
                                   ): Unit =
  setupMockApiSave4LaterServiceWithOnly(
    fetchSubscriptionTypeFrontEnd = fetchSubscriptionTypeFrontEnd,
    fetchBusinessDetailsSupport = fetchBusinessDetailsSupport,
    removeAll = removeAll
  )

  // children must not override this method, update here when Save4LaterService changes
  protected final def setupMockApiSave4LaterServiceWithOnly(
                                                             fetchSubscriptionTypeFrontEnd: MockConfiguration[Future[Option[SubscriptionTypeFrontEnd]]] = DoNotConfigure,
                                                             fetchBusinessDetailsSupport: MockConfiguration[Future[Option[BusinessDetailsSupport]]] = DoNotConfigure,
                                                             removeAll: MockConfiguration[Future[Unit]] = DoNotConfigure
                                                           ): Unit = {
    implicit val connector: Save4LaterConnector = mockApiSave4LaterConnector

    mockFetchFromSave4Later(subscriptionTypeFrontEndName, fetchSubscriptionTypeFrontEnd)
    mockFetchFromSave4Later(businessDetailsSupportName, fetchBusinessDetailsSupport)
    removeAll ifConfiguredThen (response => when(connector.removeAll(any())(any(), any())).thenReturn(response))

    setupMockSave4LaterServiceOnlySaveFunctions()
  }

  // children must not override this method, update here when Save4LaterService changes
  protected final def verifySave4LaterService(
                                               fetchBusinessType: Option[Int] = None,
                                               saveBusinessType: Option[Int] = None,
                                               fetchBusinessCustomerDetails: Option[Int] = None,
                                               saveBusinessCustomerDetails: Option[Int] = None,
                                               fetchNewApplicationType: Option[Int] = None,
                                               saveNewApplicationType: Option[Int] = None,
                                               fetchGroupMemberDetails: Option[Int] = None,
                                               saveGroupMemberDetails: Option[Int] = None,
                                               fetchGroupDeclaration: Option[Int] = None,
                                               saveGroupDeclaration: Option[Int] = None,
                                               fetchBusinessDirectors: Option[Int] = None,
                                               saveBusinessDirectors: Option[Int] = None,
                                               fetchPartnerDetails: Option[Int] = None,
                                               savePartnerDetails: Option[Int] = None,
                                               fetchAdditionalBusinessPremisesList: Option[Int] = None,
                                               saveAdditionalBusinessPremisesList: Option[Int] = None,
                                               fetchTradingActivity: Option[Int] = None,
                                               saveTradingActivity: Option[Int] = None,
                                               fetchProducts: Option[Int] = None,
                                               saveProducts: Option[Int] = None,
                                               fetchSuppliers: Option[Int] = None,
                                               saveSuppliers: Option[Int] = None,
                                               fetchApplicationDeclaration: Option[Int] = None,
                                               saveApplicationDeclaration: Option[Int] = None,
                                               fetchBusinessDetails: Option[Int] = None,
                                               saveBusinessDetails: Option[Int] = None,
                                               fetchBusinessRegistrationDetails: Option[Int] = None,
                                               saveBusinessRegistrationDetails: Option[Int] = None,
                                               fetchBusinessContacts: Option[Int] = None,
                                               saveBusinessContacts: Option[Int] = None,
                                               fetchPlaceOfBusiness: Option[Int] = None,
                                               savePlaceOfBusiness: Option[Int] = None,
                                               fetchAll: Option[Int] = None,
                                               removeAll: Option[Int] = None
                                             ): Unit = {

    implicit val connector: Save4LaterConnector = mockMainStoreSave4LaterConnector

    verifySave4LaterFetch(businessTypeName, fetchBusinessType)
    verifySave4LaterSave(businessTypeName, saveBusinessType)
    verifySave4LaterFetch(businessCustomerDetailsName, fetchBusinessCustomerDetails)
    verifySave4LaterSave(businessCustomerDetailsName, saveBusinessCustomerDetails)
    verifySave4LaterFetch(newApplicationTypeName, fetchNewApplicationType)
    verifySave4LaterSave(newApplicationTypeName, saveNewApplicationType)
    verifySave4LaterFetch(groupMembersName, fetchGroupMemberDetails)
    verifySave4LaterSave(groupMembersName, saveGroupMemberDetails)
    verifySave4LaterFetch(groupDeclarationName, fetchGroupDeclaration)
    verifySave4LaterSave(groupDeclarationName, saveGroupDeclaration)
    verifySave4LaterFetch(businessDirectorsName, fetchBusinessDirectors)
    verifySave4LaterSave(businessDirectorsName, saveBusinessDirectors)
    verifySave4LaterFetch(partnersName, fetchPartnerDetails)
    verifySave4LaterSave(partnersName, savePartnerDetails)
    verifySave4LaterFetch(additionalBusinessPremisesName, fetchAdditionalBusinessPremisesList)
    verifySave4LaterSave(additionalBusinessPremisesName, saveAdditionalBusinessPremisesList)
    verifySave4LaterFetch(tradingActivityName, fetchTradingActivity)
    verifySave4LaterSave(tradingActivityName, saveTradingActivity)
    verifySave4LaterFetch(productsName, fetchProducts)
    verifySave4LaterSave(productsName, saveProducts)
    verifySave4LaterFetch(suppliersName, fetchSuppliers)
    verifySave4LaterSave(suppliersName, saveSuppliers)
    verifySave4LaterFetch(applicationDeclarationName, fetchApplicationDeclaration)
    verifySave4LaterSave(applicationDeclarationName, saveApplicationDeclaration)
    verifySave4LaterFetch(businessDetailsName, fetchBusinessDetails)
    verifySave4LaterSave(businessDetailsName, saveBusinessDetails)
    verifySave4LaterFetch(businessRegistrationDetailsName, fetchBusinessRegistrationDetails)
    verifySave4LaterSave(businessRegistrationDetailsName, saveBusinessRegistrationDetails)
    verifySave4LaterFetch(businessContactsName, fetchBusinessContacts)
    verifySave4LaterSave(businessContactsName, saveBusinessContacts)
    verifySave4LaterFetch(placeOfBusinessName, fetchPlaceOfBusiness)
    verifySave4LaterSave(placeOfBusinessName, savePlaceOfBusiness)
    fetchAll ifDefinedThen (count => verify(connector, times(count)).fetchAll(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
    removeAll ifDefinedThen (count => verify(connector, times(count)).removeAll(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
  }

  // children must not override this method, update here when Save4LaterService changes
  protected final def verifyApiSave4LaterService(
                                                  fetchSubscriptionTypeFrontEnd: Option[Int] = None,
                                                  saveSubscriptionTypeFrontEnd: Option[Int] = None,
                                                  fetchBusinessDetailsSupport: Option[Int] = None,
                                                  saveBusinessDetailsSupport: Option[Int] = None,
                                                  fetchAll: Option[Int] = None,
                                                  removeAll: Option[Int] = None
                                                ): Unit = {

    implicit val connector: Save4LaterConnector = mockApiSave4LaterConnector

    verifySave4LaterFetch(subscriptionTypeFrontEndName, fetchSubscriptionTypeFrontEnd)
    verifySave4LaterSave(subscriptionTypeFrontEndName, saveSubscriptionTypeFrontEnd)
    verifySave4LaterFetch(businessDetailsSupportName, fetchBusinessDetailsSupport)
    verifySave4LaterSave(businessDetailsSupportName, saveBusinessDetailsSupport)
    fetchAll ifDefinedThen (count => verify(connector, times(count)).fetchAll(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
    removeAll ifDefinedThen (count => verify(connector, times(count)).removeAll(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
  }

}

object MockSave4LaterService {

  val defaultApplicationStatus: ApplicationStatus = testApplicationStatus()
  val defaultBusinessType: BusinessType = testBusinessDetailsEntityTypes(SoleTrader)
  val defaultBusinessCustomerDetails: BusinessCustomerDetails = testBusinessCustomerDetails("SOP")
  val defaultNewApplicationType: NewApplicationType = testNewApplicationType
  val defaultGroupMemberDetails: GroupMembers = testGroupMemberDetails
  val defaultGroupDeclaration: GroupDeclaration = testGroupDeclaration
  val defaultBusinessDirectors: BusinessDirectors = testBusinessDirectors
  val defaultPartnerDetails: Partners = testPartnerDetails
  val defaultAdditionalBusinessPremisesList: AdditionalBusinessPremisesList = testAdditionalPremisesList
  val defaultTradingActivity: TradingActivity = testTradingActivity()
  val defaultProducts: Products = testProducts()
  val defaultSuppliers: Suppliers = testSuppliers
  val defaultApplicationDeclaration: ApplicationDeclaration = testApplicationDeclaration
  val defaultBusinessDetails: BusinessDetails = testBusinessDetails()
  val defaultBusinessContacts: BusinessContacts = testBusinessContactsDefault()
  val defaultPlaceOfBusiness: PlaceOfBusiness = testPlaceOfBusinessDefault()

  // api datacache
  val defaultSubscriptionTypeFrontEnd: SubscriptionTypeFrontEnd = testSubscriptionTypeFrontEnd()
  val defaultBusinessDetailsSupport: BusinessDetailsSupport = testBusinessDetailsSupport(false)

  //
  val defaultFetchAll: CacheMap = createCacheMap("SOP")
  val defaultRemoveAll: Future[HttpResponse] = Future.successful(HttpResponse.apply(OK, ""))
}
