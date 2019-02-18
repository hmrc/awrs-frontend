/*
 * Copyright 2019 HM Revenue & Customs
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

package services.apis.mocks

import connectors.AWRSNotificationConnector
import connectors.mock.MockAWRSNotificationConnector
import models.StatusContactType._
import models.{StatusContactType, StatusNotification, ViewedStatusResponse}
import services.KeyStoreService
import services.apis.AwrsAPI12Cache
import services.mocks.MockKeyStoreService
import utils.{AwrsUnitTestTraits, TestUtil}


trait MockAwrsAPI12Cache extends AwrsUnitTestTraits with MockKeyStoreService with MockAWRSNotificationConnector {

  import MockAwrsAPI12Cache._

  object TestAPI12Cache extends AwrsAPI12Cache {
    override val awrsNotificationConnector: AWRSNotificationConnector = mockAWRSNotificationConnector
    override val keyStoreService: KeyStoreService = TestKeyStoreService
  }


  def setupMockAwrsAPI12Cache(notificationKeyStore: Option[StatusContactType],
                              notificationConnector: MockConfiguration[Option[StatusContactType]] = DoNotConfigure,
                              viewedStatusConnector: MockConfiguration[Boolean] = DoNotConfigure,
                              markViewedConnector: MockConfiguration[Boolean] = DoNotConfigure): Unit =
    setupMockAwrsAPI12CacheRaw(
      notificationKeyStore = defaultNotification(notificationKeyStore),
      notificationConnector = notificationConnector match {
        case Configure(config) => defaultNotification(config)
        case DoNotConfigure => DoNotConfigure
      },
      viewedStatusConnector = viewedStatusConnector match {
        case Configure(config) => ViewedStatusResponse(config)
        case DoNotConfigure => DoNotConfigure
      },
      markViewedConnector = markViewedConnector
    )

  def setupMockAwrsAPI12CacheRaw(notificationKeyStore: Option[StatusNotification],
                                 notificationConnector: MockConfiguration[Option[StatusNotification]],
                                 viewedStatusConnector: MockConfiguration[Option[ViewedStatusResponse]],
                                 markViewedConnector: MockConfiguration[Boolean]): Unit = {
    notificationConnector match {
      case Configure(config) => setupMockAWRSNotificationConnectorWithOnly(fetchNotificationCache = config)
      case _ =>
    }
    setupMockKeyStoreServiceWithOnly(statusNotification = notificationKeyStore)
    viewedStatusConnector match {
      case Configure(viewed) => setupMockAWRSNotificationConnectorWithOnly(getNotificationViewedStatus = viewed)
      case _ =>
    }
    markViewedConnector match {
      case Configure(mark) => setupMockAWRSNotificationConnectorWithOnly(markNotificationViewedStatusAsViewed = mark)
      case _ =>
    }
  }

}

object MockAwrsAPI12Cache {

  val defaultMindedToReject = TestUtil.testStatusNotificationMindedToReject
  val defaultMindedToRevoke = TestUtil.testStatusNotificationMindedToRevoke
  val defaultNoLongerMindedToRevoke = TestUtil.testStatusNotificationNoLongerMindedToRevoke

  def defaultNotification(notificationType: Option[StatusContactType]): Option[StatusNotification] =
    notificationType match {
      case Some(MindedToReject) => defaultMindedToReject
      case Some(MindedToRevoke) => defaultMindedToRevoke
      case Some(NoLongerMindedToRevoke) => defaultNoLongerMindedToRevoke
      case None => None
      case _ => throw new NoSuchElementException("unimplemented default notification type in MockAwrsAPI12Cache.defaultNotification")
    }


}
