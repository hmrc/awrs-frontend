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

package models

import play.api.libs.json.Json
import utils.TestUtil._
import utils.{AwrsFieldConfig, AwrsUnitTestTraits}

class FormModelsTest extends AwrsUnitTestTraits {

  "TradingActivity" should {

    "transform the broker selection into an other selection and be able to do the reverse" in {
      val tradingActivity = testTradingActivity(wholesalerType = List("05"), otherWholesaler = None)

      val jsonAfterBrokerSelected = Json.toJson(tradingActivity)
      jsonAfterBrokerSelected.\\("wholesalerType").toString() should include("99")
      jsonAfterBrokerSelected.\\("wholesalerType").toString() shouldNot include("05")
      jsonAfterBrokerSelected.\\("otherWholesaler").toString() should include("Broker")

      jsonAfterBrokerSelected.as[TradingActivity] shouldBe tradingActivity
    }

    "transform the producer selection into an other selection and be able to do the reverse" in {
      val tradingActivity = testTradingActivity(wholesalerType = List("04"), otherWholesaler = None)

      val jsonAfterBrokerSelected = Json.toJson(tradingActivity)
      jsonAfterBrokerSelected.\\("wholesalerType").toString() should include("99")
      jsonAfterBrokerSelected.\\("wholesalerType").toString() shouldNot include("04")
      jsonAfterBrokerSelected.\\("otherWholesaler").toString() should include("Producer")

      jsonAfterBrokerSelected.as[TradingActivity] shouldBe tradingActivity
    }

    "transform both a broker and producer selection into an other selection and be able to do the reverse" in {
      val tradingActivity = testTradingActivity(wholesalerType = List("04", "05"), otherWholesaler = None)

      val jsonAfterBrokerSelected = Json.toJson(tradingActivity)
      jsonAfterBrokerSelected.\\("wholesalerType").toString() should include("99")
      jsonAfterBrokerSelected.\\("wholesalerType").toString() shouldNot include("04")
      jsonAfterBrokerSelected.\\("wholesalerType").toString() shouldNot include("05")
      jsonAfterBrokerSelected.\\("otherWholesaler").toString() should include("Broker")
      jsonAfterBrokerSelected.\\("otherWholesaler").toString() should include("Producer")

      jsonAfterBrokerSelected.as[TradingActivity] shouldBe tradingActivity
    }

    "transform both a broker and producer selection into an other selection and remain unaffected by an existing other selection and be able to do the reverse" in {
      val tradingActivity = testTradingActivity(wholesalerType = List("04", "05", "99"), otherWholesaler = Some("Something else"))

      val jsonAfterBrokerSelected = Json.toJson(tradingActivity)
      jsonAfterBrokerSelected.\\("wholesalerType").toString() should include("99")
      jsonAfterBrokerSelected.\\("wholesalerType").toString() shouldNot include("04")
      jsonAfterBrokerSelected.\\("wholesalerType").toString() shouldNot include("05")
      jsonAfterBrokerSelected.\\("otherWholesaler").toString() should include("Broker")
      jsonAfterBrokerSelected.\\("otherWholesaler").toString() should include("Producer")
      jsonAfterBrokerSelected.\\("otherWholesaler").toString() should include("Something else")

      jsonAfterBrokerSelected.as[TradingActivity] shouldBe tradingActivity
    }

    "trim the fields to the correct length when producer or broker is selected" in {
      val maxLength = "a" * AwrsFieldConfig.otherWholesalerLen
      val notMaxLength = "a" * (AwrsFieldConfig.otherWholesalerLen - 1)

      val testCases = Seq(
        (List("04", "99"), maxLength, AwrsFieldConfig.otherWholesalerLen),
        (List("05", "99"), maxLength, AwrsFieldConfig.otherWholesalerLen),
        (List("04", "05", "99"), maxLength, AwrsFieldConfig.otherWholesalerLen),
        (List("99"), notMaxLength, notMaxLength.length)
      )

      for ((wholesalerType: List[String], otherWholesaler: String, expectedLength: Int) <- testCases) {
        withClue(s"test data:\nwholesalerType=$wholesalerType\notherWholesaler=$otherWholesaler\nexpectedLength=$expectedLength\n") {
          val tradingActivity = testTradingActivity(wholesalerType = wholesalerType, otherWholesaler = Some(otherWholesaler))
          val jsonAfterBrokerSelected = Json.toJson(tradingActivity)

          val otherWholesalerStr = jsonAfterBrokerSelected.\\("otherWholesaler").head.toString.replaceAll("\"","")
          withClue(s"otherWholesaler after trim: $otherWholesalerStr\nlength:${otherWholesalerStr.length}\n") {
            otherWholesalerStr.length shouldBe expectedLength
          }
        }
      }
    }

  }

}
