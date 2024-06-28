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
import com.fasterxml.jackson.core.JsonParseException
import controllers.auth.StandardAuthRetrievals
import models.MatchBusinessData
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.audit.model.EventTypes
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.{AccountUtils, LoggingUtils}
import uk.gov.hmrc.http.HttpReads.Implicits._
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BusinessMatchingConnectorImpl @Inject()(servicesConfig: ServicesConfig,
                                          val auditable: Auditable,
                                          val accountUtils: AccountUtils,
                                          val http: HttpClientV2) extends BusinessMatchingConnector {
  val serviceUrl: String = servicesConfig.baseUrl("business-matching")
  val baseUri = "business-matching"
  val lookupUri = "business-lookup"
}

trait BusinessMatchingConnector extends LoggingUtils {

  val baseUri: String
  val serviceUrl: String
  val lookupUri: String
  val http: HttpClientV2
  val accountUtils: AccountUtils

  def lookup(lookupData: MatchBusinessData, userType: String, authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[JsValue] = {

    val url = s"$serviceUrl/${accountUtils.authLink(authRetrievals)}/$baseUri/$lookupUri/${lookupData.utr}/$userType"
    debug(s"[BusinessMatchingConnector][lookup] Call $url")
    http.post(url"$url").withBody(Json.toJson(lookupData)).execute[HttpResponse] map { response =>
      auditMatchCall(lookupData, userType, response)
      response.status match {
        case OK | NOT_FOUND =>
          info(s"[BusinessMatchingConnector][lookup] - postUrl = $url && " +
            s"response.status = ${response.status} &&  response.body = ${response.body}")
          //try catch added to handle JsonParseException in case ETMP/DES response with contact Details with ',' in it
          try {
            Json.parse(response.body)
          } catch {
            case _: JsonParseException => truncateContactDetails(response.body)
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
                            (implicit hc: HeaderCarrier, ec: ExecutionContext): Unit = {
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
