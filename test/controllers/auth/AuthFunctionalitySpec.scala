/*
 * Copyright 2022 HM Revenue & Customs
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

import config.ApplicationConfig
import connectors.mock.MockAuthConnector
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContent, Result, Results}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{MissingBearerToken, PlayAuthConnector}
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthFunctionalitySpec extends PlaySpec with MockitoSugar with BeforeAndAfterEach with MockAuthConnector{

  lazy implicit val fakeRequest: FakeRequest[AnyContent] = FakeRequest()

   protected abstract class AuthFunctionalityServer() extends AuthFunctionality {
    val authConnector: PlayAuthConnector = mockAuthConnector
    implicit val applicationConfig: ApplicationConfig = mock[ApplicationConfig]
    override val signInUrl: String = "sign-url"
  }

  "authorisedAction" must {
    "call the function body and authorise" when {
      "the authorised call succeeds" in new AuthFunctionalityServer {
        setAuthMocks()
        private val result: Result = Results.Ok("Result")
        val future: StandardAuthRetrievals => Future[Result] = _ => Future.successful(result)

        await(authorisedAction(future)) mustBe result
      }
    }

    "redirect and fail to authorise" when {
      "the authorised call does not pass" in new AuthFunctionalityServer {
        when(mockAuthConnector.authorise[Unit](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.failed(MissingBearerToken("No authenticated bearer token")))
        private val result: Result = Results.Ok("Result")
        val future: StandardAuthRetrievals => Future[Result] = _ => Future.successful(result)

        val res: Future[Result] = authorisedAction(future)
        status(res) mustBe 303
      }
    }
  }

}
