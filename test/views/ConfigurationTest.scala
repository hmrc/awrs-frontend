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

package views

import java.util.UUID

import play.api.test.FakeRequest
import utils.AwrsUnitTestTraits


class ConfigurationTest extends AwrsUnitTestTraits {

  lazy val userId = s"user-${UUID.randomUUID}"

  "showHint1" must {
    "error is there is no session status" in {
      intercept[RuntimeException](Configuration.showHint1(FakeRequest()))
    }

    "return false for certain session types" in {
      val types = List("Approved", "Approved with Conditions", "Revoked", "Revoked under Review/Appeal")
      types map { sessionType =>
        val fakeRequestWithSession = FakeRequest().withSession(
          "status" -> s"$sessionType"
        )

        Configuration.showHint1(fakeRequestWithSession) mustBe false
      }
    }

    "return true for other session types" in {
      val sessionType = "Pending"

      val fakeRequestWithSession = FakeRequest().withSession(
        "status" -> s"$sessionType"
      )

      Configuration.showHint1(fakeRequestWithSession) mustBe true
    }
  }


}
