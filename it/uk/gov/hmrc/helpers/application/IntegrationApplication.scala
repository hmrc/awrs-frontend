
package uk.gov.hmrc.helpers.application

import org.scalatest.TestSuite
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import uk.gov.hmrc.helpers.wiremock.WireMockConfig

trait IntegrationApplication extends GuiceOneServerPerSuite with WireMockConfig {
  self: TestSuite =>

  val currentAppBaseUrl: String = "awrs-frontend"
  val testAppUrl: String        = s"http://localhost:$port/$currentAppBaseUrl"


  def appConfig(extraConfig: (String,String)*): Map[String, Any] = Map(
    "microservice.services.auth.host"       -> wireMockHost,
    "microservice.services.auth.port"       -> wireMockPort,
    "auditing.consumer.baseUri.host"        -> wireMockHost,
    "auditing.consumer.baseUri.port"        -> wireMockPort,
    "microservice.services.etmp-hod.host" -> wireMockHost,
    "microservice.services.etmp-hod.port" -> wireMockPort,
    "microservice.services.enrolment-store-proxy.host" -> wireMockHost,
    "microservice.services.enrolment-store-proxy.port" -> wireMockPort,
    "microservice.services.cachable.short-lived-cache.host" -> wireMockHost,
    "microservice.services.cachable.short-lived-cache.port" -> wireMockPort,
    "microservice.services.cachable.session-cache.host" -> wireMockHost,
    "microservice.services.cachable.session-cache.port" -> wireMockPort,
    "microservice.services.tax-enrolments.host" -> wireMockHost,
    "microservice.services.tax-enrolments.port" -> wireMockPort,
    "microservice.services.awrs.host" -> wireMockHost,
    "microservice.services.awrs.port" -> wireMockPort,
    "microservice.services.address-lookup.host" -> wireMockHost,
    "microservice.services.address-lookup.port" -> wireMockPort,
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck"

  ) ++ extraConfig

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(appConfig())
    .build()

  lazy val ws: WSClient = app.injector.instanceOf[WSClient]
}
