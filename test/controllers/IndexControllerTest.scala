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
import models.BusinessCustomerDetails
import org.mockito.Mockito._
import play.api.libs.json.JsValue
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services._
import models.CacheMap
import utils.AwrsSessionKeys
import utils.TestUtil._
import views.html.awrs_index

import scala.concurrent.Future

class IndexControllerTest extends ServicesUnitTestFixture {

  lazy val cachemap: CacheMap = CacheMap("", Map[String, JsValue]())

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockApplicationService)
    reset(mockAccountUtils)
  }

  val template: awrs_index = app.injector.instanceOf[views.html.awrs_index]

  lazy val testIndexController: IndexController = new IndexController(mockMCC, mockIndexService, testAPI9,
    mockApplicationService, testSave4LaterService, mockDeEnrolService, mockAuthConnector, mockAuditable, mockAccountUtils, mockAppConfig, template) {
    override val signInUrl = "/sign-in"
  }

  "showIndex" must {

    "de-enrol the user and redirect to business customer frontend" when {
      "the user has an enrolment and the AWRS application status is Withdrawn" in {
        setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
        when(mockAppConfig.businessCustomerStartPage).thenReturn("/business-customer/business-verification/awrs")
        val request = SessionBuilder.buildRequestWithSession(userId, "LTD")
          .withSession("status" -> "Withdrawal")
        val result = testIndexController.showIndex().apply(request)

        redirectLocation(result).get mustBe "/business-customer/business-verification/awrs"
      }

      "the user has an enrolment and the AWRS application status is De-Registered" in {
        setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
        when(mockAppConfig.businessCustomerStartPage).thenReturn("/business-customer/business-verification/awrs")
        val request = SessionBuilder.buildRequestWithSession(userId, "LTD")
          .withSession("status" -> "De-Registered")
        val result = testIndexController.showIndex().apply(request)

        redirectLocation(result).get mustBe "/business-customer/business-verification/awrs"
      }
    }

    "display the index page" when {
      "businessType exists in session" in {
        val result = callShowIndex()
        status(result) mustBe OK
      }

      "the AWRS status in session is Withdrawal, businessType exists in session, but there is no enrolment" in {
        setupMockSave4LaterService(
          fetchBusinessCustomerDetails = testReviewDetails,
          fetchAll = Future.successful(None)
        )
        setupMockKeyStoreService(Future.successful(None))
        setupMockApplicationService(hasAPI5ApplicationChanged = false)
        setupMockIndexService()
        mockAuthNoEnrolment

        val request = SessionBuilder.buildRequestWithSession(userId, "LTD")
          .withSession("status" -> "Withdrawal")
        val result = testIndexController.showIndex().apply(request)

        status(result) mustBe OK
      }
    }

    "redirect back to the home controller" when {
      "business type is missing from session" in {
        val result = callShowIndex(businessType = None)
        status(result) mustBe SEE_OTHER

        redirectLocation(result).get mustBe "/alcohol-wholesale-scheme"
      }
      "business name is missing from session" in {
        val result = callShowIndex(reviewDetails = None)
        status(result) mustBe SEE_OTHER

        redirectLocation(result).get mustBe "/alcohol-wholesale-scheme"
      }
    }

    "clear sessionJourneyStartLocation from session when index is shown" in {
      val result = callShowIndex(startSection = testInitSessionJouneyStartLocation)
      val responseSessionMap = await(result).session(FakeRequest()).data
      // the session variable for sessionJourneyStartLocation in the response must have been removed
      responseSessionMap.get(AwrsSessionKeys.sessionJouneyStartLocation) mustBe None
    }

  }

  "showLastLocation" must {
    "route to the previous page if it exists in session" in {
      val previousLoc = "/alcohol-wholesale-scheme/supplier-addresses/edit"
      callShowLastLocationWith(previousLocation = previousLoc) {
        result =>
          redirectLocation(result).get mustBe previousLoc
      }
    }
    "route to index page if it exists in session" in {
      callShowLastLocationWith(previousLocation = None) {
        result =>
          redirectLocation(result).get mustBe "/alcohol-wholesale-scheme/index"
      }
    }
  }

  private def callShowLastLocationWith(previousLocation: Option[String])(test: Future[Result] => Any) : Unit ={
    setAuthMocks()
    val result = testIndexController.showLastLocation().apply(SessionBuilder.buildRequestWithSession(userId, "SOP", previousLocation))
    test(result)
  }

  val testInitSessionJouneyStartLocation = "Some location"

  private def callShowIndex(businessType: Option[String] = "SOP", startSection: Option[String] = None, reviewDetails: Option[BusinessCustomerDetails] = Some(testReviewDetails)): Future[Result] = {
    setupMockSave4LaterService(
      fetchBusinessCustomerDetails = Future.successful(reviewDetails),
      fetchAll = Future.successful(None)
    )

    setupMockAwrsAPI9(keyStore = testSubscriptionStatusTypePending, connector = DoNotConfigure, reviewDetails.isDefined)
    setupMockApplicationService(hasAPI5ApplicationChanged = false)
    setupMockIndexService()
    setAuthMocks()

    val request = businessType match {
      case Some(bt) => SessionBuilder.buildRequestWithSession(userId, bt)
      case _ => SessionBuilder.buildRequestWithSession(userId)
    }
    val requestWithStart = startSection match {
      case Some(definedSection) => request.withSession(request.session.+((AwrsSessionKeys.sessionJouneyStartLocation, definedSection)).data.toSeq: _*)
      case _ => request
    }
    testIndexController.showIndex().apply(requestWithStart)
  }

}
