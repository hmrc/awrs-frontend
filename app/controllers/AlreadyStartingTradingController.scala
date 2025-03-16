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
import forms.AWRSEnums.BooleanRadioEnum
import forms.AlreadyStartingTradingForm._
import models.NewAWBusiness
import play.api.mvc._
import services.DataCacheKeys._
import services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AccountUtils
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, ViewApplicationType}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AlreadyStartingTradingController @Inject()(val mcc: MessagesControllerComponents,
                                                 val save4LaterService: Save4LaterService,
                                                 val businessDetailsService: BusinessDetailsService,
                                                 val keyStoreService: KeyStoreService,
                                                 val deEnrolService: DeEnrolService,
                                                 val authConnector: DefaultAuthConnector,
                                                 val auditable: Auditable,
                                                 val accountUtils: AccountUtils,
                                                 val mainStoreSave4LaterConnector: AwrsDataCacheConnector,
                                                 implicit val applicationConfig: ApplicationConfig,
                                                 template: views.html.awrs_already_starting_trading) extends FrontendController(mcc) with JourneyPage with SaveAndRoutable with DataCacheService {

  override val section: String = businessDetailsName
  override implicit val ec: ExecutionContext = mcc.executionContext
  val signInUrl: String = applicationConfig.signIn

  def showBusinessDetails(isLinearMode: Boolean): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { implicit ar =>
      val businessType = request.getBusinessType
      implicit val viewApplicationType: ViewApplicationType = if (isLinearMode) LinearViewMode else EditSectionOnlyMode
      restrictedAccessCheck {
        businessDetailsService.businessDetailsPageRenderMode(ar) flatMap {_ =>
          for {
            alreadyTrading <- keyStoreService.fetchAlreadyTrading
            startTradingDetails <- save4LaterService.mainStore.fetchTradingStartDetails(ar)
          } yield {
            (startTradingDetails, alreadyTrading) match {
              case (Some(NewAWBusiness(BooleanRadioEnum.YesString, _)), _) =>
                Redirect(routes.TradingDateController.showBusinessDetails(isLinearMode))
              case (Some(NewAWBusiness(_, Some(_))), Some(data)) =>
                val yesNo = if (data) BooleanRadioEnum.YesString else BooleanRadioEnum.NoString
                Ok(template(alreadyStartedTradingForm.fill(yesNo), businessType))
              case _ =>
                Ok(template(alreadyStartedTradingForm, businessType))
            }
          }
        }
      }
    }
  }

  def saveBusinessDetails(newAlreadyTrading: Boolean,
                          existingAlreadyTrading: Option[Boolean],
                          tradingStartDetails: Option[NewAWBusiness],
                          authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier, viewMode: ViewApplicationType): Future[Result] =
    keyStoreService.saveAlreadyTrading(newAlreadyTrading).flatMap{_ =>
      (newAlreadyTrading, existingAlreadyTrading, tradingStartDetails) match {
        case (nat, Some(eat), Some(NewAWBusiness(yOrN,_))) if nat != eat =>
          // If alreadytrading flag has changed, clear any current start date
          save4LaterService.mainStore.saveTradingStartDetails(authRetrievals, NewAWBusiness(yOrN, None))
        case _ => Future.successful(())
      }
    }.map(_ => Redirect(routes.TradingDateController.showBusinessDetails(viewMode == LinearViewMode)))

  override def save(id: Int,
                    redirectRoute: (Option[RedirectParam], Boolean) => Future[Result],
                    viewApplicationType: ViewApplicationType,
                    isNewRecord: Boolean,
                    authRetrievals: StandardAuthRetrievals)
                   (implicit request: Request[AnyContent]): Future[Result] = {
    implicit val viewMode: ViewApplicationType = viewApplicationType
    businessDetailsService.businessDetailsPageRenderMode(authRetrievals) flatMap {_ =>
      alreadyStartedTradingForm.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(template(formWithErrors, request.getBusinessType))),
        newAWBusiness =>
          keyStoreService.fetchAlreadyTrading.flatMap{currentAlreadyTrading =>
            save4LaterService.mainStore.fetchTradingStartDetails(authRetrievals).flatMap{tradingStartDetails =>
              saveBusinessDetails(newAWBusiness ==BooleanRadioEnum.YesString, currentAlreadyTrading, tradingStartDetails, authRetrievals)
            }
          }
      )
    }
  }
}
