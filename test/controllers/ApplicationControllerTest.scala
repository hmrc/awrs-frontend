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

package controllers

import builders.SessionBuilder
import connectors.mock.MockAuthConnector
import org.mockito.Mockito.when
import play.api.mvc.Result
import play.api.test.Helpers._
import utils.AwrsUnitTestTraits

import scala.concurrent.Future


class ApplicationControllerTest extends AwrsUnitTestTraits with MockAuthConnector {

  val testApplicationController = new ApplicationController(mockMCC, mockAppConfig)

  "Authorised users" must {
    "be redirected to feedback-survey page" in {
      getWithAuthorisedUser { result =>
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include ("/feedback/AWRS")
      }
    }
  }

  "unauthorised users" must {
    "be directed to the unauthorised page" in {
      val result = testApplicationController.unauthorised.apply(SessionBuilder.buildRequestWithSession(userId))
      status(result) mustBe UNAUTHORIZED
    }
  }

  "timedout users" must {
    "be redirected to signout" in {
      val result = testApplicationController.timedOut().apply(SessionBuilder.buildRequestWithSession(userId))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get must include ("/feedback/AWRS")
    }
  }

  "keep alive" must {
    "keep alive the session" in {
      val result = testApplicationController.keepAlive.apply(SessionBuilder.buildRequestWithSession(userId))
      status(result) mustBe OK
    }
  }

  def getWithAuthorisedUser(test: Future[Result] => Any): Unit = {
    setAuthMocks()

    when(mockAppConfig.signOut)
      .thenReturn("/feedback/AWRS")

    val result = testApplicationController.logout.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

}
