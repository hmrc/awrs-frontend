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

package connectors.mock

import connectors.AWRSNotificationConnector
import models.{StatusNotification, ViewedStatusResponse}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import utils.AwrsUnitTestTraits
import utils.TestUtil._

import scala.concurrent.Future


trait MockAWRSNotificationConnector extends AwrsUnitTestTraits {

  import MockAWRSNotificationConnector._

  // need to be lazy incase of overrides
  lazy val mockAWRSNotificationConnector: AWRSNotificationConnector = mock[AWRSNotificationConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAWRSNotificationConnector)
  }

  def setupMockAWRSNotificationConnector(fetchNotificationCache: Future[Option[StatusNotification]] = Future.successful(defaultStatusNotification),
                                         deleteNotificationFromCache: Future[Boolean] = Future.successful(true),
                                         getNotificationViewedStatus: Future[Option[ViewedStatusResponse]] = ViewedStatusResponse(true),
                                         markNotificationViewedStatusAsViewed: Future[Option[Boolean]] = true,
                                         sendConfirmationEmail: Future[Boolean] = Future.successful(true)
                                        ): Unit = {
    setupMockAWRSNotificationConnectorWithOnly(
      fetchNotificationCache = fetchNotificationCache,
      deleteNotificationFromCache = deleteNotificationFromCache,
      getNotificationViewedStatus = getNotificationViewedStatus,
      markNotificationViewedStatusAsViewed = markNotificationViewedStatusAsViewed,
      sendConfirmationEmail = sendConfirmationEmail
    )
  }

  def setupMockAWRSNotificationConnectorWithOnly(fetchNotificationCache: MockConfiguration[Future[Option[StatusNotification]]] = DoNotConfigure,
                                                 deleteNotificationFromCache: MockConfiguration[Future[Boolean]] = DoNotConfigure,
                                                 getNotificationViewedStatus: MockConfiguration[Future[Option[ViewedStatusResponse]]] = DoNotConfigure,
                                                 markNotificationViewedStatusAsViewed: MockConfiguration[Future[Option[Boolean]]] = DoNotConfigure,
                                                 sendConfirmationEmail: MockConfiguration[Future[Boolean]] = DoNotConfigure
                                                ): Unit = {
    fetchNotificationCache ifConfiguredThen (config => when(mockAWRSNotificationConnector.fetchNotificationCache(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(config))
    deleteNotificationFromCache ifConfiguredThen (config => when(mockAWRSNotificationConnector.deleteFromNotificationCache(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(config))
    getNotificationViewedStatus ifConfiguredThen (config => when(mockAWRSNotificationConnector.getNotificationViewedStatus(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(config))
    markNotificationViewedStatusAsViewed ifConfiguredThen (config => when(mockAWRSNotificationConnector.markNotificationViewedStatusAsViewed(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(config))
    sendConfirmationEmail ifConfiguredThen (config => when(mockAWRSNotificationConnector.sendConfirmationEmail(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(config))
  }

  def verifyAWRSNotificationConnector(fetchNotificationCache: Option[Int] = None,
                                      deleteNotificationFromCache: Option[Int] = None,
                                      getNotificationViewedStatus: Option[Int] = None,
                                      markNotificationViewedStatusAsViewed: Option[Int] = None,
                                      sendConfirmationEmail: Option[Int] = None
                                     ): Unit = {
    fetchNotificationCache ifDefinedThen (count => verify(mockAWRSNotificationConnector, times(count)).fetchNotificationCache(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
    deleteNotificationFromCache ifDefinedThen (count => verify(mockAWRSNotificationConnector, times(count)).deleteFromNotificationCache(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
    getNotificationViewedStatus ifDefinedThen (count => verify(mockAWRSNotificationConnector, times(count)).getNotificationViewedStatus(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
    markNotificationViewedStatusAsViewed ifDefinedThen (count => verify(mockAWRSNotificationConnector, times(count)).markNotificationViewedStatusAsViewed(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
    sendConfirmationEmail ifDefinedThen (count => verify(mockAWRSNotificationConnector, times(count)).sendConfirmationEmail(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
  }

}

object MockAWRSNotificationConnector {
  val defaultStatusNotification: Option[StatusNotification] = testStatusNotificationNoAlert
}
