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
import forms.ReapplicationForm._
import services.{DeEnrolService, KeyStoreService, Save4LaterService}
import utils.{AccountUtils, AwrsSessionKeys, LoggingUtils}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current


import scala.concurrent.Future

object ReapplicationController extends ReapplicationController {
  override val save4LaterService = Save4LaterService
  override val authConnector = FrontendAuthConnector
  override val deEnrolService = DeEnrolService
  override val keyStoreService = KeyStoreService
}

trait ReapplicationController extends AwrsController with LoggingUtils {
  val deEnrolService: DeEnrolService
  val save4LaterService: Save4LaterService
  val keyStoreService: KeyStoreService

  def show = async {
    implicit user => implicit request =>
      Future.successful(Ok(views.html.awrs_reapplication_confirmation(reapplicationForm)))
  }

  def submit = async {
    implicit user => implicit request =>
      reapplicationForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(views.html.awrs_reapplication_confirmation(formWithErrors)))
        ,
        success => for {
          _ <- deEnrolService.deEnrolAWRS(AccountUtils.getAwrsRefNo.toString(), getBusinessName.get, getBusinessType.get)
          _ <- deEnrolService.refreshProfile
          _ <- save4LaterService.mainStore.removeAll
          _ <- save4LaterService.api.removeAll
          _ <- keyStoreService.removeAll
        } yield Redirect(controllers.routes.HomeController.showOrRedirect(request.session.get(AwrsSessionKeys.sessionCallerId))).removingFromSession(
          AwrsSessionKeys.sessionBusinessType,
          AwrsSessionKeys.sessionBusinessName,
          AwrsSessionKeys.sessionPreviousLocation,
          AwrsSessionKeys.sessionCurrentLocation,
          AwrsSessionKeys.sessionStatusType,
          AwrsSessionKeys.sessionAwrsRefNo,
          AwrsSessionKeys.sessionJouneyStartLocation,
          AwrsSessionKeys.sessionSectionStatus
        )
      )

  }

}
