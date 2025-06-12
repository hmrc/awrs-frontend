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
import models._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.mocks.{MockIndexService, MockKeyStoreService, MockSave4LaterService}
import utils.AwrsUnitTestTraits
import utils.TestUtil._
import views.html.awrs_group_representative_change_confirm

import scala.concurrent.Future

class BusinessNameChangeControllerTest extends AwrsUnitTestTraits
  with MockAuthConnector
  with MockSave4LaterService
  with MockKeyStoreService
  with MockIndexService {

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val newBusinessName = "Changed"

  val testBusinessNameChange: BusinessNameChangeConfirmation = BusinessNameChangeConfirmation("Yes")

  val mockTemplate: awrs_group_representative_change_confirm = app.injector.instanceOf[views.html.awrs_group_representative_change_confirm]

  val testBusinessNameChangeController: BusinessNameChangeController =
    new BusinessNameChangeController(mockMCC, testKeyStoreService, testSave4LaterService, mockDeEnrolService,
      mockIndexService, mockAuthConnector, mockAuditable, mockAccountUtils, mockAppConfig, mockTemplate) {
    override val signInUrl: String = applicationConfig.signIn
  }

  "show confirm" must {
    "show the confirm page" in {
      setAuthMocks()
      val result = testBusinessNameChangeController.showConfirm().apply(SessionBuilder.buildRequestWithSession(userId))
      status(result) must be(OK)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("businessNameChangeConfirmation-heading").text() must include("awrs.business_name_change.heading")
      document.getElementById("businessNameChangeConfirmation-bullets").text() must include("awrs.business_name_change.warning.bullet.1")
      document.getElementById("businessNameChangeConfirmation-bullets").text() must include("awrs.business_name_change.warning.bullet.2")
      document.getElementById("businessNameChangeConfirmation-bullets").text() must include("awrs.business_name_change.warning.bullet.3")
    }
  }

  "Submitting the business name change confirmation form with " must {
    "Authenticated and authorised users" must {
      "redirect to view section for Business Details page when valid data is provided" in {
        continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("businessNameChangeConfirmation" -> "Yes")) {
          result =>
            redirectLocation(result).get must include("/alcohol-wholesale-scheme/view-section/businessDetails")
        }
      }
      "save form data to Save4Later and redirect to Index page " in {
        continueWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody("businessNameChangeConfirmation" -> "Yes")) {
          result =>
            status(result) must be(SEE_OTHER)
            verifySave4LaterService(saveBusinessCustomerDetails = 1)
        }
      }
    }
  }

  private def continueWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any): Unit = {
    setupMockKeyStoreServiceWithOnly(fetchBusinessNameChange = testBusinessNameDetails(businessName = newBusinessName))
    setupMockSave4LaterService()
    setAuthMocks()
    when(mockMainStoreSave4LaterConnector.fetchData4Later[BusinessNameDetails](ArgumentMatchers.any(), ArgumentMatchers.eq("businessNameDetails"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(Option(BusinessNameDetails(Some("test"), None, None))))

    val result = testBusinessNameChangeController.callToAction().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId).withMethod("POST"))
    test(result)
  }

}
