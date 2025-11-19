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

import models._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Format
import repositories.SessionCacheRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.DataKey
import utils.TestConstants._
import utils.TestUtil._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.concurrent.Await

class KeyStoreConnectorSpec extends AnyWordSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val format: Format[SubscriptionStatusType] = SubscriptionStatusType.formats

  val mockSessionCacheRepository: SessionCacheRepository = mock[SessionCacheRepository]

  val testKeyStoreConnector: KeyStoreConnector = new KeyStoreConnector() {
    override val sessionCacheRepository: SessionCacheRepository = mockSessionCacheRepository
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSessionCacheRepository)
  }

  def await[A](future: Future[A]): A = Await.result(future, 5.seconds)

  "KeyStoreConnector" must {
    "fetch saved data from keystore" in {
      when(mockSessionCacheRepository.getFromSession[SubscriptionStatusType](DataKey[SubscriptionStatusType](testUtr)))
        .thenReturn(Future.successful(Some(testSubscriptionStatusTypePendingGroup)))

      val result = testKeyStoreConnector.fetchDataFromKeystore[SubscriptionStatusType](testUtr)
      await(result) mustBe Some(testSubscriptionStatusTypePendingGroup)
    }

    "save data to keystore" in {
      when(mockSessionCacheRepository.putSession[SubscriptionStatusType](DataKey[SubscriptionStatusType](testUtr), testSubscriptionStatusTypePendingGroup))
        .thenReturn(Future.successful(testSubscriptionStatusTypePendingGroup))

      val result = testKeyStoreConnector.saveDataToKeystore[SubscriptionStatusType](testUtr, testSubscriptionStatusTypePendingGroup)
      val cacheMap = await(result)

      cacheMap.id mustBe "" // HeaderCarrier has no session ID in test
      cacheMap.data.keys must contain(testUtr)
      cacheMap.data(testUtr) mustBe play.api.libs.json.Json.toJson(testSubscriptionStatusTypePendingGroup)
    }

  }
}
