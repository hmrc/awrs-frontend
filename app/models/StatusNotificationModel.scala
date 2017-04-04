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

package models

import com.fasterxml.jackson.databind.JsonMappingException
import play.api.libs.json._


sealed trait StatusContactType {
  def code: String

  def name: String

  override def toString: String = f"$name($code)"
}

object StatusContactType {

  val allStatus: Set[StatusContactType] =
    Set(Rejected,
      Revoked,
      ApprovedWithConditions,
      MindedToReject,
      NoLongerMindedToReject,
      MindedToRevoke,
      NoLongerMindedToRevoke,
      Other)

  implicit val reader = new Reads[StatusContactType] {
    def reads(json: JsValue): JsResult[StatusContactType] =
      JsSuccess(json match {
        case JsString(code) => apply(code)
        case _ => throw new JsonMappingException(s"Unexpected StatusContactType: $json")
      })
  }

  implicit val writer: Writes[StatusContactType] = new Writes[StatusContactType] {
    def writes(v: StatusContactType): JsValue = v match {
      case value => JsString(v.code)
    }
  }

  def apply(code: String): StatusContactType = code match {
    case Rejected.code => Rejected
    case Revoked.code => Revoked
    case ApprovedWithConditions.code => ApprovedWithConditions
    case MindedToReject.code => MindedToReject
    case NoLongerMindedToReject.code => NoLongerMindedToReject
    case MindedToRevoke.code => MindedToRevoke
    case NoLongerMindedToRevoke.code => NoLongerMindedToRevoke
    case Other.code => Other
    case _ => Other
  }

  case object Rejected extends StatusContactType {
    val code = "REJR"
    val name = "Rejected"
  }

  case object Revoked extends StatusContactType {
    val code = "REVR"
    val name = "Revoked"
  }

  case object ApprovedWithConditions extends StatusContactType {
    val code = "CONA"
    val name = "Approved with Conditions"
  }

  case object MindedToReject extends StatusContactType {
    val code = "MTRJ"
    val name = "Minded to Reject"
  }

  case object NoLongerMindedToReject extends StatusContactType {
    val code = "NMRJ"
    val name = "No longer minded to Reject"
  }

  case object MindedToRevoke extends StatusContactType {
    val code = "MTRV"
    val name = "Minded to Revoke"
  }

  case object NoLongerMindedToRevoke extends StatusContactType {
    val code = "NMRV"
    val name = "No longer minded to Revoke"
  }

  case object Other extends StatusContactType {
    val code = "OTHR"
    val name = "Other"
  }

}

case class StatusNotification(registrationNumber: Option[String],
                              contactNumber: Option[String],
                              contactType: Option[StatusContactType],
                              status: Option[FormBundleStatus],
                              storageDatetime: Option[String])

object StatusNotification {

  import play.api.libs.functional.syntax._

  implicit val reader: Reads[StatusNotification] = (
    (JsPath \ "registrationNumber").readNullable[String] and
      (JsPath \ "contactNumber").readNullable[String] and
      (JsPath \ "contactType").readNullable[StatusContactType] and
      (JsPath \ "status").readNullable[FormBundleStatus] and
      (JsPath \ "storageDatetime").readNullable[String]
    ) (StatusNotification.apply _)

  implicit val writer = Json.writes[StatusNotification]

}


case class ViewedStatusResponse(viewed: Boolean)

object ViewedStatusResponse {
  implicit val format = Json.format[ViewedStatusResponse]
}
