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
   given formats: Format[DeRegistrationDate] = Json.format[DeRegistrationDate]
   given optionFormats: Format[Option[DeRegistrationDate]] = Format.optionWithNull[DeRegistrationDate]
}


object DeRegistrationReason {
  given formats: Format[DeRegistrationReason] = Json.format[DeRegistrationReason]
  given optionFormats: Format[Option[DeRegistrationReason]] = Format.optionWithNull[DeRegistrationReason]
}




object DeRegistrationConfirmation {
  given formats: Format[DeRegistrationConfirmation] = Json.format[DeRegistrationConfirmation]
}

object DeRegistration {
  given formats: Format[DeRegistration] = Json.format[DeRegistration]
  given optionFormats: Format[Option[DeRegistration]] = Format.optionWithNull[DeRegistration]
  val dateFormat = "yyyy-MM-dd"

  // N.B. for the sake of simplicity this is not made into an overload of apply because it would break the above Json.format call
  def toDeRegistration(someDate: Option[DeRegistrationDate], someReason: Option[DeRegistrationReason]): Option[DeRegistration] =
    (someDate, someReason) match {
      case (Some(date), Some(reason)) => Some(DeRegistration(date.proposedEndDate.toString(dateFormat), reason.deregistrationReason.get, reason.deregReasonOther))
      case _ => None
    }
}


object DeRegistrationSuccessResponseType {
  given reader: Reads[DeRegistrationSuccessResponseType] = new Reads[DeRegistrationSuccessResponseType] {

    def reads(js: JsValue): JsResult[DeRegistrationSuccessResponseType] =
      for {
        processingDate <- (js \ "processingDate").validate[String]
      } yield {
        DeRegistrationSuccessResponseType(processingDate = processingDate)
      }

  }

  given writter: OWrites[DeRegistrationSuccessResponseType] = Json.writes[DeRegistrationSuccessResponseType]
}

object DeRegistrationFailureResponseType {
  given reader: Reads[DeRegistrationFailureResponseType] = new Reads[DeRegistrationFailureResponseType] {

    def reads(js: JsValue): JsResult[DeRegistrationFailureResponseType] =
      for {
        reason <- (js \ "reason").validate[String]
      } yield {
        DeRegistrationFailureResponseType(reason = reason)
      }

  }
  given writter: OWrites[DeRegistrationFailureResponseType] = Json.writes[DeRegistrationFailureResponseType]
}


object DeRegistrationType {

  given reader: Reads[DeRegistrationType] = new Reads[DeRegistrationType] {
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

  given writter: Writes[DeRegistrationType] = new Writes[DeRegistrationType] {
    def writes(info: DeRegistrationType): JsObject =
      info.response match {
        case Some(r: DeRegistrationSuccessResponseType) => DeRegistrationSuccessResponseType.writter.writes(r)
        case Some(r: DeRegistrationFailureResponseType) => DeRegistrationFailureResponseType.writter.writes(r)
        case _ => Json.obj("unknown" -> "Etmp returned invalid json")
      }
  }

}
