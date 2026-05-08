/*
 * Copyright 2026 HM Revenue & Customs
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
import play.api.http.Status
import play.api.libs.json
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.LoggingUtils

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class BusinessCustomerCacheConnector @Inject() (
    servicesConfig: ServicesConfig,
    http: HttpClientV2,
    val auditable: Auditable
) extends LoggingUtils {

  val serviceName        = "business-customer"
  val serviceURL: String = servicesConfig.baseUrl(serviceName)

  private[connectors] def handleResponse[T](
      response: HttpResponse
  )(implicit
      formats: json.Format[T]
  ): Option[T] =
    response.status match {
      case Status.OK =>
        Try(Json.parse(response.body).validate[T].asOpt).getOrElse(None)

      case Status.NOT_FOUND =>
        logger.warn(s"[BusinessCustomerService] no business customer cache data found")
        None

      case status =>
        logger.warn(s"[BusinessCustomerService] received unexpected status $status for business customer cache request")
        None
    }

  def getReviewBusinessDetails[T](implicit
      hc: HeaderCarrier,
      formats: json.Format[T],
      ec: ExecutionContext
  ): Future[Option[T]] = {

    val getUrl = s"$serviceURL/$serviceName/external-data/awrs"

    http
      .get(url"$getUrl")
      .execute[HttpResponse]
      .map(handleResponse[T])
      .recover { case error =>
        logger.warn(s"[BusinessCustomerCacheConnector] received ${error.getCause} for business customer cache request")
        None
      }
  }

}
