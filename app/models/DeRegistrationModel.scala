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

package models

import play.api.libs.json.{Writes, _}

import scala.util.Try

// to/from frontend models
case class DeRegistrationDate(proposedEndDate: TupleDate)

case class DeRegistrationReason(deregistrationReason: Option[String], deregReasonOther: Option[String])

case class DeRegistrationConfirmation(deRegistrationConfirmation: Option[String])

case class DeRegistration(deregistrationDate: String, deregistrationReason: String, deregReasonOther: Option[String])


// to/from middle service/etmp models
sealed trait DeRegistrationResponseType

case class DeRegistrationType(response: Option[DeRegistrationResponseType])

case class DeRegistrationSuccessResponseType(processingDate: String) extends DeRegistrationResponseType

case class DeRegistrationFailureResponseType(reason: String) extends DeRegistrationResponseType


object DeRegistrationDate {
   implicit val formats: Format[DeRegistrationDate] = Json.format[DeRegistrationDate]
   implicit val optionFormats = Format.optionWithNull[DeRegistrationDate]
}


object DeRegistrationReason {
  implicit val formats: Format[DeRegistrationReason] = Json.format[DeRegistrationReason]
  implicit val optionFormats = Format.optionWithNull[DeRegistrationReason]
}




object DeRegistrationConfirmation {
  implicit val formats: Format[DeRegistrationConfirmation] = Json.format[DeRegistrationConfirmation]
}

object DeRegistration {
  implicit val formats: Format[DeRegistration] = Json.format[DeRegistration]
  implicit val optionFormats = Format.optionWithNull[DeRegistration]
  val dateFormat = "YYYY-MM-dd"

  // N.B. for the sake of simplicity this is not made into an overload of apply because it would break the above Json.format call
  def toDeRegistration(someDate: Option[DeRegistrationDate], someReason: Option[DeRegistrationReason]): Option[DeRegistration] =
    (someDate, someReason) match {
      case (Some(date), Some(reason)) => Some(DeRegistration(date.proposedEndDate.toString(dateFormat), reason.deregistrationReason.get, reason.deregReasonOther))
      case _ => None
    }
}


object DeRegistrationSuccessResponseType {
  implicit val reader = new Reads[DeRegistrationSuccessResponseType] {

    def reads(js: JsValue): JsResult[DeRegistrationSuccessResponseType] =
      for {
        processingDate <- (js \ "processingDate").validate[String]
      } yield {
        DeRegistrationSuccessResponseType(processingDate = processingDate)
      }

  }

  implicit val writter = Json.writes[DeRegistrationSuccessResponseType]
}

object DeRegistrationFailureResponseType {
  implicit val reader = new Reads[DeRegistrationFailureResponseType] {

    def reads(js: JsValue): JsResult[DeRegistrationFailureResponseType] =
      for {
        reason <- (js \ "reason").validate[String]
      } yield {
        DeRegistrationFailureResponseType(reason = reason)
      }

  }
  implicit val writter = Json.writes[DeRegistrationFailureResponseType]
}


object DeRegistrationType {

  implicit val reader = new Reads[DeRegistrationType] {
    def reads(js: JsValue): JsResult[DeRegistrationType] = {
      val successResponse = Try(js.asOpt[DeRegistrationSuccessResponseType]).getOrElse(None)
      val failureResponse = Try(js.asOpt[DeRegistrationFailureResponseType]).getOrElse(None)
      val s = (successResponse, failureResponse) match {
        case (r@Some(_), None) => DeRegistrationType(r)
        case (None, r@Some(_)) => DeRegistrationType(r)
        case _ => DeRegistrationType(None)
      }
      JsSuccess(s)
    }
  }

  implicit val writter = new Writes[DeRegistrationType] {
    def writes(info: DeRegistrationType) =
      info.response match {
        case Some(r: DeRegistrationSuccessResponseType) => DeRegistrationSuccessResponseType.writter.writes(r)
        case Some(r: DeRegistrationFailureResponseType) => DeRegistrationFailureResponseType.writter.writes(r)
        case _ => Json.obj("unknown" -> "Etmp returned invalid json")
      }
  }

}
