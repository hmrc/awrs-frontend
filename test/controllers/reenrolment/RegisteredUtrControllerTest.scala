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

package controllers.reenrolment

import builders.SessionBuilder
import connectors.mock.MockAuthConnector
import forms.reenrolment.RegisteredUtrForm
import models.AwrsStatus.Approved
import models.reenrolment.AwrsRegisteredPostcode
import models.{AwrsEnrolmentUtr, Business, EnrolResponse, Identifier, Info, SearchResult}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{EnrolService, ServicesUnitTestFixture}
import services.mocks.{MockIndexService, MockKeyStoreService}
import uk.gov.hmrc.http.BadRequestException
import utils.{AwrsUnitTestTraits, TestUtil}
import views.html.reenrolment.awrs_registered_utr

import scala.concurrent.{Await, Future}

class RegisteredUtrControllerTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture with MockAuthConnector
  with MockKeyStoreService
  with MockIndexService {

  override def beforeEach(): Unit = {
    reset(mockDeEnrolService, mockAuthConnector,
      mockAuditable, mockAccountUtils,
      mockEnrolService, mockAwrsFeatureSwitches, mockAppConfig)
    super.beforeEach()
  }

  def testRequest(answer: String): FakeRequest[AnyContentAsFormUrlEncoded] =
    TestUtil.populateFakeRequest[AwrsEnrolmentUtr](FakeRequest(), RegisteredUtrForm.awrsEnrolmentUtrForm.form, AwrsEnrolmentUtr(answer))

  def testSearchResult(ref:String) = SearchResult(List(
    Business(ref,
      Some("12/12/2013"),
      Approved,
      Info(Some("Business Name"), Some("Trading Name"), Some("Full name"), None),
      None)))
  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val template: awrs_registered_utr = app.injector.instanceOf[views.html.reenrolment.awrs_registered_utr]
  val mockEnrolService: EnrolService = mock[EnrolService]

  val testAwrsUtrController: RegisteredUtrController = new RegisteredUtrController(mockMCC,
    testKeyStoreService, mockDeEnrolService, mockAuthConnector,
    mockAuditable, mockAccountUtils,
    mockEnrolService, mockAwrsFeatureSwitches, mockAppConfig, template)

  "AwrsUtrController" must {
    "show not found when feature is not enabled" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUtr()
      setupEnrolmentJourneyFeatureSwitchMock(false)
      val res = testAwrsUtrController.showArwsUtrPage().apply(SessionBuilder.buildRequestWithSession(userId))
      status(res) mustBe 404
    }

    "show the UTR page when enrolmentJourney is enabled" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUtr()
      setupEnrolmentJourneyFeatureSwitchMock(true)
      when(mockAccountUtils.isSaAccount(ArgumentMatchers.any())).thenReturn(true)
      val res = testAwrsUtrController.showArwsUtrPage().apply(SessionBuilder.buildRequestWithSession(userId))
      status(res) mustBe 200
    }

    "redirect to kickout if searchResults are not in cache" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUtr(utr = Some(AwrsEnrolmentUtr("6232113818078")),
        registeredPostcode = Some(AwrsRegisteredPostcode("NE98 1ZZ")),
        searchResult = None)

      setupEnrolmentJourneyFeatureSwitchMock(true)
      when(mockAccountUtils.isSaAccount(ArgumentMatchers.any())).thenReturn(true)
      val res = testAwrsUtrController.saveAndContinue().apply(testRequest("6232113818078"))
      val result: Result = Await.result(res, 5.seconds)
      result.header.status mustBe 303
      result.header.headers("Location") mustBe controllers.reenrolment.routes.KickoutController.showURNKickOutPage.url
    }

    "Redirect to Kickout Page if enrolment fails" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUtr(utr = Some(AwrsEnrolmentUtr("6232113818078")),
        registeredPostcode = Some(AwrsRegisteredPostcode("NE98 1ZZ")),
        searchResult = Some(testSearchResult("TestAWRSRef")))

      setupEnrolmentJourneyFeatureSwitchMock(true)
      when(mockAccountUtils.isSaAccount(ArgumentMatchers.any())).thenReturn(true)
      when(mockEnrolService.enrolAWRS(ArgumentMatchers.eq("TestAWRSRef"),
        ArgumentMatchers.eq("NE98 1ZZ"),
        ArgumentMatchers.eq(Some("6232113818078")),
        ArgumentMatchers.eq("SOP"),
        ArgumentMatchers.eq(Map.empty))
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.failed(new BadRequestException("")))

      val res = testAwrsUtrController.saveAndContinue().apply(testRequest("6232113818078"))
      val result: Result = Await.result(res, 5.seconds)
      result.header.status mustBe 303
      result.header.headers("Location") mustBe controllers.reenrolment.routes.KickoutController.showURNKickOutPage.url

    }

    "enrol SA UTR for AWRS if no errors" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUtr(utr = Some(AwrsEnrolmentUtr("6232113818078")),
        registeredPostcode = Some(AwrsRegisteredPostcode("NE98 1ZZ")),
        searchResult = Some(testSearchResult("TestAWRSRef")))

      setupEnrolmentJourneyFeatureSwitchMock(true)
      when(mockAccountUtils.isSaAccount(ArgumentMatchers.any())).thenReturn(true)
      when(mockEnrolService.enrolAWRS(ArgumentMatchers.eq("TestAWRSRef"),
        ArgumentMatchers.eq("NE98 1ZZ"),
        ArgumentMatchers.eq(Some("6232113818078")),
        ArgumentMatchers.eq("SOP"),
        ArgumentMatchers.eq(Map.empty))
      (ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(EnrolResponse("serviceName", "state", identifiers = List(Identifier("AWRS", "AWRS_Ref_No")))))

      val res = testAwrsUtrController.saveAndContinue().apply(testRequest("6232113818078"))
      val result: Result = Await.result(res, 5.seconds)
      result.header.status mustBe 303
      result.header.headers("Location") mustBe controllers.reenrolment.routes.SuccessfulEnrolmentController.showSuccessfulEnrolmentPage.url
    }

    "enrol CT UTR for AWRS  if no errors" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUtr(utr = Some(AwrsEnrolmentUtr("6232113818078")),
        registeredPostcode = Some(AwrsRegisteredPostcode("NE98 1ZZ")),
        searchResult = Some(testSearchResult("TestAWRSRef")))
      setupEnrolmentJourneyFeatureSwitchMock(true)
      when(mockAccountUtils.isSaAccount(ArgumentMatchers.any())).thenReturn(false)
      when(mockEnrolService.enrolAWRS(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(EnrolResponse("serviceName", "state", identifiers = List(Identifier("AWRS", "AWRS_Ref_No")))))
      val res = testAwrsUtrController.saveAndContinue().apply(testRequest("6232113818078"))
      val result: Result = Await.result(res, 5.seconds)
      result.header.status mustBe 303
      result.header.headers("Location") mustBe controllers.reenrolment.routes.SuccessfulEnrolmentController.showSuccessfulEnrolmentPage.url
    }

    "save should return 400 if form has errors" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUtr()
      setupEnrolmentJourneyFeatureSwitchMock(true)
      when(mockAccountUtils.isSaAccount(ArgumentMatchers.any())).thenReturn(true)
      val res = testAwrsUtrController.saveAndContinue().apply(testRequest("SomethingWithError"))
      status(res) mustBe 400
    }

    "reflect Corporation Tax in title if user has CT UTR and form has errors" in {
      setAuthMocks()
      setupEnrolmentJourneyFeatureSwitchMock(true)
      when(mockAccountUtils.isSaAccount(ArgumentMatchers.any())).thenReturn(false)

      val res = contentAsString(testAwrsUtrController.saveAndContinue().apply(testRequest("SomethingWithError")))
      res must include ("awrs.reenrolment.registered_utr.title.ct")
    }

    "reflect Self Assessment in title if logged-in user has SA UTR and form has errors" in {
      setAuthMocks()
      setupEnrolmentJourneyFeatureSwitchMock(true)
      when(mockAccountUtils.isSaAccount(ArgumentMatchers.any())).thenReturn(true)

      val res = contentAsString(testAwrsUtrController.saveAndContinue().apply(testRequest("SomethingWithError")))
      res must include ("awrs.reenrolment.registered_utr.title.sa")
    }
  }
}
