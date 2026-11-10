/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.helpers.application

import play.api.inject.BindingKey
import play.api.libs.json._
import repositories.CacheRepository
import uk.gov.hmrc.helpers.IntegrationSpec
import uk.gov.hmrc.mongo.cache.DataKey

trait S4LStub extends IntegrationSpec {

  lazy val shortLivedCacheRepository: CacheRepository =
    app.injector.instanceOf[CacheRepository](BindingKey(classOf[CacheRepository]).qualifiedWith("awrs"))

  lazy val apiShortLivedCacheRepository: CacheRepository =
    app.injector.instanceOf[CacheRepository](BindingKey(classOf[CacheRepository]).qualifiedWith("awrs-api"))

  def stubS4LGet(id: String, key: String = "", data: Option[JsObject] = None, scenarioState: Option[(String, String, String)] = None, api: Boolean = false): Unit = {
    data.foreach { d =>
      if (api) {
        await(apiShortLivedCacheRepository.put[JsObject](id, DataKey[JsObject](key), d))
      } else {
        await(shortLivedCacheRepository.put[JsObject](id, DataKey[JsObject](key), d))
      }
    }
  }

  def stubS4LPut(id: String, key: String, data: JsObject, api: Boolean = false): Unit = {
    if (api) {
      await(apiShortLivedCacheRepository.put[JsObject](id, DataKey[JsObject](key), data))
    } else {
      await(shortLivedCacheRepository.put[JsObject](id, DataKey[JsObject](key), data))
    }
  }
}
