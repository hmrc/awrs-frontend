/*
 * Copyright 2021 HM Revenue & Customs
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

import audit.Auditable
import config.ApplicationConfig
import javax.inject.Inject
import models._
import org.joda.time.Period
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import utils.LoggingUtils

import scala.concurrent.{ExecutionContext, Future}

class EmailVerificationConnector @Inject()(http: DefaultHttpClient,
                                           val auditable: Auditable,
                                           implicit val applicationConfig: ApplicationConfig
                                          ) extends RawResponseReads with LoggingUtils {

  lazy val serviceURL: String = applicationConfig.servicesConfig.baseUrl("email-verification")
  val baseURI = "/email-verification"
  val sendEmail = "/verification-requests"
  val verifyEmail = "/verified-email-check"
  val continueUrl: String = applicationConfig.emailVerificationBaseUrl + controllers.routes.EmailVerificationController.showSuccess.url
  val defaultEmailExpiryPeriod: String = Period.days(1).toString
  val defaultTemplate = "awrs_email_verification"

  def sendVerificationEmail(emailAddress: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    val verificationRequest = EmailVerificationRequest(email = emailAddress,
      templateId = defaultTemplate,
      templateParameters = None,
      linkExpiryDuration = defaultEmailExpiryPeriod,
      continueUrl = continueUrl)
    val postURL = s"""$serviceURL$baseURI$sendEmail"""
    http.POST(postURL, verificationRequest, Seq.empty).map {
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

  def isEmailAddressVerified(email: Option[String])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    email match {
      case Some(emailAddress) =>
        val verifyURL = s"""$serviceURL$baseURI$verifyEmail"""

        http.POST(verifyURL, Json.obj("email" -> emailAddress), Seq.empty).map { _ =>
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
