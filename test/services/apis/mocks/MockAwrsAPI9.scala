/*
 * Copyright 2020 HM Revenue & Customs
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

import connectors.mock.MockAWRSConnector
import models.{BusinessCustomerDetails, FormBundleStatus, SubscriptionStatusType}
import models.FormBundleStatus._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import services.apis.AwrsAPI9
import services.mocks.MockKeyStoreService
import services.mocks.MockSave4LaterService
import utils.{AwrsUnitTestTraits, TestUtil}


trait MockAwrsAPI9 extends AwrsUnitTestTraits with MockKeyStoreService with MockSave4LaterService with MockAWRSConnector {

  lazy val testAPI9 = new AwrsAPI9(mockAccountUtils, mockAWRSConnector, testKeyStoreService, testSave4LaterService)

  def setupMockAwrsAPI9(keyStore: Option[SubscriptionStatusType],
                        connector: MockConfiguration[SubscriptionStatusType] = DoNotConfigure): Unit = {
    connector match {
      case Configure(status) => setupMockAWRSConnectorWithOnly(checkStatus = status)
      case _ =>
    }
    setupMockKeyStoreServiceWithOnly(subscriptionStatusType = keyStore)
    setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = MockAwrsAPI9.defaultBusinessCustomerDetails)

    when(mockAccountUtils.hasAwrs(ArgumentMatchers.any()))
      .thenReturn(true)
  }

}

object MockAwrsAPI9 {

  def defaultSubscriptionStatusType(status: Option[FormBundleStatus]): Option[SubscriptionStatusType] =
    status match {
      case Some(Pending) => Some(TestUtil.testSubscriptionStatusTypePending)
      case Some(Approved) => Some(TestUtil.testSubscriptionStatusTypeApproved)
      case Some(ApprovedWithConditions) => Some(TestUtil.testSubscriptionStatusTypeApprovedWithConditions)
      case Some(Rejected) => Some(TestUtil.testSubscriptionStatusTypeRejected)
      case Some(RejectedUnderReviewOrAppeal) => Some(TestUtil.testSubscriptionStatusTypeRejectedUnderReviewOrAppeal)
      case Some(Revoked) => Some(TestUtil.testSubscriptionStatusTypeRevoked)
      case Some(RevokedUnderReviewOrAppeal) => Some(TestUtil.testSubscriptionStatusTypeRevokedUnderReviewOrAppeal)
      case None => None
      case _ => throw new NoSuchElementException("unimplemented default notification type in MockAwrsAPI9.defaultSubscriptionStatusType")
    }

  val defaultBusinessCustomerDetails: BusinessCustomerDetails = MockSave4LaterService.defaultBusinessCustomerDetails

}
