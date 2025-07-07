/*
 * Copyright 2025 HM Revenue & Customs
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

package models.reenrolment

import models.SuccessfulSubscriptionResponse
import play.api.libs.json.{Json, OFormat}


case class Identifier(key: String, value: String)
case class Verifier(key: String, value: String)
case class Enrolment(identifiers: Seq[Identifier], verifiers: Seq[Verifier])
case class EnrolmentSuccessResponse(service: String, enrolments: Seq[Enrolment])

object EnrolmentSuccessResponse {
  implicit val formats: OFormat[SuccessfulSubscriptionResponse] = Json.format[SuccessfulSubscriptionResponse]
  implicit val identifierReads: OFormat[Identifier] = Json.format[Identifier]
  implicit val verifierReads: OFormat[Verifier] = Json.format[Verifier]
  implicit val enrolmentReads: OFormat[Enrolment] = Json.format[Enrolment]
  implicit val responseReads: OFormat[EnrolmentSuccessResponse] = Json.format[EnrolmentSuccessResponse]
}
