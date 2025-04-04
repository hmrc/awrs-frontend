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

package forms.validation.util

import org.scalatest.verbs.MustVerb
import org.scalatest.wordspec.AnyWordSpecLike
import utils.TestUtil.convertToAnyMustWrapper

class UTRValidatorTest extends AnyWordSpecLike with MustVerb {
  "UTRValidator" should {
    "validate UTR" in {
      val utr = "8951309411"
      UTRValidator.validateUTR(utr) mustBe (true)
    }
    "does not validate UTR" in {
      val utr = "123456789"
      UTRValidator.validateUTR(utr) mustBe  false
    }
  }
}
