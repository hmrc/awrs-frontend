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

package controllers.auth

import java.io.{PrintWriter, StringWriter}

import com.googlecode.htmlcompressor.compressor.HtmlCompressor
import config.ApplicationGlobal
import models.FormBundleStatus
import models.FormBundleStatus.{Rejected, RejectedUnderReviewOrAppeal, Revoked, RevokedUnderReviewOrAppeal}
import play.api.mvc._
import play.twirl.api._
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.{AccountUtils, LoggingUtils, SessionUtil}



import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

trait AwrsController extends FrontendController with Actions with LoggingUtils with AccountUtils {

  type AsyncUserRequest = (AuthContext) => (Request[AnyContent]) => Future[Result]

  implicit class StackTraceUtil(e: Throwable) {
    def getStacktraceString: String = {
      val stringWriter: StringWriter = new StringWriter()
      val printWriter: PrintWriter = new PrintWriter(stringWriter)
      e.printStackTrace(printWriter)
      stringWriter.toString
    }
  }

  @inline def async(body: AsyncUserRequest): Action[AnyContent] =
    AuthorisedFor(AwrsRegistrationRegime, pageVisibility = GGConfidence).async {
      implicit user => implicit request =>
        Try(executeBody(body)) match {
          case Success(future) => future
          case Failure(ex) =>
            val logInfo = s"Error occured for:\nBusiness name : ${getBusinessName.fold("unknown")(x => x)}\nAWRS reference : " + {
              hasAwrs match {
                case true => s"${getUtrOrName()}\n"
                case _ => "Does not have an AWRS reference number\n"
              }
            } + "stacktrace : " + ex.getStacktraceString + "\n"
            warn(logInfo)
            showErrorPage
        }
    }

  private def executeBody(body: AsyncUserRequest)(implicit user: AuthContext, request: Request[AnyContent]): Future[Result] =
    body(user)(request).recover {
      case error =>
        warn(error.getStacktraceString)
        showErrorPageRaw
    }

  def asyncRestrictedAccess(body: AsyncUserRequest): Action[AnyContent] =
    async {
      implicit user => implicit request =>
        getSessionStatus match {
          case Some(Rejected) | Some(RejectedUnderReviewOrAppeal) | Some(Revoked) | Some(RevokedUnderReviewOrAppeal) =>
            Future.successful(Redirect(controllers.routes.ApplicationStatusController.showStatus()))
          case _ => body(user)(request)
        }
    }

  def asyncPostSubmission(body: AsyncUserRequest): Action[AnyContent] =
    async {
      implicit user => implicit request =>
        getSessionStatus match {
          case Some(models.FormBundleStatus.NotFound(_)) | None => showNotFoundPage
          case _ => body(user)(request)
        }
    }

  def showErrorPageRaw(implicit request: Request[AnyContent]): Result =
    AwrsController.showErrorPageRaw

  def showErrorPage(implicit request: Request[AnyContent]): Future[Result] =
    AwrsController.showErrorPage

  def showBadRequestRaw(implicit request: Request[AnyContent]): Result =
    AwrsController.showBadRequestRaw

  def showBadRequest(implicit request: Request[AnyContent]): Future[Result] =
    AwrsController.showBadRequest

  def showNotFoundPageRaw(implicit request: Request[AnyContent]): Result =
    AwrsController.showNotFoundPageRaw

  def showNotFoundPage(implicit request: Request[AnyContent]): Future[Result] =
    AwrsController.showNotFoundPage

  implicit val sessionUtil = SessionUtil.sessionUtilForRequest
  implicit val sessionUtilForResult = SessionUtil.sessionUtilForResult

  def getSessionStatus(implicit request: Request[AnyContent]): Option[FormBundleStatus] =
    request getSessionStatus

  def getSessionStatusStr(implicit request: Request[AnyContent]): Option[String] =
    request getSessionStatusStr

  def getBusinessType(implicit request: Request[AnyContent]): Option[String] =
    request getBusinessType

  def getBusinessName(implicit request: Request[AnyContent]): Option[String] =
    request getBusinessName

  def Ok(content: Content)(implicit request: Request[AnyContent]) = OkNoLocation(content) addLocation

  // OKNoLocation helper is used to avoid storing the location history to the session
  def OkNoLocation(content: Content)(implicit request: Request[AnyContent]) = play.api.mvc.Results.Ok(AwrsController.prettify(content)).as("text/html; charset=utf-8")

}


object AwrsController extends Results {

  def showErrorPageRaw(implicit request: Request[AnyContent]): Result =
    InternalServerError(views.html.awrs_application_error()(request))

  def showErrorPage(implicit request: Request[AnyContent]): Future[Result] =
    Future.successful(showErrorPageRaw)

  def showBadRequestRaw(implicit request: Request[AnyContent]): Result =
    BadRequest(views.html.awrs_application_error()(request))

  def showBadRequest(implicit request: Request[AnyContent]): Future[Result] =
    Future.successful(showBadRequestRaw)

  def showNotFoundPageRaw(implicit request: Request[AnyContent]): Result =
    NotFound(ApplicationGlobal.notFoundTemplate(request))

  def showNotFoundPage(implicit request: Request[AnyContent]): Future[Result] =
    Future.successful(showNotFoundPageRaw)

  private val compressor: HtmlCompressor = new HtmlCompressor()

  def prettify(content: Content): String = {
    val output: String = content.body.trim()
    compressor.compress(output)
  }
}
