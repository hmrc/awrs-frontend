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

package controllers

import audit.Auditable
import config.ApplicationConfig
import connectors.AwrsDataCacheConnector
import controllers.auth.StandardAuthRetrievals
import controllers.util.{JourneyPage, RedirectParam, SaveAndRoutable}
import forms.TradingNameForm._
import javax.inject.Inject
import models._
import play.api.mvc._
import services.DataCacheKeys._
import services.{BusinessDetailsService, DataCacheService, KeyStoreService, Save4LaterService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.AccountUtils
import views.Configuration.NewApplicationMode
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, SubViewTemplateHelper, ViewApplicationType}

import scala.concurrent.{ExecutionContext, Future}

class TradingNameController @Inject()(val mcc: MessagesControllerComponents,
                                      val save4LaterService: Save4LaterService,
                                      val keyStoreService: KeyStoreService,
                                      val businessDetailsService: BusinessDetailsService,
                                      val authConnector: DefaultAuthConnector,
                                      val auditable: Auditable,
                                      val accountUtils: AccountUtils,
                                      val mainStoreSave4LaterConnector: AwrsDataCacheConnector,
                                      implicit val applicationConfig: ApplicationConfig) extends FrontendController(mcc) with JourneyPage with SaveAndRoutable with DataCacheService {

  override val section: String = businessDetailsName
  override implicit val ec: ExecutionContext = mcc.executionContext
  val signInUrl: String = applicationConfig.signIn

  def showTradingName(isLinearMode: Boolean): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
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
          businessDetails <- save4LaterService.mainStore.fetchBusinessNameDetails(ar)
        } yield {
          val businessName = businessCustomerDetails.map(_.businessName).getOrElse("")
          businessDetails match {
            case Some(data) =>
              val businessNameDetails = BusinessNameDetails(Some(businessName), data.doYouHaveTradingName, data.tradingName)
              Ok(views.html.awrs_trading_name(businessType, businessName, tradingNameForm(businessType.get, accountUtils.hasAwrs(ar.enrolments)).form.fill(businessNameDetails), ar.enrolments, accountUtils))
            case _ => Ok(views.html.awrs_trading_name(businessType, businessName, tradingNameForm(businessType.get, accountUtils.hasAwrs(ar.enrolments)).form, ar.enrolments, accountUtils))
          }
        }
      }
    }
  }

  def saveBusinessDetails(id: Int,
                          redirectRoute: (Option[RedirectParam], Boolean) => Future[Result],
                          isNewRecord: Boolean,
                          businessDetails: BusinessNameDetails,
                          authRetrievals: StandardAuthRetrievals)
                         (implicit request: Request[AnyContent], hc: HeaderCarrier, viewApplicationType: ViewApplicationType): Future[Result] = {
    save4LaterService.mainStore.saveBusinessNameDetails(authRetrievals, businessDetails) flatMap { _ =>
      businessDetailsService.businessDetailsPageRenderMode(authRetrievals) flatMap {
        case NewApplicationMode => Future.successful(Redirect(routes.TradingLegislationDateController.showBusinessDetails()))
        case _ => redirectRoute(Some(RedirectParam("No", id)), isNewRecord)
      }
    }
  }

  override def save(id: Int,
           redirectRoute: (Option[RedirectParam], Boolean) => Future[Result],
           viewApplicationType: ViewApplicationType,
           isNewRecord: Boolean,
           authRetrievals: StandardAuthRetrievals)
          (implicit request: Request[AnyContent]): Future[Result] = {
    implicit val viewMode: ViewApplicationType = viewApplicationType
    val businessType = request.getBusinessType
    tradingNameForm(businessType.get, accountUtils.hasAwrs(authRetrievals.enrolments)).bindFromRequest.fold(
      formWithErrors =>
        for {
          businessCustomerDetails <- save4LaterService.mainStore.fetchBusinessCustomerDetails(authRetrievals)
        } yield {
          BadRequest(views.html.awrs_trading_name(businessType, businessCustomerDetails.fold("")(x => x.businessName), formWithErrors, authRetrievals.enrolments, accountUtils))
        }
      ,
      businessNameDetails => {
        (accountUtils.hasAwrs(authRetrievals.enrolments), businessType) match {
          case (true, Some("LLP_GRP") | Some("LTD_GRP")) =>
            save4LaterService.mainStore.fetchBusinessCustomerDetails(authRetrievals) flatMap {
              case Some(businessCustomerDetails) =>
                if (businessCustomerDetails.businessName != businessNameDetails.businessName.get) {
                  keyStoreService.saveBusinessNameChange(businessNameDetails) map { _ =>
                    Redirect(routes.BusinessNameChangeController.showConfirm())
                  }
                } else {
                  saveBusinessDetails(id, redirectRoute, isNewRecord, businessNameDetails, authRetrievals)
                }
              case None => saveBusinessDetails(id, redirectRoute, isNewRecord, businessNameDetails, authRetrievals)
            }
          case _ => saveBusinessDetails(id, redirectRoute, isNewRecord, businessNameDetails, authRetrievals)
        }
      }
    )
  }
}
