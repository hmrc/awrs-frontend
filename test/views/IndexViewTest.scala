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
import controllers.{IndexController, routes}
import models.FormBundleStatus._
import models.SubscriptionStatusType
import org.jsoup.Jsoup
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.libs.json.JsValue
import play.api.mvc.Result
import play.api.test.Helpers._
import services.DataCacheKeys._
import services._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolments, User}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.TestUtil._
import utils.{AwrsUnitTestTraits, TestUtil}
import view_models._
import views.html.awrs_index

import scala.concurrent.Future

class IndexViewTest extends AwrsUnitTestTraits with ServicesUnitTestFixture {

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockApplicationService)
  }
  val template: awrs_index = app.injector.instanceOf[views.html.awrs_index]

  val testIndexController: IndexController = new IndexController(mockMCC, mockIndexService, testAPI9, mockApplicationService, testSave4LaterService, mockDeEnrolService, mockAuthConnector, mockAuditable, mockAccountUtils, mockAppConfig, template) {
    override val signInUrl = "/sign-in"
  }

  lazy val cachemap: CacheMap = CacheMap("", Map[String, JsValue]())
  lazy val noStatus: IndexViewModel = IndexViewModel(Nil)

  // N.B. this data is sufficient to test the following properties in the index-table.
  // 1) populate the section with the expected status
  // 2) populate the section name correctly when a count is given. e.g. (count) added for additional premises, directors, partners, group members and suppliers.
  // Since the IndexViewModel are generated by the IndexService, its validity is covered by the IndexService tests
  lazy val testIndexViewModel: (IndexStatus, Int) => IndexViewModel = (sectionStatus: IndexStatus, count: Int) =>
    IndexViewModel(
      List(
        SectionModel(businessDetailsName, routes.TradingNameController.showTradingName(isLinearMode = true).url, "awrs.index_page.business_details_text", sectionStatus),
        SectionModel(additionalBusinessPremisesName, routes.AdditionalPremisesController.showPremisePage(isLinearMode = true, isNewRecord = true).url, "awrs.index_page.additional_premises_text", sectionStatus, count)
      )
    )

  "IndexView" must {

    "Index-table" must {
      def checkSectionStatus(testData: IndexViewModel)(implicit result: Future[Result]): Unit = {
        val document = Jsoup.parse(contentAsString(result))
        testData.sectionModels.foreach {
          spec =>
            val sectionName = document.select(s"#${spec.id}").text()
            spec.size match {
              case Some(count) =>
                sectionName must include(Messages(spec.text, count))
                sectionName must include(count.toString)
              case _ =>
                sectionName must include(Messages(spec.text))

            }
            document.select(s"#${spec.id}_status").text() mustBe Messages(spec.status.messagesKey)
        }
      }

      "Generated the Index-table as specified by the IndexViewModel" in {
        List(SectionComplete, SectionIncomplete, SectionEdited, SectionNotStarted).foreach {
          sectionStatus =>
            val testData = testIndexViewModel(sectionStatus, 10)
            implicit val result: Future[Result] = showIndexPageAPI4(indexStatusModel = testData)
            checkSectionStatus(testData = testData)
        }
      }
    }

    "The call to action and unsubmitted changes banner" must {

      "In API4 jouney" must {
        "When the application is incomplete, the call to action button must be save_and_logout" in {
          val result = showIndexPageAPI4(allSectionComplete = false)
          val document = Jsoup.parse(contentAsString(result))
          document.select("#save_and_logout") must not be null
          document.select("#continue").size mustBe 0
          document.select("#submit_changes").size mustBe 0
        }

        "When the application is complete, the call to action button must be continue" in {
          val result = showIndexPageAPI4(allSectionComplete = true)
          val document = Jsoup.parse(contentAsString(result))
          document.select("#save_and_logout").size() mustBe 0
          document.select("#continue") must not be null
          document.select("#submit_changes").size mustBe 0
        }
      }

      "In API5 jouney" must {
        "When the application is unmodified or incomplete, the call to action button must not be displayed" in {
          //N.B. the application can still be incomplete in an API5 if there are missing data from Etmp or changes made to the front end which required additional input
          List((false, false), (false, true), (true, false)).foreach {
            case (allSectionComplete, hasApplicationChanged) =>
              val result = showIndexPageAPI5(allSectionComplete = allSectionComplete, hasApplicationChanged = hasApplicationChanged)
              val document = Jsoup.parse(contentAsString(result))
              document.select("#save_and_logout").size() mustBe 0
              document.select("#continue").size() mustBe 0
              document.select("#submit_changes").size() mustBe 0
          }
        }

        "When the application is modified and complete, the call to action button must be submit_changes" in {
          val result = showIndexPageAPI5(allSectionComplete = true, hasApplicationChanged = true)
          val document = Jsoup.parse(contentAsString(result))
          document.select("#save_and_logout").size() mustBe 0
          document.select("#continue").size() mustBe 0
          document.select("#submit_changes") must not be null
          document.select("#changes-banner").size() mustBe 1
        }

        "When the application is unmodified, do not display the unsubmitted changes banner" in {
          val result = showIndexPageAPI5(hasApplicationChanged = false)
          val document = Jsoup.parse(contentAsString(result))
          document.select("#changes-banner").size() mustBe 0
        }

        "When the application is modified, display the unsubmitted changes banner" in {
          val result = showIndexPageAPI5(hasApplicationChanged = true)
          val document = Jsoup.parse(contentAsString(result))
          document.select("#changes-banner") must not be null
        }

        "check for logged in username" in {
          val username = "ACME"
          val result = showIndexPageAPI5()
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("loggedInUserName").text() must be (Messages("awrs.generic.name.screen-reader-tip") + username)
        }
      }
    }

    "The status window" must {
      def checkElementsText(elementId: String, expected: String)(implicit result: Future[Result]): Unit = {
        val document = Jsoup.parse(contentAsString(result))
        document.select(s"#$elementId").text() must include(expected)
      }

      def checkAwrsRefNo(shouldExists: Boolean)(implicit result: Future[Result]): Unit = {
        val document = Jsoup.parse(contentAsString(result))
        shouldExists match {
          case true =>
            document.select("#awrsRefNo") must not be null
          case false =>
            document.select("#awrsRefNo").size() mustBe 0
        }
      }

      case class LinkExpectations(href: String, text: String)

      def checkLinks(linkId: String, expectations: Option[LinkExpectations])(implicit result: Future[Result]): Unit = {
        val document = Jsoup.parse(contentAsString(result))
        val link = document.select(s"#$linkId")
        expectations match {
          case Some(exp) =>
            link.attr("href") must be(exp.href)
            link.toString must include(exp.href)
            link.text() mustBe exp.text
          case None =>
            link.size() mustBe 0
        }
      }

      def checkWithdrawLink(shouldExists: Boolean)(implicit result: Future[Result]): Unit =
        checkLinks(linkId = "withdraw_link", shouldExists match {
          case true =>
            LinkExpectations(href = routes.WithdrawalController.showWithdrawalReasons.toString, text = Messages("awrs.index_page.withdraw_link"))
          case _ => None
        }
        )

      def checkDeRegLink(shouldExists: Boolean)(implicit result: Future[Result]): Unit =
        checkLinks(linkId = "de_reg_page_link", shouldExists match {
          case true =>
            LinkExpectations(href = routes.DeRegistrationController.showReason.toString, text = Messages("awrs.index_page.de_registration_link"))
          case _ => None
        }
        )

      def checkViewApplicationLink(shouldExists: Boolean)(implicit result: Future[Result]): Unit =
        checkLinks(linkId = "view-application", shouldExists match {
          case true =>
            LinkExpectations(href = routes.ViewApplicationController.show(false).toString, text = Messages("awrs.generic.print_application"))
          case _ => None
        }
        )

      def checkViewApplicationStatusLink(shouldExists: Boolean)(implicit result: Future[Result]): Unit =
        checkLinks(linkId = "status-page_link", shouldExists match {
          case true =>
            LinkExpectations(href = routes.ApplicationStatusController.showStatus().toString, text = Messages("awrs.index_page.view_application_status_link_text"))
          case _ => None
        }
        )

      "display application-status showing correct application status" in {
        {
          implicit val result: Future[Result] = showIndexPageAPI4()
          checkElementsText(elementId = "application-status", expected = s"${Messages("awrs.index_page.draft")} ${Messages("awrs.index_page.application_status_text").toLowerCase}")
          checkAwrsRefNo(shouldExists = false)
          checkWithdrawLink(shouldExists = false)
          checkDeRegLink(shouldExists = false)
          checkViewApplicationStatusLink (shouldExists = false)
        }
        {
          implicit val result: Future[Result] = showIndexPageAPI5(someStatus = testSubscriptionStatusTypePending)
          checkElementsText(elementId = "application-status", expected = s"${Messages("awrs.index_page.application_status_text")} ${Pending.name.toLowerCase}")
          checkAwrsRefNo(shouldExists = false)
          checkWithdrawLink(shouldExists = true)
          checkDeRegLink(shouldExists = false)
          checkViewApplicationStatusLink (shouldExists = true)
        }
        {
          implicit val result: Future[Result] = showIndexPageAPI5(someStatus = testSubscriptionStatusTypeApproved)
          checkElementsText(elementId = "application-status", expected = s"${Messages("awrs.index_page.application_status_text")} ${Approved.name.toLowerCase}")
          checkAwrsRefNo(shouldExists = true)
          checkWithdrawLink(shouldExists = false)
          checkDeRegLink(shouldExists = true)
          checkViewApplicationStatusLink (shouldExists = true)
        }
        {
          implicit val result: Future[Result] = showIndexPageAPI5(someStatus = testSubscriptionStatusTypeApprovedWithConditions)
          checkElementsText(elementId = "application-status", expected = s"${Messages("awrs.index_page.application_status_text")} ${ApprovedWithConditions.name.toLowerCase}")
          checkAwrsRefNo(shouldExists = true)
          checkWithdrawLink(shouldExists = false)
          checkDeRegLink(shouldExists = true)
          checkViewApplicationStatusLink (shouldExists = true)
        }
        // n.b. currently rejected users must simply be redirected, this will not be the case in these tests because
        // the status is not stored in the session. These tests are left in in-case this behaviour changes in the future.
        {
          implicit val result: Future[Result] = showIndexPageAPI5(someStatus = testSubscriptionStatusTypeRejected)
          checkElementsText(elementId = "application-status", expected = Rejected.name.toLowerCase)
          checkAwrsRefNo(shouldExists = false)
          checkWithdrawLink(shouldExists = false)
          checkDeRegLink(shouldExists = false)
        }
      }

      "show one view link only when it is specifed" in {
        List(true, false).foreach {
          showLink =>
            implicit val result: Future[Result] = showIndexPageAPI4(showOneViewLink = showLink)
            checkViewApplicationLink(shouldExists = showLink)
        }
      }

    }


  }

  private def showIndexPage(hasAwrs: Boolean,
                            hasApplicationChanged: Boolean,
                            allSectionComplete: Boolean,
                            showOneViewLink: Boolean,
                            businessName: String,
                            indexStatusModel: view_models.IndexViewModel,
                            someStatus: Option[SubscriptionStatusType]): Future[Result] = {
    resetAuthConnector()
    setupMockSave4LaterService(
      fetchBusinessCustomerDetails = testReviewDetails,
      fetchAll = cachemap
    )
    setupMockAwrsAPI9(keyStore = someStatus, connector = DoNotConfigure)
    setupMockApplicationService(hasAPI5ApplicationChanged = hasApplicationChanged)
    setupMockIndexService(
      showOneViewLink = showOneViewLink,
      showContinueButton = allSectionComplete,
      getStatus = indexStatusModel
    )
    setAuthMocks(Future.successful(new ~(new ~( new ~(Enrolments(TestUtil.defaultEnrolmentSet), Some(AffinityGroup.Organisation)), Credentials("fakeCredID", "type")), Some(User))))
    testIndexController.showIndex().apply(SessionBuilder.buildRequestWithSession(userId, "SOP"))
  }


  private def showIndexPageAPI4(allSectionComplete: Boolean = false,
                                showOneViewLink: Boolean = false,
                                indexStatusModel: view_models.IndexViewModel = noStatus,
                                businessName: String = "My Business"): Future[Result] =
    showIndexPage(
      hasAwrs = false, // can never have awrs in api4
      hasApplicationChanged = false, // can never be modified in api4
      allSectionComplete = allSectionComplete,
      showOneViewLink = showOneViewLink,
      businessName = businessName,
      indexStatusModel = indexStatusModel,
      someStatus = None // status will always be draft in api4
    )

  private def showIndexPageAPI5(hasApplicationChanged: Boolean = false,
                                allSectionComplete: Boolean = false,
                                showOneViewLink: Boolean = false,
                                indexStatusModel: view_models.IndexViewModel = noStatus,
                                someStatus: Option[SubscriptionStatusType] = testSubscriptionStatusTypePending,
                                businessName: String = "My Business"
                               ): Future[Result] =
    showIndexPage(
      hasAwrs = true,
      hasApplicationChanged = hasApplicationChanged,
      allSectionComplete = allSectionComplete,
      showOneViewLink = showOneViewLink,
      businessName = businessName,
      indexStatusModel = indexStatusModel,
      someStatus = someStatus
    )

}