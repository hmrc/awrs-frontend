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

import models.CacheMap
import play.api.libs.json
import repositories.{APIShortLivedCacheRepository, SessionCacheRepository, ShortLivedCacheRepository}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class AwrsDataCacheConnector @Inject()(repository: ShortLivedCacheRepository) extends Save4LaterConnector {
  override val shortLivedCacheRepository: ShortLivedCacheRepository = repository
}

class AwrsAPIDataCacheConnector @Inject()(repository: APIShortLivedCacheRepository) extends Save4LaterConnector {
  override val apiShortLivedCacheRepository: APIShortLivedCacheRepository = repository

  override def fetchData4Later[T](utr: String, formId: String)(implicit hc: HeaderCarrier, formats: json.Format[T], ec: ExecutionContext): Future[Option[T]] =
    apiShortLivedCacheRepository.fetchData4Later[T](utr, DataKey[T](formId))

  override def saveData4Later[T](utr: String, formId: String, data: T)(implicit hc: HeaderCarrier, formats: json.Format[T], ec: ExecutionContext): Future[Option[T]] =
    apiShortLivedCacheRepository.saveData4Later[T](utr, DataKey[T](formId), data)

  override def fetchAll(utr: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[CacheMap]] =
    apiShortLivedCacheRepository.fetchAll(utr).map(_.map(cacheItemToCacheMap))

  override def removeAll(cacheId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    apiShortLivedCacheRepository.removeAll(cacheId)
}

class AwrsKeyStoreConnector @Inject()(repository: SessionCacheRepository) extends KeyStoreConnector {
  override val sessionCacheRepository: SessionCacheRepository = repository
}

class BusinessCustomerDataCacheConnector @Inject()(repository: SessionCacheRepository) extends KeyStoreConnector {
  override val sessionCacheRepository: SessionCacheRepository = repository
}

trait KeyStoreConnector {

  val sessionCacheRepository: SessionCacheRepository

  @inline def fetchDataFromKeystore[T](key: String)(implicit hc: HeaderCarrier, formats: json.Format[T], ec: ExecutionContext): Future[Option[T]] =
    sessionCacheRepository.getFromSession[T](DataKey[T](key))

  @inline def saveDataToKeystore[T](formID: String, data: T)(implicit hc: HeaderCarrier, formats: json.Format[T], ec: ExecutionContext): Future[CacheMap] =
    sessionCacheRepository.putSession[T](DataKey[T](formID), data).map { _ =>
      // Return a minimal CacheMap for compatibility
      CacheMap(hc.sessionId.map(_.value).getOrElse(""), Map(formID -> json.Json.toJson(data)))
    }

  @inline def removeAll()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    sessionCacheRepository.deleteFromSession

  protected def cacheItemToCacheMap(cacheItem: CacheItem): CacheMap = {
    // Convert JsObject to Map[String, JsValue]
    val dataMap = cacheItem.data.value.toMap
    CacheMap(cacheItem.id, dataMap)
  }
}

trait Save4LaterConnector {

  def shortLivedCacheRepository: ShortLivedCacheRepository = throw new NotImplementedError("shortLivedCacheRepository not implemented")
  def apiShortLivedCacheRepository: APIShortLivedCacheRepository = throw new NotImplementedError("apiShortLivedCacheRepository not implemented")

  @inline def fetchData4Later[T](utr: String, formId: String)(implicit hc: HeaderCarrier, formats: json.Format[T], ec: ExecutionContext): Future[Option[T]] =
    shortLivedCacheRepository.fetchData4Later[T](utr, DataKey[T](formId))

  @inline def saveData4Later[T](utr: String, formId: String, data: T)(implicit hc: HeaderCarrier, formats: json.Format[T], ec: ExecutionContext): Future[Option[T]] =
    shortLivedCacheRepository.saveData4Later[T](utr, DataKey[T](formId), data)

  @inline def fetchAll(utr: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[CacheMap]] =
    shortLivedCacheRepository.fetchAll(utr).map(_.map(cacheItemToCacheMap))

  @inline def removeAll(cacheId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    shortLivedCacheRepository.removeAll(cacheId)

  protected def cacheItemToCacheMap(cacheItem: CacheItem): CacheMap = {
    // Convert JsObject to Map[String, JsValue]
    val dataMap = cacheItem.data.value.toMap
    CacheMap(cacheItem.id, dataMap)
  }
}
