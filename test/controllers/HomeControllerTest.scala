/*
 * Copyright 2021 HM Revenue & Customs
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
import forms.AWRSEnums
import models.{ApplicationStatus, BusinessCustomerDetails}
import org.joda.time.LocalDateTime
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.libs.json.JsResultException
import play.api.mvc.Result
import play.api.test.Helpers._
import services.mocks.MockSave4LaterService
import services.{BusinessCustomerService, CheckEtmpService}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import utils.AwrsUnitTestTraits
import utils.TestUtil._
import views.html.awrs_application_too_soon_error

import scala.concurrent.Future


class HomeControllerTest extends AwrsUnitTestTraits
  with MockAuthConnector
  with MockSave4LaterService {

  val mockBusinessCustomerService: BusinessCustomerService = mock[BusinessCustomerService]
  val mockCheckEtmpService: CheckEtmpService = mock[CheckEtmpService]

  val tooSoonError: awrs_application_too_soon_error = app.injector.instanceOf[awrs_application_too_soon_error]

  val testHomeController: HomeController = new HomeController(mockMCC, mockBusinessCustomerService
    , mockDeEnrolService, mockAuthConnector, mockAuditable, mockAccountUtils, testSave4LaterService, mockAppConfig, tooSoonError) {
    override val signInUrl: String = applicationConfig.signIn
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockBusinessCustomerService, mockAppConfig, mockAccountUtils)

    when(mockAppConfig.countryCodes)
      .thenReturn(mockCountryCodes)
    when(mockAppConfig.templateAppError)
      .thenReturn(mockAppError)
    when(mockCountryCodes.countries)
      .thenReturn(
        """[
          |"United Kingdom"
          |]""".stripMargin)
  }

  "HomeController" must {

    "redirect to the Business Type page if the save4Later review details are present but the user does not have an AWRS enrolment" in {
      when(mockCheckEtmpService.validateBusinessDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(false))
      showWithSave4Later() { result =>
        status(result) mustBe 303
        redirectLocation(result).get must include("/alcohol-wholesale-scheme/business-type")
      }
    }

    "redirect to the Business customer matching if the save4Later review details are present WITHOUT SAFEID and the user does not have an AWRS enrolment" in {
      when(mockAppConfig.businessCustomerStartPage)
        .thenReturn("http://localhost:9923/business-customer/awrs")

      showWithSave4LaterWithoutSafeId() { result =>
        status(result) mustBe 303
        redirectLocation(result).get must include("http://localhost:9923/business-customer/awrs")
      }
    }


    "redirect to the Business Type page if the save4Later review details are present but the user does not have an AWRS enrolment and they came from BTA" in {
      showWithSave4Later(callerId = Some("BTA")) { result =>
        status(result) mustBe 303
        redirectLocation(result).get must include("/alcohol-wholesale-scheme/business-type")
      }
    }

    "redirect to the Business Type page if the save4Later review details are not present but the keystore review details are present and the user does not have an AWRS enrolment" in {
      showWithKeystore { result =>
        status(result) mustBe 303
        redirectLocation(result).get must include("/alcohol-wholesale-scheme/business-type")
      }
    }

    "redirect to the Business Type page if the save4Later review details are present and the user has an AWRS enrolment" in {
      showWithSave4LaterAndAwrs() { result =>
        status(result) mustBe 303
        redirectLocation(result).get mustBe "/alcohol-wholesale-scheme/business-type"
      }
    }

    "redirect to the Business Type page if the save4Later review details are not present but the keystore review details is present and the user has an AWRS enrolment" in {
      showWithKeystoreAndAwrs { result =>
        status(result) mustBe 303
        redirectLocation(result).get mustBe "/alcohol-wholesale-scheme/business-type"
      }
    }

    "redirect to the Business Customer Frontend if the save4Later and keystore review details are not present" in {
      when(mockAppConfig.businessCustomerStartPage)
        .thenReturn("http://localhost:9923/business-customer/awrs")

      showWithoutKeystore { result =>
        status(result) mustBe 303
        redirectLocation(result).get must include("/business-customer/awrs")
      }
    }

    "redirect to the Business Type page if the save4Later review details are present and the user came from BTA" in {
      showWithSave4LaterAndAwrs(Some("BTA")) { result =>
        status(result) mustBe 303
        redirectLocation(result).get mustBe "/alcohol-wholesale-scheme/business-type"
      }
    }

    "show error page if a runtime error is produced" in {
      showWithException() { result =>
        status(result) mustBe 500
      }
    }

    "redirect to Business Type page if for AWRS Registered users JSResultException produced" in {
      showWithJsResultExceptionAndAwrs() { result =>
        status(result) mustBe 303
        redirectLocation(result).get mustBe "/alcohol-wholesale-scheme/business-type"
      }
    }

    "redirect to Business Type page if for Non reistered AWRS users JSResultException produced" in {
      showWithJsResultException() { result =>
        status(result) mustBe 303
        redirectLocation(result).get mustBe "/alcohol-wholesale-scheme/business-type"
      }
    }

    "show recent withdrawal error page if the user has withdrawn within 24 hours" in {
      showWithException(testApplicationStatus()) { result =>
        val document = Jsoup.parse(contentAsString(result))
        document.select("#application-error-header").text() must be(Messages("awrs.generic.wait_info",Messages("awrs.generic.wait_info_withdraw")))
      }
    }

    "show recent re-registration error page if the user has de-registered within 24 hours" in {
      showWithException(testApplicationStatus(AWRSEnums.ApplicationStatusEnum.DeRegistered)) { result =>
        val document = Jsoup.parse(contentAsString(result))
        document.select("#application-error-header").text() must be(Messages("awrs.generic.wait_info",Messages("awrs.generic.wait_info_de-registration")))
      }
    }

    "redirect to the business type page if the user has withdrawn more than 24 hours ago" in {
      showWithSave4Later(testApplicationStatus(updatedDate = LocalDateTime.now().minusHours(25))) { result =>
        status(result) mustBe 303
        redirectLocation(result).get must include("/alcohol-wholesale-scheme/business-type")
      }
    }

    "redirect to the Business Type page if validate business details returns true" in {
      when(mockCheckEtmpService.validateBusinessDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(true))
      showWithSave4Later() { result =>
        status(result) mustBe 303
        redirectLocation(result).get must include("/alcohol-wholesale-scheme/business-type")
      }
    }
  }

  private def showWithSave4Later(applicationStatus: Option[ApplicationStatus] = None, callerId: Option[String] = None)(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = testBusinessCustomerDetails("SOP"), fetchApplicationStatus = applicationStatus)
    setAuthMocks()
    val result = testHomeController.showOrRedirect(callerId).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }
  private def showWithSave4LaterWithoutSafeId(applicationStatus: Option[ApplicationStatus] = None, callerId: Option[String] = None)(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = testBusinessCustomerDetailsWithoutSafeID("SOP"), fetchApplicationStatus = applicationStatus)
    setAuthMocks(Future.successful(new ~( new ~(Enrolments(Set(Enrolment("IR-CT", Seq(EnrolmentIdentifier("utr", "0123456")), "activated"))), Some(AffinityGroup.Organisation)), Credentials("fakeCredID", "type"))))
    val result = testHomeController.showOrRedirect(callerId).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }


  private def showWithKeystore(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = None, fetchApplicationStatus = None)
    when(mockBusinessCustomerService.getReviewBusinessDetails[BusinessCustomerDetails](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testBusinessCustomerDetails("SOP"))))
    setAuthMocks()
    val result = testHomeController.showOrRedirect().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def showWithSave4LaterAndAwrs(callerId: Option[String] = None)(test: Future[Result] => Any) {
    resetAuthConnector()
    setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = testBusinessCustomerDetails("SOP"), fetchApplicationStatus = None)
    setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
    val result = testHomeController.showOrRedirect(callerId).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def showWithKeystoreAndAwrs(test: Future[Result] => Any) {
    resetAuthConnector()
    setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = None, fetchApplicationStatus = None)
    when(mockBusinessCustomerService.getReviewBusinessDetails[BusinessCustomerDetails](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testBusinessCustomerDetails("SOP"))))
    setAuthMocks()
    val result = testHomeController.showOrRedirect().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def showWithoutKeystore(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = None, fetchApplicationStatus = None)
    when(mockBusinessCustomerService.getReviewBusinessDetails[BusinessCustomerDetails](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
    setAuthMocks(Future.successful(
      new ~( new ~(Enrolments(Set(Enrolment("IR-CT", Seq(EnrolmentIdentifier("UTR", "0123456")), "activated"))), Some(AffinityGroup.Organisation)), Credentials("fakeCredID", "type"))))
    val result = testHomeController.showOrRedirect().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def showWithException(applicationStatus: Option[ApplicationStatus] = None)(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = None, fetchApplicationStatus = applicationStatus)
    when(mockBusinessCustomerService.getReviewBusinessDetails[BusinessCustomerDetails](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.failed(new RuntimeException("An error occurred")))
    setAuthMocks(Future.successful(new ~( new ~(Enrolments(Set(Enrolment("IR-CT", Seq(EnrolmentIdentifier("utr", "0123456")), "activated"))), Some(AffinityGroup.Organisation)), Credentials("fakeCredID", "type"))))
    val result = testHomeController.showOrRedirect().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def showWithJsResultExceptionAndAwrs(applicationStatus: Option[ApplicationStatus] = None)(test: Future[Result] => Any) {
    resetAuthConnector()
    setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = None, fetchApplicationStatus =  applicationStatus)
    when(mockMainStoreSave4LaterConnector.fetchData4Later[ApplicationStatus](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.failed(new JsResultException(Nil)),Future.successful(None))
    when(mockBusinessCustomerService.getReviewBusinessDetails[BusinessCustomerDetails](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testBusinessCustomerDetails("SOP"))))
    setAuthMocks()
    val result = testHomeController.showOrRedirect().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def showWithJsResultException(applicationStatus: Option[ApplicationStatus] = None)(test: Future[Result] => Any) {
    resetAuthConnector()
    setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = testBusinessCustomerDetails("SOP"), fetchApplicationStatus = applicationStatus)
    when(mockMainStoreSave4LaterConnector.fetchData4Later[ApplicationStatus](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.failed(new JsResultException(Nil)),Future.successful(applicationStatus))
    when(mockBusinessCustomerService.getReviewBusinessDetails[BusinessCustomerDetails](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testBusinessCustomerDetails("SOP"))))
    setAuthMocks()
    val result = testHomeController.showOrRedirect().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }
}
