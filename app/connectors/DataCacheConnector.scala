/*
 * Copyright 2018 HM Revenue & Customs
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

import config.{AwrsAPIShortLivedCache, AwrsSessionCache, AwrsShortLivedCache, BusinessCustomerSessionCache}
import play.api.libs.json
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache, ShortLivedCache}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }


object AwrsDataCacheConnector extends Save4LaterConnector {
  override val shortLivedCache: ShortLivedCache = AwrsShortLivedCache
}

object AwrsAPIDataCacheConnector extends Save4LaterConnector {
  override val shortLivedCache: ShortLivedCache = AwrsAPIShortLivedCache
}

object AwrsKeyStoreConnector extends KeyStoreConnector {
  override val sessionCache: SessionCache = AwrsSessionCache
}

object BusinessCustomerDataCacheConnector extends KeyStoreConnector {
  override val sessionCache: SessionCache = BusinessCustomerSessionCache
}


trait KeyStoreConnector {

  val sessionCache: SessionCache

  @inline def fetchDataFromKeystore[T](key: String)(implicit hc: HeaderCarrier, formats: json.Format[T]): Future[Option[T]] =
    sessionCache.fetchAndGetEntry[T](key)

  @inline def saveDataToKeystore[T](formID: String, data: T)(implicit hc: HeaderCarrier, formats: json.Format[T]): Future[CacheMap] =
    sessionCache.cache(formID, data)

  @inline def removeAll()(implicit hc: HeaderCarrier): Future[HttpResponse] =
    sessionCache.remove()

}

trait Save4LaterConnector {

  val shortLivedCache: ShortLivedCache

  @inline def fetchData4Later[T](utr: String, formId: String)(implicit hc: HeaderCarrier, formats: json.Format[T]): Future[Option[T]] =
    shortLivedCache.fetchAndGetEntry[T](utr, formId)

  @inline def saveData4Later[T](utr: String, formId: String, data: T)(implicit hc: HeaderCarrier, formats: json.Format[T]): Future[Option[T]] =
    shortLivedCache.cache(utr, formId, data) flatMap {
      data => Future.successful(data.getEntry[T](formId))
    }

  @inline def fetchAll(utr: String)(implicit hc: HeaderCarrier): Future[Option[CacheMap]] =
    shortLivedCache.fetch(utr)

  @inline def removeAll(cacheId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    shortLivedCache.remove(cacheId)

}
