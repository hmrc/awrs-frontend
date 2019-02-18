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

package config

import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import play.api.Mode.Mode
import play.api.mvc.{EssentialFilter, Request}
import play.api.{Application, Configuration, Play}
import play.filters.csrf.CSRFFilter
import play.twirl.api.Html
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, RunMode}
import uk.gov.hmrc.play.frontend.bootstrap.DefaultFrontendGlobal
import uk.gov.hmrc.play.frontend.filters.SessionCookieCryptoFilter
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import uk.gov.hmrc.play.frontend.filters.{CacheControlFilter, FrontendAuditFilter, FrontendLoggingFilter, HeadersFilter, MicroserviceFilterSupport}

object ApplicationGlobal extends DefaultFrontendGlobal with RunMode {


  override val auditConnector = AwrsFrontendAuditConnector
  override val loggingFilter = AwrsFrontendLoggingFilter
  override val frontendAuditFilter = AwrsFrontendAuditFilter

  // this override removes the RecoveryFilter as the filter auto handles all status Not Found.
  // when upgrading bootstrap please ensure this is up to date without RecoveryFilter.
  override protected lazy val defaultFrontendFilters: Seq[EssentialFilter] = Seq(
    metricsFilter,
    HeadersFilter,
    SessionCookieCryptoFilter,
    deviceIdFilter,
    loggingFilter,
    frontendAuditFilter,
    CacheControlFilter.fromConfig("caching.allowedContentTypes"))

  override def onStart(app: Application) {
    super.onStart(app)
    new ApplicationCrypto(Play.current.configuration.underlying).verifyConfiguration()
  }

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html =
    views.html.error_template(pageTitle, heading, message)

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig("microservice.metrics")

  object AwrsFrontendLoggingFilter extends FrontendLoggingFilter with MicroserviceFilterSupport {
    override def controllerNeedsLogging(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsLogging
  }

  object ControllerConfiguration extends ControllerConfig {
    lazy val controllerConfigs = Play.current.configuration.underlying.as[Config]("controllers")
  }

  object BusinessCustomerFrontendLoggingFilter extends FrontendLoggingFilter with MicroserviceFilterSupport{
    override def controllerNeedsLogging(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsLogging
  }

  object AwrsFrontendAuditFilter extends FrontendAuditFilter with RunMode with AppName with MicroserviceFilterSupport{

    override lazy val maskedFormFields = Seq.empty

    override lazy val applicationPort = None

    override lazy val auditConnector = AwrsFrontendAuditConnector

    override def controllerNeedsAuditing(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsAuditing

    override protected def mode: Mode = Play.current.mode
    override protected def runModeConfiguration: Configuration = Play.current.configuration

    override protected def appNameConfiguration: Configuration = Play.current.configuration
  }

}
