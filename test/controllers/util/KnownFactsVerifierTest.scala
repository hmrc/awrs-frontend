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

package controllers.util

import models.AwrsEnrolmentUtr
import models.reenrolment._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class KnownFactsVerifierTest extends AnyWordSpecLike with Matchers with GuiceOneAppPerSuite {

  private val testAwrsRef = "XAAW00000123456"
  private val testUtr = "1234567890"
  private val testPostcode = "AA1 1AA"

  private def createTestEnrolment(awrsRef: String = testAwrsRef,
                                  utr: String = testUtr,
                                  postcode: String = testPostcode,
                                  isSA: Boolean = true): Enrolment = {
    val identifierType = if (isSA) "SAUTR" else "CTUTR"
    Enrolment(
      identifiers = Seq(Identifier("AWRSRefNumber", awrsRef)),
      verifiers = Seq(
        Verifier(identifierType, utr),
        Verifier("Postcode", postcode)
      )
    )
  }

  "KnownFactsVerifier" should {
    "return true" when {
      "all known facts match exactly for SA UTR" in {
        val enrolment = createTestEnrolment()
        val response = Some(KnownFactsResponse("IR-AWRS", Seq(enrolment)))

        val result = KnownFactsVerifier.knownFactsVerified(
          response,
          testAwrsRef,
          isSA = true,
          AwrsEnrolmentUtr(testUtr),
          AwrsRegisteredPostcode(testPostcode)
        )

        result shouldBe true
      }

      "all known facts match exactly for CT UTR" in {
        val enrolment = createTestEnrolment(isSA = false)
        val response = Some(KnownFactsResponse("IR-AWRS", Seq(enrolment)))

        val result = KnownFactsVerifier.knownFactsVerified(
          response,
          testAwrsRef,
          isSA = false,
          AwrsEnrolmentUtr(testUtr),
          AwrsRegisteredPostcode(testPostcode)
        )

        result shouldBe true
      }

      "postcode matches case insensitively" in {
        val enrolment = createTestEnrolment(postcode = testPostcode.toLowerCase)
        val response = Some(KnownFactsResponse("IR-AWRS", Seq(enrolment)))

        val result = KnownFactsVerifier.knownFactsVerified(
          response,
          testAwrsRef,
          isSA = true,
          AwrsEnrolmentUtr(testUtr),
          AwrsRegisteredPostcode(testPostcode)
        )

        result shouldBe true
      }

      "postcode has different spacing" in {
        val enrolment = createTestEnrolment(postcode = "AA11AA")
        val response = Some(KnownFactsResponse("IR-AWRS", Seq(enrolment)))

        val result = KnownFactsVerifier.knownFactsVerified(
          response,
          testAwrsRef,
          isSA = true,
          AwrsEnrolmentUtr(testUtr),
          AwrsRegisteredPostcode(testPostcode)
        )

        result shouldBe true
      }
    }

    "return false" when {
      "response is None" in {
        val result = KnownFactsVerifier.knownFactsVerified(
          None,
          testAwrsRef,
          isSA = true,
          AwrsEnrolmentUtr(testUtr),
          AwrsRegisteredPostcode(testPostcode)
        )

        result shouldBe false
      }

      "AWRS reference doesn't match" in {
        val enrolment = createTestEnrolment(awrsRef = "XAAW00000987654")
        val response = Some(KnownFactsResponse("IR-AWRS", Seq(enrolment)))

        val result = KnownFactsVerifier.knownFactsVerified(
          response,
          testAwrsRef,
          isSA = true,
          AwrsEnrolmentUtr(testUtr),
          AwrsRegisteredPostcode(testPostcode)
        )

        result shouldBe false
      }

      "UTR doesn't match" in {
        val enrolment = createTestEnrolment(utr = "0987654321")
        val response = Some(KnownFactsResponse("IR-AWRS", Seq(enrolment)))

        val result = KnownFactsVerifier.knownFactsVerified(
          response,
          testAwrsRef,
          isSA = true,
          AwrsEnrolmentUtr(testUtr),
          AwrsRegisteredPostcode(testPostcode)
        )

        result shouldBe false
      }

      "postcode doesn't match" in {
        val enrolment = createTestEnrolment(postcode = "BB1 1BB")
        val response = Some(KnownFactsResponse("IR-AWRS", Seq(enrolment)))

        val result = KnownFactsVerifier.knownFactsVerified(
          response,
          testAwrsRef,
          isSA = true,
          AwrsEnrolmentUtr(testUtr),
          AwrsRegisteredPostcode(testPostcode)
        )

        result shouldBe false
      }

      "SA/CT UTR type doesn't match" in {
        val enrolment = createTestEnrolment(isSA = false) // CT UTR in response
        val response = Some(KnownFactsResponse("IR-AWRS", Seq(enrolment)))

        val result = KnownFactsVerifier.knownFactsVerified(
          response,
          testAwrsRef,
          isSA = true, // But we're checking for SA UTR
          AwrsEnrolmentUtr(testUtr),
          AwrsRegisteredPostcode(testPostcode)
        )

        result shouldBe false
      }

      "multiple enrolments exist but none match" in {
        val enrolment1 = createTestEnrolment(awrsRef = "XAAW00000999999")
        val enrolment2 = createTestEnrolment(utr = "9999999999")
        val response = Some(KnownFactsResponse("IR-AWRS", Seq(enrolment1, enrolment2)))

        val result = KnownFactsVerifier.knownFactsVerified(
          response,
          testAwrsRef,
          isSA = true,
          AwrsEnrolmentUtr(testUtr),
          AwrsRegisteredPostcode(testPostcode)
        )

        result shouldBe false
      }

      "enrolment has no verifiers" in {
        val enrolment = Enrolment(
          identifiers = Seq(Identifier("AWRSRefNumber", testAwrsRef)),
          verifiers = Seq.empty
        )
        val response = Some(KnownFactsResponse("IR-AWRS", Seq(enrolment)))

        val result = KnownFactsVerifier.knownFactsVerified(
          response,
          testAwrsRef,
          isSA = true,
          AwrsEnrolmentUtr(testUtr),
          AwrsRegisteredPostcode(testPostcode)
        )

        result shouldBe false
      }
    }
  }
}