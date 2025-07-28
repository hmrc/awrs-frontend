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

package controllers.auth

import com.googlecode.htmlcompressor.compressor.HtmlCompressor
import config.ApplicationConfig
import models.FormBundleStatus
import models.FormBundleStatus.{DeRegistered, Rejected, RejectedUnderReviewOrAppeal, Revoked, RevokedUnderReviewOrAppeal, Withdrawal}
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import play.twirl.api.Content
import services.DeEnrolService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AccountUtils, AwrsSessionKeys, LoggingUtils, SessionUtil}
import views.html._
import views.html.helpers.awrsErrorNotFoundTemplate

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

trait AwrsController extends LoggingUtils with AuthFunctionality with I18nSupport with Results {

  val authConnector: AuthConnector
  val accountUtils: AccountUtils
  val deEnrolService: DeEnrolService

  def restrictedAccessCheck(body: => Future[Result])(implicit request: Request[AnyContent],
                                                     authRetrievals: StandardAuthRetrievals, hc: HeaderCarrier, ec: ExecutionContext): Future[Result] =
    getSessionStatus match {
      case Some(Rejected) | Some(RejectedUnderReviewOrAppeal) | Some(Revoked) | Some(RevokedUnderReviewOrAppeal) =>
        Future.successful(Redirect(controllers.routes.ApplicationStatusController.showStatus()))
      case e@(Some(Withdrawal) | Some(DeRegistered)) =>
        val enrolments = authRetrievals.enrolments
        if(accountUtils.hasAwrs(enrolments)){
          logger.info(s"[AwrsController][restrictedAccessCheck] De-Enrolled and redirecting to business-customer for status ${e.get.name}")
          deEnrolService.deEnrolAWRS(accountUtils.getAwrsRefNo(enrolments), getBusinessName.getOrElse(""), getBusinessType.getOrElse(""))
          Future.successful(Redirect(applicationConfig.businessCustomerStartPage).removingFromSession(AwrsSessionKeys.sessionStatusType))
        } else {
          body
        }
      case _ => body
    }

  def showErrorPageRaw(implicit request: Request[AnyContent]): Result =
    AwrsController.showErrorPageRaw(applicationConfig.templateAppError)

  def showErrorPage(implicit request: Request[AnyContent]): Future[Result] =
    AwrsController.showErrorPage(applicationConfig.templateAppError)

  def showNotFoundPage(implicit request: Request[AnyContent]): Future[Result] =
    AwrsController.showNotFoundPage(applicationConfig.templateNotFound)

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
  def OkNoLocation(content: Content): Result = Results.Ok(AwrsController.prettify(content)).as("text/html; charset=utf-8")

}


object AwrsController extends Results {

  def showErrorPageRaw(errorPage: awrs_application_error)
                      (implicit request: Request[AnyContent], messages: Messages, applicationConfig: ApplicationConfig): Result =
    InternalServerError(errorPage())

  def showErrorPage(errorPage: awrs_application_error)
                   (implicit request: Request[AnyContent], messages: Messages, applicationConfig: ApplicationConfig): Future[Result] =
    Future.successful(showErrorPageRaw(errorPage))

  def showBadRequestRaw(errorPage: awrs_application_error)(implicit request: Request[AnyContent], messages: Messages, applicationConfig: ApplicationConfig): Result =
    BadRequest(errorPage())

  def showBadRequest(errorPage: awrs_application_error)
                    (implicit request: Request[AnyContent], messages: Messages, applicationConfig: ApplicationConfig): Future[Result] =
    Future.successful(showBadRequestRaw(errorPage))

  def showNotFoundPageRaw(notFound: awrsErrorNotFoundTemplate)
                         (implicit request: Request[AnyContent], messages: Messages, applicationConfig: ApplicationConfig): Result =
    NotFound(notFound())

  def showNotFoundPage(notFound: awrsErrorNotFoundTemplate)
                      (implicit request: Request[AnyContent], messages: Messages, applicationConfig: ApplicationConfig): Future[Result] =
    Future.successful(showNotFoundPageRaw(notFound))

  private val compressor: HtmlCompressor = new HtmlCompressor()

  def prettify(content: Content): String = {
    val output: String = content.body.trim()
    compressor.compress(output)
  }
}
