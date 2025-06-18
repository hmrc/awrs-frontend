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

import org.bson.codecs.Codec
import org.mongodb.scala.model.IndexModel
import play.api.libs.json._
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, Sensitive}
import uk.gov.hmrc.mongo.cache.{CacheIdType, CacheItem, DataKey, MongoCacheRepository}
import uk.gov.hmrc.mongo.{MongoComponent, MongoDatabaseCollection, TimestampSupport}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

abstract class ShortLivedCache (
                                 cacheRepo: MongoCacheRepository[String],
                                 override val collectionName: String
                      )(implicit ec: ExecutionContext) extends MongoDatabaseCollection  {

  implicit val crypto: Decrypter with Encrypter

  def cache[A](
                cacheId: String,
                formId : String,
                body   : A
              )(implicit
                wts: Writes[A],
                ec : ExecutionContext
              ): Future[CacheMap] = {
    val sensitive        = SensitiveA(body)
    val encryptionFormat = JsonEncryption.sensitiveEncrypter[A, SensitiveA[A]]
    cacheRepo.put(cacheId)(DataKey(formId), sensitive)(encryptionFormat)
      .map((cm: CacheItem) => {
        new CryptoCacheMap(cm)
      })
  }


  def fetchAndGetEntry[A](
                           cacheId: String,
                           key    : String
                         )(implicit
                           rds: Reads[A],
                           ec : ExecutionContext
                         ): Future[Option[A]] =
    try {

      cacheRepo.findById(cacheId).map(_.flatMap{ci =>
        val decryptionFormat: Reads[SensitiveA[A]] = JsonEncryption.sensitiveDecrypter[A, SensitiveA[A]](SensitiveA.apply)
        val encryptedEntry: Option[JsResult[SensitiveA[A]]] = ci.data.value.get(key).map(_.validate(decryptionFormat))
        encryptedEntry.map(_.get.decryptedValue)
      })
    } catch {
      case e: SecurityException =>
        throw new RuntimeException(s"Failed to fetch a decrypted entry by cacheId:$cacheId and key:$key", e)
    }

  def remove(cacheId: String): Future[Unit] =
    cacheRepo.deleteEntity(cacheId)

  def fetch[A](cacheId: String)(implicit ec: ExecutionContext): Future[Option[CacheMap]] =
    cacheRepo.findById(cacheId).map(_.map(ci => CacheMap(ci.id, scala.collection.immutable.Map(ci.data.value.toSeq: _*))))

  override def indexes: Seq[IndexModel] = cacheRepo.indexes

  class CryptoCacheMap(cm: CacheItem)(implicit crypto: Decrypter with Encrypter)
    extends CacheMap(cm.id, scala.collection.immutable.Map(cm.data.value.toSeq: _*)) {

    override def getEntry[A](key: String)(implicit fjs: Reads[A]): Option[A] =
      try {
        val decryptionFormat: Reads[SensitiveA[A]] = JsonEncryption.sensitiveDecrypter[A, SensitiveA[A]](SensitiveA.apply)
        val encryptedEntry: Option[JsResult[SensitiveA[A]]] = data.get(key).map(_.validate(decryptionFormat))
        encryptedEntry.map(_.get.decryptedValue)
      } catch {
        case e: SecurityException => throw new RuntimeException(s"Failed to fetch a decrypted entry by key:$key", e)
      }
  }
}

