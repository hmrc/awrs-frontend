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

import play.api.libs.json.{Json, OFormat}

case class SuccessfulSubscriptionResponse(processingDate: String, awrsRegistrationNumber: String, etmpFormBundleNumber: String)

object SuccessfulSubscriptionResponse {
  implicit val formats: OFormat[SuccessfulSubscriptionResponse] = Json.format[SuccessfulSubscriptionResponse]
}
case class SelfHealSubscriptionResponse(regimeRefNumber: String)

object SelfHealSubscriptionResponse {
  implicit val formats: OFormat[SelfHealSubscriptionResponse] = Json.format[SelfHealSubscriptionResponse]
}

case class EnrolRequest(portalId: String, serviceName: String, friendlyName: String, knownFacts: Seq[String])

object EnrolRequest {
  implicit val formats: OFormat[EnrolRequest] = Json.format[EnrolRequest]
}

case class Identifier(`type`: String, value: String)

object Identifier {
  implicit val formats: OFormat[Identifier] = Json.format[Identifier]
}

case class EnrolResponse(serviceName: String, state: String, identifiers: Seq[Identifier])

object EnrolResponse {
  implicit val formats: OFormat[EnrolResponse] = Json.format[EnrolResponse]
}

case class SuccessfulUpdateSubscriptionResponse(processingDate: String, etmpFormBundleNumber: String)

object SuccessfulUpdateSubscriptionResponse {
  implicit val formats: OFormat[SuccessfulUpdateSubscriptionResponse] = Json.format[SuccessfulUpdateSubscriptionResponse]
}

case class SuccessfulUpdateGroupBusinessPartnerResponse(processingDate: String)

object SuccessfulUpdateGroupBusinessPartnerResponse {
  implicit val formats: OFormat[SuccessfulUpdateGroupBusinessPartnerResponse] = Json.format[SuccessfulUpdateGroupBusinessPartnerResponse]
}

case class WithdrawalResponse(processingDate: String)

object WithdrawalResponse {
  implicit val formats: OFormat[WithdrawalResponse] = Json.format[WithdrawalResponse]
}

// de-enroll
case class DeEnrolRequest(keepAgentAllocations: Boolean)

object DeEnrolRequest {
  implicit val format: OFormat[DeEnrolRequest] = Json.format[DeEnrolRequest]
}
