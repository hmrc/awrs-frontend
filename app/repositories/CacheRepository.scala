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

import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.mongo.cache.{CacheItem, CacheIdType, DataKey, MongoCacheRepository}
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

class CacheRepository(
    collectionName: String,
    ttl: Duration,
    mongoComponent: MongoComponent,
    timestampSupport: TimestampSupport
)(implicit ec: ExecutionContext) {

  private val cacheRepo = new MongoCacheRepository[String](
    mongoComponent = mongoComponent,
    collectionName = collectionName,
    ttl = ttl,
    timestampSupport = timestampSupport,
    cacheIdType = CacheIdType.SimpleCacheId
  )

  def get[T: Reads](id: String, dataKey: DataKey[T]): Future[Option[T]] =
    cacheRepo.get[T](id)(dataKey)

  def put[T: Writes](id: String, dataKey: DataKey[T], data: T): Future[T] =
    cacheRepo.put[T](id)(dataKey, data).map(_ => data)

  def findById(id: String): Future[Option[CacheItem]] =
    cacheRepo.findById(id)

  def deleteById(id: String): Future[Unit] =
    cacheRepo.deleteEntity(id)
}
