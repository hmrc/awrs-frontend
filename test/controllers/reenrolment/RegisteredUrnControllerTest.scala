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
import forms.reenrolment.RegisteredUrnForm
import models.AwrsStatus.Approved
import models.{AwrsEnrolmentUrn, Business, Info, SearchResult}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ServicesUnitTestFixture
import services.mocks.{MockIndexService, MockKeyStoreService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AwrsUnitTestTraits, TestUtil}
import views.html.reenrolment.awrs_registered_urn

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class RegisteredUrnControllerTest extends AwrsUnitTestTraits
  with ServicesUnitTestFixture with MockAuthConnector
  with MockKeyStoreService
  with MockIndexService {

  def testRequest(answer: String): FakeRequest[AnyContentAsFormUrlEncoded] =
    TestUtil.populateFakeRequest[AwrsEnrolmentUrn](FakeRequest(), RegisteredUrnForm.awrsEnrolmentUrnForm.form, AwrsEnrolmentUrn(answer))

  def testSearchResult(ref:String) = SearchResult(List(
    Business(ref,
      Some("12/12/2013"),
      Approved,
      Info(Some("Business Name"), Some("Trading Name"), Some("Full name"), None),
      None)))
  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val template: awrs_registered_urn = app.injector.instanceOf[views.html.reenrolment.awrs_registered_urn]

  val testAwrsUrnController: RegisteredUrnController = new RegisteredUrnController(mockMCC,
    testKeyStoreService, mockDeEnrolService, mockAuthConnector,
    mockAuditable, mockAccountUtils, testLookupService, mockAwrsFeatureSwitches, mockAppConfig, template)

  "AwrsUrnController" must {

    "show the URN page" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUrn()
      val res = testAwrsUrnController.showArwsUrnPage().apply(SessionBuilder.buildRequestWithSession(userId))
      status(res) mustBe 200
    }

    "save the URN to keystore if no errors" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUrn()
      when(mockLookupConnector.queryByUrn(ArgumentMatchers.eq("XAAW00000123456"))
      (any[HeaderCarrier](), any[ExecutionContext]()))
        .thenReturn(Future.successful(Some(testSearchResult("XAAW00000123456"))))
      val res = testAwrsUrnController.saveAndContinue().apply(testRequest("XAAW00000123456"))
      status(res) mustBe 303
    }

    "save should return 400 if form has errors" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUrn()
      val res = testAwrsUrnController.saveAndContinue().apply(testRequest("SomthingWithError"))
      status(res) mustBe 400
    }
    
     "save should lookup the urn and save result found in keystore" in {
      setAuthMocks()
      setupMockKeystoreServiceForAwrsUrn()

      when(mockLookupConnector.queryByUrn(ArgumentMatchers.eq("XXAW00000000051"))(any[HeaderCarrier](),any[ExecutionContext]())).thenReturn(Future(Some(testSearchResult("XXAW00000000051"))))
      val res = testAwrsUrnController.saveAndContinue().apply(testRequest("XXAW00000000051"))
      status(res) mustBe 303
      verifyKeyStoreService(saveSearchResults = 1)

    }
  }

}
