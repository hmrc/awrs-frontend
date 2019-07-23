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
import config.FrontendAuthConnector
import connectors.mock.MockAuthConnector
import forms.AWRSEnums
import models.{ApplicationStatus, BusinessCustomerDetails}
import org.joda.time.LocalDateTime
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.libs.json.JsResultException
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.BusinessCustomerService
import services.mocks.MockSave4LaterService
import utils.AwrsUnitTestTraits
import utils.TestUtil._

import scala.concurrent.Future


class HomeControllerTest extends AwrsUnitTestTraits
  with MockAuthConnector
  with MockSave4LaterService {

  val request = FakeRequest()
  val mockBusinessCustomerService = mock[BusinessCustomerService]

  object TestHomeController extends HomeController {
    override val authConnector = mockAuthConnector
    override val businessCustomerService = mockBusinessCustomerService
    override val save4LaterService = TestSave4LaterService

  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockBusinessCustomerService)
  }

  "HomeController" should {
    "use the correct AuthConnector" in {
      HomeController.authConnector shouldBe FrontendAuthConnector
    }

    "redirect to the Business Type page if the save4Later review details are present but the user does not have an AWRS enrolment" in {
      showWithSave4Later() { result =>
        status(result) shouldBe 303
        redirectLocation(result).get should include("/alcohol-wholesale-scheme/business-type")
      }
    }

    "redirect to the Business customer matching if the save4Later review details are present WITHOUT SAFEID and the user does not have an AWRS enrolment" in {
      showWithSave4LaterWithoutSafeId() { result =>
        status(result) shouldBe 303
        redirectLocation(result).get should include("http://localhost:9923/business-customer/awrs")
      }
    }


    "redirect to the Business Type page if the save4Later review details are present but the user does not have an AWRS enrolment and they came from BTA" in {
      showWithSave4Later(callerId = Some("BTA")) { result =>
        status(result) shouldBe 303
        redirectLocation(result).get should include("/alcohol-wholesale-scheme/business-type")
      }
    }

    "redirect to the Business Type page if the save4Later review details are not present but the keystore review details are present and the user does not have an AWRS enrolment" in {
      showWithKeystore { result =>
        status(result) shouldBe 303
        redirectLocation(result).get should include("/alcohol-wholesale-scheme/business-type")
      }
    }

    "redirect to the Business Type page if the save4Later review details are present and the user has an AWRS enrolment" in {
      showWithSave4LaterAndAwrs() { result =>
        status(result) shouldBe 303
        redirectLocation(result).get shouldBe "/alcohol-wholesale-scheme/business-type"
      }
    }

    "redirect to the Business Type page if the save4Later review details are not present but the keystore review details is present and the user has an AWRS enrolment" in {
      showWithKeystoreAndAwrs { result =>
        status(result) shouldBe 303
        redirectLocation(result).get shouldBe "/alcohol-wholesale-scheme/business-type"
      }
    }

    "redirect to the Business Customer Frontend if the save4Later and keystore review details are not present" in {
      showWithoutKeystore { result =>
        status(result) shouldBe 303
        redirectLocation(result).get should include("/business-customer/awrs")
      }
    }

    "redirect to the Business Type page if the save4Later review details are present and the user came from BTA" in {
      showWithSave4LaterAndAwrs(Some("BTA")) { result =>
        status(result) shouldBe 303
        redirectLocation(result).get shouldBe "/alcohol-wholesale-scheme/business-type"
      }
    }

    "show error page if a runtime error is produced" in {
      showWithException() { result =>
        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("application-error-header").text() should be("Sorry, we are experiencing technical difficulties")
      }
    }

    "redirect to Business Type page if for AWRS Registered users JSResultException produced" in {
      showWithJsResultExceptionAndAwrs() { result =>
        status(result) shouldBe 303
        redirectLocation(result).get shouldBe "/alcohol-wholesale-scheme/business-type"
      }
    }

    "redirect to Business Type page if for Non reistered AWRS users JSResultException produced" in {
      showWithJsResultException() { result =>
        status(result) shouldBe 303
        redirectLocation(result).get shouldBe "/alcohol-wholesale-scheme/business-type"
      }
    }

    "show recent withdrawal error page if the user has withdrawn within 24 hours" in {
      showWithException(testApplicationStatus()) { result =>
        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("application-error-header").text() should be(Messages("awrs.generic.wait_info",Messages("awrs.generic.wait_info_withdraw")))
      }
    }

    "show recent re-registration error page if the user has de-registered within 24 hours" in {
      showWithException(testApplicationStatus(AWRSEnums.ApplicationStatusEnum.DeRegistered)) { result =>
        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("application-error-header").text() should be(Messages("awrs.generic.wait_info",Messages("awrs.generic.wait_info_de-registration")))
      }
    }

    "redirect to the business type page if the user has withdrawn more than 24 hours ago" in {
      showWithSave4Later(testApplicationStatus(updatedDate = LocalDateTime.now().minusHours(25))) { result =>
        status(result) shouldBe 303
        redirectLocation(result).get should include("/alcohol-wholesale-scheme/business-type")
      }
    }

  }

  private def showWithSave4Later(applicationStatus: Option[ApplicationStatus] = None, callerId: Option[String] = None)(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = testBusinessCustomerDetails("SOP"), fetchApplicationStatus = applicationStatus)
    val result = TestHomeController.showOrRedirect(callerId).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }
  private def showWithSave4LaterWithoutSafeId(applicationStatus: Option[ApplicationStatus] = None, callerId: Option[String] = None)(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = testBusinessCustomerDetailsWithoutSafeID("SOP"), fetchApplicationStatus = applicationStatus)
    val result = TestHomeController.showOrRedirect(callerId).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }


  private def showWithKeystore(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = None, fetchApplicationStatus = None)
    when(mockBusinessCustomerService.getReviewBusinessDetails[BusinessCustomerDetails](Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(testBusinessCustomerDetails("SOP"))))
    val result = TestHomeController.showOrRedirect().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def showWithSave4LaterAndAwrs(callerId: Option[String] = None)(test: Future[Result] => Any) {
    setUser(hasAwrs = true)
    setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = testBusinessCustomerDetails("SOP"), fetchApplicationStatus = None)
    val result = TestHomeController.showOrRedirect(callerId).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def showWithKeystoreAndAwrs(test: Future[Result] => Any) {
    setUser(hasAwrs = true)
    setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = None, fetchApplicationStatus = None)
    when(mockBusinessCustomerService.getReviewBusinessDetails[BusinessCustomerDetails](Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(testBusinessCustomerDetails("SOP"))))
    val result = TestHomeController.showOrRedirect().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def showWithoutKeystore(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = None, fetchApplicationStatus = None)
    when(mockBusinessCustomerService.getReviewBusinessDetails[BusinessCustomerDetails](Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
    val result = TestHomeController.showOrRedirect().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def showWithException(applicationStatus: Option[ApplicationStatus] = None)(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = None, fetchApplicationStatus = applicationStatus)
    when(mockBusinessCustomerService.getReviewBusinessDetails[BusinessCustomerDetails](Matchers.any(), Matchers.any())).thenReturn(Future.failed(new RuntimeException("An error occurred")))
    val result = TestHomeController.showOrRedirect().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def showWithJsResultExceptionAndAwrs(applicationStatus: Option[ApplicationStatus] = None)(test: Future[Result] => Any) {
    setUser(hasAwrs = true)
    setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = None, fetchApplicationStatus =  applicationStatus)
    when(mockMainStoreSave4LaterConnector.fetchData4Later[ApplicationStatus](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.failed(new JsResultException(Nil)),Future.successful(None))
    val result = TestHomeController.showOrRedirect().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def showWithJsResultException(applicationStatus: Option[ApplicationStatus] = None)(test: Future[Result] => Any) {
    setUser(hasAwrs = false)
    setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = testBusinessCustomerDetails("SOP"), fetchApplicationStatus = applicationStatus)
    when(mockMainStoreSave4LaterConnector.fetchData4Later[ApplicationStatus](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.failed(new JsResultException(Nil)),Future.successful(applicationStatus))
    when(mockBusinessCustomerService.getReviewBusinessDetails[BusinessCustomerDetails](Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(testBusinessCustomerDetails("SOP"))))
    val result = TestHomeController.showOrRedirect().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }
}
