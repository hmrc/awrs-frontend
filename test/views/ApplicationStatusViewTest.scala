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

package views

import builders.SessionBuilder
import controllers.ApplicationStatusController
import models.BusinessDetailsEntityTypes.Llp
import models.FormBundleStatus._
import models._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{CheckEtmpService, ServicesUnitTestFixture}
import utils.TestUtil._
import utils.{AwrsUnitTestTraits, TestUtil}
import views.html.awrs_application_status

import scala.concurrent.Future

class ApplicationStatusViewTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture {

  val template: awrs_application_status = app.injector.instanceOf[views.html.awrs_application_status]

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val businessCustomerDetailsFormID = "businessCustomerDetails"
  val formId = "legalEntity"

  lazy val testBusinessType: BusinessType = BusinessType(legalEntity = Option("LTD_GRP"), isSaAccount = None, isOrgAccount = None)
  lazy val testBCAddress: BCAddress = BCAddress("addressLine1", "addressLine2", Option("addressLine3"), Option("addressLine4"), Option("Ne4 9hs"), Option("country"))

  lazy val testBusinessCustomerGroup: BusinessCustomerDetails = BusinessCustomerDetails("ACME", Some("SOP"), testBCAddress, "sap123", "safe123", true, Some("agent123"))
  lazy val testBusinessCustomer: BusinessCustomerDetails = BusinessCustomerDetails("ACME", Some("SOP"), testBCAddress, "sap123", "safe123", false, Some("agent123"))
  lazy val testNewApplicationType: NewApplicationType = NewApplicationType(Some(true))

  val testEtmpCheckService: CheckEtmpService = mock[CheckEtmpService]

  lazy val testSubscriptionTypeFrontEnd: SubscriptionTypeFrontEnd = TestUtil.testSubscriptionTypeFrontEnd(legalEntity = Some(testBusinessDetailsEntityTypes(Llp)))

  val testApplicationStatusController = new ApplicationStatusController(mockMCC, testStatusManagementService, mockAuditable, mockAccountUtils, mockAuthConnector, testSave4LaterService, mockDeEnrolService, mockAppConfig, template)

