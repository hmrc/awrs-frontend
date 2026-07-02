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

package controllers.util

import models.{AwrsEnrolmentUrn, AwrsEnrolmentUtr, AwrsPostcodeModel}
import models.reenrolment.KnownFactsResponse

object KnownFactsVerifier {

  def knownFactsVerified(optionalKnownFactsResponse: Option[KnownFactsResponse], awrsRef: String, isSA: Boolean, utr: String, postCode: String): Boolean = {

    optionalKnownFactsResponse match {
      case Some(knownFactsResponse) => knownFactsResponse.enrolments
        .filter(_.identifiers.exists(identifier =>
          identifier.key == "AWRSRefNumber" && AwrsEnrolmentUrn.sanitiseAndCompare(identifier.value, awrsRef)))
        .exists(awrsEnrolment =>
          awrsEnrolment.verifiers.exists(verifier => verifier.key == (if (isSA) "SAUTR" else "CTUTR") &&
          AwrsEnrolmentUtr.sanitiseAndCompare(verifier.value, utr)) &&
          awrsEnrolment.verifiers.exists(verifier => verifier.key == "Postcode" &&
          AwrsPostcodeModel.sanitiseAndCompare(verifier.value, postCode)))
      case None => false
    }
  }
}
