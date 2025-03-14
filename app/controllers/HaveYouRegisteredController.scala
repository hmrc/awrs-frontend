/*
 * Copyright 2025 HM Revenue & Customs
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
import forms.HaveYouRegisteredForm.haveYouRegisteredForm
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import services.{DeEnrolService, KeyStoreService}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{AWRSFeatureSwitches, AccountUtils}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class HaveYouRegisteredController @Inject()(val mcc: MessagesControllerComponents,
                                            val keyStoreService: KeyStoreService,
                                            val authConnector: DefaultAuthConnector,
                                            val accountUtils: AccountUtils,
                                            val deEnrolService: DeEnrolService,
                                            val auditable: Auditable,
                                            val awrsFeatureSwitches: AWRSFeatureSwitches,
                                            implicit val applicationConfig: ApplicationConfig,
                                            template: views.html.awrs_have_you_registered) extends FrontendController(mcc) with AwrsController{

  implicit val ec: ExecutionContext = mcc.executionContext
  val signInUrl: String = applicationConfig.signIn


  def showHaveYouRegisteredPage(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    btaAuthorisedAction { _ =>
      if (awrsFeatureSwitches.enrolmentJourney().enabled) {
        keyStoreService.fetchHaveYouRegistered flatMap {
          case Some(hasUserRegistered) => Future.successful(Ok(template(haveYouRegisteredForm.form.fill(hasUserRegistered))))
          case _ => Future.successful(Ok(template(haveYouRegisteredForm.form)))
        }
      } else {
        Future.successful(NotFound)
      }
    }
  }

  def saveAndContinue(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    btaAuthorisedAction { _ =>
      if (awrsFeatureSwitches.enrolmentJourney().enabled) {
        haveYouRegisteredForm.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(template(formWithErrors))),
          haveYouRegisteredData =>
            keyStoreService.saveHaveYouRegistered(haveYouRegisteredData) flatMap { _ =>
              if (haveYouRegisteredData.hasUserRegistered.getOrElse(false)) {
                Future.successful(Redirect(controllers.routes.AwrsUrnController.showArwsUrnPage))
              }
              else {
                Future.successful(Redirect(applicationConfig.businessCustomerStartPage))
              }
            }
        )
      } else {
        Future.successful(NotFound)
      }
    }
  }

  def showLastLocation(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    btaAuthorisedAction { implicit standardAuthRetrievals =>
      restrictedAccessCheck {
        Future.successful(Redirect(sessionUtil(request).getPreviousLocation.fold("/alcohol-wholesale-scheme/have-you-registered")(x => x)))
      }
    }
  }
}
