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
import play.api.libs.json._

case class SubscriptionStatusType(processingDate: String,
                                  formBundleStatus: FormBundleStatus,
                                  deRegistrationDate: Option[String],
                                  groupBusinessPartner: Boolean,
                                  businessContactNumber: Option[String],
                                  safeId : Option[String] = None)

object SubscriptionStatusType {
  // TODO add date validation back in once we get the expected formats
  //  val dateFormatIn = "yyyy-MM-dd'T'HH:mm:ssZ"
  //  val dateFormatOut = "dd MMMM yyyy"
  //
  //  def parseDate (dateString:String):String =
  //    DateTime.parse(dateString, DateTimeFormat.forPattern(dateFormatIn)).toString(dateFormatOut)
  //
  //  def parseDate (dateString:Option[String]):Option[String] = dateString match{
  //    case Some(date)=> Some(parseDate(date))
  //    case None => None
  //  }


  val reader: Reads[SubscriptionStatusType] = new Reads[SubscriptionStatusType] {

    def reads(js: JsValue): JsResult[SubscriptionStatusType] = {
      for {
        processingDate <- (js \ "processingDate").validate[String] //.map(dateString=>parseDate(dateString))
        formBundleStatus <- (js \ "formBundleStatus").validate[FormBundleStatus]
        deRegistrationDate <- (js \ "deRegistrationDate").validateOpt[String] //.map(dateString=>parseDate(dateString))
        groupBusinessPartner <- (js \ "groupBusinessPartner").validate[Boolean]
        businessContactNumber <- (js \ "businessContactNumber").validateOpt[String]
        safeId <- (js \ "safeId").validateOpt[String]
      } yield {
        SubscriptionStatusType(processingDate = processingDate,
          formBundleStatus = formBundleStatus,
          deRegistrationDate = deRegistrationDate,
          groupBusinessPartner = groupBusinessPartner,
          businessContactNumber = businessContactNumber,
          safeId = safeId
        )
      }
    }
  }

  implicit val formats: OFormat[SubscriptionStatusType] = Json.format[SubscriptionStatusType]
}

sealed trait FormBundleStatus {
  def code: String

  def name: String

  override def toString: String = f"$name($code)"
}

object FormBundleStatus {

  val allStatus: Set[FormBundleStatus] =
    Set(NoStatus,
      Pending,
      Withdrawal,
      Approved,
      ApprovedWithConditions,
      Rejected,
      RejectedUnderReviewOrAppeal,
      Revoked,
      RevokedUnderReviewOrAppeal,
      DeRegistered)

  implicit val reader: Reads[FormBundleStatus] = new Reads[FormBundleStatus] {
    def reads(json: JsValue): JsResult[FormBundleStatus] =
      JsSuccess(json match {
        case JsString(code) => apply(code)
        case _ => apply("-01")
      })
  }

  implicit val writer: Writes[FormBundleStatus] = new Writes[FormBundleStatus] {
    def writes(v: FormBundleStatus): JsValue = JsString(v.code)
  }

  def apply(code: String): FormBundleStatus = code match {
    case NoStatus.code => NoStatus
    case Pending.code => Pending
    case Withdrawal.code => Withdrawal
    case Approved.code => Approved
    case ApprovedWithConditions.code => ApprovedWithConditions
    case Rejected.code => Rejected
    case RejectedUnderReviewOrAppeal.code => RejectedUnderReviewOrAppeal
    case Revoked.code => Revoked
    case RevokedUnderReviewOrAppeal.code => RevokedUnderReviewOrAppeal
    case DeRegistered.code => DeRegistered
    case _ => NotFound(code)
  }

  case object NoStatus extends FormBundleStatus {
    val code = "00"
    val name = "None"
  }

  case object Pending extends FormBundleStatus {
    val code = "01"
    val name = "Pending"
  }

  case object Withdrawal extends FormBundleStatus {
    val code = "02"
    val name = "Withdrawal"
  }

  case object Approved extends FormBundleStatus {
    val code = "04"
    val name = "Approved"
  }

  case object ApprovedWithConditions extends FormBundleStatus {
    val code = "05"
    val name = "Approved with Conditions"
  }

  case object Rejected extends FormBundleStatus {
    val code = "06"
    val name = "Rejected"
  }

  case object RejectedUnderReviewOrAppeal extends FormBundleStatus {
    val code = "07"
    val name = "Rejected under Review/Appeal"
  }

  case object Revoked extends FormBundleStatus {
    val code = "08"
    val name = "Revoked"
  }

  case object RevokedUnderReviewOrAppeal extends FormBundleStatus {
    val code = "09"
    val name = "Revoked under Review/Appeal"
  }

  case object DeRegistered extends FormBundleStatus {
    val code = "10"
    val name = "De-Registered"
  }

  case class NotFound(code: String) extends FormBundleStatus {
    val name = "Not Found"
  }
}
