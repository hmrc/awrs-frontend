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

package controllers

import builders.SessionBuilder
import config.ApplicationConfig
import forms.BusinessContactsForm
import models.BusinessContacts
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.DataCacheKeys._
import services.{EmailVerificationService, ServicesUnitTestFixture}
import utils.TestUtil._
import utils.{AwrsUnitTestTraits, TestUtil}

import scala.concurrent.Future

class BusinessContactsControllerTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture {

  implicit val mockConfig: ApplicationConfig = mockAppConfig

  val businessCustomerDetailsFormId = "businessCustomerDetails"
  val mockEmailVerificationService: EmailVerificationService = mock[EmailVerificationService]

  val mockTemplate = app.injector.instanceOf[views.html.awrs_business_contacts]


  def testRequest(premises: BusinessContacts): FakeRequest[AnyContentAsFormUrlEncoded] =
    TestUtil.populateFakeRequest[BusinessContacts](FakeRequest(), BusinessContactsForm.businessContactsValidationForm, premises)

  val testBusinessContactsController: BusinessContactsController =
    new BusinessContactsController(mockMCC, mockEmailVerificationService, testSave4LaterService, mockDeEnrolService,
      mockAuthConnector, mockAuditable, mockAccountUtils, mockAppConfig, mockTemplate) {
    override val signInUrl = "/sign-in"
  }

  "BusinessContactsController" must {
    "Authorised users" must {

      "AWRS Contact Details entered " must {

        "save invalid form data and show the form error" in {
          continueWithAuthorisedUserCustomerDetailsMock()(FakeRequest().withFormUrlEncodedBody()) {
            result =>
              status(result) must be(BAD_REQUEST)
          }
        }

        "save invalid form data with no customer details and show internal server error" in {
          continueWithAuthorisedUserCustomerDetailsMock("SOP", false)(FakeRequest().withFormUrlEncodedBody()) {
            result =>
              status(result) must be(INTERNAL_SERVER_ERROR)
          }
        }

        "save form data in Save4Later and when contact details entered are valid" in {
          continueWithAuthorisedUser()(testRequest(testBusinessContactsDefault())) {
            result =>
              status(result) must be(SEE_OTHER)
              verifySave4LaterService(saveBusinessContacts = 1)
          }
        }

        "redirect to additional trading premises page when contact details entered are valid" in {
          continueWithAuthorisedUser()(testRequest(testBusinessContactsDefault())) {
            result =>
              redirectLocation(result).get must be("/alcohol-wholesale-scheme/additional-premises")
              verifySave4LaterService(saveBusinessContacts = 1)
          }
        }

        "redirect to group members page when contact details entered are valid for a group business type" in {
          continueWithAuthorisedUser(businessType = "LTD_GRP")(testRequest(testBusinessContactsDefault())) {
            result =>
              redirectLocation(result).get must include(f"/alcohol-wholesale-scheme/group-member")
              verifySave4LaterService(saveBusinessContacts = 1)
          }
        }

        "redirect to business partners page when contact details entered are valid for a LLP business type" in {
          continueWithAuthorisedUser(businessType = "LLP")(testRequest(testBusinessContactsDefault())) {
            result =>
              redirectLocation(result).get must include(f"/alcohol-wholesale-scheme/business-partners")
              verifySave4LaterService(saveBusinessContacts = 1)
          }
        }
      }
    }

    "Users who entered from the summary edit view" must {
      "return to the summary view after clicking return" in {
        returnWithAuthorisedUser(testRequest(testBusinessContactsDefault())) {
          result =>
            redirectLocation(result).get must include(f"/alcohol-wholesale-scheme/view-section/$businessContactsName")
            verifySave4LaterService(saveBusinessContacts = 1)
        }
      }
    }
  }

  private def continueWithAuthorisedUser(businessType: String = "SOP")(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(
      fetchBusinessCustomerDetails = testBusinessCustomerDetails(businessType),
      fetchBusinessContacts = testBusinessContactsDefault(),
      fetchGroupMemberDetails = None,
      fetchPartnerDetails = None,
      fetchAdditionalBusinessPremisesList = None
    )
    setAuthMocks()
    val result = testBusinessContactsController.saveAndContinue().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, businessType))
    test(result)
  }

  private def continueWithAuthorisedUserCustomerDetailsMock(businessType: String = "SOP", customerDetails: Boolean = true)(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(
      fetchBusinessCustomerDetails = if (customerDetails) testBusinessCustomerDetails(businessType) else None
    )
    setAuthMocks()
    val result = testBusinessContactsController.saveAndContinue().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, businessType))
    test(result)
  }

  private def returnWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(
      fetchBusinessCustomerDetails = testBusinessCustomerDetails("SOP"),
      fetchBusinessContacts = testBusinessContactsDefault()
    )
    setAuthMocks()
    val result = testBusinessContactsController.saveAndReturn().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, testBusinessCustomerDetails("SOP").businessType.get))
    test(result)
  }

}
