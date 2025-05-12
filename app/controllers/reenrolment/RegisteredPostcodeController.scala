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
import forms.reenrolment.RegisteredPostcodeForm.awrsRegisteredPostcodeForm
import play.api.mvc._
import services.{DeEnrolService, KeyStoreService}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{AWRSFeatureSwitches, AccountUtils}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegisteredPostcodeController @Inject()(mcc: MessagesControllerComponents,
                                             implicit val applicationConfig: ApplicationConfig,
                                             val authConnector: DefaultAuthConnector,
                                             val accountUtils: AccountUtils,
                                             val deEnrolService: DeEnrolService,
                                             val auditable: Auditable,
                                             awrsFeatureSwitches: AWRSFeatureSwitches,
                                             keyStoreService: KeyStoreService,
                                             template: views.html.reenrolment.awrs_registered_postcode) extends FrontendController(mcc) with AwrsController {


  val signInUrl: String = applicationConfig.signIn
  implicit val ec: ExecutionContext = mcc.executionContext

  def showPostCode(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    enrolmentEligibleAuthorisedAction { implicit ar =>
      restrictedAccessCheck {
        if (awrsFeatureSwitches.enrolmentJourney().enabled)
          keyStoreService.fetchAwrsRegisteredPostcode map {
            case Some(registeredPostcode) => Ok(template(awrsRegisteredPostcodeForm.form.fill(registeredPostcode)))
            case _ => Ok(template(awrsRegisteredPostcodeForm.form))
          }
        else Future.successful(NotFound)
      }
    }
  }

  def saveAndContinue: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    enrolmentEligibleAuthorisedAction { implicit ar =>
      restrictedAccessCheck {
        awrsRegisteredPostcodeForm.bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(template(formWithErrors))),
          postcode => {
            keyStoreService.saveAwrsRegisteredPostcode(postcode) flatMap {
              _ => Future.successful(Redirect(controllers.reenrolment.routes.RegisteredUtrController.showArwsUtrPage))
            }
          })
      }
    }
  }
}