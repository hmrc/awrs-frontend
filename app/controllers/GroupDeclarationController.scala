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
import controllers.auth.{AwrsController, AwrsRegistrationRegime}
import forms.GroupDeclarationForm._
import services._
import utils.AccountUtils
import scala.concurrent.Future
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

trait GroupDeclarationController extends AwrsController with AccountUtils {

  val save4LaterService: Save4LaterService

  def showGroupDeclaration = asyncRestrictedAccess {
    implicit user => implicit request =>
      save4LaterService.mainStore.fetchGroupDeclaration map {
        case Some(data) => Ok(views.html.awrs_group_declaration(groupDeclarationForm.fill(data)))
        case _ => Ok(views.html.awrs_group_declaration(groupDeclarationForm))
      }
  }

  def sendConfirmation = async {
    implicit user => implicit request =>
      groupDeclarationForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(views.html.awrs_group_declaration(formWithErrors))),
        groupDeclarationData =>
          save4LaterService.mainStore.saveGroupDeclaration(groupDeclarationData) flatMap {
            case _ => Future.successful(Redirect(controllers.routes.IndexController.showIndex()))
          }
      )
  }
}

object GroupDeclarationController extends GroupDeclarationController {
  override val authConnector = FrontendAuthConnector
  override val save4LaterService = Save4LaterService
}
