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
import forms.ProductsForm._
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.DataCacheKeys._
import services.Save4LaterService
import utils.AccountUtils
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, ViewApplicationType}

import scala.concurrent.Future

trait ProductsController extends AwrsController with JourneyPage with AccountUtils with SaveAndRoutable {

  override val section = productsName

  def showProducts(isLinearMode: Boolean): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    restrictedAccessCheck {
      authorisedAction { ar =>
        implicit val viewApplicationType: ViewApplicationType = if (isLinearMode) {
          LinearViewMode
        } else {
          EditSectionOnlyMode
        }

        save4LaterService.mainStore.fetchProducts(ar) flatMap {
          case Some(data) => Future.successful(Ok(views.html.awrs_products(productsForm.fill(data))))
          case _ => Future.successful(Ok(views.html.awrs_products(productsForm)))
        }
      }
    }
  }

  def save(id: Int, redirectRoute: (Option[RedirectParam], Boolean) => Future[Result], viewApplicationType: ViewApplicationType, isNewRecord: Boolean, authRetrievals: StandardAuthRetrievals)
          (implicit request: Request[AnyContent]): Future[Result] = {
    implicit val viewMode = viewApplicationType
    productsForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.awrs_products(formWithErrors)))
      ,
      productsData =>
        save4LaterService.mainStore.saveProducts(authRetrievals, productsData) flatMap {
          _ => redirectRoute(Some(RedirectParam("No", id)), isNewRecord)
        }
    )
  }

}

object ProductsController extends ProductsController {
  override val authConnector = FrontendAuthConnector
  override val save4LaterService = Save4LaterService
  val signInUrl = ExternalUrls.signIn
}
