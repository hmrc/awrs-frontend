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

package views.html.helpers

import utils.AwrsUnitTestTraits

class PackageTest extends AwrsUnitTestTraits {

  type Testdata = Map[Int, String]

  def Testdata(data: (Int, String)*) = Map[Int, String](data: _*)

  def test(expectations: Testdata) = expectations.foreach { case (key: Int, value: String) => ordinalIntSuffix(key) shouldBe value }

  "ordinalIntSuffix" should {

    "return words for 1 - 20" in {
      val testData = Testdata(
        1 -> "first",
        2 -> "second",
        3 -> "third",
        4 -> "fourth",
        5 -> "fifth",
        6 -> "sixth",
        7 -> "seventh",
        8 -> "eighth",
        9 -> "ninth",
        10 -> "tenth",
        11 -> "eleventh",
        12 -> "twelfth",
        13 -> "thirteenth",
        14 -> "fourteenth",
        15 -> "fifteenth",
        16 -> "sixteenth",
        17 -> "seventeenth",
        18 -> "eighteenth",
        19 -> "nineteenth",
        20 -> "twentieth"
      )
      test(testData)
    }

    "return 1st 2nd 3rd for 1 2 3 in anything but 1 2 3 or 11 12 and 13" in {
      val testData = Testdata(
        21 -> "21st",
        22 -> "22nd",
        23 -> "23rd",
        121 -> "121st",
        122 -> "122nd",
        123 -> "123rd"
      )
      test(testData)
    }


    "return nth for other numbers" in {
      val testData = Testdata(
        0 -> "0th",
        24 -> "24th",
        25 -> "25th",
        26 -> "26th",
        27 -> "27th",
        28 -> "28th",
        29 -> "29th",
        30 -> "30th"
      )
      test(testData)
    }

  }

}
