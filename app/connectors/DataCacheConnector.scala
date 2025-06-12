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

package connectors

import caching.{CacheMap, ShortLivedCache}
import config.{AwrsAPIShortLivedCache, AwrsSessionCache, AwrsShortLivedCache, BusinessCustomerSessionCache}
import play.api.libs.json
import uk.gov.hmrc.mongo.cache.SessionCacheRepository
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import caching.CachingImplicits._
import play.api.libs.json.Format
import play.api.mvc.RequestHeader

class AwrsDataCacheConnector @Inject()(awrsShortLivedCache: AwrsShortLivedCache) extends Save4LaterConnector {
  override val shortLivedCache: ShortLivedCache = awrsShortLivedCache
}

class AwrsAPIDataCacheConnector @Inject()(awrsAPIShortLivedCache: AwrsAPIShortLivedCache) extends Save4LaterConnector {
  override val shortLivedCache: ShortLivedCache = awrsAPIShortLivedCache
}

class AwrsKeyStoreConnector @Inject()(awrsSessionCache: AwrsSessionCache) extends KeyStoreConnector {
  override val sessionCache: SessionCacheRepository = awrsSessionCache
}

class BusinessCustomerDataCacheConnector @Inject()(businessCustomerSessionCache: BusinessCustomerSessionCache) extends KeyStoreConnector {
  override val sessionCache: SessionCacheRepository = businessCustomerSessionCache
}

trait KeyStoreConnector {

  val sessionCache: SessionCacheRepository

  @inline def fetchDataFromKeystore[T](key: String)(implicit requestHeader: RequestHeader, formats: json.Format[T]): Future[Option[T]] =
    sessionCache.getFromSession[T](key)

  @inline def saveDataToKeystore[T](formID: String, data: T)(implicit requestHeader: RequestHeader, formats: Format[T]): Future[(String, String)] =
    sessionCache.putSession(formID, data)


  @inline def removeAll()(implicit requestHeader: RequestHeader): Future[Unit] =
    sessionCache.cacheRepo.deleteEntity(requestHeader)
}

trait Save4LaterConnector {

  val shortLivedCache: ShortLivedCache

  @inline def fetchData4Later[T](utr: String, formId: String)(implicit formats: json.Format[T], ec: ExecutionContext): Future[Option[T]] =
    shortLivedCache.fetchAndGetEntry[T](utr, formId)

  @inline def saveData4Later[T](utr: String, formId: String, data: T)(implicit formats: json.Format[T], ec: ExecutionContext): Future[Option[T]] =
    shortLivedCache.cache(utr, formId, data) flatMap { data =>
      Future.successful(data.getEntry[T](formId))
    }

  @inline def fetchAll(utr: String)(implicit ec: ExecutionContext): Future[Option[CacheMap]] =
    shortLivedCache.fetch(utr)

  @inline def removeAll(cacheId: String): Future[Unit] =
    shortLivedCache.remove(cacheId)
}
