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
import controllers.auth.AwrsController
import play.api.mvc.{Action, AnyContent}
import services._
import utils.AwrsConfig.emailVerificationEnabled
import views.html.{awrs_email_verification_error, awrs_email_verification_success}

import scala.concurrent.Future

trait EmailVerificationController extends AwrsController {

  val save4LaterService: Save4LaterService
  val emailVerificationService: EmailVerificationService
  val isEmailVerificationEnabled: Boolean

  def checkEmailVerification: Action[AnyContent] = async {
    implicit user =>
      implicit request =>
        isEmailVerificationEnabled match {
          case true =>
            for {
              businessContacts <- save4LaterService.mainStore.fetchBusinessContacts
              isEmailVerified <- emailVerificationService.isEmailVerified(businessContacts)
            } yield {
              isEmailVerified match {
                case true =>
                  Redirect(routes.ApplicationDeclarationController.showApplicationDeclaration())
                case _ =>
                  Ok(awrs_email_verification_error(businessContacts.fold("")(x => x.email.fold("")(x => x))))
              }
            }
          case _ =>
            Future.successful(Redirect(routes.ApplicationDeclarationController.showApplicationDeclaration()))
        }
  }

  def resend: Action[AnyContent] = async {
    implicit user =>
      implicit request =>
        save4LaterService.mainStore.fetchBusinessContacts flatMap {
          case Some(businessDetails) => {
            val email = businessDetails.email
            email match {
              case Some(businessEmail) => {
                emailVerificationService.sendVerificationEmail(businessEmail) map {
                  case true => {
                    Ok(awrs_email_verification_error(businessEmail, resent = true))}
                  case _ => showErrorPageRaw
                }
              }
              case _ => {
                Future.successful(showErrorPageRaw)
              }
            }
          }
          case _ => Future.successful(showErrorPageRaw)
        }
  }

  def showSuccess: Action[AnyContent]  = Action.async {
    implicit request =>
      Future.successful(Ok(awrs_email_verification_success()))
  }

}

object EmailVerificationController extends EmailVerificationController {
  override val authConnector = FrontendAuthConnector
  override val save4LaterService = Save4LaterService
  override val emailVerificationService = EmailVerificationService
  override val isEmailVerificationEnabled = emailVerificationEnabled
}
