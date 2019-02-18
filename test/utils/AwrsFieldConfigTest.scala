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

package utils

import uk.gov.hmrc.play.test.UnitSpec

class AwrsFieldConfigTest extends UnitSpec with FakeAWRSPlayApplication with AwrsFieldConfig{
  "AWRS Field Config" should {

    "populate max length for declaration name" in {
      AwrsFieldConfig.applicationDeclarationNameLen shouldBe 140
    }

    "populate max length for address postcode" in {
      AwrsFieldConfig.addressPostcodeLen shouldBe 20
    }

    "populate max length for declaration role" in {
      AwrsFieldConfig.applicationDeclarationRoleLen shouldBe 40
    }

    "populate max length for business details trading name" in {
      AwrsFieldConfig.tradingNameLen shouldBe 120
    }

    "populate max length for business details address line 1" in {
      AwrsFieldConfig.addressLine1Len shouldBe 35
    }

    "populate max length for business details address line 2" in {
      AwrsFieldConfig.addressLine2Len shouldBe 35
    }

    "populate max length for business details address line 3" in {
      AwrsFieldConfig.addressLine3Len shouldBe 35
    }

    "populate max length for business details address line 4" in {
      AwrsFieldConfig.addressLine4Len shouldBe 35
    }

    "populate max length for business details contact First Name" in {
      AwrsFieldConfig.firstNameLen shouldBe 35
    }

    "populate max length for business details contact Last Name" in {
      AwrsFieldConfig.lastNameLen shouldBe 35
    }

    "populate max length for business details contact Email" in {
      AwrsFieldConfig.emailLen shouldBe 100
    }

    "populate max length for business details contact Telephone" in {
      AwrsFieldConfig.telephoneLen shouldBe 24
    }
  }
}
