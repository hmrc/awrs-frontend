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

import builders.SessionBuilder
import config.FrontendAuthConnector
import connectors.mock.MockAuthConnector
import forms.GroupMemberDetailsForm
import models._
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.DataCacheKeys._
import services.mocks.MockSave4LaterService
import utils.TestUtil._
import utils.{AwrsUnitTestTraits, TestUtil}

import scala.concurrent.Future

class GroupMemberControllerTest extends AwrsUnitTestTraits
  with MockSave4LaterService
  with MockAuthConnector {

  val request = FakeRequest()

  val formId = "groupMember"

  private def testRequest(group: GroupMember) =
    TestUtil.populateFakeRequest[GroupMember](FakeRequest(), GroupMemberDetailsForm.groupMemberValidationForm, group)

  lazy val fakeRequest = testRequest(testGroupMember)

  object TestGroupMemberController extends GroupMemberController {
    override val authConnector = mockAuthConnector
    override val save4LaterService = TestSave4LaterService
  }

  "GroupMemberController" must {
    "use the correct AuthConnector" in {
      GroupMemberController.authConnector shouldBe FrontendAuthConnector
    }
  }

  "Submitting the application declaration form with " should {

    "Authenticated and authorised users" should {
      "redirect to additional premises page when valid data is provided for LTD" in {
        continueWithAuthorisedUser(1, Some(testGroupMemberDetails), testGroupMemberDetails, "LTD_GRP", fakeRequest) {
          result =>
            redirectLocation(result).get should include("/alcohol-wholesale-scheme/additional-premises")
        }
      }
      "redirect to business partners page when valid data is provided for LLP" in {
        continueWithAuthorisedUser(1, Some(testGroupMemberDetails), testGroupMemberDetails, "LLP_GRP", fakeRequest) {
          result =>
            redirectLocation(result).get should include("/alcohol-wholesale-scheme/business-partners")
        }
      }
      "save form data to Save4Later and redirect to business partners page for LLP" in {
        continueWithAuthorisedUser(1, Some(testGroupMemberDetails), testGroupMemberDetails, "LLP_GRP", fakeRequest) {
          result =>
            status(result) should be(SEE_OTHER)
            redirectLocation(result).get should include("/alcohol-wholesale-scheme/business-partners")
            verifySave4LaterService(saveGroupMemberDetails = 1)
        }
      }
      "save form data to Save4Later for second member and redirect to business partners page for LLP" in {
        continueWithAuthorisedUser(2, Some(testGroupMemberDetailsAddAnother), testGroupMemberDetails2Members, "LLP", fakeRequest) {
          result =>
            status(result) should be(SEE_OTHER)
            redirectLocation(result).get should include("/alcohol-wholesale-scheme/business-partners")
            verifySave4LaterService(saveGroupMemberDetails = 1)
        }
      }
      "save form data to Save4Later for first member where no data currently exists and redirect to business partners page for LLP" in {
        continueWithAuthorisedUser(1, None, testGroupMemberDetails, "LLP_GRP", fakeRequest) {
          result =>
            status(result) should be(SEE_OTHER)
            redirectLocation(result).get should include("/alcohol-wholesale-scheme/business-partners")
            verifySave4LaterService(saveGroupMemberDetails = 1)
        }
      }
      "save form data to Save4Later for first member where no data currently exists and redirect to next member page " in {
        continueWithAuthorisedUser(1, None, testGroupMemberDetailsAddAnother, "LLP", testRequest(testGroupMemberDefault(addAnotherGrpMember = "Yes"))) {
          result =>
            redirectLocation(result).get should include("/alcohol-wholesale-scheme/group-member?id=2")
            status(result) should be(SEE_OTHER)
            verifySave4LaterService(saveGroupMemberDetails = 1)
        }
      }
      "get out of range member should return NOT FOUND error " in {
        getWithAuthorisedUserCt(100) {
          result =>
            status(result) should be(NOT_FOUND)
        }
      }
    }
  }

  "Users who entered from the summary edit view" should {
    "return to the summary view after clicking return" in {
      returnWithAuthorisedUser(1, None, testGroupMemberDetailsAddAnother, "LLP", fakeRequest) {
        result =>
          redirectLocation(result).get should include(f"/alcohol-wholesale-scheme/view-section/$groupMembersName")
          verifySave4LaterService(saveGroupMemberDetails = 1)
      }
    }
  }

  "When loading the delete page we" should {
    "see to the selected suppliers delete confirmation page" in {
      showDeleteWithAuthorisedUser() {
        result =>
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("deleteConfirmation-heading").text shouldBe Messages("awrs.delete.confirmation_heading", Messages("awrs.view_application.group"))
          status(result) shouldBe 200
      }
    }
  }

  "When submitting the delete confirmation page we" should {
    "be routed back to the summary page after confirming Yes" in {
      deleteWithAuthorisedUser()(deleteConfirmation_Yes) {
        result =>
          status(result) shouldBe 303
          redirectLocation(result).get should include("/alcohol-wholesale-scheme/view-section/groupMember")
          verifySave4LaterService(saveGroupMemberDetails = 1)
      }
    }
    "be routed back to the summary page after confirming No" in {
      deleteWithAuthorisedUser()(deleteConfirmation_No) {
        result =>
          status(result) shouldBe 303
          redirectLocation(result).get should include("/alcohol-wholesale-scheme/view-section/groupMember")
          verifySave4LaterService(saveGroupMemberDetails = 0)
      }
    }
    "be shown an error if nothing is selected" in {
      deleteWithAuthorisedUser()(deleteConfirmation_None) {
        result =>
          status(result) shouldBe 400
      }
    }
    "be routed back to the summary page after confirming Yes for a record that is not the first" in {
      deleteWithAuthorisedUser(id = 2, members = testGroupMemberDetails2Members)(deleteConfirmation_Yes) {
        result =>
          status(result) shouldBe 303
          redirectLocation(result).get should include("/alcohol-wholesale-scheme/view-section/groupMember")
          verifySave4LaterService(saveGroupMemberDetails = 1)
      }
    }
  }

  private def getWithAuthorisedUserCt(memberId: Int)(test: Future[Result] => Any): Future[Any] = {
    setupMockSave4LaterServiceWithOnly(fetchGroupMemberDetails = testGroupMemberDetails)
    val result = TestGroupMemberController.showMemberDetails(memberId, isLinearMode = true, isNewRecord = true).apply(SessionBuilder.buildRequestWithSession(userId, "LTD_GRP"))
    test(result)
  }

  private def continueWithAuthorisedUser(memberId: Int, testDetailsFetch: Option[GroupMembers], testDetailsSave: GroupMembers, businessType: String, fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchGroupMemberDetails = testDetailsFetch, fetchPartnerDetails = None, fetchAdditionalBusinessPremisesList = None)
    val result = TestGroupMemberController.saveAndContinue(memberId, true).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, businessType))
    test(result)
  }

  private def returnWithAuthorisedUser(memberId: Int, testDetailsFetch: Option[GroupMembers], testDetailsSave: GroupMembers, businessType: String, fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchGroupMemberDetails = testDetailsFetch)
    val result = TestGroupMemberController.saveAndReturn(memberId, true).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, businessType))
    test(result)
  }

  private def showDeleteWithAuthorisedUser(id: Int = 1, members: GroupMembers = testGroupMemberDetails)(test: Future[Result] => Any): Future[Any] = {
    setupMockSave4LaterServiceWithOnly(fetchGroupMemberDetails = members)
    val result = TestGroupMemberController.showDelete(id).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def deleteWithAuthorisedUser(id: Int = 1, members: GroupMembers = testGroupMemberDetails)(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchGroupMemberDetails = members)
    val result = TestGroupMemberController.actionDelete(id).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }
}
