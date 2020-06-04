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

package connectors.mock

import java.util.UUID

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment, Enrolments}
import uk.gov.hmrc.auth.core.retrieve.{GGCredId, ~}
import utils.{AccountUtils, AwrsUnitTestTraits, TestUtil}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

sealed trait BType

case object SoleTrader extends BType
case object LTD extends BType

trait MockAuthConnector extends AwrsUnitTestTraits {
  lazy val userId = s"user-${UUID.randomUUID}"
  // need to be lazy in case of overrides
  lazy val mockAuthConnector: DefaultAuthConnector = mock[DefaultAuthConnector]

  def setUser(businessType: BType = SoleTrader, hasAwrs: Boolean = false): Unit = {
    reset(mockAuthConnector)
  }

  val authResultDefault: Enrolments ~ Some[AffinityGroup.Organisation.type] ~ GGCredId =
    new ~(
      new ~(
        Enrolments(TestUtil.defaultEnrolmentSet),
        Some(AffinityGroup.Organisation)
      ),
      GGCredId("CredID")
    )

  def mockAuthNoEnrolment: OngoingStubbing[Future[Enrolments ~ Option[AffinityGroup] ~ GGCredId]] = {
    when(mockAuthConnector.authorise[Enrolments ~ Option[AffinityGroup] ~ GGCredId](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(new ~(new ~(Enrolments(Set()), Some(AffinityGroup.Organisation)), GGCredId("CredID"))))
  }

  def setAuthMocks(
                    authResult: Future[Enrolments ~ Option[AffinityGroup] ~ GGCredId] = Future.successful(authResultDefault),
                    mockAccountUtils: Option[AccountUtils] = None
                  ): OngoingStubbing[Future[Enrolments ~ Option[AffinityGroup] ~ GGCredId]] = {
    authResult.foreach{ case Enrolments(e) ~ _ ~ _ =>
      val enrolment: Option[Enrolment] = e.find(_.key == "HMRC-AWRS-ORG")

      mockAccountUtils.foreach { utils =>
        when(utils.hasAwrs(ArgumentMatchers.any()))
          .thenReturn(enrolment.isDefined)

        when(utils.getAwrsRefNo(ArgumentMatchers.any()))
          .thenReturn("0123456")
      }
    }

    when(mockAuthConnector.authorise[Enrolments ~ Option[AffinityGroup] ~ GGCredId](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(authResult)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    setUser()
  }
}
