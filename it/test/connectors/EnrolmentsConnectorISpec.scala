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

package connectors

import models.KnownFacts
import models.enrolment.{Enrolment, EnrolmentSuccessResponse, Identifier, Verifier}
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.test.Injecting
import uk.gov.hmrc.helpers.{EnrolmentsLookupStub, IntegrationSpec}

import scala.concurrent.ExecutionContext.Implicits.global


class EnrolmentsConnectorISpec extends IntegrationSpec with Injecting with Matchers {

  private val connector: EnrolmentsConnector = inject[EnrolmentsConnector]

  "lookupEnrolments" when {
    "return EnrolmentSuccessResponse when service returns valid JSON" in {
      val urn = "1234567890"
      val postcode = "SW1A 2AA"

      EnrolmentsLookupStub.stubEnrolmentSuccessResponse(urn)(status = 200)

      val successResponse = EnrolmentSuccessResponse(
        service = "IR-SA",
        enrolments = Seq(
          Enrolment(
            identifiers = Seq(
              Identifier(key = "UTR", value = "1234567890")
            ),
            verifiers = Seq(
              Verifier(key = "NINO", value = "AB112233D"),
              Verifier(key = "Postcode", value = postcode)
            )
          )
        )
      )


      val knownFacts = KnownFacts(urn = urn)

      await(connector.lookupEnrolments(knownFacts)) shouldBe successResponse



    }
  }
}
