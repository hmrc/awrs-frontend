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
case class KnownFactsResponse(service: String, enrolments: Seq[Enrolment])

object KnownFactsResponse {
  given formats: OFormat[SuccessfulSubscriptionResponse] = Json.format[SuccessfulSubscriptionResponse]
  given identifierReads: OFormat[Identifier] = Json.format[Identifier]
  given verifierReads: OFormat[Verifier] = Json.format[Verifier]
  given enrolmentReads: OFormat[Enrolment] = Json.format[Enrolment]
  given responseReads: OFormat[KnownFactsResponse] = Json.format[KnownFactsResponse]
}
