/*
 * Copyright 2020 HM Revenue & Customs
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

import java.io.{PrintWriter, StringWriter}

import com.googlecode.htmlcompressor.compressor.HtmlCompressor
import config.ApplicationConfig
import models.FormBundleStatus
import models.FormBundleStatus.{Rejected, RejectedUnderReviewOrAppeal, Revoked, RevokedUnderReviewOrAppeal}
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import play.twirl.api.Content
import uk.gov.hmrc.auth.core.AuthConnector
import utils.{AccountUtils, LoggingUtils, SessionUtil}

import scala.concurrent.Future

trait AwrsController extends LoggingUtils with AuthFunctionality with I18nSupport with Results {

  val authConnector: AuthConnector
  val accountUtils: AccountUtils

  def restrictedAccessCheck(body: => Future[Result])(implicit request: Request[AnyContent]): Future[Result] =
    getSessionStatus match {
      case Some(Rejected) | Some(RejectedUnderReviewOrAppeal) | Some(Revoked) | Some(RevokedUnderReviewOrAppeal) =>
        Future.successful(Redirect(controllers.routes.ApplicationStatusController.showStatus()))
      case _ => body
    }

  def showErrorPageRaw(implicit request: Request[AnyContent]): Result =
    AwrsController.showErrorPageRaw

  def showErrorPage(implicit request: Request[AnyContent]): Future[Result] =
    AwrsController.showErrorPage

  def showNotFoundPage(implicit request: Request[AnyContent]): Future[Result] =
    AwrsController.showNotFoundPage

  implicit val sessionUtil: Request[AnyContent] => SessionUtil.SessionUtilForRequest = SessionUtil.sessionUtilForRequest
  implicit val sessionUtilForResult: Result => SessionUtil.SessionUtilForResult = SessionUtil.sessionUtilForResult

  def getSessionStatus(implicit request: Request[AnyContent]): Option[FormBundleStatus] =
    request getSessionStatus

  def getSessionStatusStr(implicit request: Request[AnyContent]): Option[String] =
    request getSessionStatusStr

  def getBusinessType(implicit request: Request[AnyContent]): Option[String] =
    request getBusinessType

  def getBusinessName(implicit request: Request[AnyContent]): Option[String] =
    request getBusinessName

  def Ok(content: Content)(implicit request: Request[AnyContent]): Result = OkNoLocation(content) addLocation

  // OKNoLocation helper is used to avoid storing the location history to the session
  def OkNoLocation(content: Content)(implicit request: Request[AnyContent]): Result = play.api.mvc.Results.Ok(AwrsController.prettify(content)).as("text/html; charset=utf-8")

}


object AwrsController extends Results {

  def showErrorPageRaw(implicit request: Request[AnyContent], messages: Messages, applicationConfig: ApplicationConfig): Result =
    InternalServerError(views.html.awrs_application_error())

  def showErrorPage(implicit request: Request[AnyContent], messages: Messages, applicationConfig: ApplicationConfig): Future[Result] =
    Future.successful(showErrorPageRaw)

  def showBadRequestRaw(implicit request: Request[AnyContent], messages: Messages, applicationConfig: ApplicationConfig): Result =
    BadRequest(views.html.awrs_application_error())

  def showBadRequest(implicit request: Request[AnyContent], messages: Messages, applicationConfig: ApplicationConfig): Future[Result] =
    Future.successful(showBadRequestRaw)

  def showNotFoundPageRaw(implicit request: Request[AnyContent], messages: Messages, applicationConfig: ApplicationConfig): Result =
    NotFound(views.html.helpers.awrsErrorNotFoundTemplate())

  def showNotFoundPage(implicit request: Request[AnyContent], messages: Messages, applicationConfig: ApplicationConfig): Future[Result] =
    Future.successful(showNotFoundPageRaw)

  private val compressor: HtmlCompressor = new HtmlCompressor()

  def prettify(content: Content): String = {
    val output: String = content.body.trim()
    compressor.compress(output)
  }
}
