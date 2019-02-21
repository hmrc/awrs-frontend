/*
 * Copyright 2019 HM Revenue & Customs
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

import builders.AuthBuilder
import controllers.auth.Utr._
import org.mockito.Mockito._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.AwrsUnitTestTraits

sealed trait BType

case object SoleTrader extends BType
case object LTD extends BType

trait MockAuthConnector extends AwrsUnitTestTraits {
  lazy val userId = s"user-${UUID.randomUUID}"
  // need to be lazy in case of overrides
  lazy val mockAuthConnector = mock[AuthConnector]

  def setUser(businessType: BType = SoleTrader, hasAwrs: Boolean = false) = {
    reset(mockAuthConnector)
    (businessType, hasAwrs) match {
      case (_, true) => AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector, awrsUtr)
      case (SoleTrader, _) => AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector, saUtr)
      case _ => AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector, ctUtr)
    }
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    setUser()
  }
}
