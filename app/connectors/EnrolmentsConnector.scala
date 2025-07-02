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
import config.ApplicationConfig
import models.KnownFacts
import models.enrolment.{EnrolmentResponse, EnrolmentSuccessResponse, EnrolmentsErrorResponse}
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.LoggingUtils

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EnrolmentsConnector @Inject() (val servicesConfig: ServicesConfig, val http: HttpClientV2, val auditable: Auditable)
    extends LoggingUtils {

  private val enrolmentsServiceUrl: String = servicesConfig.baseUrl("enrolment-store-proxy")

  def lookupEnrolments(knownFacts: KnownFacts)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[EnrolmentResponse] = {
    http
      .post(url"$enrolmentsServiceUrl/enrolments")
      .withBody(Json.toJson(knownFacts))
      .setHeader("X-Hmrc-Origin" -> "AWRS")
      .execute[JsValue]
      .map { enrolmentsJson =>
        enrolmentsJson.as[EnrolmentSuccessResponse]
      } recover { case e: Exception =>
      warn(s"Error received from enrolments service: $e")
      EnrolmentsErrorResponse(e)
    }
  }

}
