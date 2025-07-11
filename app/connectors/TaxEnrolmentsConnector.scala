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

package connectors

import audit.Auditable
import metrics.AwrsMetrics
import models._
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import services.GGConstants._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.LoggingUtils

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaxEnrolmentsConnector @Inject()(servicesConfig: ServicesConfig,
                                       http: HttpClientV2,
                                       metrics: AwrsMetrics,
                                       val auditable: Auditable) extends LoggingUtils {
  val serviceURL: String = servicesConfig.baseUrl("tax-enrolments")
  val AWRS_SERVICE_NAME = "HMRC-AWRS-ORG"
  val EnrolmentIdentifierName = "AWRSRefNumber"

  val deEnrolURI = "tax-enrolments/de-enrol"
  val enrolmentUrl = s"$serviceURL/tax-enrolments"

  val emptyResponse: EnrolResponse = EnrolResponse("", "", Seq.empty)

  def enrol(requestPayload: RequestPayload,
            groupId: String,
            awrsRegistrationNumber: String,
            auditMap:Map[String, String])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[EnrolResponse]] = {
    val timer = metrics.startTimer(ApiType.API4Enrolment)
    val enrolmentKey = s"$AWRS_SERVICE_NAME~$EnrolmentIdentifierName~$awrsRegistrationNumber"
    val postUrl = s"$enrolmentUrl/groups/$groupId/enrolments/$enrolmentKey"
    val response = send(postUrl, requestPayload, auditMap, awrsRegistrationNumber).map(_ => Option(emptyResponse))
    timer.stop()
    response
  }

  def send(postUrl: String, requestPayload: RequestPayload,
           auditMap: Map[String, String],
           awrsRegistrationNumber: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val jsonData: JsValue = Json.toJson(requestPayload)
    http.post(url"$postUrl").withBody(jsonData).execute[HttpResponse].map {
      processResponse(_, postUrl, requestPayload, auditMap, awrsRegistrationNumber)
    }
  }

  def processResponse(response: HttpResponse, postUrl: String,
                      requestPayload: RequestPayload, auditMap: Map[String, String], awrsRegistrationNumber: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): HttpResponse = {
    response.status match {
      case OK | CREATED =>
        metrics.incrementSuccessCounter(ApiType.API4Enrolment)
        response
      case BAD_REQUEST =>
        metrics.incrementFailedCounter(ApiType.API4Enrolment)
        audit(transactionName = auditEMACTxName,
                  detail = auditMap ++ Map("verifiers" -> requestPayload.verifiers.toString, "Error" -> response.body),
                  eventType = eventTypeFailure)
        logger.warn(s"[TaxEnrolmentsConnector][enrol] - " +
          s"enrolment url:$postUrl, " +
          s"AWRS Ref:$awrsRegistrationNumber, " +
          s"Bad Request Exception :${response.body}, " +
          s"Service: $AWRS_SERVICE_NAME")
        throw new BadRequestException(response.body)
      case NOT_FOUND =>
        metrics.incrementFailedCounter(ApiType.API4Enrolment)
        logger.warn(s"[TaxEnrolmentsConnector][enrol] - " +
          s"Not Found Exception :${response.body}, " +
          s"AWRS Ref:$awrsRegistrationNumber, " +
          s"Service: $AWRS_SERVICE_NAME}")
        throw new NotFoundException(response.body)
      case SERVICE_UNAVAILABLE =>
        metrics.incrementFailedCounter(ApiType.API4Enrolment)
        logger.warn(s"[TaxEnrolmentsConnector][enrol] - " +
          s"enrolment url:$postUrl, " +
          s"AWRS Ref:$awrsRegistrationNumber, " +
          s"Service Unavailable Exception:${response.body}, " +
          s"Service: $AWRS_SERVICE_NAME}")
        throw new ServiceUnavailableException(response.body)
      case BAD_GATEWAY =>
        metrics.incrementFailedCounter(ApiType.API4Enrolment)
        createWarning(postUrl, None, response.body, response.status, awrsRegistrationNumber, Some("BAD_GATEWAY"))
        throw new BadGatewayException(response.body)
      case status =>
        metrics.incrementFailedCounter(ApiType.API4Enrolment)
        createWarning(postUrl, Some(status), response.body, response.status, awrsRegistrationNumber)
        throw new InternalServerException(response.body)
    }
  }

  private def createWarning(postUrl: String,
                            optionStatus: Option[Int],
                            responseBody: String,
                            responseStatus: Int,
                            awrsRegistrationNumber: String,
                            optionErrorStatus: Option[String] = None): Unit = {
    val errorStatus = optionErrorStatus.fold("")(status => " - " + status)
    logger.warn(s"[TaxEnrolmentsConnector][enrol]$errorStatus" +
      s"enrolment url:$postUrl, " +
      s"AWRS Ref:$awrsRegistrationNumber, " +
      optionStatus.map(status => s"status:$status Exception") +
      s"Service: $AWRS_SERVICE_NAME" +
      s"Reponse Body: $responseBody," +
      s"Reponse Status: $responseStatus")
  }

  def deEnrol(awrsRef: String, businessName: String, businessType: String)
             (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    val timer = metrics.startTimer(ApiType.API10DeEnrolment)
    val jsonData = Json.toJson(DeEnrolRequest(keepAgentAllocations))

    val auditMap: Map[String, String] = Map("UserDetail" -> businessName, "legal-entity" -> businessType)
    val auditSubscribeTxName: String = "AWRS ETMP de-enrol"

    val postUrl = s"$serviceURL/$deEnrolURI/$service"
    http.post(url"$postUrl").withBody(jsonData).execute[HttpResponse].map {
      response =>
        timer.stop()
        response.status match {
          case OK => warn(s"[TaxEnrolmentsConnector][deEnrol] - Ok")
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
