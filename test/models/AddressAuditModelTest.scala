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

import org.scalatest.mockito.MockitoSugar
import services.helper.AddressComparator
import uk.gov.hmrc.play.test.UnitSpec
import utils.AwrsTestJson._

class AddressAuditModelTest extends UnitSpec with MockitoSugar {
  "AddressAudits" should {
    "be built correctly from a relevant posted http body" in {
      val model = auditAddressJson.as[AddressAudits]
      withClue(s"added uprn to the json and model and\nList(\n${model.addressAudits.mkString("\n")}\n)\n") {
        model.addressAudits.size shouldBe 4
      }
    }
  }

  "Address comparator" should {
    "return true if two addresses are different" in {
      val model = auditAddressJson.as[AddressAudits]
      val toAddress = model.addressAudits(2).toAddress
      val fromAddress = model.addressAudits(2).fromAddress

      withClue(s"comparing to and from address\n$toAddress\n$fromAddress\n") {
        AddressComparator.isDifferent(toAddress, fromAddress) shouldBe true
      }
    }

    "return false if two addresses are the same" in {
      val model = auditAddressJson.as[AddressAudits]
      val toAddress = model.addressAudits(1).toAddress
      val fromAddress = model.addressAudits(1).fromAddress

      withClue(s"comparing to and from address\n$toAddress\n$fromAddress\n") {
        AddressComparator.isDifferent(toAddress, fromAddress) shouldBe false
      }
    }
  }
}
