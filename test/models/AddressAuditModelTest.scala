/*
 * Copyright 2023 HM Revenue & Customs
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

import org.scalatestplus.mockito.MockitoSugar
import services.helper.AddressComparator
import org.scalatestplus.play.PlaySpec
import utils.AwrsTestJson._

class AddressAuditModelTest extends PlaySpec with MockitoSugar {
  "AddressAudits" must {
    "be built correctly from a relevant posted http body" in {
      val model = auditAddressJson.as[AddressAudits]
      withClue(s"added uprn to the json and model and\nList(\n${model.addressAudits.mkString("\n")}\n)\n") {
        model.addressAudits.size mustBe 4
      }
    }
  }

  "Address comparator" must {
    "return true if two addresses are different" in {
      val model = auditAddressJson.as[AddressAudits]
      val toAddress = model.addressAudits(2).toAddress
      val fromAddress = model.addressAudits(2).fromAddress

      withClue(s"comparing to and from address\n$toAddress\n$fromAddress\n") {
        AddressComparator.isDifferent(toAddress, fromAddress) mustBe true
      }
    }

    "return false if two addresses are the same" in {
      val model = auditAddressJson.as[AddressAudits]
      val toAddress = model.addressAudits(1).toAddress
      val fromAddress = model.addressAudits(1).fromAddress

      withClue(s"comparing to and from address\n$toAddress\n$fromAddress\n") {
        AddressComparator.isDifferent(toAddress, fromAddress) mustBe false
      }
    }
  }
}
