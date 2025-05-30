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

import models.reenrolment.AwrsRegisteredPostcode
import org.scalatestplus.play.PlaySpec

class AwrsRegisteredPostcodeModelTest extends PlaySpec {

  "AwrsRegisteredPostcode sanitisation" must {

    "ensure differently structured versions of the same postcode are equal after sanitisation" in {

      val sanitisedPostcodeVersion = "ne270jz"
      val postcodeVersion1 = "N E 27 0JZ"
      val postcodeVersion2 = "(nE)_27-0[*]jZ"

      AwrsRegisteredPostcode.sanitise(postcodeVersion1) mustBe AwrsRegisteredPostcode.sanitise(postcodeVersion2)
      AwrsRegisteredPostcode.sanitise(postcodeVersion1) mustBe sanitisedPostcodeVersion
    }
  }
}
