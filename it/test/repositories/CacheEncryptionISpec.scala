/*
 * Copyright 2026 HM Revenue & Customs
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

package repositories

import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.Filters
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import uk.gov.hmrc.helpers.IntegrationSpec
import uk.gov.hmrc.helpers.application.S4LStub
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.cache.DataKey

class CacheEncryptionISpec extends IntegrationSpec with S4LStub with Matchers with BeforeAndAfterEach {

  private lazy val mongoComponent: MongoComponent = app.injector.instanceOf[MongoComponent]

  private val utr = "5810451"
  private val cacheKey = "businessContacts"
  private val data = Json.obj("email" -> "test@example.com", "telephone" -> "01234567890")

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(shortLivedCacheRepository.deleteById(utr))
    await(apiShortLivedCacheRepository.deleteById(utr))
    clearSessionCache()
  }

  private def rawStoredValue(collectionName: String, id: String, dataKey: String): JsValue = {
    val document = await(mongoComponent.database.getCollection(collectionName).find(Filters.equal("_id", id)).first().toFuture())
    (Json.parse(document.toJson()) \ "data" \ dataKey).as[JsValue]
  }

  private def assertEncrypted(stored: JsValue): Unit = {
    stored mustBe a[JsString]
    stored.toString must not include "test@example.com"
  }

  "CacheRepository (awrs and awrs-api)" must {
    "store the main store data encrypted and read it back decrypted" in {
      await(shortLivedCacheRepository.put[JsObject](utr, DataKey[JsObject](cacheKey), data))

      assertEncrypted(rawStoredValue("awrs-short-lived-cache", utr, cacheKey))
      await(shortLivedCacheRepository.get[JsObject](utr, DataKey[JsObject](cacheKey))) mustBe Some(data)
      await(shortLivedCacheRepository.findById(utr)).map(_.data \ cacheKey).flatMap(_.toOption) mustBe Some(data)
    }

    "store the api store data encrypted and read it back decrypted" in {
      await(apiShortLivedCacheRepository.put[JsObject](utr, DataKey[JsObject](cacheKey), data))

      assertEncrypted(rawStoredValue("awrs-api-short-lived-cache", utr, cacheKey))
      await(apiShortLivedCacheRepository.get[JsObject](utr, DataKey[JsObject](cacheKey))) mustBe Some(data)
    }
  }

  "CacheRepository (session)" must {
    "store session data encrypted and read it back decrypted" in {
      val sessionId = SessionCacheId.from(cacheHeaderCarrier)
      await(sessionCacheRepository.put[JsObject](sessionId, DataKey[JsObject](cacheKey), data))

      assertEncrypted(rawStoredValue("sessions", sessionId, cacheKey))
      await(sessionCacheRepository.get[JsObject](sessionId, DataKey[JsObject](cacheKey))) mustBe Some(data)
    }
  }
}
