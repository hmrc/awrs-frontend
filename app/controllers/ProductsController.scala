/*
 * Copyright 2018 HM Revenue & Customs
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
import forms.ProductsForm._
import play.api.Logger
import play.api.mvc.{AnyContent, Request, Result}
import services.DataCacheKeys._
import services.Save4LaterService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.AccountUtils
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, ViewApplicationType}

import scala.concurrent.Future

trait ProductsController extends AwrsController with JourneyPage with AccountUtils with SaveAndRoutable {

  override val section = productsName

  def showProducts(isLinearMode: Boolean) = asyncRestrictedAccess {
    implicit user => implicit request =>
      implicit val viewApplicationType = isLinearMode match {
        case true => LinearViewMode
        case false => EditSectionOnlyMode
      }

      save4LaterService.mainStore.fetchProducts flatMap {
        case Some(data) => Future.successful(Ok(views.html.awrs_products(productsForm.fill(data))))
        case _ => Future.successful(Ok(views.html.awrs_products(productsForm)))
      }
  }

  def save(id: Int, redirectRoute: (Option[RedirectParam], Boolean) => Future[Result], viewApplicationType: ViewApplicationType, isNewRecord: Boolean)(implicit request: Request[AnyContent], user: AuthContext): Future[Result] = {
    implicit val viewMode = viewApplicationType
    productsForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.awrs_products(formWithErrors)))
      ,
      productsData =>
        save4LaterService.mainStore.saveProducts(productsData) flatMap {
          _ => redirectRoute(Some(RedirectParam("No", id)), isNewRecord)
        }
    )
  }

}

object ProductsController extends ProductsController {
  override val authConnector = FrontendAuthConnector
  override val save4LaterService = Save4LaterService
}
