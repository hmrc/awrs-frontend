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
import org.mockito.Mockito.{verify, when}
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{EnrolService, ServicesUnitTestFixture}
import services.mocks.{MockIndexService, MockKeyStoreService}
import utils.{AwrsUnitTestTraits, TestUtil}
import views.html.reenrolment.awrs_registered_utr

import scala.concurrent.{Await, Future}

class RegisteredUtrControllerTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture with MockAuthConnector
  with MockKeyStoreService
  with MockIndexService {

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
    mockAuditable, mockAccountUtils, mockMatchingService,
    mockEnrolService, mockAwrsFeatureSwitches, mockAppConfig, template)

  "AwrsUtrController" must {

    "show the UTR page" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUtr()
      when(mockAccountUtils.isSaAccount(ArgumentMatchers.any())).thenReturn(true)
      val res = testAwrsUtrController.showArwsUtrPage().apply(SessionBuilder.buildRequestWithSession(userId))
      status(res) mustBe 200
    }

    "redirect to kickout if UTR/postcode do not match" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUtr(utr = Some(AwrsEnrolmentUtr("6232113818078")),
        registeredPostcode = Some(AwrsRegisteredPostcode("NE98 1ZZ")),
        searchResult = Some(testSearchResult("TestAWRSRef")))

      when(mockAccountUtils.isSaAccount(ArgumentMatchers.any())).thenReturn(true)
      when(mockMatchingService.verifyUTRandPostCode(ArgumentMatchers.any(), ArgumentMatchers.any(),
        ArgumentMatchers.any(), ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(false))


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

      when(mockAccountUtils.isSaAccount(ArgumentMatchers.any())).thenReturn(true)
      when(mockMatchingService.verifyUTRandPostCode(ArgumentMatchers.any(), ArgumentMatchers.any(),
        ArgumentMatchers.any(), ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(true))
      when(mockEnrolService.enrolAWRS(ArgumentMatchers.eq("TestAWRSRef"),
        ArgumentMatchers.eq("NE98 1ZZ"),
        ArgumentMatchers.eq(Some("6232113818078")),
        ArgumentMatchers.eq("SOP"),
        ArgumentMatchers.eq(Map.empty))
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(EnrolResponse("serviceName", "state", identifiers = List(Identifier("AWRS", "AWRS_Ref_No")))))

      val res = testAwrsUtrController.saveAndContinue().apply(testRequest("6232113818078"))
      status(res) mustBe 303
      verify(mockEnrolService).enrolAWRS(ArgumentMatchers.eq("TestAWRSRef"),
        ArgumentMatchers.eq("NE98 1ZZ"),
        ArgumentMatchers.eq(Some("6232113818078")),
        ArgumentMatchers.eq("SOP"),
        ArgumentMatchers.eq(Map.empty))(ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    "enrol CT UTR for AWRS  if no errors" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUtr(utr = Some(AwrsEnrolmentUtr("6232113818078")),
        registeredPostcode = Some(AwrsRegisteredPostcode("NE98 1ZZ")),
        searchResult = Some(testSearchResult("TestAWRSRef")))
      when(mockAccountUtils.isSaAccount(ArgumentMatchers.any())).thenReturn(false)
      when(mockMatchingService.verifyUTRandPostCode(ArgumentMatchers.any(), ArgumentMatchers.any(),
        ArgumentMatchers.any(), ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(true))
      when(mockEnrolService.enrolAWRS(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val res = testAwrsUtrController.saveAndContinue().apply(testRequest("6232113818078"))
      status(res) mustBe 303
      verify(mockEnrolService).enrolAWRS(ArgumentMatchers.eq("TestAWRSRef"),
        ArgumentMatchers.eq("NE98 1ZZ"),
        ArgumentMatchers.eq(Some("6232113818078")),
        ArgumentMatchers.eq("CT"),
        ArgumentMatchers.eq(Map.empty))(ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    "save should return 400 if form has errors" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUtr()
      when(mockAccountUtils.isSaAccount(ArgumentMatchers.any())).thenReturn(true)
      val res = testAwrsUtrController.saveAndContinue().apply(testRequest("SomethingWithError"))
      status(res) mustBe 400
    }

    "reflect Corporation Tax in title if user has CT UTR and form has errors" in {
      setAuthMocks()
      when(mockAccountUtils.isSaAccount(ArgumentMatchers.any())).thenReturn(false)

      val res = contentAsString(testAwrsUtrController.saveAndContinue().apply(testRequest("SomethingWithError")))
      res must include ("awrs.reenrolment.registered_utr.title.ct")
    }

    "reflect Self Assessment in title if logged-in user has SA UTR and form has errors" in {
      setAuthMocks()
      when(mockAccountUtils.isSaAccount(ArgumentMatchers.any())).thenReturn(true)

      val res = contentAsString(testAwrsUtrController.saveAndContinue().apply(testRequest("SomethingWithError")))
      res must include ("awrs.reenrolment.registered_utr.title.sa")
    }
  }
}
