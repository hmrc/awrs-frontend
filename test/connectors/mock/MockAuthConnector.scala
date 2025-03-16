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

package connectors.mock

import java.util.UUID

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment, Enrolments, User}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import utils.{AccountUtils, AwrsUnitTestTraits, TestUtil}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.auth.core.CredentialRole

sealed trait BType

case object SoleTrader extends BType
case object LTD extends BType

trait MockAuthConnector extends AwrsUnitTestTraits {
  lazy val userId = s"user-${UUID.randomUUID}"
  lazy val mockAuthConnector: DefaultAuthConnector = mock[DefaultAuthConnector]

  def resetAuthConnector(): Unit = {
    reset(mockAuthConnector)
  }

  def authResultDefault(role: CredentialRole = User): Enrolments ~ Some[AffinityGroup.Organisation.type] ~ Some[Credentials] ~ Some[CredentialRole] =
    new ~(new ~( new ~(Enrolments(TestUtil.defaultEnrolmentSet),Some(AffinityGroup.Organisation)),
      Some(Credentials("CredID", "type"))),
      Some(role)
    )

  def mockAuthNoEnrolment: OngoingStubbing[Future[Enrolments ~ Option[AffinityGroup] ~ Option[Credentials] ~ Some[CredentialRole]]] = {
    when(mockAuthConnector.authorise[Enrolments ~ Option[AffinityGroup] ~ Option[Credentials] ~ Some[CredentialRole]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(
        new ~(new ~(new ~(Enrolments(Set()), Some(AffinityGroup.Organisation)),
          Some(Credentials("CredID", "type"))),
          Some(User)
      )
      ))
  }

  def setAuthMocks(
                    authResult: Future[Enrolments ~ Option[AffinityGroup] ~ Option[Credentials] ~ Some[CredentialRole]] = Future.successful(authResultDefault()),
                    mockAccountUtils: Option[AccountUtils] = None
                  ): OngoingStubbing[Future[Enrolments ~ Option[AffinityGroup] ~ Option[Credentials] ~ Option[CredentialRole]]] = {
    authResult.foreach{ case Enrolments(e) ~ _ ~ _ ~ _ =>
      val enrolment: Option[Enrolment] = e.find(_.key == "HMRC-AWRS-ORG")

      mockAccountUtils.foreach { utils =>
        when(utils.hasAwrs(ArgumentMatchers.any()))
          .thenReturn(enrolment.isDefined)

        when(utils.getUtr(ArgumentMatchers.any()))
          .thenReturn("TestUTR")

        when(utils.getAwrsRefNo(ArgumentMatchers.any()))
          .thenReturn("0123456")
      }
    }

    when(mockAuthConnector.authorise[Enrolments ~ Option[AffinityGroup] ~ Option[Credentials] ~ Option[CredentialRole]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(authResult)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    resetAuthConnector()
  }
}
