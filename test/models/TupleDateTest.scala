/*
 * Copyright 2019 HM Revenue & Customs
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


class TupleDateTest extends UnitSpec with Matchers with MockitoSugar {
  "TupleDate" should {
    "transform the date into the correct format" in {
      val day = "31"
      val month = "03"
      val year = "2017"
      val date: TupleDate = TupleDate(day, month, year)

      date.toString("YYYY-MM-dd") shouldBe f"$year-$month-$day"
      date.toString("dd-MM-YYYY") shouldBe f"$day-$month-$year"
    }
  }
}
