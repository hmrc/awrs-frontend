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

package services

import connectors.AwrsDataCacheConnector
import controllers.auth.StandardAuthRetrievals
import uk.gov.hmrc.http.HeaderCarrier
import models.CacheMap
import utils.AccountUtils

import scala.concurrent.{ExecutionContext, Future}

trait DataCacheService {

  val accountUtils: AccountUtils
  val keyStoreService: KeyStoreService
  val save4LaterService: Save4LaterService
  val mainStoreSave4LaterConnector: AwrsDataCacheConnector

  def backUpSave4LaterInKeyStore(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] =
    keyStoreService.saveSave4LaterBackup(save4LaterConnector = mainStoreSave4LaterConnector, authRetrievals, accountUtils)

  def fetchMainStore(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[CacheMap]] =
    save4LaterService.mainStore.fetchAll(authRetrievals) flatMap {
      case None => keyStoreService.fetchSave4LaterBackup
      case found@Some(_) => Future.successful(found)
    }

}
