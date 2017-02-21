/*
 * Copyright 2017 HM Revenue & Customs
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

import controllers.auth.ExternalUrls
import play.api.Play
import play.api.mvc.Action
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.frontend.controller.{UnauthorisedAction, FrontendController}
import utils.SessionUtil._
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

object ApplicationController extends ApplicationController {}

trait ApplicationController extends FrontendController with RunMode {

  import play.api.Play.current

  def unauthorised = Action { implicit request =>
    Unauthorized(views.html.unauthorised())
  }

  def logout = Action { implicit request =>
    request getBusinessName match {
      case None => Redirect(ExternalUrls.signOut)
      case _ => Redirect(routes.SurveyController.showSurvey)
    }
  }

  def timedOut() = UnauthorisedAction {
    implicit request =>
      Redirect(ExternalUrls.signOut)
  }

  def keepAlive = UnauthorisedAction {
    implicit request =>
      Ok("OK")
  }

}