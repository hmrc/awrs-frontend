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

package services.mocks

import models.{FormBundleStatus, StatusContactType, StatusInfoType}
import services.{StatusManagementService, StatusReturnType}
import services.apis.mocks.{MockAwrsAPI11, MockAwrsAPI12Cache, MockAwrsAPI9}
import utils.AwrsUnitTestTraits
import scala.language.implicitConversions

trait MockTestStatusManagementService extends AwrsUnitTestTraits
  with MockAwrsAPI9
  with MockAwrsAPI11
  with MockAwrsAPI12Cache
  with MockKeyStoreService {

  val testStatusManagementService = new StatusManagementService(testAPI9, testAPI11, testAPI12Cache, testKeyStoreService, mockAuditable)

  case class MockStatusManagementServiceConfiguration(api9: CacheConfigurationLocation = NotCachedLocally,
                                                      api11: CacheConfigurationLocation = NotCachedLocally,
                                                      api12Cache: CacheConfigurationLocation = NotCachedLocally,
                                                      statusPageViewed: Boolean = MockTestStatusManagementService.defaultViewedStatusConnector)

  def setupMockTestStatusManagementService(status: FormBundleStatus,
                                           notification: Option[StatusContactType],
                                           statusInfo: MockConfiguration[Option[StatusInfoType]] = DoNotConfigure, // here the DoNotConfigure will use default configuration instead
                                           configuration: MockStatusManagementServiceConfiguration = MockStatusManagementServiceConfiguration()): Unit = {

    implicit def optionToMockConfig[T](someValue: Option[T]): MockConfiguration[T] =
      someValue match {
        case Some(v) => Configure(v)
        case None => DoNotConfigure
      }

    def setLocalData[T](dependent: CacheConfigurationLocation)(data: Option[T]): Option[T] = dependent match {
      case CachedLocally => data
      case _ => None
    }
    def setNoneLocalData[T](dependent: CacheConfigurationLocation)(data: Option[T]): Option[T] = dependent match {
      case CachedLocally => None
      case _ => data
    }

    lazy val api9Data = MockAwrsAPI9.defaultSubscriptionStatusType(status)
    lazy val api11Data =
      statusInfo match {
        case Configure(info) => info
        case _ => MockAwrsAPI11.defaultStatusInfoType(status, notification)
      }
    lazy val api12CacheData = notification

    setupMockAwrsAPI9(
      keyStore = setLocalData(configuration.api9)(api9Data),
      connector = setNoneLocalData(configuration.api9)(api9Data)
    )
    setupMockAwrsAPI11(
      keyStore = setLocalData(configuration.api11)(api11Data),
      connector = setNoneLocalData(configuration.api11)(api11Data)
    )
    setupMockAwrsAPI12Cache(
      notificationKeyStore = setLocalData(configuration.api12Cache)(api12CacheData),
      notificationConnector = setNoneLocalData(configuration.api12Cache)(api12CacheData),
      viewedStatusConnector = configuration.statusPageViewed,
      markViewedConnector = MockTestStatusManagementService.defaultMarkViewedConnector
    )

  }

}

object MockTestStatusManagementService {

  val defaultViewedStatusConnector = false

  val defaultMarkViewedConnector = true

  val expectedRetrieveStatusOutput: (FormBundleStatus, Option[StatusContactType]) => StatusReturnType = (status: FormBundleStatus, notification: Option[StatusContactType]) =>
    StatusReturnType(
      wasViewed = defaultViewedStatusConnector,
      MockAwrsAPI9.defaultSubscriptionStatusType(Some(status)),
      MockAwrsAPI12Cache.defaultNotification(notification),
      MockAwrsAPI11.defaultStatusInfoType(Some(status), notification))

}
