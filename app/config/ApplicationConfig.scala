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

package config

import play.api.Play._
import uk.gov.hmrc.play.config.ServicesConfig


trait ApplicationConfig {

  val assetsPrefix: String
  val betaFeedbackUrl: String
  val betaFeedbackUnauthenticatedUrl: String
  val analyticsToken: Option[String]
  val analyticsHost: String
  val externalReportProblemUrl: String
  val reportAProblemPartialUrl:String
  val homeUrl: String
  val defaultTimeoutSeconds: Int
  val timeoutCountdown: Int
  val logoutCallbackUrl: String

}

object ApplicationConfig extends ApplicationConfig with ServicesConfig {

  private def loadConfig(key: String) = getConfString(key,throw new Exception(s"Missing configuration key: $key"))

  private val contactFrontendService = baseUrl("contact-frontend")
  private val contactHost = loadConfig(s"contact-frontend.host")
  private val awrsHost = loadConfig(s"awrs.host")

  override lazy val assetsPrefix: String = getString(s"assets.url") + getString(s"assets.version")
  override lazy val betaFeedbackUrl = "/alcohol-wholesale-scheme/feedback"
  override lazy val analyticsToken: Option[String] = Some(getString(s"google-analytics.token"))
  override lazy val analyticsHost: String = getString(s"google-analytics.host")
  override lazy val externalReportProblemUrl = s"$contactHost/contact/problem_reports"
  override lazy val betaFeedbackUnauthenticatedUrl = s"$contactHost/contact/beta-feedback-unauthenticated"
  override lazy val reportAProblemPartialUrl = s"$contactFrontendService/contact/problem_reports?secure=true"
  override lazy val homeUrl = s"$awrsHost/"
  override lazy val defaultTimeoutSeconds: Int = getString(s"defaultTimeoutSeconds").toInt
  override lazy val timeoutCountdown: Int = getString(s"timeoutCountdown").toInt
  override lazy val logoutCallbackUrl = loadConfig("tamc.external-urls.logout-callback-url")

}
