/*
 * Copyright 2021 HM Revenue & Customs
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

import audit.Auditable
import javax.inject.Inject
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.address.client.v1.RecordSet
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import utils.LoggingUtils
import uk.gov.hmrc.http.HttpReads.Implicits.readJsValue

import scala.concurrent.{ExecutionContext, Future}

sealed trait AddressLookupResponse

case class AddressLookupSuccessResponse(addressList: RecordSet) extends AddressLookupResponse
case class AddressLookupErrorResponse(cause: Exception) extends AddressLookupResponse

class AddressLookupConnector @Inject()(servicesConfig: ServicesConfig,
                                       val http: DefaultHttpClient,
                                       val auditable: Auditable) extends LoggingUtils {

  val addressLookupUrl: String = servicesConfig.baseUrl("address-lookup")

  def lookup(postcode: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AddressLookupResponse] = {
    val headers = Seq("X-Hmrc-Origin" -> "AWRS")

    http.POST[JsValue, JsValue](s"$addressLookupUrl/lookup", Json.obj("postcode" -> postcode), headers) map {
      addressListJson =>
        AddressLookupSuccessResponse(RecordSet.fromJsonAddressLookupService(addressListJson))
    } recover {
      case e: Exception =>
        warn(s"Error received from address lookup service: $e")
        AddressLookupErrorResponse(e)
    }
  }
}

trait HasAddressLookupConnector {
  val addressLookupConnector: AddressLookupConnector
}
