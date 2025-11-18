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

package repositories

import config.ApplicationConfig
import play.api.libs.json.{Format, Reads, Writes}
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey, MongoCacheRepository}
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ShortLivedCacheRepository @Inject() (
    mongoComponent: MongoComponent,
    timestampSupport: TimestampSupport
)(implicit
    ec: ExecutionContext,
    appConfig: ApplicationConfig
) {

  private val cacheRepo = new MongoCacheRepository[String](
    mongoComponent = mongoComponent,
    collectionName = "awrs-short-lived-cache",
    ttl = appConfig.mongoDbExpireAfterMinutes,
    timestampSupport = timestampSupport,
    cacheIdType = UTRCacheId
  )

  def fetchData4Later[T: Reads](utr: String, dataKey: DataKey[T])(implicit ec: ExecutionContext): Future[Option[T]] =
    cacheRepo.get[T](utr)(dataKey)

  def saveData4Later[T: Writes](utr: String, dataKey: DataKey[T], data: T)(implicit ec: ExecutionContext): Future[Option[T]] =
    cacheRepo.put[T](utr)(dataKey, data).map(_ => Some(data))

  def fetchAll(utr: String)(implicit ec: ExecutionContext): Future[Option[CacheItem]] =
    cacheRepo.findById(utr)

  def removeAll(utr: String)(implicit ec: ExecutionContext): Future[Unit] =
    cacheRepo.deleteEntity(utr)
}
