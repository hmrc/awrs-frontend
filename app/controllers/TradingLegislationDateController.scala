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

package controllers

import audit.Auditable
import config.ApplicationConfig
import connectors.AwrsDataCacheConnector
import controllers.auth.StandardAuthRetrievals
import controllers.util.{JourneyPage, RedirectParam, SaveAndRoutable}
import forms.TradingLegislationDateForm.tradingLegislationForm
import forms.AWRSEnums.BooleanRadioEnum
import models.NewAWBusiness
import play.api.mvc._
import services.DataCacheKeys.businessDetailsName
import services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.WithUrlEncodedOnlyFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AccountUtils
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, ViewApplicationType}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TradingLegislationDateController @Inject()(val mcc: MessagesControllerComponents,
                                                 val save4LaterService: Save4LaterService,
                                                 val businessDetailsService: BusinessDetailsService,
                                                 val keyStoreService: KeyStoreService,
                                                 val deEnrolService: DeEnrolService,
                                                 val authConnector: DefaultAuthConnector,
                                                 val auditable: Auditable,
                                                 val accountUtils: AccountUtils,
                                                 val mainStoreSave4LaterConnector: AwrsDataCacheConnector,
                                                 implicit val applicationConfig: ApplicationConfig,
                                                 template: views.html.awrs_legislation_date) extends FrontendController(mcc) with JourneyPage with SaveAndRoutable with DataCacheService with WithUrlEncodedOnlyFormBinding {

  override val section: String = businessDetailsName
  override implicit val ec: ExecutionContext = mcc.executionContext
  val signInUrl: String = applicationConfig.signIn

  def showBusinessDetails(isLinearMode: Boolean): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { implicit ar =>
      restrictedAccessCheck {
        businessDetailsService.businessDetailsPageRenderMode(ar) flatMap {_ =>
          implicit val viewApplicationType: ViewApplicationType = if (isLinearMode) LinearViewMode else EditSectionOnlyMode
          val businessType = request.getBusinessType
          for {
            maybeAWBusiness <- save4LaterService.mainStore.fetchTradingStartDetails(ar)
          } yield {
            maybeAWBusiness match {
              case Some(data) =>
                Ok(template(tradingLegislationForm.fill(data.newAWBusiness), businessType))
              case _ => Ok(template(tradingLegislationForm, businessType))
            }
          }
        }
      }
    }
  }

  def saveBusinessDetails(id: Int, businessDetails: NewAWBusiness, redirectRoute: (Option[RedirectParam], Boolean) => Future[Result], authRetrievals: StandardAuthRetrievals)
                         (implicit hc: HeaderCarrier, requestHeader: RequestHeader, viewApplicationType: ViewApplicationType): Future[Result] = {
    save4LaterService.mainStore.saveTradingStartDetails(authRetrievals, businessDetails) flatMap {
      case NewAWBusiness(BooleanRadioEnum.YesString, _) =>
        keyStoreService.saveAlreadyTrading(already = true) flatMap { _ =>
          keyStoreService.saveIsNewBusiness(isNewBusiness = false) flatMap { _ =>
            viewApplicationType match {
              case LinearViewMode =>
                Future.successful(Redirect(routes.TradingDateController.showBusinessDetails(true)))
              case _ =>
                redirectRoute(Some(RedirectParam("No", id)), false)
            }
          }
        }
      case _ =>
        keyStoreService.saveIsNewBusiness(isNewBusiness = true) flatMap { _ =>
          viewApplicationType match {
            case LinearViewMode =>
              Future.successful(Redirect(routes.AlreadyStartingTradingController.showBusinessDetails(true)))
            case _ =>
              redirectRoute(Some(RedirectParam("No", id)), false)
          }
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
    businessDetailsService.businessDetailsPageRenderMode(authRetrievals) flatMap {_ =>
      val businessType = request.getBusinessType
      tradingLegislationForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(template(formWithErrors, businessType))),
        newAWBusiness =>
          save4LaterService.mainStore.fetchTradingStartDetails(authRetrievals) flatMap { fetchedAW =>
            val awToSave = fetchedAW match {
              case Some(fetchedAW) if fetchedAW.newAWBusiness == newAWBusiness => fetchedAW
              case _ => NewAWBusiness(newAWBusiness, None)
            }

            saveBusinessDetails(id, awToSave, redirectRoute, authRetrievals)
          }
      )
    }
  }

}
