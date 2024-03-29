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

package exceptions

case class DESValidationException(message: String) extends Exception(message)

case class DuplicateSubscriptionException(message: String) extends Exception(message)

case class PendingDeregistrationException(message: String) extends Exception(message)

case class GovernmentGatewayException(message: String) extends Exception(message)

case class ResubmissionException(message: String) extends Exception(message)

case class InvalidStateException(message: String) extends Exception(message)

object ResubmissionException {
  val resubmissionMessage = "Your resubmission has been rejected as you made no changes to your application."
}

case class DeEnrollException(message: String) extends Exception(message)
