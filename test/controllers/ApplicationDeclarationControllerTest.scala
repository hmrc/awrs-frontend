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

import audit.TestAudit
import builders.SessionBuilder
import config.FrontendAuthConnector
import connectors.mock.MockAuthConnector
import exceptions._
import forms.ApplicationDeclarationForm
import models.FormBundleStatus._
import models._
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.{Configuration, Play}
import play.api.Mode.Mode
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services._
import services.mocks.{MockKeyStoreService, MockSave4LaterService}
import uk.gov.hmrc.play.audit.model.Audit
import utils.TestUtil._
import utils.{AwrsSessionKeys, AwrsUnitTestTraits, TestUtil}

import scala.concurrent.Future
import uk.gov.hmrc.http.HttpResponse

class ApplicationDeclarationControllerTest extends AwrsUnitTestTraits
  with MockAuthConnector
  with MockSave4LaterService
  with MockKeyStoreService {

  val request = FakeRequest()
  val mockEnrolService = mock[EnrolService]
  val mockApplicationService = mock[ApplicationService]
  val mockAudit = mock[Audit]

  val formId = "applicationDeclaration"

  val subscribeSuccessResponse = SuccessfulSubscriptionResponse(processingDate = "2001-12-17T09:30:47Z", awrsRegistrationNumber = "ABCDEabcde12345", etmpFormBundleNumber = "123456789012345")
  val subscribeUpdateSuccessResponse = SuccessfulUpdateSubscriptionResponse(processingDate = "2001-12-17T09:30:47Z", etmpFormBundleNumber = "123456789012345")
  val enrolSuccessResponse = EnrolResponse("serviceName", "state", identifiers = List(Identifier("AWRS", "AWRS_Ref_No")))
  val testReviewDetails = BusinessCustomerDetails("ACME", Some("SOP"), BCAddress("line1", "line2", Option("line3"), Option("line4"), Option("postcode"), Option("country")), "sap123", "safe123", false, Some("agent123"))

  private def testRequest(declaration: ApplicationDeclaration) =
    TestUtil.populateFakeRequest[ApplicationDeclaration](FakeRequest(), ApplicationDeclarationForm.applicationDeclarationValidationForm, declaration)

  object TestApplicationDeclarationController extends ApplicationDeclarationController {
    override val authConnector = mockAuthConnector
    override val save4LaterService = TestSave4LaterService
    override val keyStoreService = TestKeyStoreService
    override val enrolService = mockEnrolService
    override val applicationService = mockApplicationService

    override protected def mode: Mode = Play.current.mode

    override protected def runModeConfiguration: Configuration = Play.current.configuration
  }

  object TestApplicationBusinessDirectorsController extends BusinessDirectorsController {
    override val authConnector = mockAuthConnector
    override val save4LaterService = TestSave4LaterService
  }

  "ApplicationDeclarationController" must {
    "show error page if a DES Validation Exception is encountered" in {
      saveWithException(testRequest(testApplicationDeclarationTrue), DESValidationException("Validation against schema failed")) { result =>
        await(result)
        val document = Jsoup.parse(contentAsString(result))
        document.getElementsByClass("page-header").text() should include(Messages("awrs.application_des_validation.heading"))
      }
    }

    "show error page if a GovernmentGatewayException is encountered" in {
      saveWithException(testRequest(testApplicationDeclarationTrue), GovernmentGatewayException("There was a problem with the admin service")) { result =>
        val document = Jsoup.parse(contentAsString(result))
        document.getElementsByClass("page-header").text() should include(Messages("awrs.application_government_gateway_error.heading"))
      }
    }

    "show error page if a DuplicateSubscriptionException is encountered" in {
      saveWithException(testRequest(testApplicationDeclarationTrue), DuplicateSubscriptionException("This subscription already exists")) { result =>
        val document = Jsoup.parse(contentAsString(result))
        document.getElementsByClass("page-header").text() should include(Messages("awrs.application_duplicate_request.heading"))
      }
    }

    "show error page if a PendingDeregistrationException is encountered" in {
      saveWithException(testRequest(testApplicationDeclarationTrue), PendingDeregistrationException("You cannot submit a new application while your cancelled application is still pending")) { result =>
        val document = Jsoup.parse(contentAsString(result))
        document.getElementsByClass("page-header").text() should include(Messages("awrs.application_pending_deregistration.heading"))
      }
    }

    "show error page if api 6 is submitted without any changes" in {
      updateWithException(testRequest(testApplicationDeclarationTrue),
        ResubmissionException(ResubmissionException.resubmissionMessage)) {
        result =>
          status(result) should be(INTERNAL_SERVER_ERROR)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByTag("h1").text should be(Messages("awrs.application_resubmission_error.heading"))
          document.getElementsByTag("p").text should include(Messages("awrs.application_resubmission_error.message"))
      }
    }

    "show error page if a Run time exception is encountered" in {
      saveWithException(testRequest(testApplicationDeclarationTrue), new Exception("Runtime Exception")) { result =>
        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("application-error-header").text() should be("Sorry, we’re experiencing technical difficulties")
      }
    }

    "return true if AWRS user is enrolled and has status of Pending" in {
      implicit val request = FakeRequest().withSession(AwrsSessionKeys.sessionStatusType -> Pending.name)
      val result = TestApplicationDeclarationController.isEnrolledApplicant
      result shouldBe true
    }

    "return true if AWRS user is enrolled and has status of Approved" in {
      implicit val request = FakeRequest().withSession(AwrsSessionKeys.sessionStatusType -> Approved.name)
      val result = TestApplicationDeclarationController.isEnrolledApplicant
      result shouldBe true
    }

    "return true if AWRS user is enrolled and has status of ApprovedWithConditions" in {
      implicit val request = FakeRequest().withSession(AwrsSessionKeys.sessionStatusType -> ApprovedWithConditions.name)
      val result = TestApplicationDeclarationController.isEnrolledApplicant
      result shouldBe true
    }

    "return false if AWRS user is enrolled and has status of Rejected" in {
      implicit val request = FakeRequest().withSession(AwrsSessionKeys.sessionStatusType -> Rejected.name)
      val result = TestApplicationDeclarationController.isEnrolledApplicant
      result shouldBe false
    }
  }

  "Submitting the application declaration form with " should {
    "Authenticated and authorised users" should {
      "redirect to Confirmation page when valid data is provided" in {
        continueWithAuthorisedUser(testRequest(testApplicationDeclarationTrue)) {
          result =>
            redirectLocation(result).get should include("/alcohol-wholesale-scheme/confirmation")
        }
      }
      "save form data to Save4Later and redirect to Confirmation page " in {
        continueWithAuthorisedUser(testRequest(testApplicationDeclarationTrue)) {
          result =>
            status(result) should be(SEE_OTHER)
            verifySave4LaterService(saveApplicationDeclaration = 1)
        }
      }
      "redirect to Confirmation page when valid updated data is provided" in {
        continueUpdateWithAuthorisedUser(testRequest(testApplicationDeclarationTrue)) {
          result =>
            redirectLocation(result).get should include("/alcohol-wholesale-scheme/update-confirmation")
        }
      }
      "redirect to Confirmation page should fail when confirmation is not checked" in {
        continueUpdateWithAuthorisedUser(testRequest(testApplicationDeclaration)) {
          result =>
            status(result) should be(BAD_REQUEST)
        }
      }
    }
  }

  private def continueWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(
      fetchBusinessCustomerDetails = testReviewDetails,
      fetchAll = MockSave4LaterService.defaultFetchAll,
      fetchBusinessRegistrationDetails = testBusinessRegistrationDetails("SOP")
    )
    setupMockKeyStoreServiceOnlySaveFunctions()
    when(mockApplicationService.sendApplication()(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(subscribeSuccessResponse))
    when(mockEnrolService.enrolAWRS(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(enrolSuccessResponse)))
    val result = TestApplicationDeclarationController.sendApplication().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, "SOP"))
    test(result)
  }

  private def continueUpdateWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    setUser(hasAwrs = true)
    setupMockSave4LaterServiceWithOnly(fetchAll = MockSave4LaterService.defaultFetchAll)
    setupMockKeyStoreServiceOnlySaveFunctions()
    when(mockApplicationService.updateApplication()(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(subscribeUpdateSuccessResponse))
    val result = TestApplicationDeclarationController.sendApplication().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, "SOP"))
    test(result)
  }

  private def saveWithException(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded], exception: Exception)(test: Future[Result] => Any) {
    setupMockSave4LaterServiceWithOnly(fetchAll = createCacheMap("SOP"), fetchBusinessCustomerDetails = testReviewDetails, fetchBusinessRegistrationDetails = testBusinessRegistrationDetails("SOP"))
    setupMockKeyStoreServiceOnlySaveFunctions()
    when(mockApplicationService.sendApplication()(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.failed(exception))
    when(mockEnrolService.enrolAWRS(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(enrolSuccessResponse)))

    val result = TestApplicationDeclarationController.sendApplication().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, "SOP"))
    test(result)
  }

  private def updateWithException(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded], exception: Exception)(test: Future[Result] => Any) {
    setUser(hasAwrs = true)
    setupMockSave4LaterServiceWithOnly(fetchAll = createCacheMap("SOP"), fetchBusinessRegistrationDetails = testBusinessRegistrationDetails("SOP"))
    setupMockKeyStoreServiceOnlySaveFunctions()
    when(mockApplicationService.updateApplication()(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.failed(exception))

    val result = TestApplicationDeclarationController.sendApplication().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, "SOP"))
    test(result)
  }

}
