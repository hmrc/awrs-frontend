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
import models.StatusContactType.{MindedToReject, MindedToRevoke, NoLongerMindedToRevoke}
import models.{FormBundleStatus, StatusContactType, StatusInfoType}
import models.FormBundleStatus._
import services.apis.AwrsAPI11
import services.mocks.MockKeyStoreService
import utils.{AwrsUnitTestTraits, TestUtil}


trait MockAwrsAPI11 extends AwrsUnitTestTraits with MockKeyStoreService with MockAWRSConnector {

  val testAPI11 = new AwrsAPI11(mockAWRSConnector, testKeyStoreService, mockAuditable)

  def setupMockAwrsAPI11(keyStore: Option[StatusInfoType],
                         connector: MockConfiguration[StatusInfoType] = DoNotConfigure): Unit = {
    connector match {
      case Configure(status) => setupMockAWRSConnectorWithOnly(getStatusInfo = status)
      case _ =>
    }
    setupMockKeyStoreServiceWithOnly(statusInfoType = keyStore)
  }

}

object MockAwrsAPI11 {

  def defaultStatusInfoType(status: Option[FormBundleStatus],
                            notification: Option[StatusContactType]): Option[StatusInfoType] =
    (status, notification) match {
      case (Some(Pending), None) => None
      case (Some(Pending), Some(MindedToReject)) => Some(TestUtil.testStatusInfoTypeMindedToReject)
      case (Some(Approved), None) => None
      case (Some(Approved | ApprovedWithConditions), Some(MindedToRevoke)) => Some(TestUtil.testStatusInfoTypeMindedToRevoke)
      case (Some(ApprovedWithConditions), None) => Some(TestUtil.testStatusInfoTypeApprovedWithConditions)
      case (Some(Approved | ApprovedWithConditions | Rejected), Some(MindedToReject)) => None // TODO check this
      case (Some(Approved | ApprovedWithConditions), Some(NoLongerMindedToRevoke)) => Some(TestUtil.testStatusInfoTypeNoLongerMindedToRevoke)
      case (Some(Rejected), _) => Some(TestUtil.testStatusInfoTypeRejected)
      case (Some(RejectedUnderReviewOrAppeal), _) => Some(TestUtil.testStatusInfoTypeRejectedUnderReviewOrAppeal)
      case (Some(Revoked), _) => Some(TestUtil.testStatusInfoTypeRevoked)
      case (Some(RevokedUnderReviewOrAppeal), _) => Some(TestUtil.testStatusInfoTypeRevokedUnderReviewOrAppeal)
      case (None, _) => None
      case _ => throw new NoSuchElementException(s"unimplemented default notification type in MockAwrsAPI11.defaultStatusInfoType:\n($status, $notification)")
    }


}
