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

package controllers.auth

import play.api.{Configuration, Play}
import play.api.Play.current
import play.api.Mode.Mode
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.config.ServicesConfig

object ExternalUrls extends RunMode with ServicesConfig{

  private def loadConfig(key: String) = getConfString(key,throw new Exception(s"Missing configuration key: $key"))

  lazy val companyAuthHost = loadConfig(s"auth.company-auth.host")
  lazy val loginCallback = loadConfig(s"auth.login-callback.url")
  lazy val loginPath = loadConfig(s"auth.login-path")
  lazy val accountType = loadConfig(s"auth.accountType")
  lazy val signIn = s"$companyAuthHost/gg/$loginPath?continue=$loginCallback&accountType=$accountType"
  lazy val loginURL = s"$companyAuthHost/gg/$loginPath"
  lazy val logoutCallbackUrl = loadConfig(s"auth.logout-callback.url")
  lazy val signOut = s"$companyAuthHost/gg/sign-out/?continue=$logoutCallbackUrl"
  lazy val businessCustomerStartPage = loadConfig(s"business-customer.serviceRedirectUrl")
  lazy val businessTaxAccountPage = loadConfig(s"business-tax-account.serviceRedirectUrl")

  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration
}
