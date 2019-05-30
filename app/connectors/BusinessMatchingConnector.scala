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

import com.fasterxml.jackson.core.JsonParseException
import config.{AwrsFrontendAuditConnector, WSHttp}
import controllers.auth.StandardAuthRetrievals
import models.MatchBusinessData
import play.api.Mode.Mode
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.{Configuration, Play}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.model.{Audit, EventTypes}
import uk.gov.hmrc.play.config.ServicesConfig
import utils.AccountUtils._
import utils.LoggingUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object BusinessMatchingConnector extends BusinessMatchingConnector {
  override val appName = "awrs-frontend"
  override val audit: Audit = new Audit(appName, AwrsFrontendAuditConnector)
  val baseUri = "business-matching"
  val lookupUri = "business-lookup"
  val serviceUrl = baseUrl("business-matching")
  val http: HttpGet with HttpPost = WSHttp

  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration
}

trait BusinessMatchingConnector extends ServicesConfig with RawResponseReads with LoggingUtils {

  def serviceUrl: String

  def baseUri: String

  def lookupUri: String

  def http: HttpGet with HttpPost

  def lookup(lookupData: MatchBusinessData, userType: String, authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[JsValue] = {

    val url = s"""$serviceUrl/${authLink(authRetrievals)}/$baseUri/$lookupUri/${lookupData.utr}/$userType"""
    debug(s"[BusinessMatchingConnector][lookup] Call $url")
    http.POST[JsValue, HttpResponse](url, Json.toJson(lookupData)) map { response =>
      auditMatchCall(lookupData, userType, response)
      response.status match {
        case OK | NOT_FOUND =>
          info(s"[BusinessMatchingConnector][lookup] - postUrl = $url && " +
            s"response.status = ${response.status} &&  response.body = ${response.body}")
          //try catch added to handle JsonParseException in case ETMP/DES response with contact Details with ',' in it
          try {
            Json.parse(response.body)
          } catch {
            case jse: JsonParseException => truncateContactDetails(response.body)
          }
        ///////////////////// try catch end
        case SERVICE_UNAVAILABLE =>
          warn(s"[BusinessMatchingConnector][lookup] - Service unavailableException ${lookupData.utr}")
          throw new ServiceUnavailableException("Service unavailable")
        case BAD_REQUEST =>
          warn(s"[BusinessMatchingConnector][lookup] - Bad Request Exception ${lookupData.utr}")
          throw new BadRequestException("Bad Request")
        case INTERNAL_SERVER_ERROR =>
          warn(s"[BusinessMatchingConnector][lookup] - Service Internal server error ${lookupData.utr}")
          throw new InternalServerException("Internal server error")
        case status =>
          warn(s"[BusinessMatchingConnector][lookup] - $status Exception ${lookupData.utr}")
          throw new RuntimeException("Unknown response")
      }
    }
  }

  private def truncateContactDetails(responseJson: String): JsValue = {
    val replacedX1 = responseJson.replaceAll("[\r\n\t]", "")
    val removedContactDetails = replacedX1.substring(0, replacedX1.indexOf("contactDetails"))
    val correctedJsonString = removedContactDetails.substring(0, removedContactDetails.lastIndexOf(","))
    val validJson = correctedJsonString + "}"
    Json.parse(validJson)
  }

  private def auditMatchCall(input: MatchBusinessData, userType: String, response: HttpResponse)
                            (implicit hc: HeaderCarrier) = {
    val eventType = response.status match {
      case OK | NOT_FOUND => EventTypes.Succeeded
      case _ => EventTypes.Failed
    }
    audit(transactionName = "etmpMatchCall",
      detail = Map("txName" -> "etmpMatchCall",
        "userType" -> s"$userType",
        "service" -> "AWRS (group member)",
        "utr" -> input.utr,
        "requiresNameMatch" -> s"${input.requiresNameMatch}",
        "isAnAgent" -> s"${input.isAnAgent}",
        "individual" -> s"${input.individual}",
        "organisation" -> s"${input.organisation}",
        "responseStatus" -> s"${response.status}",
        "responseBody" -> s"${response.body}"),
      eventType = eventType)
  }

}
