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
import connectors.BusinessCustomerCacheConnector
import connectors.mock.MockAuthConnector
import forms.AWRSEnums
import models.{ApplicationStatus, BusinessCustomerDetails}
import java.time.LocalDateTime
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.libs.json.JsResultException
import play.api.test.Helpers._
import services.mocks.MockSave4LaterService
import services.CheckEtmpService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import utils.AwrsUnitTestTraits
import utils.TestUtil._
import views.html.{assistant_kickout, awrs_application_too_soon_error}

import scala.concurrent.Future


class HomeControllerTest extends AwrsUnitTestTraits
  with MockAuthConnector
  with MockSave4LaterService {

  lazy val mockBusinessCustomerCacheConnector: BusinessCustomerCacheConnector = mock[BusinessCustomerCacheConnector]
  lazy val mockCheckEtmpService: CheckEtmpService = mock[CheckEtmpService]

  lazy val tooSoonError: awrs_application_too_soon_error = app.injector.instanceOf[awrs_application_too_soon_error]
  lazy val assistantKickoutView: assistant_kickout = app.injector.instanceOf[assistant_kickout]

  trait TestHomeControllerEnv {
    val testHomeController: HomeController = new HomeController(mockMCC, mockBusinessCustomerCacheConnector,
      mockDeEnrolService, mockCheckEtmpService, mockAuthConnector, mockAuditable, mockAccountUtils, testSave4LaterService,
      mockAppConfig, tooSoonError, assistantKickoutView) {
      override val signInUrl: String = applicationConfig.signIn
    }
  }

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockBusinessCustomerCacheConnector)
    reset(mockCheckEtmpService)

    when(mockCheckEtmpService.checkUsersEnrolments(any(), any())(any(),any()))
      .thenReturn(Future.successful(None))
  }

  "HomeController" must {

    "redirect to the wrong account page when an AWRS enrolment is found for the current cred ID" in new TestHomeControllerEnv {
      when(mockCheckEtmpService.validateBusinessDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(false))
      when(mockCheckEtmpService.checkUsersEnrolments(any(), any())(any(),any()))
        .thenReturn(Future.successful(Some(true)))

      setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = testBusinessCustomerDetails("test-legal-entity"), fetchApplicationStatus = None)
      setAuthMocks(Future.successful(
        new ~(new ~( new ~(Enrolments(Set(Enrolment("IR-CT", Seq(EnrolmentIdentifier("UTR", "0123456")), "activated"))), Some(AffinityGroup.Organisation)), Credentials("fakeCredID", "type")), Some(User))))
      val result = testHomeController.showOrRedirect(None).apply(SessionBuilder.buildRequestWithSession(userId))
      status(result) mustBe 303
      redirectLocation(result).get must include("/alcohol-wholesale-scheme/wrong-account")
    }

    "redirect to the Business Type page if the save4Later review details are present but the user does not have an AWRS enrolment" in new TestHomeControllerEnv {
      setAuthMocks()
      when(mockCheckEtmpService.validateBusinessDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(false))

      setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = testBusinessCustomerDetails("SOP"), fetchApplicationStatus = None)
      setAuthMocks()

      val result = testHomeController.showOrRedirect(None).apply(SessionBuilder.buildRequestWithSession(userId))
      status(result) mustBe 303
      redirectLocation(result).get must include("/alcohol-wholesale-scheme/business-type")
    }

    "redirect to the Assistant kickout page if the user is an Assistant who does not have an AWRS enrolment" in new TestHomeControllerEnv {

      setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = testBusinessCustomerDetails("SOP"), fetchApplicationStatus = None)
      setAuthMocks(Future.successful(
        new ~(new ~( new ~(Enrolments(Set(Enrolment("IR-CT", Seq(EnrolmentIdentifier("UTR", "0123456")), "activated"))), Some(AffinityGroup.Organisation)), Credentials("fakeCredID", "type")), Some(Assistant))))

      val result = testHomeController.showOrRedirect(None).apply(SessionBuilder.buildRequestWithSession(userId))
      status(result) mustBe 403
      val document = Jsoup.parse(contentAsString(result))
      document.select("#application-error-header").text() must be(Messages("awrs.generic.assistant_kickout.title"))
    }

    "redirect to the Unauthorised kickout page if the user is an Individual with no UTR" in new TestHomeControllerEnv {
      setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = testBusinessCustomerDetails("SOP"), fetchApplicationStatus = None)
      when(mockAccountUtils.getUtr(ArgumentMatchers.any()))
        .thenThrow(new UnsupportedAffinityGroup(s"[getUtr] No UTR found and affinity group was Individual"))
      setAuthMocks(Future.successful(
        new ~(new ~( new ~(Enrolments(Set(Enrolment("IR-CT", Seq.empty, "activated"))), Some(AffinityGroup.Individual)), Credentials("fakeCredID", "type")), Some(User))), mockAccountUtils)

      val result = testHomeController.showOrRedirect(None).apply(SessionBuilder.buildRequestWithSession(userId))
      status(result) mustBe 401
      val document = Jsoup.parse(contentAsString(result))
      document.select("#heading").text() must be(Messages("awrs.generic.unauthorised.heading"))
    }

    "redirect to the Business Type page if the save4Later review details are present and the user is an Assistant with an AWRS enrolment" in new TestHomeControllerEnv {
      resetAuthConnector()
      setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = testBusinessCustomerDetails("SOP"), fetchApplicationStatus = None)
      setAuthMocks(Future.successful(authResultDefault(Assistant)), Some(mockAccountUtils))

      val result = testHomeController.showOrRedirect(None).apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) mustBe 303
        redirectLocation(result).get mustBe "/alcohol-wholesale-scheme/business-type"
    }

    "redirect to the Business customer matching if the save4Later review details are present WITHOUT SAFEID and the user does not have an AWRS enrolment" in new TestHomeControllerEnv {
      when(mockAppConfig.businessCustomerStartPage)
        .thenReturn("http://localhost:9923/business-customer/awrs")
      when(mockBusinessCustomerCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = testBusinessCustomerDetailsWithoutSafeID("SOP"), fetchApplicationStatus = None)
      setAuthMocks(Future.successful(new ~(new ~( new ~(Enrolments(Set(Enrolment("IR-CT", Seq(EnrolmentIdentifier("utr", "0123456")), "activated"))), Some(AffinityGroup.Organisation)), Credentials("fakeCredID", "type")), Some(User))))

      val result = testHomeController.showOrRedirect(None).apply(SessionBuilder.buildRequestWithSession(userId))
      status(result) mustBe 303
      redirectLocation(result).get must include("http://localhost:9923/business-customer/awrs")
    }

    "redirect to the Business type page if the save4Later review details are present WITHOUT SAFEID, but details in keystore contain safeID" in new TestHomeControllerEnv {
      when(mockAppConfig.businessCustomerStartPage)
        .thenReturn("http://localhost:9923/business-customer/awrs")
      when(mockBusinessCustomerCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(testBusinessCustomerDetails("SOP"))))

      setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = testBusinessCustomerDetailsWithoutSafeID("SOP"), fetchApplicationStatus = None)
      setAuthMocks(Future.successful(new ~(new ~( new ~(Enrolments(Set(Enrolment("IR-CT", Seq(EnrolmentIdentifier("utr", "0123456")), "activated"))), Some(AffinityGroup.Organisation)), Credentials("fakeCredID", "type")), Some(User))))

      val result = testHomeController.showOrRedirect(None).apply(SessionBuilder.buildRequestWithSession(userId))
      status(result) mustBe 303
      redirectLocation(result).get mustBe "/alcohol-wholesale-scheme/business-type"
    }

    "redirect to the Business Type page if the save4Later review details are present but the user does not have an AWRS enrolment and they came from BTA" in new TestHomeControllerEnv {
      setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = testBusinessCustomerDetails("SOP"), fetchApplicationStatus = None)
      setAuthMocks()

      val result = testHomeController.showOrRedirect(Some("BTA")).apply(SessionBuilder.buildRequestWithSession(userId))
      status(result) mustBe 303
      redirectLocation(result).get must include("/alcohol-wholesale-scheme/business-type")
    }

    "redirect to the Business Type page if the save4Later review details are not present but the keystore review details are present and the user does not have an AWRS enrolment" in new TestHomeControllerEnv {
      setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = None, fetchApplicationStatus = None)
      when(mockBusinessCustomerCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testBusinessCustomerDetails("SOP"))))
      setAuthMocks()

      val result = testHomeController.showOrRedirect().apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) mustBe 303
        redirectLocation(result).get must include("/alcohol-wholesale-scheme/business-type")
    }

    "redirect to the Business Type page if the save4Later review details are present and the user has an AWRS enrolment" in new TestHomeControllerEnv {
      resetAuthConnector()
      setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = testBusinessCustomerDetails("SOP"), fetchApplicationStatus = None)
      setAuthMocks(mockAccountUtils = Some(mockAccountUtils))

      val result = testHomeController.showOrRedirect(None).apply(SessionBuilder.buildRequestWithSession(userId))
      status(result) mustBe 303
      redirectLocation(result).get mustBe "/alcohol-wholesale-scheme/business-type"
    }

    "redirect to the Business Type page if the save4Later review details are not present but the keystore review details is present and the user has an AWRS enrolment" in new TestHomeControllerEnv {

      resetAuthConnector()
      setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = None, fetchApplicationStatus = None)
      when(mockBusinessCustomerCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testBusinessCustomerDetails("SOP"))))
      setAuthMocks()

      val result = testHomeController.showOrRedirect().apply(SessionBuilder.buildRequestWithSession(userId))
      status(result) mustBe 303
      redirectLocation(result).get mustBe "/alcohol-wholesale-scheme/business-type"
    }

    "redirect to the Business Customer Frontend if the save4Later and keystore review details are not present" in new TestHomeControllerEnv {
      when(mockAppConfig.businessCustomerStartPage)
        .thenReturn("http://localhost:9923/business-customer/awrs")

      setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = None, fetchApplicationStatus = None)
      when(mockBusinessCustomerCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
      setAuthMocks(Future.successful(
        new ~(new ~( new ~(Enrolments(Set(Enrolment("IR-CT", Seq(EnrolmentIdentifier("UTR", "0123456")), "activated"))), Some(AffinityGroup.Organisation)), Credentials("fakeCredID", "type")), Some(User))))

      val result = testHomeController.showOrRedirect().apply(SessionBuilder.buildRequestWithSession(userId))
      status(result) mustBe 303
      redirectLocation(result).get must include("/business-customer/awrs")
    }

    "redirect to the Business Type page if the save4Later review details are present and the user came from BTA" in new TestHomeControllerEnv {
      resetAuthConnector()
      setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = testBusinessCustomerDetails("SOP"), fetchApplicationStatus = None)
      setAuthMocks(mockAccountUtils = Some(mockAccountUtils))

      val result = testHomeController.showOrRedirect(Some("BTA")).apply(SessionBuilder.buildRequestWithSession(userId))
      status(result) mustBe 303
      redirectLocation(result).get mustBe "/alcohol-wholesale-scheme/business-type"
    }

    "show error page if a runtime error is produced" in new TestHomeControllerEnv {
      setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = None, fetchApplicationStatus = None)
      when(mockBusinessCustomerCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.failed(new RuntimeException("An error occurred")))
      setAuthMocks(Future.successful(new ~(new ~( new ~(Enrolments(Set(Enrolment("IR-CT", Seq(EnrolmentIdentifier("utr", "0123456")), "activated"))), Some(AffinityGroup.Organisation)), Credentials("fakeCredID", "type")), Some(User))))

      val result = testHomeController.showOrRedirect().apply(SessionBuilder.buildRequestWithSession(userId))
      status(result) mustBe 500
    }

    "redirect to Business Type page if for AWRS Registered users JSResultException produced" in new TestHomeControllerEnv {
      resetAuthConnector()
      setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = None, fetchApplicationStatus =  None)
      when(mockMainStoreSave4LaterConnector.fetchData4Later[ApplicationStatus](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.failed(new JsResultException(Nil)),Future.successful(None))
      when(mockBusinessCustomerCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testBusinessCustomerDetails("SOP"))))
      setAuthMocks()

      val result = testHomeController.showOrRedirect().apply(SessionBuilder.buildRequestWithSession(userId))
      status(result) mustBe 303
      redirectLocation(result).get mustBe "/alcohol-wholesale-scheme/business-type"
    }

    "redirect to Business Type page if for Non reistered AWRS users JSResultException produced" in new TestHomeControllerEnv {
      val applicationStatus = None

      resetAuthConnector()
      setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = testBusinessCustomerDetails("SOP"), fetchApplicationStatus = applicationStatus)
      when(mockMainStoreSave4LaterConnector.fetchData4Later[ApplicationStatus](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.failed(new JsResultException(Nil)),Future.successful(applicationStatus))
      when(mockBusinessCustomerCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testBusinessCustomerDetails("SOP"))))
      setAuthMocks()

      val result = testHomeController.showOrRedirect().apply(SessionBuilder.buildRequestWithSession(userId))
      status(result) mustBe 303
      redirectLocation(result).get mustBe "/alcohol-wholesale-scheme/business-type"
    }

    "show recent withdrawal error page if the user has withdrawn within 24 hours" in new TestHomeControllerEnv {
      setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = None, fetchApplicationStatus = testApplicationStatus())
      when(mockBusinessCustomerCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.failed(new RuntimeException("An error occurred")))
      setAuthMocks(Future.successful(new ~(new ~( new ~(Enrolments(Set(Enrolment("IR-CT", Seq(EnrolmentIdentifier("utr", "0123456")), "activated"))), Some(AffinityGroup.Organisation)), Credentials("fakeCredID", "type")), Some(User))))

      val result = testHomeController.showOrRedirect().apply(SessionBuilder.buildRequestWithSession(userId))
      val document = Jsoup.parse(contentAsString(result))
      document.select("#application-error-header").text() must be(Messages("awrs.generic.wait_info",Messages("awrs.generic.wait_info_withdraw")))
    }

    "show recent re-registration error page if the user has de-registered within 24 hours" in new TestHomeControllerEnv {
      setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = None, fetchApplicationStatus = testApplicationStatus(AWRSEnums.ApplicationStatusEnum.DeRegistered))
      when(mockBusinessCustomerCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.failed(new RuntimeException("An error occurred")))
      setAuthMocks(Future.successful(new ~(new ~( new ~(Enrolments(Set(Enrolment("IR-CT", Seq(EnrolmentIdentifier("utr", "0123456")), "activated"))), Some(AffinityGroup.Organisation)), Credentials("fakeCredID", "type")), Some(User))))

      val result = testHomeController.showOrRedirect().apply(SessionBuilder.buildRequestWithSession(userId))
      val document = Jsoup.parse(contentAsString(result))
      document.select("#application-error-header").text() must be(Messages("awrs.generic.wait_info",Messages("awrs.generic.wait_info_de-registration")))
    }

    "redirect to the business type page if the user has withdrawn more than 24 hours ago" in new TestHomeControllerEnv {
      setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = testBusinessCustomerDetails("SOP"),
        fetchApplicationStatus = testApplicationStatus(updatedDate = LocalDateTime.now().minusHours(25))
      )
      setAuthMocks()

      val result = testHomeController.showOrRedirect(None).apply(SessionBuilder.buildRequestWithSession(userId))
      status(result) mustBe 303
      redirectLocation(result).get must include("/alcohol-wholesale-scheme/business-type")
    }

    "redirect to the Business Type page if validate business details returns true" in new TestHomeControllerEnv {
      when(mockCheckEtmpService.validateBusinessDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(true))

      setupMockSave4LaterServiceWithOnly(fetchBusinessCustomerDetails = testBusinessCustomerDetails("SOP"), fetchApplicationStatus = None)
      setAuthMocks()

      val result = testHomeController.showOrRedirect(None).apply(SessionBuilder.buildRequestWithSession(userId))
      status(result) mustBe 303
      redirectLocation(result).get must include("/alcohol-wholesale-scheme/business-type")
    }
  }
}
