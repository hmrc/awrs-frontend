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

package utils

import models.{WithdrawalReason, WithdrawalResponse}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.cache.client.CacheMap
import services.DataCacheKeys._

object WithdrawalTestUtils {
  val withdrawalJsonToSend: JsValue = Json.toJson(withdrawalReason())
  val withdrawResponseJson: JsValue = Json.toJson(witdrawalResponse())


  def withdrawalReason(reason: Option[String] = Some("Applied in error"), reasonOther: Option[String] = None): WithdrawalReason = WithdrawalReason(reason, None)

  def witdrawalResponse(processingDate: String = "2001-12-17T09:30:47Z"): WithdrawalResponse = WithdrawalResponse(processingDate)

  val returnedWithdrawalKeystoreCacheMap: CacheMap = CacheMap("data", Map(withdrawalReasonName -> withdrawalJsonToSend))
  val api8Repsonse = WithdrawalResponse("2001-12-17T09:30:47Z")
}
