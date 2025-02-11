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

import config.ApplicationConfig
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AWRSFeatureSwitches

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AwrsUrnKickoutController @Inject()(mcc: MessagesControllerComponents,
                                         val awrsFeatureSwitches: AWRSFeatureSwitches,
                                         implicit val applicationConfig: ApplicationConfig,
                                         template: views.html.urn_kickout
                                      ) extends FrontendController(mcc)  {

  implicit val ec: ExecutionContext = mcc.executionContext
  val signInUrl: String = applicationConfig.signIn

  def showURNKickOutPage() : Action[AnyContent] = Action.async { implicit request =>
     if(awrsFeatureSwitches.enrolmentJourney().enabled) {
        Future.successful(Ok(template()))
     } else {
        Future.successful(NotFound)
     }
  }
}