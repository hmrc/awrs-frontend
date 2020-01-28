/*
 * Copyright 2020 HM Revenue & Customs
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

package utils

class SessionSectionHashUtilTest extends AwrsUnitTestTraits {

  val encodeInt = (flags: Int) => flags.toHexString

  "toHash" should {
    "calculate the correct hash for the session based on the sections completed" in {
      SessionSectionHashUtil.toHash(Seq(false, false, false)) shouldBe encodeInt(0)
      SessionSectionHashUtil.toHash(Seq(true, false, false)) shouldBe encodeInt(1)
      SessionSectionHashUtil.toHash(Seq(true, true, false)) shouldBe encodeInt(3)
      SessionSectionHashUtil.toHash(Seq(false, false, true)) shouldBe encodeInt(4)
      SessionSectionHashUtil.toHash(Seq(true, false, true)) shouldBe encodeInt(5)
      SessionSectionHashUtil.toHash(Seq(true, true, true)) shouldBe encodeInt(7)
      SessionSectionHashUtil.toHash(Seq(false, true, true, true, true, true, true, true)) shouldBe encodeInt(0xfe)
      SessionSectionHashUtil.toHash(Seq(true, true, true, true, true, true, true, true)) shouldBe encodeInt(0xff)
    }
  }

  "isCompleted" should {
    "correctly identify if the flag is set in the hash" in {
      SessionSectionHashUtil.isCompleted(0, encodeInt(0)) shouldBe false // nothing is set
      SessionSectionHashUtil.isCompleted(0, encodeInt(1)) shouldBe true // only 0 is set
      SessionSectionHashUtil.isCompleted(1, encodeInt(2)) shouldBe true // only 1 is set
      SessionSectionHashUtil.isCompleted(1, encodeInt(3)) shouldBe true // only 0 and 1 are set
      SessionSectionHashUtil.isCompleted(1, encodeInt(4)) shouldBe false // only 2 is set
      SessionSectionHashUtil.isCompleted(2, encodeInt(4)) shouldBe true // only 2 is set
      SessionSectionHashUtil.isCompleted(8, encodeInt(0xff)) shouldBe false // 1-7 are set
      SessionSectionHashUtil.isCompleted(8, encodeInt(0x100)) shouldBe true // only 8 is set
    }
  }

}
