/*
 * Copyright 2021 HM Revenue & Customs
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
import forms.ProductsForm._
import javax.inject.Inject
import play.api.mvc._
import services.DataCacheKeys._
import services.{DeEnrolService, Save4LaterService}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AccountUtils
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, ViewApplicationType}

import scala.concurrent.{ExecutionContext, Future}

class ProductsController @Inject()(val mcc: MessagesControllerComponents,
                                   val save4LaterService: Save4LaterService,
                                   val deEnrolService: DeEnrolService,
                                   val authConnector: DefaultAuthConnector,
                                   val auditable: Auditable,
                                   val accountUtils: AccountUtils,
                                   implicit val applicationConfig: ApplicationConfig,
                                   template: views.html.awrs_products) extends FrontendController(mcc) with JourneyPage with SaveAndRoutable {

  override implicit val ec: ExecutionContext = mcc.executionContext
  override val section: String = productsName
  val signInUrl: String = applicationConfig.signIn

  def showProducts(isLinearMode: Boolean): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { implicit ar =>
      restrictedAccessCheck {
        implicit val viewApplicationType: ViewApplicationType = if (isLinearMode) {
          LinearViewMode
        } else {
          EditSectionOnlyMode
        }

        save4LaterService.mainStore.fetchProducts(ar) flatMap {
          case Some(data) => Future.successful(Ok(template(productsForm.fill(data))))
          case _ => Future.successful(Ok(template(productsForm)))
        }
      }
    }
  }

  override def save(id: Int, redirectRoute: (Option[RedirectParam], Boolean) => Future[Result], viewApplicationType: ViewApplicationType, isNewRecord: Boolean, authRetrievals: StandardAuthRetrievals)
          (implicit request: Request[AnyContent]): Future[Result] = {
    implicit val viewMode: ViewApplicationType = viewApplicationType
    productsForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(template(formWithErrors)))
      ,
      productsData =>
        save4LaterService.mainStore.saveProducts(authRetrievals, productsData) flatMap {
          _ => redirectRoute(Some(RedirectParam("No", id)), isNewRecord)
        }
    )
  }
}
