package connectors

import audit.Auditable
import metrics.AwrsMetrics
import models.ApiType
import models.reenrolment.Groups
import play.api.http.Status.{NO_CONTENT, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.LoggingUtils

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ESConnector @Inject()(servicesConfig: ServicesConfig,
                            http: HttpClientV2,
                            metrics: AwrsMetrics,
                            val auditable: Auditable) extends LoggingUtils {

  val serviceURL: String = servicesConfig.baseUrl("enrolment-store-proxy")
  val enrolmentStoreProxyServiceUrl: String = s"${serviceURL}/enrolment-store-proxy"
  val AWRS_SERVICE_NAME = "HMRC-AWRS-ORG"
  val EnrolmentIdentifierName = "AWRSRefNumber"

  def query(awrsReferenceNumber: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] = {
    val timer = metrics.startTimer(ApiType.API4Enrolment)
    val enrolmentKey = s"$AWRS_SERVICE_NAME~$EnrolmentIdentifierName~$awrsReferenceNumber"

    val result = http.get(url"$enrolmentStoreProxyServiceUrl/enrolment-store/enrolments/${enrolmentKey}/groups").execute[HttpResponse].map {
      processResponse(_, awrsReferenceNumber)
    }
    timer.stop()
    result
  }

  private def processResponse(response: HttpResponse, awrsRef:String): Option[String] = {
    response.status match {
      case OK =>
        metrics.incrementSuccessCounter(ApiType.ES9DeEnrolment)
        Json.parse(response.body).as[Groups].principalGroupIds.headOption
      case NO_CONTENT =>
        metrics.incrementSuccessCounter(ApiType.ES9DeEnrolment)
        None
      case status =>
        warn(s"[ESConnector][ES1 Query- $awrsRef, $status ] - ${response.body} ")
        metrics.incrementFailedCounter(ApiType.ES9DeEnrolment)
        None
    }

  }

}
