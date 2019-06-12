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

import _root_.models._
import connectors.{AwrsAPIDataCacheConnector, AwrsDataCacheConnector, Save4LaterConnector}
import controllers.auth.StandardAuthRetrievals
import services.DataCacheKeys._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.AccountUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait Save4LaterUtil {

  val save4LaterConnector: Save4LaterConnector

  @inline def fetchData4Later[T](formId: String, authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier, formats: play.api.libs.json.Format[T]): Future[Option[T]] = {
    save4LaterConnector.fetchData4Later[T](AccountUtils.getS4LCacheID(authRetrievals.enrolments), formId)
  }

  @inline def saveData4Later[T](formId: String, data: T, authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier, formats: play.api.libs.json.Format[T]): Future[T] =
    save4LaterConnector.saveData4Later[T](AccountUtils.getS4LCacheID(authRetrievals.enrolments), formId, data) map (_.get)

  @inline def fetchAll(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[Option[CacheMap]] =
    save4LaterConnector.fetchAll(AccountUtils.getS4LCacheID(authRetrievals.enrolments))

  @inline def removeAll(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    save4LaterConnector.removeAll(AccountUtils.getS4LCacheID(authRetrievals.enrolments))
}

trait Save4LaterService {

  val mainStoreSave4LaterConnector: Save4LaterConnector
  val apiSave4LaterConnector: Save4LaterConnector


  implicit def convertUtil[T](f: Option[T]): T = f.get

  trait MainStore extends Save4LaterUtil {

    @inline def saveBusinessType(businessTypeData: BusinessType, authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[BusinessType] =
      saveData4Later[BusinessType](businessTypeName, businessTypeData, authRetrievals)

    @inline def fetchBusinessType(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[Option[BusinessType]] =
      fetchData4Later[BusinessType](businessTypeName, authRetrievals)

    @inline def fetchBusinessAddress(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[BCAddress] =
      fetchData4Later[BusinessCustomerDetails](businessCustomerDetailsName, authRetrievals).map(_.map(_.businessAddress).get)

    @inline def saveBusinessCustomerDetails(authRetrievals: StandardAuthRetrievals, businessCustomerDetailsData: BusinessCustomerDetails)(implicit hc: HeaderCarrier): Future[BusinessCustomerDetails] =
      saveData4Later[BusinessCustomerDetails](businessCustomerDetailsName, businessCustomerDetailsData, authRetrievals)

    @inline def fetchBusinessCustomerDetails(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[Option[BusinessCustomerDetails]] =
      fetchData4Later[BusinessCustomerDetails](businessCustomerDetailsName, authRetrievals)

    @inline def saveNewApplicationType(newApplicationTypeData: NewApplicationType, authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[NewApplicationType] =
      saveData4Later(newApplicationTypeName, newApplicationTypeData, authRetrievals)

    @inline def fetchNewApplicationType(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[Option[NewApplicationType]] =
      fetchData4Later[NewApplicationType](newApplicationTypeName, authRetrievals)

    @inline def saveGroupMembers(authRetrievals: StandardAuthRetrievals, data: GroupMembers)(implicit hc: HeaderCarrier): Future[GroupMembers] =
      saveData4Later[GroupMembers](groupMembersName, data, authRetrievals)

    @inline def fetchGroupMembers(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[Option[GroupMembers]] =
      fetchData4Later[GroupMembers](groupMembersName, authRetrievals)

    @inline def saveGroupDeclaration(authRetrievals: StandardAuthRetrievals, groupDeclarationData: GroupDeclaration)(implicit hc: HeaderCarrier): Future[GroupDeclaration] =
      saveData4Later[GroupDeclaration](groupDeclarationName, groupDeclarationData, authRetrievals)

    @inline def fetchGroupDeclaration(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[Option[GroupDeclaration]] =
      fetchData4Later[GroupDeclaration](groupDeclarationName, authRetrievals)

    @inline def saveBusinessDirectors(authRetrievals: StandardAuthRetrievals, data: BusinessDirectors)(implicit hc: HeaderCarrier): Future[BusinessDirectors] =
      saveData4Later[BusinessDirectors](businessDirectorsName, data, authRetrievals)

    @inline def fetchBusinessDirectors(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[Option[BusinessDirectors]] =
      fetchData4Later[BusinessDirectors](businessDirectorsName, authRetrievals)

    @inline def savePartnerDetails(authRetrievals: StandardAuthRetrievals, data: Partners)(implicit hc: HeaderCarrier): Future[Partners] =
      saveData4Later[Partners](partnersName, data, authRetrievals)

    @inline def fetchPartnerDetails(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[Option[Partners]] =
      fetchData4Later[Partners](partnersName, authRetrievals)

    @inline def saveAdditionalBusinessPremisesList(authRetrievals: StandardAuthRetrievals, data: AdditionalBusinessPremisesList)(implicit hc: HeaderCarrier): Future[AdditionalBusinessPremisesList] =
      saveData4Later[AdditionalBusinessPremisesList](additionalBusinessPremisesName, data, authRetrievals)

    @inline def fetchAdditionalBusinessPremisesList(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[Option[AdditionalBusinessPremisesList]] =
      fetchData4Later[AdditionalBusinessPremisesList](additionalBusinessPremisesName, authRetrievals)

    @inline def saveTradingActivity(authRetrievals: StandardAuthRetrievals, tradingActivity: TradingActivity)(implicit hc: HeaderCarrier): Future[TradingActivity] =
      saveData4Later[TradingActivity](tradingActivityName, tradingActivity, authRetrievals)

    @inline def fetchTradingActivity(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[Option[TradingActivity]] =
      fetchData4Later[TradingActivity](tradingActivityName, authRetrievals)

    @inline def saveProducts(authRetrievals: StandardAuthRetrievals, products: Products)(implicit hc: HeaderCarrier): Future[Products] =
      saveData4Later[Products](productsName, products, authRetrievals)

    @inline def fetchProducts(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[Option[Products]] =
      fetchData4Later[Products](productsName, authRetrievals)

    @inline def saveSuppliers(authRetrievals: StandardAuthRetrievals, data: Suppliers)(implicit hc: HeaderCarrier): Future[Suppliers] =
      saveData4Later[Suppliers](suppliersName, data, authRetrievals)

    @inline def fetchSuppliers(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[Option[Suppliers]] =
      fetchData4Later[Suppliers](suppliersName, authRetrievals)

    @inline def saveApplicationDeclaration(authRetrievals: StandardAuthRetrievals, applicationDeclarationData: ApplicationDeclaration)(implicit hc: HeaderCarrier): Future[ApplicationDeclaration] =
      saveData4Later[ApplicationDeclaration](applicationDeclarationName, applicationDeclarationData, authRetrievals)

    @inline def fetchApplicationDeclaration(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[Option[ApplicationDeclaration]] =
      fetchData4Later[ApplicationDeclaration](applicationDeclarationName, authRetrievals)

    @inline def saveBusinessDetails(authRetrievals: StandardAuthRetrievals, businessDetails: BusinessDetails)(implicit hc: HeaderCarrier): Future[BusinessDetails] =
      saveData4Later(businessDetailsName, businessDetails, authRetrievals)

    @inline def fetchBusinessDetails(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[Option[BusinessDetails]] =
      fetchData4Later[BusinessDetails](businessDetailsName, authRetrievals)

    @inline def saveNoneBusinessRegistrationDetails(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[Option[BusinessRegistrationDetails]] =
      saveData4Later[Option[BusinessRegistrationDetails]](businessRegistrationDetailsName, None, authRetrievals)

    @inline def saveBusinessRegistrationDetails(authRetrievals: StandardAuthRetrievals, businessDetails: BusinessRegistrationDetails)(implicit hc: HeaderCarrier): Future[BusinessRegistrationDetails] =
      saveData4Later(businessRegistrationDetailsName, businessDetails, authRetrievals)

    @inline def fetchBusinessRegistrationDetails(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[Option[BusinessRegistrationDetails]] =
      fetchData4Later[BusinessRegistrationDetails](businessRegistrationDetailsName, authRetrievals)

    @inline def saveBusinessContacts(authRetrievals: StandardAuthRetrievals, businessContacts: BusinessContacts)(implicit hc: HeaderCarrier): Future[BusinessContacts] =
      saveData4Later(businessContactsName, businessContacts, authRetrievals)

    @inline def fetchBusinessContacts(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[Option[BusinessContacts]] =
      fetchData4Later[BusinessContacts](businessContactsName, authRetrievals)

    @inline def savePlaceOfBusiness(authRetrievals: StandardAuthRetrievals, placeOfBusiness: PlaceOfBusiness)(implicit hc: HeaderCarrier): Future[PlaceOfBusiness] =
      saveData4Later(placeOfBusinessName, placeOfBusiness, authRetrievals)

    @inline def fetchPlaceOfBusiness(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[Option[PlaceOfBusiness]] =
      fetchData4Later[PlaceOfBusiness](placeOfBusinessName, authRetrievals)

    @inline def saveApplicationStatus(authRetrievals: StandardAuthRetrievals, applicationStatus: ApplicationStatus)(implicit hc: HeaderCarrier): Future[ApplicationStatus] =
      saveData4Later(applicationStatusName, applicationStatus, authRetrievals)

    @inline def fetchApplicationStatus(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[Option[ApplicationStatus]] =
      fetchData4Later[ApplicationStatus](applicationStatusName, authRetrievals)
  }

  trait APIStore extends Save4LaterUtil {
    // this trait defines named save4later calls for data fetched from api 5 or issues in relation to the data retrieved from api5

    @inline def saveSubscriptionTypeFrontEnd(subscriptionTypeFrontEnd: SubscriptionTypeFrontEnd, authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[SubscriptionTypeFrontEnd] =
      saveData4Later[SubscriptionTypeFrontEnd](subscriptionTypeFrontEndName, subscriptionTypeFrontEnd, authRetrievals)

    /* TODO backwards compatibility code for AWRS-1800 to be removed after 28 days */
    @inline def fetchSubscriptionTypeFrontEnd_old(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[Option[SubscriptionTypeFrontEnd_old]] = {
      val fetch = fetchData4Later[SubscriptionTypeFrontEnd_old](subscriptionTypeFrontEndName, authRetrievals)
      fetch.recover {
        case _ => None
      }
    }

    /* end old code block */

    @inline def fetchSubscriptionTypeFrontEnd(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[Option[SubscriptionTypeFrontEnd]] = {
      val fetch = fetchData4Later[SubscriptionTypeFrontEnd](subscriptionTypeFrontEndName, authRetrievals)
      fetch.recover {
        case _ => None
      }
    }

    @inline def saveBusinessDetailsSupport(businessDetailsSupport: BusinessDetailsSupport, authRetrievals: StandardAuthRetrievals)
                                          (implicit hc: HeaderCarrier): Future[BusinessDetailsSupport] =
      saveData4Later[BusinessDetailsSupport](businessDetailsSupportName, businessDetailsSupport, authRetrievals)

    @inline def fetchBusinessDetailsSupport(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[Option[BusinessDetailsSupport]] =
      fetchData4Later[BusinessDetailsSupport](businessDetailsSupportName, authRetrievals) flatMap {
        case None =>
          fetchSubscriptionTypeFrontEnd(authRetrievals) flatMap {
            case None => Future.successful(None)
            case fe@_ =>
              val model = BusinessDetailsSupport.evaluate(fe)
              saveBusinessDetailsSupport(model, authRetrievals) flatMap (_ => Future.successful(Some(model)))
          }
        case model@_ => Future.successful(model)
      }
  }

  lazy val api = new APIStore {
    override val save4LaterConnector: Save4LaterConnector = apiSave4LaterConnector
  }

  lazy val mainStore = new MainStore {
    override val save4LaterConnector: Save4LaterConnector = mainStoreSave4LaterConnector
  }

}

object Save4LaterService extends Save4LaterService {
  override val mainStoreSave4LaterConnector: Save4LaterConnector = AwrsDataCacheConnector
  override val apiSave4LaterConnector: Save4LaterConnector = AwrsAPIDataCacheConnector
}
