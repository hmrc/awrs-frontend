/*
 * Copyright 2020 HM Revenue & Customs
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

package models

import play.api.libs.json._

import scala.util.{Failure, Success, Try}

case class EmailRequest(apiType: ApiTypes.ApiType,
                        businessName: String,
                        email: String,
                        reference: Option[String] = None,
                        isNewBusiness: Option[Boolean] = None,
                        deregistrationDateStr : Option[String] = None)

object ApiTypes extends Enumeration {

  implicit def convertToString(value: Value): String = value.toString

  type ApiType = Value

  val API4: ApiTypes.Value = Value("api4")
  val API6Pending: ApiTypes.Value = Value("api6.pending")
  val API6Approved: ApiTypes.Value = Value("api6.approved")
  val API10: ApiTypes.Value = Value("api10")
  val API8: ApiTypes.Value = Value("api8")

  implicit val reader: Reads[ApiTypes.Value] = new Reads[ApiTypes.Value] {

    def reads(js: JsValue): JsResult[ApiTypes.Value] = js match {
      case JsString(s) =>
        Try(ApiTypes.withName(s)) match {
          case Success(value) => JsSuccess(value)
          case Failure(e) => JsError("api_type.invalid") //This key doesn't exist
        }
      case _ => JsError("error.expected.jsstring") //This key doesn't exist
    }

  }

  implicit val writer: Writes[ApiTypes.Value] = new Writes[ApiTypes.Value] {
    def writes(apiType: ApiTypes.Value): JsValue = Json.toJson(apiType.toString)
  }

}

object EmailRequest {
  implicit val formats: OFormat[EmailRequest] = Json.format[EmailRequest]
}
