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

package models

import play.api.libs.json.Reads._
import play.api.libs.json.{Json, _}

import scala.util.Try

sealed trait ES0Response

case class ES0SuccessResponse(principalUserIDList: List[String], delegatedUserIDList: List[String]) extends ES0Response

case object ES0NoContent extends ES0Response

case class ES0CallInfo(response: Option[ES0Response])

case class ES0FailureResponse(code: String, message: String) extends ES0Response //TODO separate case class for multuple errors

object ES0SuccessResponse {

  implicit val reader: Reads[ES0SuccessResponse] = new Reads[ES0SuccessResponse] {

    def reads(js: JsValue): JsResult[ES0SuccessResponse] = {
      for {
        principalUserIds <- (js \ "principalUserIds").validate[List[String]]
        delegatedUserIds <- (js \ "delegatedUserIds").validate[List[String]]
      } yield {
        ES0SuccessResponse(principalUserIDList = principalUserIds, delegatedUserIDList = delegatedUserIds)
      }
    }
  }

  implicit val formats: OFormat[ES0SuccessResponse] = Json.format[ES0SuccessResponse]
}

object ES0FailureResponse {

  implicit val reader: Reads[ES0FailureResponse] = new Reads[ES0FailureResponse] {

    def reads(js: JsValue): JsResult[ES0FailureResponse] = {
      for {
        code <- (js \ "code").validate[String]
        message <- (js \ "message").validate[String]
      } yield {
        ES0FailureResponse(code, message)
      }
    }
  }
  implicit val formats: OFormat[ES0FailureResponse] = Json.format[ES0FailureResponse]
}

object ES0CallInfo {

  implicit val reader: Reads[ES0CallInfo] = new Reads[ES0CallInfo] {

    def reads(js: JsValue): JsResult[ES0CallInfo] = {
      val successResponse = Try(js.asOpt[ES0SuccessResponse]).getOrElse(None)
      val failureResponse = Try(js.asOpt[ES0FailureResponse]).getOrElse(None)
      val s = (successResponse, failureResponse) match {
        case (r@Some(_), None) => ES0CallInfo(r)
        case (None, r@Some(_)) => ES0CallInfo(r)
        case _ => ES0CallInfo(None)
      }
      JsSuccess(s)
    }
  }


  implicit val writer: Writes[ES0CallInfo] = new Writes[ES0CallInfo] {
    def writes(info: ES0CallInfo): JsObject =
      info.response match {
        case Some(r: ES0SuccessResponse) => ES0SuccessResponse.formats.writes(r)
        case Some(r: ES0FailureResponse) => ES0FailureResponse.formats.writes(r)
        case _ => Json.obj("unknown" -> "Enrolment store returned 500 response")
      }
  }

}