/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.util

import controllers.auth.AwrsController
import play.api.mvc.{Action, AnyContent, Request}

import scala.concurrent.Future


trait JourneyPage extends AwrsController {

  val section: String

  /*
  * This override is designed for controlling the behaviour of the back button during the linear journey, it adds
  * the JourneyStartLocation to the session if and only if it is not already defined.
  * This variable is used to determine at which point the user entered the linear journey and will be cleared when
  * hitting the index page.
  */
  override def asyncRestrictedAccess(body: AsyncUserRequest): Action[AnyContent] =
  super.asyncRestrictedAccess {
    implicit user => implicit request =>
      val future = body(user)(request)
      // only add the JourneyStartLocation if it's not already defined in the session
      // because otherwise every section will overwrite this variable
      request.getJourneyStartLocation match {
        case None => future.flatMap(result => Future.successful(result.addJouneyStartLocationToSession(section)))
        case _ => future
      }
  }

  def getJourneyStartLocation(implicit request: Request[AnyContent]): Option[String] =
    request getJourneyStartLocation
}
