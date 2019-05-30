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

import akka.actor.ActorSystem
import com.typesafe.config.Config
import play.api.Mode.Mode
import play.api.{Configuration, Play}
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.http.cache.client.{SessionCache, ShortLivedCache, ShortLivedHttpCaching}
import uk.gov.hmrc.http.hooks.HttpHooks
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.config.{AppName, RunMode, ServicesConfig}
import uk.gov.hmrc.play.http.ws.{WSDelete, WSGet, WSPost, WSPut}
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever
import uk.gov.hmrc.http.{CorePost, HttpDelete, HttpGet, HttpPost, HttpPut}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.config.LoadAuditingConfig

object AwrsFrontendAuditConnector extends AuditConnector with AppName {
  override lazy val auditingConfig = LoadAuditingConfig(s"auditing")

  override protected def appNameConfiguration: Configuration = Play.current.configuration
}

trait Hooks extends HttpHooks with HttpAuditing {
  override val hooks = Seq(AuditingHook)
  override lazy val auditConnector: AuditConnector = AwrsFrontendAuditConnector
}

trait WSHttp extends HttpGet with WSGet with HttpPut with WSPut with HttpPost with WSPost with HttpDelete with WSDelete with Hooks with AppName
object WSHttp extends WSHttp {
  override protected def appNameConfiguration: Configuration = Play.current.configuration

  override protected def actorSystem: ActorSystem = Play.current.actorSystem

  override protected def configuration: Option[Config] = Option(Play.current.configuration.underlying)
}

object CachedStaticHtmlPartialProvider extends CachedStaticHtmlPartialRetriever {
  override val httpGet = WSHttp
}

object FrontendAuthConnector extends FrontendAuthConnector {
  override lazy val serviceUrl = baseUrl("auth")
  override def http: CorePost = WSHttp
  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration
}

trait FrontendAuthConnector extends PlayAuthConnector with ServicesConfig {
  override lazy val serviceUrl = baseUrl("auth")
  override def http: CorePost = WSHttp
  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration
}

object BusinessCustomerSessionCache extends SessionCache with AppName with ServicesConfig{
  override lazy val http = WSHttp
  override lazy val defaultSource: String = getConfString("cachable.session-cache.review-details.cache","business-customer-frontend")

  override lazy val baseUri = baseUrl("cachable.session-cache")
  override lazy val domain = getConfString("cachable.session-cache.domain", throw new Exception(s"Could not find config 'cachable.session-cache.domain'"))

  override protected def appNameConfiguration: Configuration = Play.current.configuration

  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration
}

object AwrsSessionCache extends SessionCache with AppName with ServicesConfig{
  override lazy val http = WSHttp
  override lazy val defaultSource: String = getConfString("cachable.session-cache.awrs-frontend.cache","awrs-frontend")

  override lazy val baseUri = baseUrl("cachable.session-cache")
  override lazy val domain = getConfString("cachable.session-cache.domain", throw new Exception(s"Could not find config 'cachable.session-cache.domain'"))

  override protected def appNameConfiguration: Configuration = Play.current.configuration

  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration
}

object AwrsShortLivedCaching extends ShortLivedHttpCaching with ServicesConfig{

  override lazy val defaultSource: String = getConfString("cachableshort-lived-cache.awrs-frontend.cache","awrs-frontend")
  override lazy val baseUri: String = baseUrl("cachable.short-lived-cache")
  override lazy val domain: String = getConfString("cachable.short-lived-cache.domain", throw new Exception(s"Could not find config 'cachable.short-lived-cache.domain'"))

  override def http: HttpGet with HttpPut with HttpDelete = WSHttp

  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration
}

object AwrsAPIDataShortLivedCaching extends ShortLivedHttpCaching with ServicesConfig{

  override lazy val defaultSource: String = getConfString("cachableshort-lived-cache.awrs-frontend-api.cache","awrs-frontend-api")
  override lazy val baseUri: String = baseUrl("cachable.short-lived-cache")
  override lazy val domain: String = getConfString("cachable.short-lived-cache.domain", throw new Exception(s"Could not find config 'cachable.short-lived-cache.domain'"))

  override def http: HttpGet with HttpPut with HttpDelete = WSHttp

  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration
}

object AwrsShortLivedCache extends ShortLivedCache {
  override implicit lazy val crypto = new ApplicationCrypto(Play.current.configuration.underlying).JsonCrypto
  override lazy val shortLiveCache = AwrsShortLivedCaching
}

object AwrsAPIShortLivedCache extends ShortLivedCache {
  override implicit lazy val crypto = new ApplicationCrypto(Play.current.configuration.underlying).JsonCrypto
  override lazy val shortLiveCache = AwrsAPIDataShortLivedCaching
}

object AuthClientConnector extends PlayAuthConnector with ServicesConfig {
  val serviceUrl: String = baseUrl("auth")
  lazy val http = WSHttp

  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration
}
