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

package views

import builders.SessionBuilder
import connectors.mock.MockAuthConnector
import controllers.GroupMemberController
import models._
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Result
import play.api.test.Helpers._
import services.DataCacheKeys._
import services.JourneyConstants
import services.mocks.MockSave4LaterService
import utils.TestUtil.testBusinessCustomerDetails
import utils.{AwrsUnitTestTraits, TestUtil}

import scala.concurrent.Future

class GroupMembersViewTest extends AwrsUnitTestTraits
  with MockSave4LaterService with MockAuthConnector {

  lazy val groupMember = TestUtil.testGroupMember
  lazy val testGroupMembers = GroupMembers(List(groupMember, groupMember, groupMember))

  object TestGroupMemberController extends GroupMemberController {
    override val authConnector = mockAuthConnector
    override val save4LaterService = TestSave4LaterService
    val signInUrl = "/sign-in"
  }

  "Group member page" should {

    "display the correct headings" in {
      (1 to 3).foreach {
        id =>
          linearJourney(id) {
            result =>
              status(result) shouldBe OK
              val document = Jsoup.parse(contentAsString(result))
              document.select("#group-member-heading").text should be(Messages("awrs.group_member.top_heading", Messages("awrs.generic.tell_us_about"), views.html.helpers.ordinalIntSuffix(id)))
          }
          editJourney(id) {
            result =>
              status(result) shouldBe OK
              val document = Jsoup.parse(contentAsString(result))
              document.getElementById("group-member-heading").text should be(Messages("awrs.group_member.top_heading", Messages("awrs.generic.edit"), views.html.helpers.ordinalIntSuffix(id)))
          }
      }
      // if the user adds a new group member from the section view
      postLinearJourneyAddition {
        result =>
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("group-member-heading").text should be(Messages("awrs.group_member.top_heading", Messages("awrs.generic.tell_us_about"), views.html.helpers.ordinalIntSuffix(1)))
      }
    }

    groupEntities.foreach {
      legalEntity =>
        s"$legalEntity" should {
          Seq(true, false).foreach {
            isLinear =>
              s"see a progress message for the isLinearJourney is set to $isLinear" in {
                val test: Future[Result] => Unit = result => {
                  implicit val doc = Jsoup.parse(contentAsString(result))
                  testId(shouldExist = true)(targetFieldId = "progress-text")
                  val journey = JourneyConstants.getJourney(legalEntity)
                  val expectedSectionNumber = journey.indexOf(groupMembersName) + 1
                  val totalSectionsForBusinessType = journey.size
                  val expectedSectionName = Messages("awrs.index_page.group_member_details_text")
                  val expected = Messages("awrs.generic.section_progress", expectedSectionNumber, totalSectionsForBusinessType, expectedSectionName)
                  testText(expectedText = expected)(targetFieldId = "progress-text")
                }
                eitherJourney(isLinearJourney = isLinear, entityType = legalEntity)(test)
              }
          }
        }
    }

  }

  private def linearJourney(memberId: Int)(test: Future[Result] => Any): Future[Any] = {
    setupMockSave4LaterServiceWithOnly(fetchGroupMemberDetails = testGroupMembers)
    setAuthMocks()
    val result = TestGroupMemberController.showMemberDetails(memberId, isLinearMode = true, isNewRecord = true).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def editJourney(memberId: Int)(test: Future[Result] => Any): Future[Any] = {
    setupMockSave4LaterServiceWithOnly(fetchGroupMemberDetails = testGroupMembers)
    setAuthMocks()
    val result = TestGroupMemberController.showMemberDetails(memberId, isLinearMode = false, isNewRecord = false).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def postLinearJourneyAddition(test: Future[Result] => Any): Future[Any] = {
    setupMockSave4LaterServiceWithOnly(fetchGroupMemberDetails = None)
    setAuthMocks()
    val result = TestGroupMemberController.showMemberDetails(1, isLinearMode = false, isNewRecord = true).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def eitherJourney(id: Int = 1, isLinearJourney: Boolean, isNewRecord: Boolean = true, entityType: String)(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(
      fetchBusinessCustomerDetails = testBusinessCustomerDetails(entityType),
      fetchGroupMemberDetails = testGroupMembers
    )
    setAuthMocks()
    val result = TestGroupMemberController.showMemberDetails(id = id, isLinearMode = isLinearJourney, isNewRecord = isNewRecord).apply(SessionBuilder.buildRequestWithSession(userId, entityType))
    test(result)
  }
}
