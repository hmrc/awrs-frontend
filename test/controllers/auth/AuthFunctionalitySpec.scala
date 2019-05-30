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

import connectors.mock.MockAuthConnector
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import play.api.mvc.{AnyContentAsEmpty, Result, Results}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{AuthConnector, MissingBearerToken}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class AuthFunctionalitySpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with MockAuthConnector{

  val authConnector = mockAuthConnector

  lazy implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  protected abstract class AuthFunctionalityServer() extends AuthFunctionality {
    val authConnector = mockAuthConnector
    override val signInUrl: String = "sign-url"
  }

  "authorisedAction" should {
    "call the function body and authorise" when {
      "the authorised call succeeds" in new AuthFunctionalityServer {
        setAuthMocks()
        private val result: Result = Results.Ok("Result")
        val future: StandardAuthRetrievals => Future[Result] = _ => Future.successful(result)

        await(authorisedAction(future)) shouldBe result
      }
    }

    "redirect and fail to authorise" when {
      "the authorised call does not pass" in new AuthFunctionalityServer {
        when(mockAuthConnector.authorise[Unit](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.failed(MissingBearerToken("No authenticated bearer token")))
        private val result: Result = Results.Ok("Result")
        val future: StandardAuthRetrievals => Future[Result] = _ => Future.successful(result)

        val res: Result = await(authorisedAction(future))
        status(res) shouldBe 303
      }
    }
  }

}
