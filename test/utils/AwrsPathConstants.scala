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

object AwrsPathConstants extends AwrsPathConstants

trait AwrsPathConstants {

  lazy val api4SOP = "/api4/sop.json"
  lazy val api4Partner = "/api4/partner.json"

  lazy val api5LTD = "/api5/ltd.json"
  lazy val api5SoleTrader = "/api5/soleTrader.json"
  lazy val api5Partner = "/api5/partner.json"
  lazy val api5LLP = "/api5/llp.json"
  lazy val api5LLPGRP = "/api5/llpGrp.json"
  lazy val api5LTDGRP = "/api5/ltdGrp.json"

  lazy val api6LTD = "/api6/ltd.json"
  lazy val api3Json = "/api3/updateGrpPartnerRequest.json"

  lazy val auditAddress = "/audit/addressData.json"

  lazy val matchSuccessResponse = "/business_matching/matchSuccessResponse.json"
  lazy val matchFailureResponse = "/business_matching/matchFailureResponse.json"
}
