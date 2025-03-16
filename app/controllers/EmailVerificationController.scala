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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import services._
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AccountUtils
import views.html.{awrs_email_verification_error, awrs_email_verification_success}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmailVerificationController @Inject()(mcc: MessagesControllerComponents,
                                            emailVerificationService: EmailVerificationService,
                                            val auditable: Auditable,
                                            val accountUtils: AccountUtils,
                                            val deEnrolService: DeEnrolService,
                                            val save4LaterService: Save4LaterService,
                                            val authConnector: DefaultAuthConnector,
                                            implicit val applicationConfig: ApplicationConfig,
                                            templateError: awrs_email_verification_error,
                                            templateSuccess: awrs_email_verification_success) extends FrontendController(mcc) with AwrsController {

  implicit val ec: ExecutionContext = mcc.executionContext
  lazy val isEmailVerificationEnabled: Boolean = applicationConfig.emailVerificationEnabled
  lazy val signInUrl: String = applicationConfig.signIn

  def checkEmailVerification: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { ar =>
      if (isEmailVerificationEnabled) {
        for {
          businessContacts <- save4LaterService.mainStore.fetchBusinessContacts(ar)
          isEmailVerified <- emailVerificationService.isEmailVerified(businessContacts)
        } yield {
          if (isEmailVerified) {
            Redirect(routes.ApplicationDeclarationController.showApplicationDeclaration)
          } else {
            Ok(templateError(businessContacts.fold("")(x => x.email.fold("")(x => x))))
          }
        }
      } else {
        Future.successful(Redirect(routes.ApplicationDeclarationController.showApplicationDeclaration))
      }
    }
  }

  def resend: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { ar =>
      save4LaterService.mainStore.fetchBusinessContacts(ar) flatMap {
        case Some(businessDetails) =>
          val email = businessDetails.email
          email match {
            case Some(businessEmail) =>
              emailVerificationService.sendVerificationEmail(businessEmail) map {
                case true =>
                  Ok(templateError(businessEmail, resent = true))
                case _ => showErrorPageRaw
              }
            case _ =>
              Future.successful(showErrorPageRaw)
          }
        case _ => Future.successful(showErrorPageRaw)
      }
    }
  }

  def showSuccess: Action[AnyContent] = Action.async {
    implicit request =>
      Future.successful(Ok(templateSuccess()))
  }

}
