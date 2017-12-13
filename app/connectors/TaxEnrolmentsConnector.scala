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
import config.WSHttp
import metrics.AwrsMetrics
import models.{RequestPayload, _}
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import services.GGConstants._
import uk.gov.hmrc.crypto.Verifier
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import utils.LoggingUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait TaxEnrolmentsConnector extends ServicesConfig with LoggingUtils {

  lazy val serviceURL = baseUrl("tax-enrolments")

  val http: HttpGet with HttpPost = WSHttp
  val metrics: AwrsMetrics

  val retryLimit = 7
  val retryWait = 1000 // milliseconds

  val AWRS_SERVICE_NAME = "HMRC-AWRS-ORG"

  val deEnrolURI = "tax-enrolments/de-enrol"

  val serviceUrl = baseUrl("tax-enrolments")

  val enrolmentUrl = s"$serviceUrl/tax-enrolments"

  def allocateEnrolmentToGroupURI(groupId: String, enrolmentKey: String) =
    s"enrolment-store/groups/$groupId/enrolments/$enrolmentKey"


  def enrol(requestPayload: RequestPayload,
            groupId: String,
            awrsRegistrationNumber: String)(implicit hc: HeaderCarrier): Future[Option[EnrolResponse]] = {
    val enrolmentKey = s"$AWRS_SERVICE_NAME~AWRSRefNumber~$awrsRegistrationNumber"
    val jsonData: JsValue = Json.toJson(requestPayload)
    val postUrl = s"""$enrolmentUrl/groups/$groupId/enrolments/$enrolmentKey"""

    println(s"\n\n\n+++++++++++++++++++++$postUrl+++++++++++++++${Json.prettyPrint(jsonData)}")

    //val timerContext = metrics.startTimer(MetricsEnum.GG_AGENT_ENROL)

    trySend(0, postUrl, jsonData, requestPayload).map { _ =>
      Option(EnrolResponse.apply("","",Seq.empty))
    }
  }

  //@tailrec
  def trySend(tries: Int, postUrl: String, jsonData: JsValue, requestPayload:RequestPayload): Future[HttpResponse] = {
    http.POST[JsValue, HttpResponse](postUrl, jsonData).flatMap {
      response =>
        Future.successful(processResponse(response, postUrl, requestPayload))
    }.recoverWith {
      case e => if (tries < retryLimit) {
        Future {
          warn(s"Retrying GG Enrol - call number: $tries")
          Thread.sleep(retryWait)
        }.flatMap(_ => trySend(tries + 1, postUrl, jsonData, requestPayload))
      }
      else {
        warn(s"Retrying GG Enrol - retry limit exceeded")
//        audit(transactionName = auditGGTxName,
//          detail = auditMap ++ Map("KnownFacts" -> enrolRequest.knownFacts.toString(), "Exception" -> e.getMessage),
//          eventType = eventTypeFailure)
        //timer.stop()
        // Code changed to hide the GG Enrol failure from the user.
        // The GG failure will need to be sorted out manually and there is nothing the user can do at the time.
        // The manual process will take place after the GG Enrol failure is picked up in Splunk.
        Future.successful(HttpResponse(OK))
      }
    }
  }

  def processResponse(response: HttpResponse, postUrl:String, requestPayload:RequestPayload): HttpResponse = {
    response.status match {
      case OK =>
        //metrics.incrementSuccessCounter(MetricsEnum.GG_AGENT_ENROL)
        response
      case BAD_REQUEST =>
        //metrics.incrementFailedCounter(MetricsEnum.GG_AGENT_ENROL)
        Logger.warn(s"[GovernmentGatewayConnector][enrol] - " +
          s"gg url:$postUrl, " +
          s"Bad Request Exception account Ref:${requestPayload.verifiers}, " +
          s"Service: $AWRS_SERVICE_NAME")
        throw new BadRequestException(response.body)
      case NOT_FOUND =>
        //metrics.incrementFailedCounter(MetricsEnum.GG_AGENT_ENROL)
        Logger.warn(s"[GovernmentGatewayConnector][enrol] - " +
          s"Not Found Exception account Ref:${requestPayload.verifiers}, " +
          s"Service: $AWRS_SERVICE_NAME}")
        throw new NotFoundException(response.body)
      case SERVICE_UNAVAILABLE =>
        //metrics.incrementFailedCounter(MetricsEnum.GG_AGENT_ENROL)
        Logger.warn(s"[GovernmentGatewayConnector][enrol] - " +
          s"gg url:$postUrl, " +
          s"Service Unavailable Exception account Ref:${requestPayload.verifiers}, " +
          s"Service: $AWRS_SERVICE_NAME}")
        throw new ServiceUnavailableException(response.body)
      case BAD_GATEWAY =>
        //metrics.incrementFailedCounter(MetricsEnum.GG_AGENT_ENROL)
        warn(postUrl, None, requestPayload.verifiers, response.body, response.status, Some("BAD_GATEWAY"))
        response
      case status =>
        //metrics.incrementFailedCounter(MetricsEnum.GG_AGENT_ENROL)
        warn(postUrl, Some(status), requestPayload.verifiers, response.body, response.status)
        throw new InternalServerException(response.body)
    }
  }

  private def warn(postUrl: String,
                   optionStatus: Option[Int],
                   verifiers: List[Verifier],
                   responseBody: String,
                   responseStatus: Int,
                   optionErrorStatus: Option[String] = None) = {
    val errorStatus = optionErrorStatus.fold("")(status => " - " + status)
    Logger.warn(s"[GovernmentGatewayConnector][enrol]$errorStatus" +
      s"gg url:$postUrl, " +
      optionStatus.map(status => s"status:$status Exception account Ref:$verifiers, ") +
      s"Service: $AWRS_SERVICE_NAME" +
      s"Reponse Body: $responseBody," +
      s"Reponse Status: $responseStatus")
  }

  def deEnrol(awrsRef: String, businessName: String, businessType: String)(implicit headerCarrier: HeaderCarrier): Future[Boolean] = {
    val timer = metrics.startTimer(ApiType.API10DeEnrolment)
    val jsonData = Json.toJson(DeEnrolRequest(keepAgentAllocations))

    val auditMap: Map[String, String] = Map("UserDetail" -> businessName, "legal-entity" -> businessType)
    val auditSubscribeTxName: String = "AWRS ETMP de-enrol"

    val postUrl = s"""$serviceURL/$deEnrolURI/$service"""

    http.POST[JsValue, HttpResponse](postUrl, jsonData) map {
      response =>
        timer.stop()
        response.status match {
          case OK => warn(s"[TaxEnrolmentsConnector][de-enrol] - Ok")
            metrics.incrementSuccessCounter(ApiType.API10DeEnrolment)
            audit(transactionName = auditSubscribeTxName, detail = auditMap, eventType = eventTypeSuccess)
            true
          case status =>
            warn(s"[TaxEnrolmentsConnector][API10 De-Enrolment - $businessName, $awrsRef, $status ] - ${response.body} ")
            metrics.incrementFailedCounter(ApiType.API10DeEnrolment)
            audit(transactionName = auditSubscribeTxName, detail = auditMap ++ Map("Business Name" -> businessName,
              "AWRS Ref" -> awrsRef,
              "Status" -> status.toString,
              "FailureReason" -> response.body), eventType = eventTypeFailure)
            false
        }
    }
  }
}

object TaxEnrolmentsConnector extends TaxEnrolmentsConnector {
  override val appName = "awrs-frontend"
  override val metrics = AwrsMetrics
  override val audit: Audit = new Audit(AppName.appName, AwrsFrontendAuditConnector)
}
