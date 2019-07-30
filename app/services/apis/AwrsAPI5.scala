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

package services.apis

import connectors.AWRSConnector
import controllers.auth.StandardAuthRetrievals
import forms.AWRSEnums.BooleanRadioEnum
import javax.inject.Inject
import models.{BusinessContacts, BusinessDetails, _}
import play.api.mvc.{AnyContent, Request}
import services.Save4LaterService
import services.helper.AwrsAPI5Helper._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class AwrsAPI5 @Inject()(val awrsConnector: AWRSConnector,
                         val save4LaterService: Save4LaterService
                        ){

  def retrieveApplication(authRetrievals: StandardAuthRetrievals)
                         (implicit hc: HeaderCarrier, request: Request[AnyContent], ec: ExecutionContext): Future[SubscriptionTypeFrontEnd] = {
    lazy val callETMP: Future[SubscriptionTypeFrontEnd] =
      awrsConnector.lookupAWRSData(authRetrievals) flatMap {
        api5Data => saveReturnedApplication(api5Data.as[AWRSFEModel], authRetrievals)
      }

    save4LaterService.mainStore.fetchBusinessType(authRetrievals) flatMap {
      case Some(_) =>
        save4LaterService.api.fetchSubscriptionTypeFrontEnd(authRetrievals) flatMap {
          case Some(subscriptionTypeFrontEnd) => Future.successful(subscriptionTypeFrontEnd)
          case _ => callETMP
        }
      case None => callETMP
    }
  }

  def saveReturnedApplication(feModel: AWRSFEModel, authRetrievals: StandardAuthRetrievals)
                             (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[SubscriptionTypeFrontEnd] = {
    val subscriptionTypeFrontEnd: SubscriptionTypeFrontEnd = feModel.subscriptionTypeFrontEnd
    lazy val updatedPremises = AdditionalBusinessPremisesList(subscriptionTypeFrontEnd.additionalPremises.get.premises.drop(1))

    for {
      dataCache <- save4LaterService.api.saveSubscriptionTypeFrontEnd(subscriptionTypeFrontEnd, authRetrievals)
      businessCustomerDetails <- save4LaterService.mainStore.saveBusinessCustomerDetails(authRetrievals, convertToBusinessCustomerDetails(subscriptionTypeFrontEnd))
      x <- getEntitySpecificData(feModel, businessCustomerDetails, authRetrievals)
      businessType <- save4LaterService.mainStore.saveBusinessType(subscriptionTypeFrontEnd.legalEntity.get, authRetrievals)
      additionalPremises <- save4LaterService.mainStore.saveAdditionalBusinessPremisesList(authRetrievals, updatedPremises)
      tradingActivity <- save4LaterService.mainStore.saveTradingActivity(authRetrievals, subscriptionTypeFrontEnd.tradingActivity.get)
      products <- save4LaterService.mainStore.saveProducts(authRetrievals, subscriptionTypeFrontEnd.products.get)
      suppliers <- save4LaterService.mainStore.saveSuppliers(authRetrievals, subscriptionTypeFrontEnd.suppliers.get)
      applicationDeclaration <- save4LaterService.mainStore.saveApplicationDeclaration(authRetrievals, subscriptionTypeFrontEnd.applicationDeclaration.get)
    } yield feModel.subscriptionTypeFrontEnd
  }


  def getEntitySpecificData(feModelOld: AWRSFEModel, businessCustomerDetails: BusinessCustomerDetails, authRetrievals: StandardAuthRetrievals)
                           (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[(BusinessDetails, BusinessRegistrationDetails, PlaceOfBusiness, BusinessContacts)] = {

    val subscriptionTypeFrontEnd: SubscriptionTypeFrontEnd = feModelOld.subscriptionTypeFrontEnd

    val businessDetails = subscriptionTypeFrontEnd.businessDetails.get
    val businessRegistrationDetails = subscriptionTypeFrontEnd.businessRegistrationDetails.get
    val businessContacts = subscriptionTypeFrontEnd.businessContacts.get
    val placeOfBusiness = subscriptionTypeFrontEnd.placeOfBusiness.get
    val legalEntity = subscriptionTypeFrontEnd.legalEntity.get.legalEntity.get

    lazy val saveOtherFields: Future[Unit] = legalEntity match {
      case "SOP" =>
        Future.successful((): Unit)
      case "LTD" =>
        for {
          businessDirectors <- save4LaterService.mainStore.saveBusinessDirectors(authRetrievals, subscriptionTypeFrontEnd.businessDirectors.get)
        } yield ()

      case "Partnership" =>
        for {
          businessPartners <- save4LaterService.mainStore.savePartnerDetails(authRetrievals, subscriptionTypeFrontEnd.partnership.get)
        } yield ()

      case "LLP_GRP" =>
        for {
          groupMembers <- save4LaterService.mainStore.saveGroupMembers(authRetrievals, subscriptionTypeFrontEnd.groupMembers.get)
          groupDeclaration <- save4LaterService.mainStore.saveGroupDeclaration(authRetrievals, subscriptionTypeFrontEnd.groupDeclaration.get)
          businessPartners <- save4LaterService.mainStore.savePartnerDetails(authRetrievals, subscriptionTypeFrontEnd.partnership.get)
        } yield ()

      case "LTD_GRP" =>
        for {
          groupMembers <- save4LaterService.mainStore.saveGroupMembers(authRetrievals, subscriptionTypeFrontEnd.groupMembers.get)
          groupDeclaration <- save4LaterService.mainStore.saveGroupDeclaration(authRetrievals, subscriptionTypeFrontEnd.groupDeclaration.get)
          businessDirectors <- save4LaterService.mainStore.saveBusinessDirectors(authRetrievals, subscriptionTypeFrontEnd.businessDirectors.get)
        } yield ()

      case _ =>
        for {
          businessPartners <- save4LaterService.mainStore.savePartnerDetails(authRetrievals, subscriptionTypeFrontEnd.partnership.get)
        } yield ()
    }

    for {
      businessDetails <- save4LaterService.mainStore.saveBusinessDetails(authRetrievals, businessDetails)
      businessRegistrationDetails <- save4LaterService.mainStore.saveBusinessRegistrationDetails(authRetrievals, businessRegistrationDetails)
      placeOfBusiness <- save4LaterService.mainStore.savePlaceOfBusiness(authRetrievals, api5CopyPlaceOfBusiness(placeOfBusiness))
      businessContacts <- save4LaterService.mainStore.saveBusinessContacts(authRetrievals, businessContacts)
      _ <- saveOtherFields
    } yield (businessDetails, businessRegistrationDetails, placeOfBusiness, businessContacts)
  }

  // This is for api5 after we bypass business customer frontend, since we want the user to be able to update their place of business the answer to this
  // question will be defaulted to no (i.e. enable validation and permit updates)
  def api5CopyPlaceOfBusiness(placeOfBusiness: PlaceOfBusiness): PlaceOfBusiness = placeOfBusiness.copy(mainPlaceOfBusiness = Some(BooleanRadioEnum.NoString))

}