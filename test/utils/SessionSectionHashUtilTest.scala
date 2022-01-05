/*
 * Copyright 2022 HM Revenue & Customs
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

  "toHash" must {
    "calculate the correct hash for the session based on the sections completed" in {
      SessionSectionHashUtil.toHash(Seq(false, false, false)) mustBe encodeInt(0)
      SessionSectionHashUtil.toHash(Seq(true, false, false)) mustBe encodeInt(1)
      SessionSectionHashUtil.toHash(Seq(true, true, false)) mustBe encodeInt(3)
      SessionSectionHashUtil.toHash(Seq(false, false, true)) mustBe encodeInt(4)
      SessionSectionHashUtil.toHash(Seq(true, false, true)) mustBe encodeInt(5)
      SessionSectionHashUtil.toHash(Seq(true, true, true)) mustBe encodeInt(7)
      SessionSectionHashUtil.toHash(Seq(false, true, true, true, true, true, true, true)) mustBe encodeInt(0xfe)
      SessionSectionHashUtil.toHash(Seq(true, true, true, true, true, true, true, true)) mustBe encodeInt(0xff)
    }
  }

  "isCompleted" must {
    "correctly identify if the flag is set in the hash" in {
      SessionSectionHashUtil.isCompleted(0, encodeInt(0)) mustBe false // nothing is set
      SessionSectionHashUtil.isCompleted(0, encodeInt(1)) mustBe true // only 0 is set
      SessionSectionHashUtil.isCompleted(1, encodeInt(2)) mustBe true // only 1 is set
      SessionSectionHashUtil.isCompleted(1, encodeInt(3)) mustBe true // only 0 and 1 are set
      SessionSectionHashUtil.isCompleted(1, encodeInt(4)) mustBe false // only 2 is set
      SessionSectionHashUtil.isCompleted(2, encodeInt(4)) mustBe true // only 2 is set
      SessionSectionHashUtil.isCompleted(8, encodeInt(0xff)) mustBe false // 1-7 are set
      SessionSectionHashUtil.isCompleted(8, encodeInt(0x100)) mustBe true // only 8 is set
    }
  }

}
