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

import models.AwrsPostcodeModel
import models.reenrolment.{Identifier, KnownFactsResponse, Verifier}

object KnownFactsVerifier {
  def knownFactsVerified(knownFactsResponse: Option[KnownFactsResponse], arws: String, isSA: Boolean, utr: String, postCode: String): Boolean = {
    knownFactsResponse match {
      case Some(knownFactsResponse) => knownFactsResponse.enrolments
        .find(_.identifiers.contains(Identifier("AWRSRefNumber", arws))).exists { enrolment =>
          enrolment.verifiers.contains(Verifier(if (isSA) "SAUTR" else "CTUTR", utr)) &&
            enrolment.verifiers.exists(verifier => verifier.key == "Postcode" && AwrsPostcodeModel.sanitiseAndCompare(verifier.value, postCode))
        }
      case None => false
    }
  }
}
