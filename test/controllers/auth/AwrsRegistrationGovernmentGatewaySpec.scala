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

package controllers.auth

import org.scalatestplus.play.OneServerPerSuite
import uk.gov.hmrc.play.test.UnitSpec

class AwrsRegistrationGovernmentGatewaySpec extends UnitSpec with OneServerPerSuite{
  "AwrsGovernmentGateway" should {

    "have login value overridden" in {
      AwrsRegistrationGovernmentGateway.loginURL shouldBe ExternalUrls.loginURL
    }

    "have continue value overridden" in {
      AwrsRegistrationGovernmentGateway.continueURL shouldBe ExternalUrls.loginCallback
    }

    "have account value overridden" in {
      AwrsRegistrationGovernmentGateway.additionalLoginParameters shouldBe Map("accountType" -> Seq(ExternalUrls.accountType))
    }
  }
}