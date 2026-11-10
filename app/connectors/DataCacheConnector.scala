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
import repositories.{CacheRepository, SessionCacheId}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.DataKey

import javax.inject.{Inject, Named}
import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}


class AwrsDataCacheConnector @Inject()(@Named("awrs") override val cacheRepository: CacheRepository)
  extends Save4LaterConnector

class AwrsApiDataCacheConnector @Inject()(@Named("awrs-api") override val cacheRepository: CacheRepository)
  extends Save4LaterConnector

class AwrsKeyStoreConnector @Inject()(@Named("session") override val cacheRepository: CacheRepository)
  extends KeyStoreConnector

trait KeyStoreConnector {

  val cacheRepository: CacheRepository

  @inline def fetchDataFromKeystore[T](key: String)(implicit hc: HeaderCarrier, formats: json.Format[T], @unused ec: ExecutionContext): Future[Option[T]] =
    cacheRepository.get[T](SessionCacheId.from(hc), DataKey[T](key))

  @inline def saveDataToKeystore[T](formID: String, data: T)(implicit hc: HeaderCarrier, formats: json.Format[T], ec: ExecutionContext): Future[CacheMap] =
    cacheRepository.put[T](SessionCacheId.from(hc), DataKey[T](formID), data).map { _ =>
      CacheMap(SessionCacheId.from(hc), Map(formID -> json.Json.toJson(data)))
    }

  @inline def removeAll()(implicit hc: HeaderCarrier, @unused ec: ExecutionContext): Future[Unit] =
    cacheRepository.deleteById(SessionCacheId.from(hc))
}

trait Save4LaterConnector {

  val cacheRepository: CacheRepository

  @inline def fetchData4Later[T](utr: String, formId: String)(implicit @unused hc: HeaderCarrier, formats: json.Format[T], @unused ec: ExecutionContext): Future[Option[T]] =
    cacheRepository.get[T](utr, DataKey[T](formId))

  @inline def saveData4Later[T](utr: String, formId: String, data: T)(implicit @unused hc: HeaderCarrier, formats: json.Format[T], ec: ExecutionContext): Future[Option[T]] =
    cacheRepository.put[T](utr, DataKey[T](formId), data).map(Some(_))

  @inline def fetchAll(utr: String)(implicit @unused hc: HeaderCarrier, ec: ExecutionContext): Future[Option[CacheMap]] =
    cacheRepository.findById(utr).map(_.map(CacheMap.fromCacheItem))

  @inline def removeAll(cacheId: String)(implicit @unused hc: HeaderCarrier, @unused ec: ExecutionContext): Future[Unit] =
    cacheRepository.deleteById(cacheId)
}
