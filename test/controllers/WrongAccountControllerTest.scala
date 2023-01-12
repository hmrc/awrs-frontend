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
import forms.BusinessDirectorsForm
import models.BusinessDirector
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ServicesUnitTestFixture
import utils.TestConstants._
import utils.{AwrsUnitTestTraits, TestUtil}
import utils.TestUtil._
import views.html.awrs_business_directors

import scala.concurrent.Future

class WrongAccountControllerTest extends AwrsUnitTestTraits with ServicesUnitTestFixture {

  val mockTemplate = app.injector.instanceOf[views.html.awrs_account_exists]


  val testWrongAccountController: WrongAccountController =
    new WrongAccountController(mockMCC, testSave4LaterService, mockDeEnrolService,mockAccountUtils, mockAuthConnector, mockAuditable, mockAppConfig, mockTemplate){
    override val signInUrl = "/sign-in"
  }


  "Wrong account business controller" must {

  }

}
