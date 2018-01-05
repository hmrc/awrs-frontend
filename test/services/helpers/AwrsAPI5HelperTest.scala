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

package services.helpers

import forms.AWRSEnums.BooleanRadioEnum
import models.{Address, BCAddress, BusinessCustomerDetails}
import services.helper.AwrsAPI5Helper
import utils.{AwrsUnitTestTraits, TestUtil}


class AwrsAPI5HelperTest extends
  AwrsUnitTestTraits {

  "AwrsAPI5Helper" should {

    "convertAddressToBCAddress" in {
      val addl1 = "address Line1"
      val addl2 = "address Line2"
      val addl3 = "address Line3"
      val addl4 = "address Line4"
      val pc = "NE28 8ER"

      def testData(countryCode: Option[String]) = {
        // "" is the country field which does not have a mapping in BC Address
        val add = Address(addl1, addl2, Some(addl3), Some(addl4), Some(pc), "", countryCode)
        AwrsAPI5Helper.convertAddressToBCAddress(add)
      }

      def expected(country: Option[String]) = BCAddress(addl1, addl2, Some(addl3), Some(addl4), Some(pc), country)

      val uk = None
      val expectedUk = Some("GB")
      val noneUK = Some("FR")
      val expectedNoneUK = Some("FR")

      testData(uk) shouldBe expected(expectedUk)
      testData(noneUK) shouldBe expected(expectedNoneUK)
    }

    "isGroup" in {
      val isGroup = List("LLP_GRP", "LTD_GRP")
      val notGroup = List("SOP", "Partnership", "LTD", "LLP", "LP")
      isGroup.foreach(x => AwrsAPI5Helper.isGroup(TestUtil.dynamicLegalEntity(x)) shouldBe true)
      notGroup.foreach(x => AwrsAPI5Helper.isGroup(TestUtil.dynamicLegalEntity(x)) shouldBe false)
    }

    "convertToBusinessCustomerDetails" in {
      val subscriptionTypeFrontEnd = TestUtil.defaultTestSubscriptionTypeFrontEnd
      val expectedBC = BusinessCustomerDetails(
        businessName = subscriptionTypeFrontEnd.businessPartnerName.fold("")(x => x),
        businessType = None,
        businessAddress = AwrsAPI5Helper.convertAddressToBCAddress(subscriptionTypeFrontEnd.additionalPremises.get.premises.head.additionalAddress.get),
        sapNumber = "",
        safeId = "",
        isAGroup = AwrsAPI5Helper.isGroup(subscriptionTypeFrontEnd.legalEntity.get),
        agentReferenceNumber = None,
        firstName = None,
        lastName = None
      )
      val convertedBC = AwrsAPI5Helper.convertToBusinessCustomerDetails(subscriptionTypeFrontEnd)

      // n.b. we do not care about any other fields in BC for the post submission journeys
      convertedBC.businessName shouldBe expectedBC.businessName
      convertedBC.businessAddress shouldBe expectedBC.businessAddress
      convertedBC.isAGroup shouldBe expectedBC.isAGroup
    }
  }
}
