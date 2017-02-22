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

package connectors

import config.{AwrsFrontendAuditConnector, WSHttp}
import controllers.auth.ExternalUrls
import metrics.AwrsMetrics
import models._
import org.joda.time.Period
import play.api.http.Status._
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.{HeaderCarrier, InternalServerException, _}
import utils.LoggingUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait EmailVerificationConnector extends ServicesConfig with RawResponseReads with LoggingUtils {

  lazy val serviceURL = baseUrl("email-verification")
  val baseURI = "/email-verification"
  val sendEmail = "/verification-requests"
  val verifyEmail = "/verified-email-addresses"
  val continueUrl = ExternalUrls.loginCallback
  val defaultEmailExpiryPeriod = Period.days(1).toString
  val defaultTemplate = "apiDeveloperEmailVerification"  // TODO create our own!!!!
  val httpGet: HttpGet
  val httpPost: HttpPost
  val metrics: AwrsMetrics
// TODO change retrun type for both these methods
  def sendVerificationEmail(emailAddress: String)(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[StatusNotification]] = {
    val verificationRequest = EmailVerificationRequest(email = emailAddress,
      templateId = defaultTemplate,
      templateParameters = None,
      linkExpiryDuration = defaultEmailExpiryPeriod,
      continueUrl = continueUrl)
    val postURL = s"""$serviceURL$baseURI$sendEmail"""
    mapResult(auditEmailVerification, emailAddress, httpPost.POST(postURL, verificationRequest)).map {
      case Some(response: HttpResponse) => Some(response.json.as[StatusNotification])
      case None => None
    }
  }

  def verifyEmailAddress(emailAddress: String)(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[ViewedStatusResponse]] = {
    val getURL = s"""$serviceURL$baseURI$verifyEmail/$emailAddress"""
    mapResult(auditVerifyEmail, emailAddress, httpGet.GET(getURL)).map {
      case Some(response: HttpResponse) => Some(response.json.as[ViewedStatusResponse])
      case None => None
    }
  }

  private def mapResult(auditTxName: String, emailAddress: String, result: Future[HttpResponse])(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[HttpResponse]] =
    result map {
      response =>
        response.status match {
          case OK =>
            warn(f"[$auditTxName] - Successful return of data")
            audit(transactionName = auditTxName, detail = Map("emailAddress" -> emailAddress), eventType = eventTypeSuccess)
            Some(response)
          case NOT_FOUND =>
            warn(f"[$auditTxName - $emailAddress ] - No data returned from email verification service")
            None
          case SERVICE_UNAVAILABLE =>
            warn(f"[$auditTxName - $emailAddress ] - Dependant systems are currently not responding")
            throw new ServiceUnavailableException("Dependant systems are currently not responding")
          case BAD_REQUEST =>
            warn(f"[$auditTxName - $emailAddress ] - The request has not passed validation")
            throw new BadRequestException("The request has not passed validation")
          case INTERNAL_SERVER_ERROR =>
            warn(f"[$auditTxName - $emailAddress ] - WSO2 is currently experiencing problems that require live service intervention")
            throw new InternalServerException("WSO2 is currently experiencing problems that require live service intervention")
          case status@_ =>
            warn(f"[$auditTxName - $emailAddress ] - Unsuccessful return of data. Status code: $status")
            throw new InternalServerException(f"Unsuccessful return of data. Status code: $status")
        }
    }
}

object EmailVerificationConnector extends EmailVerificationConnector {
  override val appName = "awrs-frontend"
  override val metrics = AwrsMetrics
  override val audit: Audit = new Audit(AppName.appName, AwrsFrontendAuditConnector)
  override val httpGet: HttpGet = WSHttp
  override val httpPost: HttpPost = WSHttp
}
