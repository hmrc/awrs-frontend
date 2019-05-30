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
import controllers.auth.{AwrsController, ExternalUrls}
import play.api.mvc.{Action, AnyContent}
import services.Save4LaterService

import scala.concurrent.Future

trait ResetController extends AwrsController {
  val save4LaterService: Save4LaterService

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

object ResetController extends ResetController {
  override val authConnector = FrontendAuthConnector
  override val save4LaterService = Save4LaterService
  val signInUrl = ExternalUrls.signIn
}
