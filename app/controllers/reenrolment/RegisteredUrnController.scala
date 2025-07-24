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

package controllers.reenrolment

import audit.Auditable
import config.ApplicationConfig
import controllers.auth.{AwrsController, StandardAuthRetrievals}
import forms.reenrolment.RegisteredUrnForm.awrsEnrolmentUrnForm
import models.reenrolment.{UserIsEnrolled, UserIsNotEnrolled}
import play.api.mvc._
import services.reenrolment.RegisteredUrnService
import services.{DeEnrolService, KeyStoreService}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{AWRSFeatureSwitches, AccountUtils}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegisteredUrnController @Inject() (mcc: MessagesControllerComponents,
                                         keyStoreService: KeyStoreService,
                                         val deEnrolService: DeEnrolService,
                                         val authConnector: DefaultAuthConnector,
                                         val auditable: Auditable,
                                         val accountUtils: AccountUtils,
                                         awrsFeatureSwitches: AWRSFeatureSwitches,
                                         implicit val applicationConfig: ApplicationConfig,
                                         registeredUrnService: RegisteredUrnService,
                                         template: views.html.reenrolment.awrs_registered_urn)
    extends FrontendController(mcc)
    with AwrsController {

  implicit val ec: ExecutionContext = mcc.executionContext
  val signInUrl: String             = applicationConfig.signIn

  def showArwsUrnPage(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    enrolmentEligibleAuthorisedAction { implicit ar =>
      restrictedAccessCheck {
        if (awrsFeatureSwitches.enrolmentJourney().enabled) {
          keyStoreService.fetchAwrsEnrolmentUrn flatMap {
            case Some(awrsUrn) => Future.successful(Ok(template(awrsEnrolmentUrnForm.form.fill(awrsUrn))))
            case _             => Future.successful(Ok(template(awrsEnrolmentUrnForm.form)))
          }
        } else { Future.successful(NotFound) }
      }
    }
  }

  def saveAndContinue(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    enrolmentEligibleAuthorisedAction { implicit ar: StandardAuthRetrievals =>
      restrictedAccessCheck {
        if (awrsFeatureSwitches.enrolmentJourney().enabled) {
          awrsEnrolmentUrnForm
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(template(formWithErrors))),
              awrsUrn => {
                println("awrsUrn " + awrsUrn)
                println("ar " + ar.plainTextCredId)
                registeredUrnService.handleAWRSRefChecks(ar.plainTextCredId, awrsUrn).map {
                  case UserIsEnrolled => Redirect(routes.DeEnrolmentConfirmationController.showDeEnrolmentConfirmationPage)
                  case UserIsNotEnrolled => Redirect(routes.RegisteredPostcodeController.showPostCode)
                  case _ => Redirect(routes.KickoutController.showURNKickOutPage)
                }
              }
            )
        } else {
          Future.successful(NotFound)
        }
      }
    }
  }

}
