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
import controllers.auth.StandardAuthRetrievals
import controllers.util.{JourneyPage, RedirectParam, SaveAndRoutable}
import forms.TradingActivityForm._
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import services.DataCacheKeys._
import services.Save4LaterService
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.AccountUtils
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, ViewApplicationType}

import scala.concurrent.{ExecutionContext, Future}

class TradingActivityController @Inject()(val mcc: MessagesControllerComponents,
                                          val save4LaterService: Save4LaterService,
                                          val authConnector: DefaultAuthConnector,
                                          val auditable: Auditable,
                                          val accountUtils: AccountUtils,
                                          implicit val applicationConfig: ApplicationConfig) extends FrontendController(mcc) with JourneyPage with SaveAndRoutable {

  override implicit val ec: ExecutionContext = mcc.executionContext
  override val section: String = tradingActivityName
  val signInUrl: String = applicationConfig.signIn

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

  override def save(id: Int, redirectRoute: (Option[RedirectParam], Boolean) => Future[Result], viewApplicationType: ViewApplicationType, isNewRecord: Boolean, authRetrievals: StandardAuthRetrievals)
          (implicit request: Request[AnyContent]): Future[Result] = {
    implicit val viewMode: ViewApplicationType = viewApplicationType

    tradingActivityForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.awrs_trading_activity(formWithErrors))),
      tradingActivityData => {

        save4LaterService.mainStore.saveTradingActivity(authRetrievals, tradingActivityData) flatMap
          (_ => redirectRoute(Some(RedirectParam("No", id)), isNewRecord))
      }
    )
  }
}