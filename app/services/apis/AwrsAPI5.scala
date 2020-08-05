/*
 * Copyright 2020 HM Revenue & Customs
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
import models.{BusinessContacts, _}
import play.api.Logging
import services.Save4LaterService
import services.helper.AwrsAPI5Helper.convertToBusinessCustomerDetails
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class AwrsAPI5 @Inject()(val awrsConnector: AWRSConnector,
                         val save4LaterService: Save4LaterService
                        ) extends Logging {

  def retrieveApplication(authRetrievals: StandardAuthRetrievals)
                         (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[SubscriptionTypeFrontEnd] = {
    lazy val callETMP: Future[SubscriptionTypeFrontEnd] =
      awrsConnector.lookupAWRSData(authRetrievals) flatMap {
        api5Data => saveReturnedApplication(api5Data.as[AWRSFEModel], authRetrievals)
      }

    save4LaterService.mainStore.fetchBusinessType(authRetrievals) flatMap {
      case Some(_) =>
        save4LaterService.api.fetchSubscriptionTypeFrontEnd(authRetrievals) flatMap {
          case Some(subscriptionTypeFrontEnd) =>
            checkSavedSubscriptionTypeFrontend(authRetrievals, subscriptionTypeFrontEnd) map {_ => subscriptionTypeFrontEnd}
          case _ => callETMP
        }
      case None => callETMP
    }
  }

  def checkSavedSubscriptionTypeFrontend(authRetrievals: StandardAuthRetrievals,
                                         subTypeFrontend: SubscriptionTypeFrontEnd)
                                        (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    save4LaterService.mainStore.fetchTradingStartDetails(authRetrievals) flatMap {
      case Some(_) => Future.successful(true)
      case _       =>
        val amendedSTFE = convertEtmpFormatSTFE(subTypeFrontend)
        val busCusDetails = convertToBusinessCustomerDetails(amendedSTFE)
        val businessDetails = amendedSTFE.businessDetails.get

        for {
          _ <- save4LaterService.api.saveSubscriptionTypeFrontEnd(amendedSTFE, authRetrievals)
          _ <- save4LaterService.mainStore.saveBusinessNameDetails(authRetrievals, BusinessNameDetails(Some(busCusDetails.businessName), businessDetails.doYouHaveTradingName, businessDetails.tradingName))
          _ <- save4LaterService.mainStore.saveTradingStartDetails(authRetrievals, businessDetails.newAWBusiness.get)
        } yield {
          logger.info("[checkSavedSubscriptionTypeFrontend] Added missing save4later details to cache")
          false
        }
    }
  }

  private[apis] def convertEtmpFormatSTFE(stfe: SubscriptionTypeFrontEnd): SubscriptionTypeFrontEnd = {
    stfe.copy(businessDetails = stfe.businessDetails.map{
      bd => bd.copy(newAWBusiness = bd.newAWBusiness.map(_.invertedBeforeMarch2016Question))
    })
  }

  def saveReturnedApplication(feModel: AWRSFEModel, authRetrievals: StandardAuthRetrievals)
                             (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[SubscriptionTypeFrontEnd] = {
    val subscriptionTypeFrontEnd: SubscriptionTypeFrontEnd = feModel.subscriptionTypeFrontEnd
    val amendedSTFE = convertEtmpFormatSTFE(subscriptionTypeFrontEnd)
    val newFeModel = feModel.copy(subscriptionTypeFrontEnd = amendedSTFE)
    lazy val updatedPremises = AdditionalBusinessPremisesList(amendedSTFE.additionalPremises.get.premises.drop(1))
    
    for {
      dataCache <- save4LaterService.api.saveSubscriptionTypeFrontEnd(amendedSTFE, authRetrievals)
      businessCustomerDetails <- save4LaterService.mainStore.saveBusinessCustomerDetails(authRetrievals, convertToBusinessCustomerDetails(amendedSTFE))
      x <- getEntitySpecificData(newFeModel, businessCustomerDetails, authRetrievals)
      businessType <- save4LaterService.mainStore.saveBusinessType(amendedSTFE.legalEntity.get, authRetrievals)
      additionalPremises <- save4LaterService.mainStore.saveAdditionalBusinessPremisesList(authRetrievals, updatedPremises)
      tradingActivity <- save4LaterService.mainStore.saveTradingActivity(authRetrievals, amendedSTFE.tradingActivity.get)
      products <- save4LaterService.mainStore.saveProducts(authRetrievals, amendedSTFE.products.get)
      suppliers <- save4LaterService.mainStore.saveSuppliers(authRetrievals, amendedSTFE.suppliers.get)
      applicationDeclaration <- save4LaterService.mainStore.saveApplicationDeclaration(authRetrievals, amendedSTFE.applicationDeclaration.get)
    } yield newFeModel.subscriptionTypeFrontEnd
  }


  def getEntitySpecificData(feModelOld: AWRSFEModel, businessCustomerDetails: BusinessCustomerDetails, authRetrievals: StandardAuthRetrievals)
                           (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[(BusinessNameDetails, NewAWBusiness, BusinessRegistrationDetails, PlaceOfBusiness, BusinessContacts)] = {

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
      businessNameDetails <- save4LaterService.mainStore.saveBusinessNameDetails(authRetrievals, BusinessNameDetails(Some(businessCustomerDetails.businessName), businessDetails.doYouHaveTradingName, businessDetails.tradingName))
      tradingStartDetails <- save4LaterService.mainStore.saveTradingStartDetails(authRetrievals, businessDetails.newAWBusiness.get)
      businessRegistrationDetails <- save4LaterService.mainStore.saveBusinessRegistrationDetails(authRetrievals, businessRegistrationDetails)
      placeOfBusiness <- save4LaterService.mainStore.savePlaceOfBusiness(authRetrievals, api5CopyPlaceOfBusiness(placeOfBusiness))
      businessContacts <- save4LaterService.mainStore.saveBusinessContacts(authRetrievals, businessContacts)
      _ <- saveOtherFields
    } yield (businessNameDetails, tradingStartDetails, businessRegistrationDetails, placeOfBusiness, businessContacts)
  }

  // This is for api5 after we bypass business customer frontend, since we want the user to be able to update their place of business the answer to this
  // question will be defaulted to no (i.e. enable validation and permit updates)
  def api5CopyPlaceOfBusiness(placeOfBusiness: PlaceOfBusiness): PlaceOfBusiness = placeOfBusiness.copy(mainPlaceOfBusiness = Some(BooleanRadioEnum.NoString))

}