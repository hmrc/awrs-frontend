/*
 * Copyright 2017 HM Revenue & Customs
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

import java.util.UUID

import _root_.models.FormBundleStatus
import _root_.models.FormBundleStatus._
import _root_.models.StatusContactType.{MindedToReject, MindedToRevoke}
import builders.AuthBuilder
import connectors.mock.MockAuthConnector
import play.api.test.FakeRequest
import services.mocks.{MockKeyStoreService, MockTestStatusManagementService}
import utils.{AwrsSessionKeys, AwrsUnitTestTraits}
import utils.TestConstants._
import uk.gov.hmrc.http.SessionKeys

class StatusManagementServiceTest extends AwrsUnitTestTraits
  with MockTestStatusManagementService with MockAuthConnector {

  implicit lazy val fakeRequest = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId,
      "businessType" -> "SOP",
      "businessName" -> "North East Wines"
    )
  }

  // alias for the function which maps to the expected output using the input of setupMockTestStatusManagementService
  val expected = MockTestStatusManagementService.expectedRetrieveStatusOutput

  "StatusManagementService" should {
    "do not call any apis if the data is already locally cached" in {
      // currently the existence of "AwrsSessionKeys.sessionStatusType" in the session is used to determine if the data
      // is cached locally.
      implicit val fakeRequest = {
        val sessionId = s"session-${UUID.randomUUID}"
        FakeRequest().withSession(
          SessionKeys.sessionId -> sessionId,
          SessionKeys.token -> "RANDOMTOKEN",
          SessionKeys.userId -> userId,
          "businessType" -> "SOP",
          "businessName" -> "North East Wines",
          AwrsSessionKeys.sessionStatusType -> "" // currently, it doesn't matter what value is here so long as the key exists
        )
      }
      setupMockKeyStoreService()

      val result = TestStatusManagementService.retrieveStatus
      val statusReturnType = await(result)

      val subscriptionStatus = statusReturnType.status
      val alertStatus = statusReturnType.notification
      val statusInfo = statusReturnType.info

      subscriptionStatus shouldBe Some(MockKeyStoreService.defaultSubscriptionStatusType)
      alertStatus shouldBe MockKeyStoreService.defaultStatusNotification
      statusInfo shouldBe Some(MockKeyStoreService.defaultStatusTypeInfo)

      verifyKeyStoreService(
        fetchSubscriptionStatusType = 1,
        fetchStatusNotification = 1,
        fetchStatusInfoType = 1
      )
      verifyAWRSConnector(
        checkStatus = 0,
        getStatusInfo = 0
      )
      verifyAWRSNotificationConnector(
        fetchNotificationCache = 0,
        getNotificationViewedStatus = 0,
        markNotificationViewedStatusAsViewed = 0
      )
    }

    "call api 12 when the status is pending, and call api 11 if the alert is minded to reject" in {
      setupMockTestStatusManagementService(
        status = Pending,
        notification = MindedToReject
      )
      val result = TestStatusManagementService.retrieveStatus
      await(result) shouldBe expected(Pending, MindedToReject)

      verifyAWRSConnector(
        checkStatus = 1,
        getStatusInfo = 1
      )
      verifyAWRSNotificationConnector(
        fetchNotificationCache = 1,
        getNotificationViewedStatus = 1,
        markNotificationViewedStatusAsViewed = 1
      )
    }

    "call api 12 when the status is pending, and do not call api 11 if there are no alerts returned from api 12" in {
      setupMockTestStatusManagementService(
        status = Pending,
        notification = None
      )
      val result = TestStatusManagementService.retrieveStatus
      await(result) shouldBe expected(Pending, None)


      verifyAWRSConnector(
        checkStatus = 1,
        getStatusInfo = 0
      )
      verifyAWRSNotificationConnector(
        fetchNotificationCache = 1,
        getNotificationViewedStatus = 1,
        markNotificationViewedStatusAsViewed = 1
      )

    }

    "call api 12 when the status is approved, and call api 11 if the alert is minded to revoke" in {
      setupMockTestStatusManagementService(
        status = Approved,
        notification = MindedToRevoke
      )
      val result = TestStatusManagementService.retrieveStatus
      await(result) shouldBe expected(Approved, MindedToRevoke)

      verifyAWRSConnector(
        checkStatus = 1,
        getStatusInfo = 1
      )
      verifyAWRSNotificationConnector(
        fetchNotificationCache = 1,
        getNotificationViewedStatus = 1,
        markNotificationViewedStatusAsViewed = 1
      )
    }

    "call api 12 when the status is approved, and do not call api 11 if the alert is not minded to revoke" in {
      setupMockTestStatusManagementService(
        status = Approved,
        notification = None
      )
      val result = TestStatusManagementService.retrieveStatus
      await(result) shouldBe expected(Approved, None)

      verifyAWRSConnector(
        checkStatus = 1,
        getStatusInfo = 0
      )
      verifyAWRSNotificationConnector(
        fetchNotificationCache = 1,
        getNotificationViewedStatus = 1,
        markNotificationViewedStatusAsViewed = 1
      )

    }

    "call api 12 when the status is approved with conditions, and call api 11 if the alert is minded to revoke" in {

      setupMockTestStatusManagementService(
        status = ApprovedWithConditions,
        notification = MindedToRevoke
      )
      val result = TestStatusManagementService.retrieveStatus
      await(result) shouldBe expected(ApprovedWithConditions, MindedToRevoke)

      verifyAWRSConnector(
        checkStatus = 1,
        getStatusInfo = 1
      )
      verifyAWRSNotificationConnector(
        fetchNotificationCache = 1,
        getNotificationViewedStatus = 1,
        markNotificationViewedStatusAsViewed = 1
      )

    }

    "call api 12 when the status is approved with conditions, and still call api 11 if the alert is not minded to revoke" in {
      setupMockTestStatusManagementService(
        status = ApprovedWithConditions,
        notification = None
      )
      val result = TestStatusManagementService.retrieveStatus
      await(result) shouldBe expected(ApprovedWithConditions, None)

      verifyAWRSConnector(
        checkStatus = 1,
        getStatusInfo = 1
      )
      verifyAWRSNotificationConnector(
        fetchNotificationCache = 1,
        getNotificationViewedStatus = 1,
        markNotificationViewedStatusAsViewed = 1
      )

    }

    "do not call api 12 for any other statuses" in {
      lazy val testMain = (status: FormBundleStatus) => {
        beforeEach()
        setupMockTestStatusManagementService(
          status = status,
          notification = None
        )
        val result = TestStatusManagementService.retrieveStatus
        await(result) shouldBe expected(status, None)

        verifyAWRSConnector(
          checkStatus = 1,
          getStatusInfo = 1
        )
        verifyAWRSNotificationConnector(
          fetchNotificationCache = 0,
          getNotificationViewedStatus = 1,
          markNotificationViewedStatusAsViewed = 1
        )
      }

      lazy val ignoreAPI12statuses = List[FormBundleStatus](
        Rejected,
        RejectedUnderReviewOrAppeal,
        Revoked,
        RevokedUnderReviewOrAppeal
      )

      ignoreAPI12statuses.foreach(x => testMain(x))
    }
  }

}
