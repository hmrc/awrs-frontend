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

import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.wordspec.AnyWordSpec

class AwrsRegisteredPostcodeSpec extends AnyWordSpec with Matchers {
  val expected = "ne981zz"

  "sanitise" should {
    "strip spaces, punctuation and normalise case" in {
      val invalidPostcode = "NE98) - [1 ZZ\\{*}]"

      AwrsRegisteredPostcode.sanitise(invalidPostcode) shouldBe expected
    }

    "handle common punctuation and spacing variants of the same postcode" in {
      val variants = Table(
        "raw",
        "NE98 1ZZ",
        "ne981zz",
        "  Ne98   1zZ  ",
        "NE98-1ZZ",
        "NE98.1ZZ",
        "NE98,1ZZ",
        "NE98:1ZZ",
        "NE98_1ZZ",
        "NE98(1ZZ)",
        "NE98[1ZZ]",
        "NE98{1ZZ}",
        "NE98^1ZZ",
        "NE98*1ZZ",
        "NE98/1ZZ"
      )

      forAll(variants) { raw =>
        AwrsRegisteredPostcode.sanitise(raw) shouldBe expected
      }
    }
  }

  "sanitise and compare" should {
    "treat differently-formatted inputs as equal" in {
      val postcode = "NE98 1ZZ"
      val postcodesToCompare = Seq(
        "ne981zz",
        " Ne98   1zZ ",
        "NE98-1ZZ",
        "NE98.1ZZ",
        "NE98,1ZZ",
        "NE98:1ZZ",
        "NE98_1ZZ",
        "NE98(1ZZ)",
        "NE98[1ZZ]",
        "NE98{1ZZ}",
        "NE98^1ZZ",
        "NE98*1ZZ",
        "NE98/1ZZ"
      )

      postcodesToCompare.foreach { alternateRepresentation =>
        withClue(s"$alternateRepresentation should equal $postcode after sanitisation") {
          AwrsRegisteredPostcode.sanitiseAndCompare(alternateRepresentation, postcode) shouldBe true
        }
      }
    }
  }

}
