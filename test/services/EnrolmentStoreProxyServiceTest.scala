/*
 * Copyright 2025 HM Revenue & Customs
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

import connectors.EnrolmentStoreProxyConnector
import models.reenrolment._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class EnrolmentStoreProxyServiceTest extends AsyncWordSpec with Matchers with MockitoSugar {

  implicit val hc: HeaderCarrier        = HeaderCarrier()
  implicit val req: Request[AnyContent] = FakeRequest()

  private val mockConnector = mock[EnrolmentStoreProxyConnector]
  private val service       = new EnrolmentStoreProxyService(mockConnector)

  private val testAwrsRef = "XAAW00000123456"
  private val testGroupId = "test-group-id"
  private val testUserId  = "user-1"

  private val testKnownFacts = AwrsKnownFacts(
    service = "HMRC-AWRS-ORG",
    knownFacts = Seq(KnownFact("AWRSRefNumber", testAwrsRef))
  )

  "EnrolmentStoreProxyService" should {
    "queryForPrincipalGroupIdOfAWRSEnrolment" should {
      "return Some(groupId) when connector returns a group ID" in {
        when(mockConnector.queryForPrincipalGroupIdOfAWRSEnrolment(testAwrsRef))
          .thenReturn(Future.successful(Some(testGroupId)))

        service.queryForPrincipalGroupIdOfAWRSEnrolment(testAwrsRef).map { result =>
          result shouldBe Some(testGroupId)
        }
      }

      "return None when connector returns None" in {
        when(mockConnector.queryForPrincipalGroupIdOfAWRSEnrolment(testAwrsRef))
          .thenReturn(Future.successful(None))

        service.queryForPrincipalGroupIdOfAWRSEnrolment(testAwrsRef).map { result =>
          result shouldBe None
        }
      }

      "propagate exceptions from the connector" in {
        val exception = new RuntimeException("Connection failed")
        when(mockConnector.queryForPrincipalGroupIdOfAWRSEnrolment(testAwrsRef))
          .thenReturn(Future.failed(exception))

        recoverToSucceededIf[RuntimeException] {
          service.queryForPrincipalGroupIdOfAWRSEnrolment(testAwrsRef)
        }
      }
    }

    "lookupKnownFacts" should {
      val testResponse = KnownFactsResponse(
        service = "HMRC-AWRS-ORG",
        enrolments = Seq(
          Enrolment(
            identifiers = Seq(Identifier("AWRSRefNumber", testAwrsRef)),
            verifiers = Seq(Verifier("SAUTR", "1234567890"))
          )
        )
      )

      "return Some(response) when connector returns a response" in {
        when(mockConnector.lookupEnrolments(any[AwrsKnownFacts]())(any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.successful(Some(testResponse)))

        service.lookupKnownFacts(testKnownFacts).map { result =>
          result shouldBe Some(testResponse)
        }
      }

      "return None when connector returns None" in {
        when(mockConnector.lookupEnrolments(any[AwrsKnownFacts]())(any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.successful(None))

        service.lookupKnownFacts(testKnownFacts).map { result =>
          result shouldBe None
        }
      }

      "propagate exceptions from the connector" in {
        val exception = new RuntimeException("Lookup failed")
        when(mockConnector.lookupEnrolments(any[AwrsKnownFacts]())(any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.failed(exception))

        recoverToSucceededIf[RuntimeException] {
          service.lookupKnownFacts(testKnownFacts)
        }
      }

      "pass through the provided known facts to the connector" in {
        val captor = org.mockito.ArgumentCaptor.forClass(classOf[AwrsKnownFacts])
        when(mockConnector.lookupEnrolments(captor.capture())(any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.successful(Some(testResponse)))

        service.lookupKnownFacts(testKnownFacts).map { _ =>
          val capturedFacts: AwrsKnownFacts = captor.getValue
          capturedFacts.service shouldBe testKnownFacts.service
          capturedFacts.knownFacts shouldBe testKnownFacts.knownFacts
        }
      }
    }

    "doesEnrolmentExist check" should {
      "return true when an Enrolment exists for the passed in URN" in {
        when(mockConnector.queryForAssignedPrincipalUsersOfAWRSEnrolment(testAwrsRef))
          .thenReturn(
            Future.successful(
              Some(EnrolledUserIds(
                principalUserIds = Seq(testUserId)
              ))))

        service.isUserAssignedToAWRSEnrolment(testUserId, testAwrsRef).map { result =>
          result shouldBe true
        }
      }

      "return false when an Enrolment does not exist for the passed in URN" in {
        when(mockConnector.queryForAssignedPrincipalUsersOfAWRSEnrolment(testUserId))
          .thenReturn(
            Future.successful(
              Some(EnrolledUserIds(
                principalUserIds = Seq("foo")
              ))))

        service.isUserAssignedToAWRSEnrolment("different-user", testAwrsRef).map { result =>
          result shouldBe false
        }
      }

      "return false when request returns None for the passed in URN" in {
        when(mockConnector.queryForAssignedPrincipalUsersOfAWRSEnrolment(testAwrsRef))
          .thenReturn(Future.successful(None))

        service.isUserAssignedToAWRSEnrolment(testUserId, testAwrsRef).map { result =>
          result shouldBe false
        }
      }

      "return false when request returns an empty object for the passed in URN" in {
        when(mockConnector.queryForAssignedPrincipalUsersOfAWRSEnrolment(testAwrsRef))
          .thenReturn(
            Future.successful(
              Some(EnrolledUserIds(
                principalUserIds = Seq.empty
              ))))

        service.isUserAssignedToAWRSEnrolment(testUserId, testAwrsRef).map { result =>
          result shouldBe false
        }
      }
    }
  }

}
