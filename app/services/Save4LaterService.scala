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
import connectors.{AwrsAPIDataCacheConnector, AwrsDataCacheConnector, Save4LaterConnector}
import services.DataCacheKeys._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.AccountUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.util.{Failure, Success, Try}

trait Save4LaterUtil {

  val save4LaterConnector: Save4LaterConnector

  @inline def fetchData4Later[T](formId: String)(implicit user: AuthContext, hc: HeaderCarrier, formats: play.api.libs.json.Format[T]): Future[Option[T]] = {
    save4LaterConnector.fetchData4Later[T](AccountUtils.getUtrOrName(), formId)
  }

  @inline def saveData4Later[T](formId: String, data: T)(implicit user: AuthContext, hc: HeaderCarrier, formats: play.api.libs.json.Format[T]): Future[T] =
    save4LaterConnector.saveData4Later[T](AccountUtils.getUtrOrName(), formId, data) map (_.get)

  @inline def fetchAll(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[CacheMap]] =
    save4LaterConnector.fetchAll(AccountUtils.getUtrOrName())

  @inline def removeAll(implicit user: AuthContext, hc: HeaderCarrier): Future[HttpResponse] =
    save4LaterConnector.removeAll(AccountUtils.getUtrOrName())
}

trait Save4LaterService {

  val mainStoreSave4LaterConnector: Save4LaterConnector
  val apiSave4LaterConnector: Save4LaterConnector


  implicit def convertUtil[T](f: Option[T]): T = f.get

  trait MainStore extends Save4LaterUtil {

    @inline def saveBusinessType(businessTypeData: BusinessType)(implicit user: AuthContext, hc: HeaderCarrier): Future[BusinessType] =
      saveData4Later[BusinessType](businessTypeName, businessTypeData)

    @inline def fetchBusinessType(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[BusinessType]] =
      fetchData4Later[BusinessType](businessTypeName)

    @inline def fetchBusinessAddress(implicit user: AuthContext, hc: HeaderCarrier): Future[BCAddress] =
      fetchData4Later[BusinessCustomerDetails](businessCustomerDetailsName).map(_.map(_.businessAddress).get)

    @inline def saveBusinessCustomerDetails(businessCustomerDetailsData: BusinessCustomerDetails)(implicit user: AuthContext, hc: HeaderCarrier): Future[BusinessCustomerDetails] =
      saveData4Later[BusinessCustomerDetails](businessCustomerDetailsName, businessCustomerDetailsData)

    @inline def fetchBusinessCustomerDetails(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[BusinessCustomerDetails]] =
      fetchData4Later[BusinessCustomerDetails](businessCustomerDetailsName)

    @inline def saveNewApplicationType(newApplicationTypeData: NewApplicationType)(implicit user: AuthContext, hc: HeaderCarrier): Future[NewApplicationType] =
      saveData4Later(newApplicationTypeName, newApplicationTypeData)

    @inline def fetchNewApplicationType(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[NewApplicationType]] =
      fetchData4Later[NewApplicationType](newApplicationTypeName)

    @inline def saveGroupMembers(data: GroupMembers)(implicit user: AuthContext, hc: HeaderCarrier): Future[GroupMembers] =
      saveData4Later[GroupMembers](groupMembersName, data)

    @inline def fetchGroupMembers(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[GroupMembers]] =
      fetchData4Later[GroupMembers](groupMembersName)

    @inline def saveGroupDeclaration(groupDeclarationData: GroupDeclaration)(implicit user: AuthContext, hc: HeaderCarrier): Future[GroupDeclaration] =
      saveData4Later[GroupDeclaration](groupDeclarationName, groupDeclarationData)

    @inline def fetchGroupDeclaration(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[GroupDeclaration]] =
      fetchData4Later[GroupDeclaration](groupDeclarationName)

    @inline def saveBusinessDirectors(data: BusinessDirectors)(implicit user: AuthContext, hc: HeaderCarrier): Future[BusinessDirectors] =
      saveData4Later[BusinessDirectors](businessDirectorsName, data)

    @inline def fetchBusinessDirectors(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[BusinessDirectors]] =
      fetchData4Later[BusinessDirectors](businessDirectorsName)

    @inline def savePartnerDetails(data: Partners)(implicit user: AuthContext, hc: HeaderCarrier): Future[Partners] =
      saveData4Later[Partners](partnersName, data)

    @inline def fetchPartnerDetails(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[Partners]] =
      fetchData4Later[Partners](partnersName)

    @inline def saveAdditionalBusinessPremisesList(data: AdditionalBusinessPremisesList)(implicit user: AuthContext, hc: HeaderCarrier): Future[AdditionalBusinessPremisesList] =
      saveData4Later[AdditionalBusinessPremisesList](additionalBusinessPremisesName, data)

    @inline def fetchAdditionalBusinessPremisesList(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[AdditionalBusinessPremisesList]] =
      fetchData4Later[AdditionalBusinessPremisesList](additionalBusinessPremisesName)

    @inline def saveTradingActivity(tradingActivity: TradingActivity)(implicit user: AuthContext, hc: HeaderCarrier): Future[TradingActivity] =
      saveData4Later[TradingActivity](tradingActivityName, tradingActivity)

    @inline def fetchTradingActivity(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[TradingActivity]] =
      fetchData4Later[TradingActivity](tradingActivityName)

