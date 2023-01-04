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

package config

import audit.Auditable
import javax.inject.Inject
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.{AwrsFieldConfig, CountryCodes}
import views.html.{awrs_application_error, unauthorised}
import views.html.view_application.subviews.subview_delete_confirmation
import views.html.helpers.awrsErrorNotFoundTemplate
import views.html.error_template

class ApplicationConfig @Inject()(val servicesConfig: ServicesConfig,
                                  val countryCodes: CountryCodes,
                                  val auditable: Auditable,
                                  val templateAppError: awrs_application_error,
                                  val templateUnauthorised: unauthorised,
                                  val templateDeleteConfirm: subview_delete_confirmation,
                                  val templateNotFound: awrsErrorNotFoundTemplate,
                                  val templateError: error_template) extends AwrsFieldConfig {

  private def loadConfig(key: String) = servicesConfig.getConfString(key, throw new Exception(s"Missing configuration key: $key"))

  private lazy val contactHost = servicesConfig.getString("contact-frontend.host")
  private lazy val awrsHost = loadConfig("awrs.host")

  lazy val assetsPrefix: String = loadConfig("assets.url") + loadConfig("assets.version")
  lazy val externalReportProblemUrl = s"$contactHost/contact/problem_reports"
  lazy val contactFormServiceIdentifier = "AWRS"
  lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  lazy val homeUrl = s"$awrsHost/"
  lazy val defaultTimeoutSeconds: Int = servicesConfig.getInt("defaultTimeoutSeconds")
  lazy val timeoutCountdown: Int = servicesConfig.getInt("timeoutCountdown")
  lazy val emailVerificationEnabled: Boolean = servicesConfig.getBoolean("email-verification.enabled")
  lazy val emailVerificationBaseUrl: String = servicesConfig.getString("email-verification.continue.baseUrl")

  //From ExternalUrls
  lazy val basGatewayHost: String = loadConfig("auth.bas-gateway-frontend.host")
  lazy val loginCallback: String = loadConfig("auth.login-callback.url")
  lazy val loginPath: String = loadConfig("auth.login-path")
  lazy val accountType: String = loadConfig("auth.accountType")
  lazy val signIn: String = s"$basGatewayHost/bas-gateway/$loginPath?continue_url=$loginCallback"
  lazy val loginURL: String = s"$basGatewayHost/bas-gateway/$loginPath"
  lazy val logoutCallbackUrl: String = loadConfig("auth.logout-callback.url")
  lazy val signOut: String = servicesConfig.getString("service-signout.url")
  lazy val businessCustomerStartPage: String = loadConfig("business-customer.serviceRedirectUrl")
  lazy val businessTaxAccountPage: String = loadConfig("business-tax-account.serviceRedirectUrl")

  lazy val cookies: String = servicesConfig.getString("urls.footer.cookies")
  lazy val accessibilityStatement: String = servicesConfig.getString("urls.footer.accessibility_statement")
  lazy val privacy: String = servicesConfig.getString("urls.footer.privacy_policy")
  lazy val termsConditions: String = servicesConfig.getString("urls.footer.terms_and_conditions")
  lazy val govukHelp: String = servicesConfig.getString("urls.footer.help_page")
}
