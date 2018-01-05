/*
 * Copyright 2018 HM Revenue & Customs
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

import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.config.ServicesConfig

object ExternalUrls extends RunMode with ServicesConfig{

  private def loadConfig(key: String) = getConfString(key,throw new Exception(s"Missing configuration key: $key"))

  val companyAuthHost = loadConfig(s"auth.company-auth.host")
  val loginCallback = loadConfig(s"auth.login-callback.url")
  val loginPath = loadConfig(s"auth.login-path")
  val accountType = loadConfig(s"auth.accountType")
  val signIn = s"$companyAuthHost/gg/$loginPath?continue=$loginCallback&accountType=$accountType"
  val loginURL = s"$companyAuthHost/gg/$loginPath"
  val logoutCallbackUrl = loadConfig(s"auth.logout-callback.url")
  val signOut = s"$companyAuthHost/gg/sign-out/?continue=$logoutCallbackUrl"
  val businessCustomerStartPage = loadConfig(s"business-customer.serviceRedirectUrl")
  val businessTaxAccountPage = loadConfig(s"business-tax-account.serviceRedirectUrl")
}
