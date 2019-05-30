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
import forms.TradingActivityForm._
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.DataCacheKeys._
import services.Save4LaterService
import utils.AccountUtils
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, ViewApplicationType}

import scala.concurrent.Future

trait TradingActivityController extends AwrsController with JourneyPage with AccountUtils with SaveAndRoutable {

  override val section = tradingActivityName

  def showTradingActivity(isLinearMode: Boolean): Action[AnyContent] = Action.async { implicit request =>
    restrictedAccessCheck {
      authorisedAction { ar =>
        implicit val viewApplicationType: ViewApplicationType = if (isLinearMode) {
          LinearViewMode
        } else {
          EditSectionOnlyMode
        }

        save4LaterService.mainStore.fetchTradingActivity(ar) map {
          case Some(data) => Ok(views.html.awrs_trading_activity(tradingActivityForm.fill(data)))
          case _ => Ok(views.html.awrs_trading_activity(tradingActivityForm))
        }
      }
    }
  }

  def save(id: Int, redirectRoute: (Option[RedirectParam], Boolean) => Future[Result], viewApplicationType: ViewApplicationType, isNewRecord: Boolean, authRetrievals: StandardAuthRetrievals)
          (implicit request: Request[AnyContent]): Future[Result] = {
    implicit val viewMode: ViewApplicationType = viewApplicationType

    tradingActivityForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.awrs_trading_activity(formWithErrors))),
      tradingActivityData =>{

        save4LaterService.mainStore.saveTradingActivity(authRetrievals, tradingActivityData) flatMap
          (_ => redirectRoute(Some(RedirectParam("No", id)), isNewRecord))
      }
    )
  }
}

object TradingActivityController extends TradingActivityController {
  override val authConnector = FrontendAuthConnector
  override val save4LaterService = Save4LaterService
  val signInUrl = ExternalUrls.signIn
}
