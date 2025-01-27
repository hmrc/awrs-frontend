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

class AwrsNoUrnController @Inject()(val mcc: MessagesControllerComponents,
                                    implicit val applicationConfig: ApplicationConfig,
                                    template: views.html.no_urn_kickout) extends FrontendController(mcc) {

 
  val signInUrl: String = applicationConfig.signIn
  implicit val ec: ExecutionContext = mcc.executionContext

  def showNoURNKickOutPage() : Action[AnyContent] = Action.async { implicit request =>
    print("test")
     if(AWRSFeatureSwitches.enrolmentJourney().enabled)
        Future.successful(Ok(template()))
     else
        Future.successful(NotFound)

   }
}