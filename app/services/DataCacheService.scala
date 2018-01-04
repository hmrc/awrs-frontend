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

package services

import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HeaderCarrier

trait DataCacheService {

  val keyStoreService: KeyStoreService
  val save4LaterService: Save4LaterService

  def backUpSave4LaterInKeyStore(implicit user: AuthContext, hc: HeaderCarrier) =
    keyStoreService.saveSave4LaterBackup(save4LaterConnector = save4LaterService.mainStoreSave4LaterConnector)

  def fetchMainStore(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[CacheMap]] =
    save4LaterService.mainStore.fetchAll flatMap {
      case None => keyStoreService.fetchSave4LaterBackup
      case found@Some(_) => Future.successful(found)
    }

}