    @inline def saveProducts(products: Products)(implicit user: AuthContext, hc: HeaderCarrier): Future[Products] =
      saveData4Later[Products](productsName, products)

    @inline def fetchProducts(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[Products]] =
      fetchData4Later[Products](productsName)

    @inline def saveSuppliers(data: Suppliers)(implicit user: AuthContext, hc: HeaderCarrier): Future[Suppliers] =
      saveData4Later[Suppliers](suppliersName, data)

    @inline def fetchSuppliers(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[Suppliers]] =
      fetchData4Later[Suppliers](suppliersName)

    @inline def saveApplicationDeclaration(applicationDeclarationData: ApplicationDeclaration)(implicit user: AuthContext, hc: HeaderCarrier): Future[ApplicationDeclaration] =
      saveData4Later[ApplicationDeclaration](applicationDeclarationName, applicationDeclarationData)

    @inline def fetchApplicationDeclaration(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[ApplicationDeclaration]] =
      fetchData4Later[ApplicationDeclaration](applicationDeclarationName)

    @inline def saveBusinessDetails(businessDetails: BusinessDetails)(implicit user: AuthContext, hc: HeaderCarrier): Future[BusinessDetails] =
      saveData4Later(businessDetailsName, businessDetails)

    @inline def fetchBusinessDetails(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[BusinessDetails]] =
      fetchData4Later[BusinessDetails](businessDetailsName)

    @inline def saveNoneBusinessRegistrationDetails()(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[BusinessRegistrationDetails]] =
      saveData4Later[Option[BusinessRegistrationDetails]](businessRegistrationDetailsName, None)

    @inline def saveBusinessRegistrationDetails(businessDetails: BusinessRegistrationDetails)(implicit user: AuthContext, hc: HeaderCarrier): Future[BusinessRegistrationDetails] =
      saveData4Later(businessRegistrationDetailsName, businessDetails)

    @inline def fetchBusinessRegistrationDetails(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[BusinessRegistrationDetails]] =
      fetchData4Later[BusinessRegistrationDetails](businessRegistrationDetailsName)

    @inline def saveBusinessContacts(businessContacts: BusinessContacts)(implicit user: AuthContext, hc: HeaderCarrier): Future[BusinessContacts] =
      saveData4Later(businessContactsName, businessContacts)

    @inline def fetchBusinessContacts(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[BusinessContacts]] =
      fetchData4Later[BusinessContacts](businessContactsName)

    @inline def savePlaceOfBusiness(placeOfBusiness: PlaceOfBusiness)(implicit user: AuthContext, hc: HeaderCarrier): Future[PlaceOfBusiness] =
      saveData4Later(placeOfBusinessName, placeOfBusiness)

    @inline def fetchPlaceOfBusiness(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[PlaceOfBusiness]] =
      fetchData4Later[PlaceOfBusiness](placeOfBusinessName)

    @inline def saveApplicationStatus(applicationStatus: ApplicationStatus)(implicit user: AuthContext, hc: HeaderCarrier): Future[ApplicationStatus] =
      saveData4Later(applicationStatusName, applicationStatus)

    @inline def fetchApplicationStatus(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[ApplicationStatus]] =
      fetchData4Later[ApplicationStatus](applicationStatusName)
  }

  trait APIStore extends Save4LaterUtil {
    // this trait defines named save4later calls for data fetched from api 5 or issues in relation to the data retrieved from api5

    @inline def saveSubscriptionTypeFrontEnd(subscriptionTypeFrontEnd: SubscriptionTypeFrontEnd)(implicit user: AuthContext, hc: HeaderCarrier): Future[SubscriptionTypeFrontEnd] =
      saveData4Later[SubscriptionTypeFrontEnd](subscriptionTypeFrontEndName, subscriptionTypeFrontEnd)

    /* TODO backwards compatibility code for AWRS-1800 to be removed after 28 days */
    @inline def fetchSubscriptionTypeFrontEnd_old(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[SubscriptionTypeFrontEnd_old]] = {
      val fetch = fetchData4Later[SubscriptionTypeFrontEnd_old](subscriptionTypeFrontEndName)
      fetch.recover {
        case _ => None
      }
    }

    /* end old code block */

    @inline def fetchSubscriptionTypeFrontEnd(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[SubscriptionTypeFrontEnd]] = {
      val fetch = fetchData4Later[SubscriptionTypeFrontEnd](subscriptionTypeFrontEndName)
      fetch.recover {
        case _ => None
      }
    }

    @inline def saveBusinessDetailsSupport(businessDetailsSupport: BusinessDetailsSupport)(implicit user: AuthContext, hc: HeaderCarrier): Future[BusinessDetailsSupport] =
      saveData4Later[BusinessDetailsSupport](businessDetailsSupportName, businessDetailsSupport)

    @inline def fetchBusinessDetailsSupport(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[BusinessDetailsSupport]] =
      fetchData4Later[BusinessDetailsSupport](businessDetailsSupportName) flatMap {
        case None =>
          fetchSubscriptionTypeFrontEnd flatMap {
            case None => Future.successful(None)
            case fe@_ =>
              val model = BusinessDetailsSupport.evaluate(fe)
              saveBusinessDetailsSupport(model) flatMap {
                case _ => Future.successful(Some(model))
              }
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
