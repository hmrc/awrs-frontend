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

package connectors

import config.{AwrsFrontendAuditConnector, WSHttp}
import exceptions.{DuplicateSubscriptionException, GovernmentGatewayException}
import metrics.AwrsMetrics
import models._
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.http._
import utils.LoggingUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{ BadRequestException, HeaderCarrier, HttpGet, HttpPost, HttpResponse, InternalServerException, NotFoundException, ServiceUnavailableException }

trait GovernmentGatewayConnector extends ServicesConfig with LoggingUtils {

  lazy val serviceURL = baseUrl("government-gateway")

  val ggFailurePattern = "(^.*government-gateway-admin.*$)".r

  val duplicateFailurePattern = "(^.*already has an active AWRS.*$)".r

  val retryLimit = 7
  val retryWait = 1000 // milliseconds

  val enrolURI = "enrol"
  val http: HttpGet with HttpPost = WSHttp
  val metrics: AwrsMetrics

  def enrol(enrolRequest: EnrolRequest, businessPartnerDetails: BusinessCustomerDetails, businessType: String)(implicit headerCarrier: HeaderCarrier): Future[Option[EnrolResponse]] = {
    val timer = metrics.startTimer(ApiType.API4Enrolment)
    val jsonData = Json.toJson(enrolRequest)

    val userOrBusinessName = businessPartnerDetails.businessName
    val safeId = businessPartnerDetails.safeId

    val auditMap: Map[String, String] = Map("safeId" -> safeId, "UserDetail" -> userOrBusinessName, "legal-entity" -> businessType)

    val postUrl = s"""$serviceURL/$enrolURI"""

    def trySend(tries: Int): Future[HttpResponse] = {
      http.POST[JsValue, HttpResponse](postUrl, jsonData).flatMap {
        response =>
          Future.successful(processResponse(response))
      }.recoverWith {
        case e => if (tries < retryLimit) {
          Future {
            warn(s"Retrying GG Enrol - call number: $tries")
            Thread.sleep(retryWait)
          }.flatMap(_ => trySend(tries + 1))
        }
        else {
          warn(s"Retrying GG Enrol - retry limit exceeded")
          audit(transactionName = auditGGTxName,
            detail = auditMap ++ Map("KnownFacts" -> enrolRequest.knownFacts.toString(), "Exception" -> e.getMessage),
            eventType = eventTypeFailure)
          timer.stop()
          // Code changed to hide the GG Enrol failure from the user.
          // The GG failure will need to be sorted out manually and there is nothing the user can do at the time.
          // The manual process will take place after the GG Enrol failure is picked up in Splunk.
          Future.successful(HttpResponse(OK))
        }
      }
    }

    def processResponse(response: HttpResponse) = {
      response.status match {
        case OK => warn(s"[GovernmentGatewayConnector][enrol] - Ok")
          metrics.incrementSuccessCounter(ApiType.API4Enrolment)
          audit(transactionName = auditSubscribeTxName, detail = auditMap, eventType = eventTypeSuccess)
          response
        case BAD_REQUEST =>
          warn(s"[GovernmentGatewayConnector][API4 Enrolment - $userOrBusinessName, $safeId ] - Bad Request Exception " +
            s"\n Enrolment Request to GG  ## $enrolRequest  \n Enrolment Response from GG " + response.body)
          metrics.incrementFailedCounter(ApiType.API4Enrolment)
          audit(transactionName = auditSubscribeTxName, detail = auditMap ++ Map("FailureReason" -> "Bad Request"), eventType = eventTypeFailure)
          response.body.toString.replace("\n", "") match {
            case ggFailurePattern(contents) => throw new GovernmentGatewayException("There was a problem with the admin service")
            case duplicateFailurePattern(contents) => throw new DuplicateSubscriptionException("This subscription already exists")
            case _ => throw new BadRequestException(response.body)
          }
        case NOT_FOUND =>
          warn(s"[GovernmentGatewayConnector][API4 Enrolment - $userOrBusinessName, $safeId ] - NotFoundException " +
            s"\n Enrolment Request to GG  ## $enrolRequest  \n Enrolment Response from GG " + response.body)
          metrics.incrementFailedCounter(ApiType.API4Enrolment)
          audit(transactionName = auditSubscribeTxName, detail = auditMap ++ Map("FailureReason" -> "Not Found"), eventType = eventTypeFailure)
          throw new NotFoundException(response.body)
        case SERVICE_UNAVAILABLE =>
          warn(s"[GovernmentGatewayConnector][API4 Enrolment - $userOrBusinessName, $safeId ] - ServiceUnavailableException " +
            s"\n Enrolment Request to GG  ## $enrolRequest  \n Enrolment Response from GG " + response.body)
          metrics.incrementFailedCounter(ApiType.API4Enrolment)
          audit(transactionName = auditSubscribeTxName, detail = auditMap ++ Map("FailureReason" -> "Service Unavailable"), eventType = eventTypeFailure)
          throw new ServiceUnavailableException(response.body)
        case INTERNAL_SERVER_ERROR =>
          warn(s"[GovernmentGatewayConnector][API4 Enrolment - $userOrBusinessName, $safeId ] - Service Internal server error " +
            s"\n Enrolment Request to GG  ## $enrolRequest  \n Enrolment Response from GG " + response.body)
          metrics.incrementFailedCounter(ApiType.API4Enrolment)
          audit(transactionName = auditSubscribeTxName, detail = auditMap ++ Map("FailureReason" -> "Internal Server Error"), eventType = eventTypeFailure)
          response.body.toString.replace("\n", "") match {
            case ggFailurePattern(contents) => throw new GovernmentGatewayException("There was a problem with the admin service")
            case duplicateFailurePattern(contents) => throw new DuplicateSubscriptionException("This subscription already exists")
            case _ => throw new InternalServerException("Internal server error")
          }
        case status =>
          warn(s"[GovernmentGatewayConnector][API4 Enrolment - $userOrBusinessName, $safeId, $status ] - InternalServerException " +
            s"\n Enrolment Request to GG  ## $enrolRequest  \n Enrolment Response from GG " + response.body)
          metrics.incrementFailedCounter(ApiType.API4Enrolment)
          audit(transactionName = auditSubscribeTxName, detail = auditMap ++ Map("FailureReason" -> "Internal Server Error"), eventType = eventTypeFailure)
          response.body.toString.replace("\n", "") match {
            case ggFailurePattern(contents) => throw new GovernmentGatewayException("There was a problem with the admin service")
            case duplicateFailurePattern(contents) => throw new DuplicateSubscriptionException("This subscription already exists")
            case _ => throw new RuntimeException(response.body)
          }
      }
    }

    trySend(0).map {
      response =>
        if (response.json != null) {
          Some(response.json.as[EnrolResponse])
        } else {
          None
        }
    }
  }
}

object GovernmentGatewayConnector extends GovernmentGatewayConnector {
  override val appName = "awrs-frontend"
 override val metrics = AwrsMetrics
  override val audit: Audit = new Audit(AppName.appName, AwrsFrontendAuditConnector)
}
