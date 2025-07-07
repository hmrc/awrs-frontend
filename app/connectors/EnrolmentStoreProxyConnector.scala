/*
 * Copyright 2025 HM Revenue & Customs
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
import models.ApiType
import models.ApiType.ApiType
import models.reenrolment.EnrolmentSuccessResponse._
import models.reenrolment.{EnrolmentSuccessResponse, PrincipalGroups, KnownFacts}
import play.api.http.Status.{NO_CONTENT, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.LoggingUtils

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EnrolmentStoreProxyConnector @Inject()(servicesConfig: ServicesConfig,
                                             http: HttpClientV2,
                                             metrics: AwrsMetrics,
                                             val auditable: Auditable) extends LoggingUtils {

  val serviceURL: String = servicesConfig.baseUrl("enrolment-store-proxy")
  private val enrolmentStoreProxyServiceUrl: String = s"$serviceURL/enrolment-store-proxy"
  val AWRS_SERVICE_NAME = "HMRC-AWRS-ORG"
  val EnrolmentIdentifierName = "AWRSRefNumber"


  //ES20
  def lookupEnrolments(knownFacts: KnownFacts)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[EnrolmentSuccessResponse]] = {
    val timer = metrics.startTimer(ApiType.ES20Query)

    val result = http
      .post(url"$enrolmentStoreProxyServiceUrl/enrolment-store/enrolments")
      .withBody(Json.toJson(knownFacts))
      .setHeader("X-Hmrc-Origin" -> "AWRS")
      .execute[HttpResponse]
      .map {
        processResponse(_, r=>Some(Json.parse(r.body).as[EnrolmentSuccessResponse]), knownFacts.awrsRefNumber, ApiType.ES20Query)
      }
    timer.stop()
    result
  }

  //ES1
  def queryForPrincipalGroupIdOfAWRSEnrolment(awrsReferenceNumber: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] = {
    val timer = metrics.startTimer(ApiType.ES1Query)
    val enrolmentKey = s"$AWRS_SERVICE_NAME~$EnrolmentIdentifierName~$awrsReferenceNumber"

    val result = http.get(url"$enrolmentStoreProxyServiceUrl/enrolment-store/enrolments/$enrolmentKey/groups?type=principal&ignore-assignments=true").execute[HttpResponse].map {
      processResponse(_, r=>Json.parse(r.body).as[PrincipalGroups].principalGroupIds.headOption, awrsReferenceNumber, ApiType.ES1Query)
    }
    timer.stop()
    result
  }

  private def processResponse[T](response: HttpResponse,extractFromResponse:HttpResponse => Option[T], awrsRef: String, apiType: ApiType): Option[T] = {
    response.status match {
      case OK =>
        info(s"[ESConnector][$apiType - $awrsRef, OK ] - ${response.body} ")
        metrics.incrementSuccessCounter(apiType)
        extractFromResponse(response)
      case NO_CONTENT =>
        info(s"[ESConnector][$apiType- $awrsRef, NO_CONTENT ] - ${response.body} ")
        metrics.incrementSuccessCounter(apiType)
        None
      case status =>
        warn(s"[ESConnector][$apiType- $awrsRef, $status ] - ${response.body} ")
        metrics.incrementFailedCounter(apiType)
        None
    }

  }

}
