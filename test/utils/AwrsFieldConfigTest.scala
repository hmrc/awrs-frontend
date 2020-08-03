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

import org.scalatestplus.play.PlaySpec

class AwrsFieldConfigTest extends PlaySpec with AwrsFieldConfig {
  "AWRS Field Config" must {

    "populate max length for declaration name" in {
      applicationDeclarationNameLen mustBe 140
    }

    "populate max length for address postcode" in {
      addressPostcodeLen mustBe 20
    }

    "populate max length for declaration role" in {
      applicationDeclarationRoleLen mustBe 40
    }

    "populate max length for business details trading name" in {
      tradingNameLen mustBe 120
    }

    "populate max length for business details address line 1" in {
      addressLineLen mustBe 35
    }

    "populate max length for business details contact First Name" in {
      firstNameLen mustBe 35
    }

    "populate max length for business details contact Last Name" in {
      lastNameLen mustBe 35
    }

    "populate max length for business details contact Email" in {
      emailLen mustBe 100
    }

    "populate max length for business details contact Telephone" in {
      telephoneLen mustBe 24
    }
  }
}
