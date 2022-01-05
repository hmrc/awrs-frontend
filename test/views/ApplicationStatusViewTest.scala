/*
 * Copyright 2022 HM Revenue & Customs
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
import models.FormBundleStatus.Rejected
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

import scala.concurrent.Future

class ApplicationStatusViewTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture {

  val template = app.injector.instanceOf[views.html.awrs_application_status]

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val businessCustomerDetailsFormID = "businessCustomerDetails"
  val formId = "legalEntity"

  lazy val testBusinessType = BusinessType(legalEntity = Option("LTD_GRP"), isSaAccount = None, isOrgAccount = None)
  lazy val testBCAddress = BCAddress("addressLine1", "addressLine2", Option("addressLine3"), Option("addressLine4"), Option("Ne4 9hs"), Option("country"))

  lazy val testBusinessCustomerGroup = BusinessCustomerDetails("ACME", Some("SOP"), testBCAddress, "sap123", "safe123", true, Some("agent123"))
  lazy val testBusinessCustomer = BusinessCustomerDetails("ACME", Some("SOP"), testBCAddress, "sap123", "safe123", false, Some("agent123"))
  lazy val testNewApplicationType = NewApplicationType(Some(true))

  val testEtmpCheckService: CheckEtmpService = mock[CheckEtmpService]

  lazy val testSubscriptionTypeFrontEnd: SubscriptionTypeFrontEnd = TestUtil.testSubscriptionTypeFrontEnd(legalEntity = Some(testBusinessDetailsEntityTypes(Llp)))

  val testApplicationStatusController = new ApplicationStatusController(mockMCC, testStatusManagementService, mockAuditable, mockAccountUtils, mockAuthConnector, testSave4LaterService, mockDeEnrolService, mockAppConfig, template)

  "viewing the status page" must {
    "display the application status decision text" in {
      viewStatusPage {
        result =>
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("govuk-details__text").text() must include(Messages("awrs.application_status.decision_answer.para_1"))
          document.getElementsByClass("govuk-details__text").text() must include(Messages("awrs.application_status.decision_answer.para_2"))
          document.getElementsByClass("govuk-details__text").text() must include(Messages("awrs.application_status.decision_answer.para_3"))
      }
    }
  }

  private def viewStatusPage(test: Future[Result] => Any) {
    resetAuthConnector()
    setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
    setupMockSave4LaterServiceWithOnly(
      fetchBusinessType = testBusinessType,
      fetchBusinessCustomerDetails = TestUtil.testBusinessCustomerDetails("LLP")
    )
    when(mockMainStoreSave4LaterConnector.fetchData4Later[NewAWBusiness](ArgumentMatchers.any(), ArgumentMatchers.eq("tradingStartDetails"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(Option(NewAWBusiness("Yes", None))))
    setupMockTestStatusManagementService(
      status = Rejected,
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
