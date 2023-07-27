/*
 * Copyright 2023 HM Revenue & Customs
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
import forms.GroupDeclarationForm._
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import services._
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AccountUtils

import scala.concurrent.{ExecutionContext, Future}

class GroupDeclarationController @Inject()(mcc: MessagesControllerComponents,
                                           val save4LaterService: Save4LaterService,
                                           val deEnrolService: DeEnrolService,
                                           val authConnector: DefaultAuthConnector,
                                           val auditable: Auditable,
                                           val accountUtils: AccountUtils,
                                           implicit val applicationConfig: ApplicationConfig,
                                           template: views.html.awrs_group_declaration) extends FrontendController(mcc) with AwrsController {

  implicit val ec: ExecutionContext = mcc.executionContext
  val signInUrl: String = applicationConfig.signIn

  def showGroupDeclaration: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { implicit ar =>
      restrictedAccessCheck {
        save4LaterService.mainStore.fetchGroupDeclaration(ar) map {
          case Some(data) => Ok(template(groupDeclarationForm.fill(data)))
          case _ => Ok(template(groupDeclarationForm))
        }
      }
    }
  }

  def sendConfirmation: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { ar =>
      groupDeclarationForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(template(formWithErrors))),
        groupDeclarationData =>
          save4LaterService.mainStore.saveGroupDeclaration(ar, groupDeclarationData) flatMap (_ => Future.successful(Redirect(controllers.routes.IndexController.showIndex)))
      )
    }
  }
}
