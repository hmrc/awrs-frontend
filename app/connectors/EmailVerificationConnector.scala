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

package connectors

import config.{AwrsFrontendAuditConnector, WSHttp}
import metrics.AwrsMetrics
import models._
import org.joda.time.Period
import play.api.{Configuration, Play}
import play.api.Mode.Mode
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http._
import utils.LoggingUtils
import utils.AwrsConfig._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpPost, NotFoundException}

trait EmailVerificationConnector extends ServicesConfig with RawResponseReads with LoggingUtils {

  lazy val serviceURL = baseUrl("email-verification")
  val baseURI = "/email-verification"
  val sendEmail = "/verification-requests"
  val verifyEmail = "/verified-email-check"
  val continueUrl = emailVerificationBaseUrl + controllers.routes.EmailVerificationController.showSuccess.url
  val defaultEmailExpiryPeriod = Period.days(1).toString
  val defaultTemplate = "awrs_email_verification"
  val httpGet: HttpGet
  val httpPost: HttpPost
  val metrics: AwrsMetrics

  def sendVerificationEmail(emailAddress: String)(implicit user: AuthContext, hc: HeaderCarrier): Future[Boolean] = {
    val verificationRequest = EmailVerificationRequest(email = emailAddress,
      templateId = defaultTemplate,
      templateParameters = None,
      linkExpiryDuration = defaultEmailExpiryPeriod,
      continueUrl = continueUrl)
    val postURL = s"""$serviceURL$baseURI$sendEmail"""
    httpPost.POST(postURL, verificationRequest).map {
      response =>
        response.status match {
          case OK | CREATED =>
            warn(f"[$auditEmailVerification] - Successful return of data")
            audit(transactionName = auditEmailVerification, detail = Map("emailAddress" -> emailAddress), eventType = eventTypeSuccess)
            true
          case CONFLICT =>
            warn(f"[$auditEmailVerification] - Successful return of data - email already verified")
            audit(transactionName = auditEmailVerification, detail = Map("emailAddress" -> emailAddress), eventType = eventTypeSuccess)
            true
          case status@_ =>
            warn(f"[$auditEmailVerification - $emailAddress ] - Unsuccessful return of data. Status code: $status")
            audit(transactionName = auditEmailVerification, detail = Map("emailAddress" -> emailAddress), eventType = eventTypeFailure)
            false
        }
    }
  }

  def isEmailAddressVerified(email: Option[String])(implicit user: AuthContext, hc: HeaderCarrier): Future[Boolean] = {
    email match {
      case Some(emailAddress) =>
        val verifyURL = s"""$serviceURL$baseURI$verifyEmail"""

        httpPost.POST(verifyURL, Json.obj("email" -> emailAddress)).map { _ =>
          audit(transactionName = auditVerifyEmail, detail = Map("emailAddress" -> emailAddress), eventType = eventTypeSuccess)
          true
        } recover {
          case _: NotFoundException =>
            warn(f"[$auditVerifyEmail] - Successful return of data - email address not verified")
            audit(transactionName = auditVerifyEmail, detail = Map("emailAddress" -> emailAddress), eventType = eventTypeSuccess)
            false
          case status =>
            warn(f"[$auditVerifyEmail] - Unsuccessful return of data. Status code: $status")
            audit(transactionName = auditVerifyEmail, detail = Map("emailAddress" -> emailAddress), eventType = eventTypeFailure)
            // if the verification service is unavailable for any reason, the decision has been made not to block the user journey
            // when they return their email address will be validated before any further submissions
            true
        }
      case _ => Future.successful(false)
    }
  }

}

object EmailVerificationConnector extends EmailVerificationConnector {
  override val appName = "awrs-frontend"
  override val metrics = AwrsMetrics
  override val audit: Audit = new Audit(appName, AwrsFrontendAuditConnector)
  override val httpGet: HttpGet = WSHttp
  override val httpPost: HttpPost = WSHttp

  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration
}
