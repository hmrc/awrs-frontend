/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers.auth

import controllers.routes
import uk.gov.hmrc.play.frontend.auth.GovernmentGateway
import play.api.mvc.{Request, Result}
import scala.concurrent.Future
import play.api.mvc.Results.Redirect

object AwrsRegistrationGovernmentGateway extends GovernmentGateway {
  override def continueURL: String = ExternalUrls.loginCallback
  override def loginURL: String = ExternalUrls.loginURL
  override def additionalLoginParameters: Map[String, Seq[String]] = Map("accountType" -> Seq(ExternalUrls.accountType))
  override def handleSessionTimeout(implicit request: Request[_]): Future[Result] = Future.successful(Redirect(routes.ApplicationController.timedOut().url))
}
