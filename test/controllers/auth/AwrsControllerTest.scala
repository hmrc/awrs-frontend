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

package controllers.auth

import builders.SessionBuilder
import controllers.IndexController
import models._
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.DataCacheKeys._
import services.ServicesUnitTestFixture
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.TestConstants._
import utils.TestUtil._
import utils.{AwrsSessionKeys, AwrsUnitTestTraits}
import view_models.{IndexViewModel, SectionComplete, SectionModel}

import scala.concurrent.Future

class AwrsControllerTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture {

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockIndexService)
  }

  val template = app.injector.instanceOf[views.html.awrs_index]

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val testIndexController: IndexController = new IndexController(mockMCC, mockIndexService, testAPI9,
    mockApplicationService, testSave4LaterService, mockDeEnrolService, mockAuthConnector, mockAuditable, mockAccountUtils, mockAppConfig, template) {
    override val signInUrl: String = "/sign-in"
  }

  "The index page which implements AwrsController" must {

    def noRedirection()(implicit result: Future[Result]): Unit = {
      status(result) mustBe OK
    }

    def redirected()(implicit result: Future[Result]): Unit = {
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some("/alcohol-wholesale-scheme/status-page")
    }

    "not perform any redirection if the status is not rejected" in {
      getWithAuthorisedUserCtWithStatusPending(implicit result => noRedirection())
      getWithAuthorisedUserCtWithStatusApproved(implicit result => noRedirection())
      getWithAuthorisedUserCtWithStatusApprovedWithConditions(implicit result => noRedirection())
    }

    "redirect back to the status page if the status is rejected" in {
      getWithAuthorisedUserCtWithStatusRejected(implicit result => redirected())
    }
  }

  lazy val returnedCachemap = CacheMap(testUtr, Map("businessCustomerDetails" -> Json.toJson(testReviewDetails)))

  def returnedCachemapAny(details: BusinessCustomerDetails) = CacheMap(testUtr, Map("businessCustomerDetails" -> Json.toJson(details)))

  lazy val indexViewModelComplete = IndexViewModel(List(
    SectionModel("businessDetails", "/alcohol-wholesale-scheme/corporate-body-business-details", "awrs.index_page.business_details_text", SectionComplete),
    SectionModel("additionalPremises", "/alcohol-wholesale-scheme/additional-premises", "awrs.index_page.additional_premises_text", SectionComplete),
    SectionModel("additionalBusinessInformation", "/alcohol-wholesale-scheme/additional-information", "awrs.index_page.additional_business_information_text", SectionComplete),
    SectionModel("aboutYourSuppliers", "/alcohol-wholesale-scheme/supplier-addresses", "awrs.index_page.suppliers_text", SectionComplete),
    SectionModel("directorsAndCompanySecretaries", "/alcohol-wholesale-scheme/business-directors", "awrs.index_page.business_directors.index_text", SectionComplete))
  )

  lazy val invalidSoleTraderCachemap = CacheMap(testUtr, Map("businessCustomerDetails" -> Json.toJson(testReviewDetails),
    "corporateBodyBusinessDetails" -> Json.toJson(testCorporateBodyBusinessDetails),
    "additionalBusinessPremises" -> Json.toJson(testAdditionalPremisesList),
    tradingActivityName -> Json.toJson(testTradingActivity()),
    productsName -> Json.toJson(testProducts()),
    "suppliers" -> Json.toJson(testSupplierAddressList)
  ))

  def getWithAuthorisedUserCt(test: Future[Result] => Any) {
    setupMockKeyStoreService(subscriptionStatusType = testSubscriptionStatusTypePending)
    setupMockSave4LaterService(
      fetchBusinessCustomerDetails = testReviewDetails,
      fetchAll = returnedCachemap
    )
    setupMockIndexService()
    setAuthMocks()
    val result = testIndexController.showIndex.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithAuthorisedUserCtWithStatus(testSubscriptionStatusType: SubscriptionStatusType)(test: Future[Result] => Any) {
    // redirection is based on what is currently stored in the session variable AwrsController.sessionStatusType ("status")
    val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withFormUrlEncodedBody("" -> "").withSession((AwrsSessionKeys.sessionStatusType, testSubscriptionStatusType.formBundleStatus.name))

    setupMockSave4LaterService(
      fetchBusinessCustomerDetails = testReviewDetails,
      fetchAll = invalidSoleTraderCachemap
    )
    setupMockKeyStoreService(subscriptionStatusType = testSubscriptionStatusType)
    setupMockIndexService(showContinueButton = false)
    setupMockApplicationService(hasAPI5ApplicationChanged = false)
    setAuthMocks()
    val result = testIndexController.showIndex.apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, "SOP"))
    test(result)
  }

  def getWithAuthorisedUserCtWithStatusPending =
    getWithAuthorisedUserCtWithStatus(testSubscriptionStatusTypePending)(_)

  def getWithAuthorisedUserCtWithStatusApproved =
    getWithAuthorisedUserCtWithStatus(testSubscriptionStatusTypeApproved)(_)

  def getWithAuthorisedUserCtWithStatusApprovedWithConditions =
    getWithAuthorisedUserCtWithStatus(testSubscriptionStatusTypeApprovedWithConditions)(_)

  def getWithAuthorisedUserCtWithStatusRejected =
    getWithAuthorisedUserCtWithStatus(testSubscriptionStatusTypeRejected)(_)

  def getWithUnAuthenticated(test: Future[Result] => Any) {
    val result = testIndexController.showIndex.apply(SessionBuilder.buildRequestWithSessionNoUser())
    test(result)
  }
}
