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

package controllers

import config.FrontendAuthConnector
import controllers.auth.{AwrsController, ExternalUrls, StandardAuthRetrievals}
import controllers.util.{JourneyPage, RedirectParam, SaveAndRoutable}
import forms.BusinessDetailsForm._
import models._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.DataCacheKeys._
import services.{DataCacheService, KeyStoreService, Save4LaterService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.AccountUtils
import views.Configuration.{NewApplicationMode, NewBusinessStartDateConfiguration, ReturnedApplicationEditMode, ReturnedApplicationMode}
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, ViewApplicationType}

import scala.concurrent.Future

trait BusinessDetailsController extends AwrsController with JourneyPage with AccountUtils with SaveAndRoutable with DataCacheService {

  override val section = businessDetailsName

  def renderMode(newApplicationType: Option[NewApplicationType], authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[NewBusinessStartDateConfiguration] = {
    val isNewApplication = newApplicationType.getOrElse(NewApplicationType(Some(false))).isNewApplication.get
    val etmpBug =
      if (isNewApplication) {
        Future.successful(false)
      } else {
        save4LaterService.api.fetchBusinessDetailsSupport(authRetrievals) flatMap {
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

  def showBusinessDetails(isLinearMode: Boolean) = Action.async { implicit request: Request[AnyContent] =>
    restrictedAccessCheck {
      authorisedAction { ar =>
        implicit val viewApplicationType = isLinearMode match {
          case true => LinearViewMode
          case false => EditSectionOnlyMode
        }
        val businessType = request.getBusinessType
        for {
          businessCustomerDetails <- save4LaterService.mainStore.fetchBusinessCustomerDetails(ar)
          businessDetails <- save4LaterService.mainStore.fetchBusinessDetails(ar)
          newApplicationType <- save4LaterService.mainStore.fetchNewApplicationType(ar)
          mode <- renderMode(newApplicationType, ar)
        } yield {
          val businessName = businessCustomerDetails.fold("")(x => x.businessName)
          businessDetails match {
            case Some(data) => {
              val extendedBusinessDetails = ExtendedBusinessDetails(Some(businessName), data.doYouHaveTradingName, data.tradingName, data.newAWBusiness)
              Ok(views.html.awrs_business_details(businessType, businessName, businessDetailsForm(businessType.get, AccountUtils.hasAwrs(ar.enrolments)).form.fill(extendedBusinessDetails), mode, ar.enrolments))
            }
            case _ => Ok(views.html.awrs_business_details(businessType, businessName, businessDetailsForm(businessType.get, AccountUtils.hasAwrs(ar.enrolments)).form, mode, ar.enrolments))
          }
        }
      }
    }
  }

  def saveBusinessDetails(id: Int,
                          redirectRoute: (Option[RedirectParam], Boolean) => Future[Result],
                          isNewRecord: Boolean,
                          businessDetails: BusinessDetails,
                          authRetrievals: StandardAuthRetrievals)
                         (implicit request: Request[AnyContent]): Future[Result] = {
    save4LaterService.mainStore.saveBusinessDetails(authRetrievals, businessDetails) flatMap {
      case _ => redirectRoute(Some(RedirectParam("No", id)), isNewRecord)
    }
  }

  def save(id: Int,
           redirectRoute: (Option[RedirectParam], Boolean) => Future[Result],
           viewApplicationType: ViewApplicationType,
           isNewRecord: Boolean,
           authRetrievals: StandardAuthRetrievals)
          (implicit request: Request[AnyContent]): Future[Result] = {
    implicit val viewMode = viewApplicationType
    val businessType = request.getBusinessType
    businessDetailsForm(businessType.get, AccountUtils.hasAwrs(authRetrievals.enrolments)).bindFromRequest.fold(
      formWithErrors =>
        for {
          businessCustomerDetails <- save4LaterService.mainStore.fetchBusinessCustomerDetails(authRetrievals)
          newApplicationType <- save4LaterService.mainStore.fetchNewApplicationType(authRetrievals)
          mode <- renderMode(newApplicationType, authRetrievals)
        } yield {
          BadRequest(views.html.awrs_business_details(businessType, businessCustomerDetails.fold("")(x => x.businessName), formWithErrors, mode, authRetrievals.enrolments))
        }
      ,
      extendedBusinessDetails => {
        (AccountUtils.hasAwrs(authRetrievals.enrolments), businessType) match {
          case (false, _) => saveBusinessDetails(id, redirectRoute, isNewRecord, extendedBusinessDetails.getBusinessDetails, authRetrievals)
          case (true, (Some("LLP_GRP") | Some("LTD_GRP"))) => {
            save4LaterService.mainStore.fetchBusinessCustomerDetails(authRetrievals) flatMap {
              case Some(businessCustomerDetails) => {
                businessCustomerDetails.businessName != extendedBusinessDetails.businessName.get match {
                  case true => {
                    keyStoreService.saveExtendedBusinessDetails(extendedBusinessDetails) flatMap {
                      case _ => Future.successful(Redirect(routes.BusinessNameChangeController.showConfirm()))
                    }
                  }
                  case false => saveBusinessDetails(id, redirectRoute, isNewRecord, extendedBusinessDetails.getBusinessDetails, authRetrievals)
                }
              }
              case None => saveBusinessDetails(id, redirectRoute, isNewRecord, extendedBusinessDetails.getBusinessDetails, authRetrievals)
            }
          }
          case (true, _) => saveBusinessDetails(id, redirectRoute, isNewRecord, extendedBusinessDetails.getBusinessDetails, authRetrievals)
        }
      }
    )
  }
}

object BusinessDetailsController extends BusinessDetailsController {
  override val authConnector = FrontendAuthConnector
  override val save4LaterService = Save4LaterService
  override val keyStoreService = KeyStoreService
  val signInUrl = ExternalUrls.signIn
}
