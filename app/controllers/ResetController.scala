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
import controllers.auth.AwrsController
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{DeEnrolService, Save4LaterService}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AccountUtils

import scala.concurrent.{ExecutionContext, Future}

class ResetController @Inject()(mcc: MessagesControllerComponents,
                                val save4LaterService: Save4LaterService,
                                val deEnrolService: DeEnrolService,
                                val authConnector: DefaultAuthConnector,
                                val auditable: Auditable,
                                val accountUtils: AccountUtils,
                                val applicationConfig: ApplicationConfig) extends FrontendController(mcc) with AwrsController {

  implicit val ec: ExecutionContext = mcc.executionContext
  val signInUrl: String = applicationConfig.signIn

  def resetApplication: Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { ar =>
      save4LaterService.mainStore.removeAll(ar)
      Future.successful(Redirect(routes.ApplicationController.logout()))
    }
  }

  def resetApplicationUpdate: Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { ar =>
      save4LaterService.mainStore.removeAll(ar)
      save4LaterService.api.removeAll(ar)
      Future.successful(Redirect(routes.ApplicationController.logout()))
    }
  }
}