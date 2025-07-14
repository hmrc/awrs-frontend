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
import controllers.auth.AwrsController
import forms.DeEnrollmentConfirmationForm.deEnrollmentConfirmationForm
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import services.DeEnrolService
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{AWRSFeatureSwitches, AccountUtils}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeEnrollmentConfirmationPageController @Inject() (mcc: MessagesControllerComponents,
                                                        implicit val applicationConfig: ApplicationConfig,
                                                        val awrsFeatureSwitches: AWRSFeatureSwitches,
                                                        val deEnrolService: DeEnrolService,
                                                        val authConnector: DefaultAuthConnector,
                                                        val accountUtils: AccountUtils,
                                                        val auditable: Auditable,
                                                        template: views.html.reenrolment.awrs_deenrolment_confirmation)
    extends FrontendController(mcc)
    with AwrsController {

  implicit val ec: ExecutionContext = mcc.executionContext
  val signInUrl: String             = applicationConfig.signIn

  def showDeEnrollmentConfirmationPage(): Action[AnyContent] = Action.async { implicit request =>
    enrolmentEligibleAuthorisedAction { implicit ar =>
      restrictedAccessCheck {
        if (awrsFeatureSwitches.enrolmentJourney().enabled) {
          Future.successful(Ok(template(deEnrollmentConfirmationForm)))
        } else {
          Future.successful(NotFound)
        }
      }
    }
  }

  def saveAndContinue: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    enrolmentEligibleAuthorisedAction { implicit ar =>
      restrictedAccessCheck {
        deEnrollmentConfirmationForm
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(BadRequest(template(formWithErrors))),
            deEnrollmentConfirmationResponse =>
              if (deEnrollmentConfirmationResponse == "Yes") {
                Future.successful(Redirect(routes.RegisteredPostcodeController.showPostCode))
              } else {
                Future.successful(Redirect(routes.KickoutController.showURNKickOutPage))

              }
          )
      }
    }
  }

}
