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
import metrics.AwrsMetrics
import models._
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.http.{HeaderCarrier, _}
import utils.LoggingUtils
import services.GGConstants._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait TaxEnrolmentsConnector extends ServicesConfig with LoggingUtils {

  lazy val serviceURL = baseUrl("tax-enrolments")

  val http: HttpGet with HttpPost = WSHttp
  val metrics: AwrsMetrics

  val deEnrolURI = "tax-enrolments/de-enrol"

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
