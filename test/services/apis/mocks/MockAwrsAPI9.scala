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

package services.apis.mocks

import connectors.AWRSConnector
import connectors.mock.MockAWRSConnector
import models.{FormBundleStatus, SubscriptionStatusType}
import models.FormBundleStatus._
import services.{KeyStoreService, Save4LaterService}
import services.apis.AwrsAPI9
import services.mocks.MockKeyStoreService
import services.mocks.MockSave4LaterService
import utils.{AwrsUnitTestTraits, TestUtil}


trait MockAwrsAPI9 extends AwrsUnitTestTraits with MockKeyStoreService with MockSave4LaterService with MockAWRSConnector {

  object TestAPI9 extends AwrsAPI9 {
    override val awrsConnector: AWRSConnector = mockAWRSConnector
    override val keyStoreService: KeyStoreService = TestKeyStoreService
    override val save4LaterService : Save4LaterService = TestSave4LaterService
  }

  def setupMockAwrsAPI9(keyStore: Option[SubscriptionStatusType],
                        connector: MockConfiguration[SubscriptionStatusType] = DoNotConfigure) = {
    connector match {
      case Configure(status) => setupMockAWRSConnectorWithOnly(checkStatus = status)
      case _ =>
    }
    setupMockKeyStoreServiceWithOnly(subscriptionStatusType = keyStore)
    setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = MockAwrsAPI9.defaultBusinessCustomerDetails)
  }

}

object MockAwrsAPI9 extends MockAwrsAPI9 {

  def defaultSubscriptionStatusType(status: Option[FormBundleStatus]): Option[SubscriptionStatusType] =
    status match {
      case Some(Pending) => TestUtil.testSubscriptionStatusTypePending
      case Some(Approved) => TestUtil.testSubscriptionStatusTypeApproved
      case Some(ApprovedWithConditions) => TestUtil.testSubscriptionStatusTypeApprovedWithConditions
      case Some(Rejected) => TestUtil.testSubscriptionStatusTypeRejected
      case Some(RejectedUnderReviewOrAppeal) => TestUtil.testSubscriptionStatusTypeRejectedUnderReviewOrAppeal
      case Some(Revoked) => TestUtil.testSubscriptionStatusTypeRevoked
      case Some(RevokedUnderReviewOrAppeal) => TestUtil.testSubscriptionStatusTypeRevokedUnderReviewOrAppeal
      case None => None
      case _ => throw new NoSuchElementException("unimplemented default notification type in MockAwrsAPI9.defaultSubscriptionStatusType")
    }

  val defaultBusinessCustomerDetails = MockSave4LaterService.defaultBusinessCustomerDetails

}
