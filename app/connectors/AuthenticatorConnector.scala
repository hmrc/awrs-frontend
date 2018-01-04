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

package connectors

import config.WSHttp
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.http._

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpGet, HttpPost, HttpResponse }


trait AuthenticatorConnector extends ServicesConfig with RawResponseReads {

  lazy val serviceURL = baseUrl("government-gateway-authentication")

  val refreshProfileURI = "government-gateway-authentication/refresh-profile"

  val http: HttpGet with HttpPost = WSHttp

  def refreshProfile(implicit hc: HeaderCarrier) :Future[HttpResponse]= {
    val postUrl = s"""$serviceURL/$refreshProfileURI"""
    http.POSTEmpty[HttpResponse](postUrl)
  }
}

object AuthenticatorConnector extends AuthenticatorConnector
