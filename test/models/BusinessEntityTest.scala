/*
 * Copyright 2017 HM Revenue & Customs
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

import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec

import BusinessDetailsEntityTypes._

class BusinessEntityTest extends UnitSpec with Matchers with MockitoSugar {
  "BusinessEntity" should {
    "transform the enums correctly back and forth to json" in {
      values.foreach { eValue =>
        val js = writer.writes(eValue)
        val enum = reader.reads(js)
        enum.isSuccess shouldBe true
        enum.get shouldBe eValue
      }
    }
  }
}