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

package utils

import utils.JsonUtil._

object AwrsTestJson extends AwrsTestJson

trait AwrsTestJson extends AwrsPathConstants {

  lazy val api4SOPJson = loadAndParseJsonWithDummyData(api4SOP)
  lazy val api4PartnerJson = loadAndParseJsonWithDummyData(api4Partner)

  lazy val api5LTDJson = loadAndParseJsonWithDummyData(api5LTD)
  lazy val api5SoleTraderJson = loadAndParseJsonWithDummyData(api5SoleTrader)
  lazy val api5PartnerJson = loadAndParseJsonWithDummyData(api5Partner)
  lazy val api5LLPJson = loadAndParseJsonWithDummyData(api5LLP)
  lazy val api5LLPGRPJson = loadAndParseJsonWithDummyData(api5LLPGRP)
  lazy val api5LTDGRPJson = loadAndParseJsonWithDummyData(api5LTDGRP)

  lazy val api6LTDJson = loadAndParseJsonWithDummyData(api6LTD)
  lazy val api3RequestJson = loadAndParseJsonWithDummyData(api3Json)

  lazy val auditAddressJson = loadAndParseJsonWithDummyData(auditAddress)

  lazy val matchSuccessResponseJson = loadAndParseJsonWithDummyData(matchSuccessResponse)
  lazy val matchFailureResponseJson = loadAndParseJsonWithDummyData(matchFailureResponse)

}
