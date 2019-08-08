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

import audit.Auditable
import config.ApplicationConfig
import connectors.AwrsDataCacheConnector
import controllers.auth.StandardAuthRetrievals
import controllers.util.{JourneyPage, RedirectParam, SaveAndRoutable}
import forms.BusinessDetailsForm._
import javax.inject.Inject
import models._
import play.api.mvc._
import services.DataCacheKeys._
import services.{DataCacheService, KeyStoreService, Save4LaterService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.AccountUtils
import views.Configuration.{NewApplicationMode, NewBusinessStartDateConfiguration, ReturnedApplicationEditMode, ReturnedApplicationMode}
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, ViewApplicationType}

import scala.concurrent.{ExecutionContext, Future}

class BusinessDetailsController @Inject()(val mcc: MessagesControllerComponents,
                                          val save4LaterService: Save4LaterService,
                                          val keyStoreService: KeyStoreService,
                                          val authConnector: DefaultAuthConnector,
                                          val auditable: Auditable,
                                          val accountUtils: AccountUtils,
                                          val mainStoreSave4LaterConnector: AwrsDataCacheConnector,
                                          implicit val applicationConfig: ApplicationConfig) extends FrontendController(mcc) with JourneyPage with SaveAndRoutable with DataCacheService {

  override val section: String = businessDetailsName
  override implicit val ec: ExecutionContext = mcc.executionContext
  val signInUrl: String = applicationConfig.signIn

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
    etmpBug flatMap { x =>
      (isNewApplication, x) match {
        case (true, _) => Future.successful(NewApplicationMode)
        case (false, false) => Future.successful(ReturnedApplicationMode)
        case (false, true) => Future.successful(ReturnedApplicationEditMode)
      }
    }
  }

  def showBusinessDetails(isLinearMode: Boolean): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    restrictedAccessCheck {
      authorisedAction { ar =>
        implicit val viewApplicationType: ViewApplicationType = if (isLinearMode) {
          LinearViewMode
        } else {
          EditSectionOnlyMode
        }
        val businessType = request.getBusinessType
        for {
          businessCustomerDetails <- save4LaterService.mainStore.fetchBusinessCustomerDetails(ar)
          businessDetails <- save4LaterService.mainStore.fetchBusinessDetails(ar)
          newApplicationType <- save4LaterService.mainStore.fetchNewApplicationType(ar)
          mode <- renderMode(newApplicationType, ar)
        } yield {
          val businessName = businessCustomerDetails.map(_.businessName).getOrElse("")
          businessDetails match {
            case Some(data) => {
              val extendedBusinessDetails = ExtendedBusinessDetails(Some(businessName), data.doYouHaveTradingName, data.tradingName, data.newAWBusiness)
              Ok(views.html.awrs_business_details(businessType, businessName, businessDetailsForm(businessType.get, accountUtils.hasAwrs(ar.enrolments)).form.fill(extendedBusinessDetails), mode, ar.enrolments, accountUtils))
            }
            case _ => Ok(views.html.awrs_business_details(businessType, businessName, businessDetailsForm(businessType.get, accountUtils.hasAwrs(ar.enrolments)).form, mode, ar.enrolments, accountUtils))
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
                         (implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
    save4LaterService.mainStore.saveBusinessDetails(authRetrievals, businessDetails) flatMap (_ => redirectRoute(Some(RedirectParam("No", id)), isNewRecord))
  }

  override def save(id: Int,
           redirectRoute: (Option[RedirectParam], Boolean) => Future[Result],
           viewApplicationType: ViewApplicationType,
           isNewRecord: Boolean,
           authRetrievals: StandardAuthRetrievals)
          (implicit request: Request[AnyContent]): Future[Result] = {
    implicit val viewMode: ViewApplicationType = viewApplicationType
    val businessType = request.getBusinessType
    businessDetailsForm(businessType.get, accountUtils.hasAwrs(authRetrievals.enrolments)).bindFromRequest.fold(
      formWithErrors =>
        for {
          businessCustomerDetails <- save4LaterService.mainStore.fetchBusinessCustomerDetails(authRetrievals)
          newApplicationType <- save4LaterService.mainStore.fetchNewApplicationType(authRetrievals)
          mode <- renderMode(newApplicationType, authRetrievals)
        } yield {
          BadRequest(views.html.awrs_business_details(businessType, businessCustomerDetails.fold("")(x => x.businessName), formWithErrors, mode, authRetrievals.enrolments, accountUtils))
        }
      ,
      extendedBusinessDetails => {
        (accountUtils.hasAwrs(authRetrievals.enrolments), businessType) match {
          case (false, _) => saveBusinessDetails(id, redirectRoute, isNewRecord, extendedBusinessDetails.getBusinessDetails, authRetrievals)
          case (true, Some("LLP_GRP") | Some("LTD_GRP")) => {
            save4LaterService.mainStore.fetchBusinessCustomerDetails(authRetrievals) flatMap {
              case Some(businessCustomerDetails) => {
                if (businessCustomerDetails.businessName != extendedBusinessDetails.businessName.get) {
                  keyStoreService.saveExtendedBusinessDetails(extendedBusinessDetails) flatMap (_ => Future.successful(Redirect(routes.BusinessNameChangeController.showConfirm())))
                } else {
                  saveBusinessDetails(id, redirectRoute, isNewRecord, extendedBusinessDetails.getBusinessDetails, authRetrievals)
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
