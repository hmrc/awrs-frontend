/*
 * Copyright 2025 HM Revenue & Customs
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

package caching

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.doReturn
import play.api.libs.json.Writes.StringWrites
import play.api.libs.json.{JsString, Json}
import uk.gov.hmrc.crypto.{Crypted, Decrypter, Encrypter, PlainBytes, PlainContent, PlainText}
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey, MongoCacheRepository}
import utils.AwrsUnitTestTraits

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ShortLivedCacheSpec extends AwrsUnitTestTraits {

  val mockMongoCacheRepository: MongoCacheRepository[String] = mock[MongoCacheRepository[String]]


  val quotedData = Json.stringify(Json.toJson("Data"))

  val testCacheItem = CacheItem(
    id = "TestCacheId",
    data = Json.obj(
      "FormId" -> JsString(quotedData)
    ),
    createdAt = Instant.now(),
    modifiedAt = Instant.now()
  )

  val shortLivedCache = new ShortLivedCache(mockMongoCacheRepository,
    "test-collection-name") {
    override implicit val crypto: Decrypter with Encrypter = TestCrypto
  }

  "ShortLivedCache" must {
    "save to cache correctly" in {
      doReturn(Future.successful(testCacheItem))
        .when(mockMongoCacheRepository)
        .put[String](ArgumentMatchers.eq("TestCacheId"))(eqTo(DataKey("FormId")), ArgumentMatchers.any)(ArgumentMatchers.any)
      val result: CacheMap = await(shortLivedCache.cache[String]("TestCacheId", "FormId", "Data"))


      result.getEntry[String]("FormId") mustBe Some("Data")
    }




  }


}

object TestCrypto extends Encrypter with Decrypter {

  override def encrypt(value: PlainContent): Crypted =
    value match {
      case PlainText(text) => Crypted(text)
      case _ => throw new RuntimeException(s"Unable to encrypt unknown message type: $value")
    }

  override def decrypt(crypted: Crypted): PlainText =
    PlainText(crypted.value)

  override def decryptAsBytes(crypted: Crypted): PlainBytes =
    PlainBytes(crypted.value.getBytes)
}