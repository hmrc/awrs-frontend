/*
 * Copyright 2017 HM Revenue & Customs
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
import services.Save4LaterService
import utils.AccountUtils

import scala.concurrent.Future

trait ResetController extends AwrsController {
  val save4LaterService: Save4LaterService

  def resetApplication = async {
    implicit user => implicit request =>
      save4LaterService.mainStore.removeAll
      Future.successful(Redirect(routes.ApplicationController.logout))
  }

  def resetApplicationUpdate = async {
    implicit user => implicit request =>
      save4LaterService.mainStore.removeAll
      save4LaterService.api.removeAll
      Future.successful(Redirect(routes.ApplicationController.logout))
  }
}

object ResetController extends ResetController {
  override val authConnector = FrontendAuthConnector
  override val save4LaterService = Save4LaterService
}
