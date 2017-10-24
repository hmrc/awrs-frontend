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

package services

import config.WSHttp
import play.api.libs.json.JsValue
import uk.gov.hmrc.address.client.v1.RecordSet
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import utils.LoggingUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpReads}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext

sealed trait AddressLookupResponse
case class AddressLookupSuccessResponse(addressList: RecordSet) extends AddressLookupResponse
case class AddressLookupErrorResponse(cause: Exception) extends AddressLookupResponse

trait AddressLookupService extends LoggingUtils {

  def http: HttpGet
  def addressLookupUrl: String

  def lookup(postcode: String)(implicit hc: HeaderCarrier): Future[AddressLookupResponse] = {
    val awrsHc = hc.withExtraHeaders("X-Hmrc-Origin" -> "AWRS")
    http.GET[JsValue](s"$addressLookupUrl/uk/addresses?postcode=$postcode")(implicitly[HttpReads[JsValue]], awrsHc, MdcLoggingExecutionContext.fromLoggingDetails(hc)
    ) map {
      addressListJson =>
        AddressLookupSuccessResponse(RecordSet.fromJsonAddressLookupService(addressListJson))
    } recover {
      case e: Exception =>
        warn(s"Error received from address lookup service: $e")
        AddressLookupErrorResponse(e)
    }
  }
}

object AddressLookupService extends AddressLookupService with ServicesConfig {
  override val http = WSHttp
  override val addressLookupUrl = baseUrl("address-lookup")
}

trait HasAddressLookupService {
  val addressLookupService: AddressLookupService
}
