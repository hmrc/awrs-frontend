/*
 * Copyright 2022 HM Revenue & Customs
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

package services.apis

import java.util.UUID

import connectors.mock.MockAuthConnector
import models.FormBundleStatus.{Approved, Pending, Revoked}
import models.StatusContactType.{MindedToReject, MindedToRevoke, NoLongerMindedToRevoke}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import services.apis.mocks.MockAwrsAPI12Cache
import uk.gov.hmrc.auth.core.retrieve.{LegacyCredentials, SimpleRetrieval}
import utils.TestConstants._
import utils.{AwrsUnitTestTraits, TestUtil}

import scala.concurrent.ExecutionContext.Implicits.global

class AwrsAPI12CacheTest extends AwrsUnitTestTraits with MockAwrsAPI12Cache with MockAuthConnector {

  implicit lazy val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest().withSession(
      "sessionId" -> sessionId,
      SimpleRetrieval("token", LegacyCredentials.reads).toString -> "RANDOMTOKEN",
      "userId"-> userId,
      "businessType" -> "SOP",
      "businessName" -> testTradingName
    )
  }

  "AwrsAPI12Cache.getNotificationCache" must {
    "do not call api12 cache if data is already in the keystore" in {
      val notification = MindedToReject
      setupMockAwrsAPI12Cache(
        notificationKeyStore = notification,
        notificationConnector = None
      )

      await(testAPI12Cache.getNotificationCache(Pending, TestUtil.defaultAuthRetrieval)) mustBe MockAwrsAPI12Cache.defaultNotification(notification)

      verifyAWRSNotificationConnector(fetchNotificationCache = 0)
      verifyKeyStoreService(
        fetchStatusNotification = 1,
        saveStatusNotification = 0)
    }

    "call api12 cache if data is not present in the keystore" in {
      val notification = MindedToReject

      setupMockAwrsAPI12Cache(
        notificationKeyStore = None,
        notificationConnector = notification
      )

      await(testAPI12Cache.getNotificationCache(Pending, TestUtil.defaultAuthRetrieval)) mustBe MockAwrsAPI12Cache.defaultNotification(notification)

      verifyAWRSNotificationConnector(fetchNotificationCache = 1)
      verifyKeyStoreService(
        fetchStatusNotification = 1,
        saveStatusNotification = 1)
    }

    "do not save to keystore if nothing was returned from api 12" in {
      setupMockAwrsAPI12Cache(
        notificationKeyStore = None,
        notificationConnector = None
      )

      await(testAPI12Cache.getNotificationCache(Pending, TestUtil.defaultAuthRetrieval)) mustBe None

      verifyAWRSNotificationConnector(fetchNotificationCache = 1)
      verifyKeyStoreService(
        fetchStatusNotification = 1,
        saveStatusNotification = 0)
    }

    "call delete notification from cache if notification is Minded to Reject and not a Pending status" in {
      val notification = MindedToReject

      setupMockAwrsAPI12Cache(
        notificationKeyStore = None,
        notificationConnector = notification
      )

      await(testAPI12Cache.getNotificationCache(Approved, TestUtil.defaultAuthRetrieval)) mustBe MockAwrsAPI12Cache.defaultNotification(notification)

      verifyAWRSNotificationConnector(fetchNotificationCache = 1, deleteNotificationFromCache = 1)
      verifyKeyStoreService(
        fetchStatusNotification = 1,
        saveStatusNotification = 0)
    }

    "call delete notification from cache if notification is Minded to Revoke and a Revoke status" in {
      val notification = MindedToRevoke

      setupMockAwrsAPI12Cache(
        notificationKeyStore = None,
        notificationConnector = notification
      )

      await(testAPI12Cache.getNotificationCache(Revoked, TestUtil.defaultAuthRetrieval)) mustBe MockAwrsAPI12Cache.defaultNotification(notification)

      verifyAWRSNotificationConnector(fetchNotificationCache = 1, deleteNotificationFromCache = 1)
      verifyKeyStoreService(
        fetchStatusNotification = 1,
        saveStatusNotification = 0)
    }

    "call delete notification from cache if notification is No Longer Minded to Revoke" in {
      val notification = NoLongerMindedToRevoke

      setupMockAwrsAPI12Cache(
        notificationKeyStore = None,
        notificationConnector = notification
      )

      await(testAPI12Cache.getNotificationCache(Approved, TestUtil.defaultAuthRetrieval)) mustBe MockAwrsAPI12Cache.defaultNotification(notification)

      verifyAWRSNotificationConnector(fetchNotificationCache = 1, deleteNotificationFromCache = 1)
      verifyKeyStoreService(
        fetchStatusNotification = 1,
        saveStatusNotification = 1)
    }
  }
}
