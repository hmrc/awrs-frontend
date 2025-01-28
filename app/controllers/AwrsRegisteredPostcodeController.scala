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
import forms.AwrsRegisteredPostcodeForm
import forms.AwrsRegisteredPostcodeForm.awrsRegisteredPostcodeForm
import models.AwrsRegisteredPostcode
import play.api.mvc._
import services.{DeEnrolService, KeyStoreService}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AccountUtils

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AwrsRegisteredPostcodeController @Inject()(val mcc: MessagesControllerComponents,
                                                 implicit val applicationConfig: ApplicationConfig,
                                                 val authConnector: DefaultAuthConnector,
                                                 val accountUtils: AccountUtils,
                                                 val deEnrolService: DeEnrolService,
                                                 val auditable: Auditable,
                                                 template: views.html.awrs_registered_postcode) extends FrontendController(mcc) with AwrsController {

 
  val signInUrl = applicationConfig.signIn
  implicit val ec: ExecutionContext = mcc.executionContext

  def showPostCode(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { implicit ar =>
      if (applicationConfig.enrolmentJourney)
        Future.successful(Ok(template(awrsRegisteredPostcodeForm.fill(AwrsRegisteredPostcode("")))))
      else
        Future.successful(NotFound)
    }
   }

  /*def showHaveYouRegisteredPage(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { implicit ar =>
      keyStoreService.fetchHaveYouRegistered flatMap {
        case Some(data) => Future.successful(Ok(template(awrsRegisteredPostcodeForm.fill(data))))
        case _ => Future.successful(Ok(template(awrsRegisteredPostcodeForm)))
      }
    }
  }*/

  def saveAndContinue = Action.async { implicit request: Request[AnyContent] =>
    awrsRegisteredPostcodeForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(template(formWithErrors))),
      data =>  Future.successful(Redirect(controllers.routes.IndexController.showIndex))

     /* awrsRegisteredPostcodeData => keyStoreService.saveHaveYouRegistered(awrsRegisteredPostcodeData) flatMap(_ => Future.successful(Redirect(controllers.routes.IndexController.showIndex)))*/
    )
  }


}