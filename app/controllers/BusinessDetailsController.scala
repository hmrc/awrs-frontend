/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import config.FrontendAuthConnector
import controllers.auth.AwrsController
import controllers.util.{JourneyPage, RedirectParam, SaveAndRoutable}
import forms.BusinessDetailsForm._
import models._
import play.api.mvc.{AnyContent, Request, Result}
import services.DataCacheKeys._
import services.Save4LaterService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.AccountUtils
import views.Configuration.{NewApplicationMode, NewBusinessStartDateConfiguration, ReturnedApplicationEditMode, ReturnedApplicationMode}
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, ViewApplicationType}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.concurrent.Future

trait BusinessDetailsController extends AwrsController with JourneyPage with AccountUtils with SaveAndRoutable {

  override val section = businessDetailsName

  def renderMode(newApplicationType: Option[NewApplicationType])(implicit hc: HeaderCarrier, authContext: AuthContext): Future[NewBusinessStartDateConfiguration] = {
    val isNewApplication = newApplicationType.getOrElse(NewApplicationType(Some(false))).isNewApplication.get
    val etmpBug =
      isNewApplication match {
        case true => Future.successful(false)
        case false =>
          save4LaterService.api.fetchBusinessDetailsSupport flatMap {
            case Some(BusinessDetailsSupport(missingProposedStartDate)) => Future.successful(missingProposedStartDate)
            // the fetchBusinessDetailsSupport should have checked the api cache for the api5 data, if None is still returned then
            // it can only mean that the API 5 data is missing
            // this should never happen since this call can only happens post api4 submission
            // given this is api5 businessDetail and subsequent newBusiness fields must exists
            case None => throw new RuntimeException("Unable to find API 5 data")
          }
      }
    etmpBug flatMap { case x =>
      (isNewApplication, x) match {
        case (true, _) => Future.successful(NewApplicationMode)
        case (false, false) => Future.successful(ReturnedApplicationMode)
        case (false, true) => Future.successful(ReturnedApplicationEditMode)
      }
    }
  }

  def showBusinessDetails(isLinearMode: Boolean) = asyncRestrictedAccess {
    implicit user => implicit request =>
      implicit val viewApplicationType = isLinearMode match {
        case true => LinearViewMode
        case false => EditSectionOnlyMode
      }
      val businessType = request.getBusinessType
      for {
        businessCustomerDetails <- save4LaterService.mainStore.fetchBusinessCustomerDetails
        businessDetails <- save4LaterService.mainStore.fetchBusinessDetails
        newApplicationType <- save4LaterService.mainStore.fetchNewApplicationType
        mode <- renderMode(newApplicationType)
      } yield {
        businessDetails match {
          case Some(data) => {
            val extendedBusinessDetails = ExtendedBusinessDetails(Some(businessCustomerDetails.get.businessName), data.doYouHaveTradingName, data.tradingName, data.newAWBusiness)
            Ok(views.html.awrs_business_details(businessType, businessCustomerDetails.fold("")(x => x.businessName), businessDetailsForm(businessType.get).form.fill(extendedBusinessDetails), mode))
          }
          case _ => Ok(views.html.awrs_business_details(businessType, businessCustomerDetails.fold("")(x => x.businessName), businessDetailsForm(businessType.get).form, mode))
        }
      }
  }

  def save(id: Int, redirectRoute: (Option[RedirectParam], Boolean) => Future[Result], viewApplicationType: ViewApplicationType, isNewRecord: Boolean)(implicit request: Request[AnyContent], user: AuthContext): Future[Result] = {
    implicit val viewMode = viewApplicationType
    val businessType = request.getBusinessType
    businessDetailsForm(businessType.get).bindFromRequest.fold(
      formWithErrors =>
        for {
          businessCustomerDetails <- save4LaterService.mainStore.fetchBusinessCustomerDetails
          newApplicationType <- save4LaterService.mainStore.fetchNewApplicationType
          mode <- renderMode(newApplicationType)
        } yield {
          BadRequest(views.html.awrs_business_details(businessType, businessCustomerDetails.fold("")(x => x.businessName), formWithErrors, mode))
        }
      ,
      extendedBusinessDetailsData => {
        businessType match {
          case Some("LLP_GRP") | Some("LTD_GRP") => {
            for {
              businessCustomerDetails <- save4LaterService.mainStore.fetchBusinessCustomerDetails
              if (businessCustomerDetails.get.businessName != extendedBusinessDetailsData.businessName)
            } yield {
              val newBusinessCustomerDetails = businessCustomerDetails.get.updateBusinessName(extendedBusinessDetailsData.businessName)
              save4LaterService.mainStore.saveBusinessCustomerDetails(newBusinessCustomerDetails)
            }
          }
          case _ =>
        }
        save4LaterService.mainStore.saveBusinessDetails(extendedBusinessDetailsData.getBusinessDetails) flatMap {
          case _ => redirectRoute(Some(RedirectParam("No", id)), isNewRecord)
        }

      }
    )
  }



  //TODO For Groups, on Edit, Check the business name has changed and, if so, redirect to the confirmation page.
  // TODO: save the customer details also, but not here!!

}

object BusinessDetailsController extends BusinessDetailsController {
  override val authConnector = FrontendAuthConnector
  override val save4LaterService = Save4LaterService
}