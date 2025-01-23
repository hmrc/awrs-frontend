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
import forms.AwrsEnrollmentUrnForm.{awrsEnrolmentUrnForm, awrsEnrolmentUrnValidationForm}
import play.api.mvc._
import services.apis.AwrsAPI5
import services.{DeEnrolService, KeyStoreService}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AccountUtils
import utils.{AWRSFeatureSwitches, AccountUtils}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AwrsUrnController @Inject()(mcc: MessagesControllerComponents,
                                  api5: AwrsAPI5,
                                  val keyStoreService: KeyStoreService,
                                  val deEnrolService: DeEnrolService,
                                  val authConnector: DefaultAuthConnector,
                                  val auditable: Auditable,
                                  val accountUtils: AccountUtils,
                                  implicit val applicationConfig: ApplicationConfig,
                                  template: views.html.awrs_urn
                                      ) extends FrontendController(mcc) with AwrsController {

  val signInUrl: String = applicationConfig.signIn

  def showURNKickOutPage() : Action[AnyContent] = Action.async { implicit request =>
     if(AWRSFeatureSwitches.enrolmentJourney().enabled)
        Future.successful(Ok(template()))
     else
        Future.successful(NotFound)
   }
}