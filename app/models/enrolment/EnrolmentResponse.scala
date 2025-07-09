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

package models.enrolment

import play.api.libs.json.{Json, Reads}

sealed trait EnrolmentResponse

case class Identifier(key: String, value: String)
case class Verifier(key: String, value: String)
case class Enrolment(identifiers: Seq[Identifier], verifiers: Seq[Verifier])
case class EnrolmentSuccessResponse(service: String, enrolments: Seq[Enrolment]) extends EnrolmentResponse

object EnrolmentSuccessResponse {
  implicit val identifierReads: Reads[Identifier]      = Json.reads[Identifier]
  implicit val verifierReads: Reads[Verifier]          = Json.reads[Verifier]
  implicit val enrolmentReads: Reads[Enrolment]        = Json.reads[Enrolment]
  implicit val responseReads: Reads[EnrolmentSuccessResponse] = Json.reads[EnrolmentSuccessResponse]
}

case class EnrolmentsErrorResponse(cause: Exception) extends EnrolmentResponse