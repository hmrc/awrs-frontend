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

package controllers.auth

import controllers.auth.AwrsRegistrationRegime._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts
import uk.gov.hmrc.play.test.UnitSpec

class AwrsRegistrationRegimeTest extends UnitSpec with OneServerPerSuite with MockitoSugar {

  "AwrsRegime" should {

    "define isAuthorised" should {

      val accounts = mock[Accounts](RETURNS_DEEP_STUBS)

      "return true when the user is registered for company tax" in {
        when(accounts.ct.isDefined).thenReturn(true)
        isAuthorised(accounts) shouldBe true
      }

      "return true when the user is registered for self-assessment" in {
        when(accounts.sa.isDefined).thenReturn(true)
        isAuthorised(accounts) shouldBe true
      }

      "return true when the user is org" in {
        when(accounts.org.isDefined).thenReturn(true)
        isAuthorised(accounts) shouldBe true
      }

      "return false when the user is not registered for company tax , self assessment or org" in {
        when(accounts.ct.isDefined).thenReturn(false)
        when(accounts.org.isDefined).thenReturn(false)
        when(accounts.sa.isDefined). thenReturn(false)
        isAuthorised(accounts) shouldBe false
      }
    }

    "define the authentication type as the Awrs Subscription GG" in {
      authenticationType shouldBe AwrsRegistrationGovernmentGateway
    }

    "define the unauthorised landing page as /unauthorised" in {
      unauthorisedLandingPage.get shouldBe "/alcohol-wholesale-scheme/unauthorised"
    }

  }
}
