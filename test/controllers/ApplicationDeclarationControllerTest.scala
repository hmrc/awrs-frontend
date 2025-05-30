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
import connectors.mock.MockAuthConnector
import exceptions._
import forms.ApplicationDeclarationForm
import models.FormBundleStatus._
import models._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services._
import services.mocks.{MockKeyStoreService, MockSave4LaterService}
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment, EnrolmentIdentifier, Enrolments, User}
import uk.gov.hmrc.play.audit.model.Audit
import utils.TestUtil._
import utils.{AwrsSessionKeys, AwrsUnitTestTraits, TestUtil}
import views.html.{awrs_application_declaration, awrs_business_directors}

import scala.concurrent.Future

class ApplicationDeclarationControllerTest extends AwrsUnitTestTraits
  with MockAuthConnector
  with MockSave4LaterService
  with MockKeyStoreService {

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val mockEnrolService: EnrolService = mock[EnrolService]
  val mockApplicationService: ApplicationService = mock[ApplicationService]
  val mockAudit: Audit = mock[Audit]
  val selfHealSuccessResponse: SelfHealSubscriptionResponse = SelfHealSubscriptionResponse("12345")

  val formId = "applicationDeclaration"

  val mockTemplate: awrs_application_declaration = app.injector.instanceOf[views.html.awrs_application_declaration]

  val mockBusinessDirectorsTemplate: awrs_business_directors = app.injector.instanceOf[views.html.awrs_business_directors]

  val subscribeSuccessResponse: SuccessfulSubscriptionResponse = SuccessfulSubscriptionResponse(processingDate = "2001-12-17T09:30:47Z", awrsRegistrationNumber = "ABCDEabcde12345", etmpFormBundleNumber = "123456789012345")
  val subscribeUpdateSuccessResponse: SuccessfulUpdateSubscriptionResponse = SuccessfulUpdateSubscriptionResponse(processingDate = "2001-12-17T09:30:47Z", etmpFormBundleNumber = "123456789012345")
  val enrolSuccessResponse: EnrolResponse = EnrolResponse("serviceName", "state", identifiers = List(Identifier("AWRS", "AWRS_Ref_No")))
  val testReviewDetails: BusinessCustomerDetails = BusinessCustomerDetails("ACME", Some("SOP"), BCAddress("line1", "line2", Option("line3"), Option("line4"), Option("postcode"), Option("country")), "sap123", "safe123", false, Some("agent123"))

  private def testRequest(declaration: ApplicationDeclaration) =
    TestUtil.populateFakeRequest[ApplicationDeclaration](FakeRequest(), ApplicationDeclarationForm.applicationDeclarationValidationForm, declaration)

  val testApplicationDeclarationController: ApplicationDeclarationController =
    new ApplicationDeclarationController(mockEnrolService, mockApplicationService, mockMCC, testSave4LaterService, testKeyStoreService, mockDeEnrolService, mockAuthConnector, mockAuditable, mockAccountUtils, mockMainStoreSave4LaterConnector, mockAppConfig, mockTemplate){

  }

  val testApplicationBusinessDirectorsController: BusinessDirectorsController =
    new BusinessDirectorsController(mockMCC, testSave4LaterService, mockDeEnrolService, mockAuthConnector, mockAuditable, mockAccountUtils, mockAppConfig, mockBusinessDirectorsTemplate)

  override def beforeEach(): Unit = {
    reset(mockAccountUtils)
    reset(mockEnrolService)
    reset(mockApplicationService)
    super.beforeEach()
  }

  "ApplicationDeclarationController" must {
    "show application declaration page without preloaded data" in {
      showWithAuthorsiedUser(testRequest(testApplicationDeclarationTrue), None) { result =>
        await(result)
        status(result) mustBe 200
      }
    }

    "show application declaration page with preloaded data" in {
      showWithAuthorsiedUser(testRequest(testApplicationDeclarationTrue), Some(ApplicationDeclaration(Some("name"), Some("role"), Some(true)))) { result =>
        await(result)
        status(result) mustBe 200
      }
    }

    "show error page if a DES Validation Exception is encountered" in {
      saveWithException(testRequest(testApplicationDeclarationTrue), DESValidationException("Validation against schema failed")) { result =>
        await(result)
        status(result) mustBe 500
      }
    }

    "show error page if a GovernmentGatewayException is encountered" in {
      saveWithException(testRequest(testApplicationDeclarationTrue), GovernmentGatewayException("There was a problem with the admin service")) { result =>
        await(result)
        status(result) mustBe 500
      }
    }

    "show error page if a DuplicateSubscriptionException is encountered" in {
      saveWithException(testRequest(testApplicationDeclarationTrue), DuplicateSubscriptionException("This subscription already exists")) { result =>
        val document = Jsoup.parse(contentAsString(result))
        document.getElementsByClass("govuk-heading-xl").text() must include(Messages("awrs.application_duplicate_request.heading"))
      }
    }

    "show error page if a PendingDeregistrationException is encountered" in {
      saveWithException(testRequest(testApplicationDeclarationTrue), PendingDeregistrationException("You cannot submit a new application while your cancelled application is still pending")) { result =>
        val document = Jsoup.parse(contentAsString(result))
        document.getElementsByClass("govuk-heading-xl").text() must include(Messages("awrs.application_pending_deregistration.heading"))
      }
    }

    "show error page if api 6 is submitted without any changes" in {
      updateWithException(testRequest(testApplicationDeclarationTrue),
        ResubmissionException(ResubmissionException.resubmissionMessage)) {
        result =>
          status(result) must be(INTERNAL_SERVER_ERROR)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByTag("h1").text must be(Messages("awrs.application_resubmission_error.heading"))
          document.getElementsByTag("p").text must include(Messages("awrs.application_resubmission_error.message"))
      }
    }

    "show error page if a Run time exception is encountered" in {
      intercept[Exception](saveWithException(testRequest(testApplicationDeclarationTrue), new Exception("Runtime Exception")) { result =>
        status(result) mustBe 500
      })
    }

    "return true if AWRS user is enrolled and has status of Pending" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(AwrsSessionKeys.sessionStatusType -> Pending.name)
      val result = testApplicationDeclarationController.isEnrolledApplicant
      result mustBe true
    }

    "return true if AWRS user is enrolled and has status of Approved" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(AwrsSessionKeys.sessionStatusType -> Approved.name)
      val result = testApplicationDeclarationController.isEnrolledApplicant
      result mustBe true
    }

    "return true if AWRS user is enrolled and has status of ApprovedWithConditions" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(AwrsSessionKeys.sessionStatusType -> ApprovedWithConditions.name)
      val result = testApplicationDeclarationController.isEnrolledApplicant
      result mustBe true
    }

    "return false if AWRS user is enrolled and has status of Rejected" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(AwrsSessionKeys.sessionStatusType -> Rejected.name)
      val result = testApplicationDeclarationController.isEnrolledApplicant
      result mustBe false
    }
  }

  "Submitting the application declaration form with " must {
    "Authenticated and authorised users" must {
      "redirect to Confirmation page when valid data is provided" in {
        continueWithAuthorisedUser(testRequest(testApplicationDeclarationTrue)) {
          result =>
            redirectLocation(result).get must include("/alcohol-wholesale-scheme/confirmation")
        }
      }
      "save form data to Save4Later and redirect to Confirmation page " in {
        continueWithAuthorisedUser(testRequest(testApplicationDeclarationTrue)) {
          result =>
            status(result) must be(SEE_OTHER)
            verifySave4LaterService(saveApplicationDeclaration = 1)
        }
      }
      "redirect to Confirmation page when valid updated data is provided" in {
        continueUpdateWithAuthorisedUser(testRequest(testApplicationDeclarationTrue)) {
          result =>
            redirectLocation(result).get must include("/alcohol-wholesale-scheme/update-confirmation")
        }
      }
      "redirect to Confirmation page must fail when confirmation is not checked" in {
        continueUpdateWithAuthorisedUser(testRequest(testApplicationDeclaration)) {
          result =>
            status(result) must be(BAD_REQUEST)
        }
      }
    }
  }

  "Self heal redirect" must {
    "redirect to application status page when valid data is provided" in {
      continueWithAuthorisedUserSelfHeal(testRequest(testApplicationDeclarationTrue)) {
        result =>
          redirectLocation(result).get must include("/alcohol-wholesale-scheme/confirmation")
      }
    }
  }

  private def showWithAuthorsiedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded], data: Option[ApplicationDeclaration])(test: Future[Result] => Any): Unit = {
    setupMockSave4LaterServiceWithOnly(
      fetchBusinessCustomerDetails = testReviewDetails,
      fetchAll = MockSave4LaterService.defaultFetchAll,
      fetchBusinessRegistrationDetails = testBusinessRegistrationDetails("SOP"),
      fetchApplicationDeclaration = data
    )
    setAuthMocks(Future.successful(new ~(new ~( new ~(Enrolments(Set(Enrolment("IR-CT", Seq(EnrolmentIdentifier("utr", "0123456")), "activated"))), Some(AffinityGroup.Organisation)), Credentials("fakeCredID", "type")), Some(User))))
    setupMockKeyStoreServiceOnlySaveFunctions()
    when(mockApplicationService.updateApplication(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(subscribeUpdateSuccessResponse))
    when(mockApplicationService.sendApplication(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Right(subscribeSuccessResponse)))
    when(mockEnrolService.enrolAWRS(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(enrolSuccessResponse)))
    val result = testApplicationDeclarationController.showApplicationDeclaration().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, "SOP"))
    test(result)
  }

  private def continueWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any): Unit = {
    setupMockSave4LaterServiceWithOnly(
      fetchBusinessCustomerDetails = testReviewDetails,
      fetchAll = MockSave4LaterService.defaultFetchAll,
      fetchBusinessRegistrationDetails = testBusinessRegistrationDetails("SOP")
    )
    setAuthMocks(Future.successful(new ~(new ~( new ~(Enrolments(Set(Enrolment("IR-CT", Seq(EnrolmentIdentifier("utr", "0123456")), "activated"))), Some(AffinityGroup.Organisation)), Credentials("fakeCredID", "type")), Some(User))))
    setupMockKeyStoreServiceOnlySaveFunctions()
    when(mockApplicationService.updateApplication(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(subscribeUpdateSuccessResponse))
    when(mockApplicationService.sendApplication(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Right(subscribeSuccessResponse)))
    when(mockEnrolService.enrolAWRS(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(enrolSuccessResponse)))
    val result = testApplicationDeclarationController.sendApplication().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, "SOP"))
    test(result)
  }

  private def continueWithAuthorisedUserSelfHeal(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any): Unit = {
    resetAuthConnector()
    reset(mockAccountUtils)
    setupMockSave4LaterServiceWithOnly(
      fetchBusinessCustomerDetails = testReviewDetails,
      fetchAll = MockSave4LaterService.defaultFetchAll,
      fetchBusinessRegistrationDetails = testBusinessRegistrationDetails("SOP")
    )
    setAuthMocks(Future.successful(new ~(new ~( new ~(Enrolments(Set(Enrolment("IR-CT", Seq(EnrolmentIdentifier("utr", "0123456")), "activated"))), Some(AffinityGroup.Organisation)), Credentials("fakeCredID", "type")), Some(User))))
    setupMockKeyStoreServiceOnlySaveFunctions()
    when(mockApplicationService.updateApplication(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(subscribeUpdateSuccessResponse))
    when(mockApplicationService.sendApplication(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Left(selfHealSuccessResponse)))
    when(mockEnrolService.enrolAWRS(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(enrolSuccessResponse)))
    val result = testApplicationDeclarationController.sendApplication().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, "SOP"))
    test(result)
  }

  private def continueUpdateWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any): Unit = {
    resetAuthConnector()
    setupMockSave4LaterServiceWithOnly(fetchAll = MockSave4LaterService.defaultFetchAll)
    setupMockKeyStoreServiceOnlySaveFunctions()
    setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
    when(mockApplicationService.updateApplication(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(subscribeUpdateSuccessResponse))
    val result = testApplicationDeclarationController.sendApplication().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, "SOP"))
    test(result)
  }

  private def saveWithException(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded], exception: Exception)(test: Future[Result] => Any): Unit = {
    setupMockSave4LaterServiceWithOnly(fetchAll = createCacheMap("SOP"), fetchBusinessCustomerDetails = testReviewDetails, fetchBusinessRegistrationDetails = testBusinessRegistrationDetails("SOP"))
    setupMockKeyStoreServiceOnlySaveFunctions()
    exception match {
      case _:DESValidationException => setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
      case _ => setAuthMocks(
        authResult = Future.successful(new ~(new ~( new ~(Enrolments(Set(Enrolment("IR-CT", Seq(EnrolmentIdentifier("utr", "0123456")), "activated"))), Some(AffinityGroup.Organisation)), Credentials("fakeCredID", "type")), Some(User))),
        mockAccountUtils = Some(mockAccountUtils)
      )
    }
    when(mockApplicationService.updateApplication(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.failed(exception))
    when(mockApplicationService.sendApplication(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.failed(exception))
    when(mockEnrolService.enrolAWRS(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(enrolSuccessResponse)))

    val result = testApplicationDeclarationController.sendApplication().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, "SOP"))
    test(result)
  }

  private def updateWithException(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded], exception: Exception)(test: Future[Result] => Any): Unit = {
    resetAuthConnector()
    setupMockSave4LaterServiceWithOnly(fetchAll = createCacheMap("SOP"), fetchBusinessRegistrationDetails = testBusinessRegistrationDetails("SOP"), fetchBusinessCustomerDetails = testReviewDetails)
    setupMockKeyStoreServiceOnlySaveFunctions()
    setAuthMocks(mockAccountUtils = Some(mockAccountUtils))
    when(mockApplicationService.updateApplication(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.failed(exception))
    when(mockApplicationService.sendApplication(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.failed(exception))

    val result = testApplicationDeclarationController.sendApplication().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId, "SOP"))
    test(result)
  }

}
