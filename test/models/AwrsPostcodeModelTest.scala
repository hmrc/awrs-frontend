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

package models

import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.Tables.Table
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class AwrsPostcodeModelTest extends PlaySpec {

  "AwrsRegisteredPostcode sanitisation" must {

    "ensure differently structured versions of the same postcode are equal after sanitisation" in {

      val sanitisedPostcodeVersion = "ne270jz"
      val postcodeVersion1         = "N E 27 0JZ"
      val postcodeVersion2         = "(nE)_27-0[*]jZ"

      AwrsPostcodeModel.sanitise(postcodeVersion1) mustBe AwrsPostcodeModel.sanitise(postcodeVersion2)
      AwrsPostcodeModel.sanitise(postcodeVersion1) mustBe sanitisedPostcodeVersion
    }

    "ensure the same postcode is equal after sanitisation" in {
      AwrsPostcodeModel.sanitiseAndCompare("ne270jz", "Ne27 0jZ ") mustBe true
    }

    "handle common punctuation and spacing variants of the same postcode" in {
      val expected = "NE981ZZ"

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
        "NE98*1ZZ"
        // "NE98/1ZZ" todo should this input be parsed into a valid postcode based on https://jira.tools.tax.service.gov.uk/browse/DL-16390
      )

      forAll(variants) { raw =>
        AwrsPostcodeModel.sanitise(raw).toUpperCase() shouldBe expected
      }
    }
  }

  "model should convert from json to domain" in {
    val json   = """{"registeredPostcode":"NE27 0JZ"}"""
    val result = Json.parse(json).as[AwrsPostcodeModel]

    result.registeredPostcode shouldBe "NE27 0JZ"
  }
}
