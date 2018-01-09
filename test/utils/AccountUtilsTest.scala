/*
 * Copyright 2018 HM Revenue & Customs
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

import uk.gov.hmrc.play.test.UnitSpec
import utils.TestConstants._

class AccountUtilsTest extends UnitSpec {

  "getUtr" should {
    "Return the user SA utr " in {
      implicit val user = builders.AuthBuilder.createUserAuthContextIndSa("userId", "Geoff Fisher", testUtr)
      val utr = AccountUtils.getUtrOrName()
      utr shouldBe testUtr
    }

    "Return the user CT utr for org" in {
      implicit val auth = builders.AuthBuilder.createUserAuthContextIndCt("userId", "", testCTUtr)
      val utr = AccountUtils.getUtrOrName()
      utr shouldBe testCTUtr
    }

    "Return the org name " in {
      implicit val auth = builders.AuthBuilder.createUserAuthContextOrg("userId", "name", testOrg)
      val orgId = AccountUtils.getUtrOrName()
      orgId shouldBe testOrg
    }

    "throw exception if no details are found" in {
      implicit val auth = builders.AuthBuilder.createUserAuthFailure("", "")
      val thrown = the[RuntimeException] thrownBy AccountUtils.getUtrOrName()
      thrown.getMessage should include("No data found")
    }
  }

  "getAuthType " should {
    "return the correct auth link based on a sole trader legal entity type" in {
      implicit val auth = builders.AuthBuilder.createUserAuthContextIndSa("userId", "Geoff Fisher", testUtr)
      val result = AccountUtils.getAuthType("SOP")
      result shouldBe s"/sa/$testUtr"
    }
    "return the correct auth link based on a partnership legal entity type" in {
      implicit val auth = builders.AuthBuilder.createUserAuthContextOrg("userId", "Geoff Fisher", testOrg)
      val result = AccountUtils.getAuthType("Partnership")
      result shouldBe s"/org/$testOrg"
    }
    "return the correct auth link based on a limited company legal entity type" in {
      implicit val auth = builders.AuthBuilder.createUserAuthContextOrg("userId", "Geoff Fisher", testOrg)
      val result = AccountUtils.getAuthType("LTD")
      result shouldBe s"/org/$testOrg"
    }
  }
}
