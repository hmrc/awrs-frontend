/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.helpers.application

import org.scalatest.TestSuite
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import uk.gov.hmrc.http.test.WireMockSupport

import scala.annotation.nowarn

trait IntegrationApplication extends GuiceOneServerPerSuite with WireMockSupport {
  self: TestSuite =>

  val currentAppBaseUrl: String = "awrs-frontend"
  val testAppUrl: String        = s"http://localhost:$port/$currentAppBaseUrl"

  @nowarn
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
    "microservice.services.cachable.short-lived-cache-api.host" -> wireMockHost,
    "microservice.services.cachable.short-lived-cache-api.port" -> wireMockPort,
    "microservice.services.cachable.session-cache.host" -> wireMockHost,
    "microservice.services.cachable.session-cache.port" -> wireMockPort,
    "microservice.services.tax-enrolments.host" -> wireMockHost,
    "microservice.services.tax-enrolments.port" -> wireMockPort,
    "microservice.services.business-matching.host" -> wireMockHost,
    "microservice.services.business-matching.port" -> wireMockPort,
    "microservice.services.awrs.host" -> wireMockHost,
    "microservice.services.awrs.port" -> wireMockPort,
    "microservice.services.awrs-notification.host" -> wireMockHost,
    "microservice.services.awrs-notification.port" -> wireMockPort,
    "microservice.services.address-lookup.host" -> wireMockHost,
    "microservice.services.address-lookup.port" -> wireMockPort,
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck"

  ) ++ extraConfig

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(appConfig())
    .build()

  lazy val ws: WSClient = app.injector.instanceOf[WSClient]
}
