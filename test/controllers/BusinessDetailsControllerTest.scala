/*
 * Copyright 2017 HM Revenue & Customs
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

import builders.{AuthBuilder, SessionBuilder}
import controllers.auth.Utr._
import forms.BusinessDetailsForm
import models._
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.DataCacheKeys._
import services.{Save4LaterService, ServicesUnitTestFixture}
import utils.TestUtil._
import utils.{AwrsUnitTestTraits, TestUtil}

import scala.concurrent.Future

class BusinessDetailsControllerTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture {

  def testRequest(businessDetails: BusinessDetails, entityType: String) =
    TestUtil.populateFakeRequest[BusinessDetails](FakeRequest(), BusinessDetailsForm.businessDetailsValidationForm(entityType), businessDetails)

  object TestBusinessDetailsController extends BusinessDetailsController {
    override val authConnector = mockAuthConnector
    override val save4LaterService = TestSave4LaterService
  }

  "BusinessDetailsController" must {

    "use the correct AwrsService" in {
      BusinessDetailsController.save4LaterService shouldBe Save4LaterService
    }

    "Users who entered from the summary edit view" should {
      "return to the summary view after clicking return" in {
        returnWithAuthorisedUser(testRequest(testBusinessDetails(), "SOP")) {
          result =>
            redirectLocation(result).get should include(f"/alcohol-wholesale-scheme/view-section/$businessDetailsName")
            verifySave4LaterService(saveBusinessDetails = 1)
        }
      }
    }

    def returnWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
      setupMockSave4LaterServiceWithOnly(
        fetchBusinessCustomerDetails = testBusinessCustomerDetails("SOP"),
        fetchBusinessDetails = testBusinessDetails(),
        fetchNewApplicationType = testNewApplicationType
      )
      val result = TestBusinessDetailsController.saveAndReturn().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, testBusinessCustomerDetails("SOP").businessType.get))
      test(result)
    }

  }
}