  "viewing the status page" must {

    "for an Approved user" in {
      viewStatusPage(Approved, {
        result =>
          val document = Jsoup.parse(contentAsString(result))
          val bodyText = document.getElementsByClass("govuk-body").text()
          val headingText = document.getElementsByClass("govuk-heading-m").text()
          val listText = document.getElementsByClass("govuk-list").text()



          val expectedLines = Seq(
            Messages("awrs.application_status.info.approved.line1"),
            Messages("awrs.application_status.info.approved.line2"),
            Messages("application_status.info.approved.line3"),
            Messages("awrs.application_status.info.approved.line4"),
            Messages("awrs.application_status.info.approved.line5"),
            Messages("awrs.application_status.info.approved.line6"),
            Messages("awrs.application_status.info.approved.line7"),
          )

          expectedLines.foreach { msg => bodyText must include(msg) }
          headingText must include(Messages("awrs.application_status.info.approved.heading1"))
          headingText must include(Messages("awrs.application_status.info.approved.heading2"))
          listText must include(Messages("awrs.application_status.info.approved.line4.1"))
          listText must include(Messages("awrs.application_status.info.approved.line4.2"))
      })
    }

  "for an Approved With Conditions user" in {
    viewStatusPage(ApprovedWithConditions, {
      result =>
        val document = Jsoup.parse(contentAsString(result))
        val headingText = document.getElementsByClass("govuk-heading-m").text()

        document.title() must include(Messages("awrs.generic.tab.title"))

        document.getElementsByClass("govuk-panel__body").text() must include(Messages("awrs.application_status.lede.main"))
        headingText must include(Messages("awrs.application_status.info.approved_with_conditions.heading"))
    })
  }

    "for a Rejected user" in {
      viewStatusPage(Rejected, {
        result =>
          val document = Jsoup.parse(contentAsString(result))
          val headingText = document.getElementsByClass("govuk-heading-m").text()

          document.title() must include(Messages("awrs.generic.tab.title"))

          document.getElementsByClass("govuk-details__summary-text").text() must include(Messages("awrs.application_status.decision_question"))
          document.getElementsByClass("govuk-details__text").text() must include(Messages("awrs.application_status.decision_answer.para_1"))
          document.getElementsByClass("govuk-details__text").text() must include(Messages("awrs.application_status.decision_answer.para_2"))
          document.getElementsByClass("govuk-details__text").text() must include(Messages("awrs.application_status.decision_answer.para_3"))
          document.getElementsByClass("govuk-details__text").text() must include(Messages("awrs.application_status.decision_answer.para_4"))
          document.getElementsByClass("govuk-details__text").text() must include(Messages("awrs.application_status.decision_answer.para_5"))

          headingText must include(Messages("awrs.application_status.info.rejected.heading"))
      })
    }

    "for a Rejected under Review or Appeal user" in {
      viewStatusPage(RejectedUnderReviewOrAppeal, {
        result =>
          val document = Jsoup.parse(contentAsString(result))
          val headingText = document.getElementsByClass("govuk-heading-m").text()

          document.title() must include(Messages("awrs.generic.tab.title"))

          document.getElementsByClass("govuk-panel__body").text() must include(Messages("awrs.application_status.under_review_or_appeal_lede.main.line1"))
          document.getElementsByClass("govuk-panel__body").text() must include(Messages("awrs.application_status.under_review_or_appeal_lede.main.line2"))
          headingText must include(Messages("awrs.application_status.info.rejected.heading"))
      })
    }

    "for a Revoked user" in {
      viewStatusPage(Revoked, {
        result =>
          val document = Jsoup.parse(contentAsString(result))
          val headingText = document.getElementsByClass("govuk-heading-m").text()

          document.title() must include(Messages("awrs.generic.tab.title"))

          document.getElementsByClass("govuk-panel__body").text() must include(Messages("awrs.application_status.lede.main"))
          headingText must include(Messages("awrs.application_status.info.rejected.heading"))
      })
    }

    "for a Revoked under Review or Appeal user" in {
      viewStatusPage(RevokedUnderReviewOrAppeal, {
        result =>
          val document = Jsoup.parse(contentAsString(result))
          val headingText = document.getElementsByClass("govuk-heading-m").text()

          document.title() must include(Messages("awrs.generic.tab.title"))

          document.getElementsByClass("govuk-panel__body").text() must include(Messages("awrs.application_status.under_review_or_appeal_lede.main.line1"))
          headingText must include(Messages("awrs.application_status.info.rejected.heading"))
      })
    }

    "for a Pending user" in {
      viewStatusPage(Pending, {
        result =>
          val document = Jsoup.parse(contentAsString(result))
          val headingText = document.getElementsByClass("govuk-heading-m").text()

          document.getElementsByClass("govuk-panel__body").text() must include(Messages("awrs.application_status.lede.main"))

          headingText must include("awrs.application_status.info.pending.heading")
      })
    }
  }

  private def viewStatusPage(status: FormBundleStatus, test: Future[Result] => Any): Unit = {
    resetAuthConnector()
    setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
    setupMockSave4LaterServiceWithOnly(
      fetchBusinessType = testBusinessType,
      fetchBusinessCustomerDetails = TestUtil.testBusinessCustomerDetails("LLP")
    )
    when(mockMainStoreSave4LaterConnector.fetchData4Later[NewAWBusiness](ArgumentMatchers.any(), ArgumentMatchers.eq("tradingStartDetails"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(Option(NewAWBusiness("Yes", None))))
    setupMockTestStatusManagementService(
      status = status,
      statusInfo = TestUtil.testStatusInfoTypeRejected,
      notification = None,
      configuration = MockStatusManagementServiceConfiguration(
        api9 = CachedLocally,
        api11 = CachedLocally,
        api12Cache = CachedLocally,
        statusPageViewed = false
      )
    )

    val result = testApplicationStatusController.showStatus(printFriendly = false, mustShow = false)
      .apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }
}
