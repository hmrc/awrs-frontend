/*
 * Copyright 2022 HM Revenue & Customs
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

sealed trait StatusInfoResponseType

case class StatusInfoType(response: Option[StatusInfoResponseType])

case class StatusInfoSuccessResponseType(processingDate: String, secureCommText: String) extends StatusInfoResponseType

case class StatusInfoFailureResponseType(reason: String) extends StatusInfoResponseType


object StatusInfoSuccessResponseType {

  implicit val reader: Reads[StatusInfoSuccessResponseType] = new Reads[StatusInfoSuccessResponseType] {

    def reads(js: JsValue): JsResult[StatusInfoSuccessResponseType] = {
      for {
        processingDate <- (js \ "processingDate").validate[String]
        secureCommText <- (js \ "secureCommText").validate[String]
      } yield {
        StatusInfoSuccessResponseType(processingDate = processingDate, secureCommText = secureCommText)
      }
    }
  }

  implicit val formats: OFormat[StatusInfoSuccessResponseType] = Json.format[StatusInfoSuccessResponseType]
}

object StatusInfoFailureResponseType {

  implicit val reader: Reads[StatusInfoFailureResponseType] = new Reads[StatusInfoFailureResponseType] {

    def reads(js: JsValue): JsResult[StatusInfoFailureResponseType] = {
      for {
        reason <- (js \ "reason").validate[String]
      } yield {
        StatusInfoFailureResponseType(reason = reason)
      }
    }
  }
  implicit val formats: OFormat[StatusInfoFailureResponseType] = Json.format[StatusInfoFailureResponseType]
}


object StatusInfoType {

  implicit val reader: Reads[StatusInfoType] = new Reads[StatusInfoType] {

    def reads(js: JsValue): JsResult[StatusInfoType] = {
      val successResponse = Try(js.asOpt[StatusInfoSuccessResponseType]).getOrElse(None)
      val failureResponse = Try(js.asOpt[StatusInfoFailureResponseType]).getOrElse(None)
      val s = (successResponse, failureResponse) match {
        case (r@Some(_), None) => StatusInfoType(r)
        case (None, r@Some(_)) => StatusInfoType(r)
        case _ => StatusInfoType(None)
      }
      JsSuccess(s)
    }
 }


  implicit val writter: Writes[StatusInfoType] = new Writes[StatusInfoType] {
    def writes(info: StatusInfoType): JsObject =
      info.response match {
        case Some(r: StatusInfoSuccessResponseType) => StatusInfoSuccessResponseType.formats.writes(r)
        case Some(r: StatusInfoFailureResponseType) => StatusInfoFailureResponseType.formats.writes(r)
        case _ => Json.obj("unknown" -> "Etmp returned invalid json")
      }
  }

}
