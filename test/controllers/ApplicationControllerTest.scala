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

package controllers

import builders.SessionBuilder
import connectors.mock.MockAuthConnector
import org.mockito.Mockito.when
import play.api.{Configuration, Play}
import play.api.Mode.Mode
import play.api.mvc.Result
import play.api.test.Helpers._
import utils.AwrsUnitTestTraits

import scala.concurrent.Future


class ApplicationControllerTest extends AwrsUnitTestTraits with MockAuthConnector {

  val testApplicationController = new ApplicationController(mockServicesConfig, mockMCC, mockAppConfig)

  "Authorised users" should {
    "be redirected to feedback-survey page" in {
      getWithAuthorisedUser { result =>
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include ("/feedback/AWRS")
      }
    }
  }

  "unauthorised users" should {
    "be directed to the unauthorised page" in {
      val result = testApplicationController.unauthorised.apply(SessionBuilder.buildRequestWithSession(userId))
      status(result) shouldBe UNAUTHORIZED
    }
  }

  "timedout users" should {
    "be redirected to signout" in {
      val result = testApplicationController.timedOut().apply(SessionBuilder.buildRequestWithSession(userId))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get should include ("/feedback/AWRS")
    }
  }

  "keep alive" should {
    "keep alive the session" in {
      val result = testApplicationController.keepAlive.apply(SessionBuilder.buildRequestWithSession(userId))
      status(result) shouldBe OK
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
