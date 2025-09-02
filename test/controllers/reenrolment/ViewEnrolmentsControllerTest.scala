/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ServicesUnitTestFixture
import utils.AwrsUnitTestTraits
import views.html.reenrolment.awrs_view_enrolments

class ViewEnrolmentsControllerTest extends AwrsUnitTestTraits with ServicesUnitTestFixture {
  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val template: awrs_view_enrolments               = app.injector.instanceOf[views.html.reenrolment.awrs_view_enrolments]

  val testViewEnrolmentController: ViewEnrolmentsController = new ViewEnrolmentsController(
    mockMCC,
    mockAppConfig,
    mockDeEnrolService,
    mockAuthConnector,
    mockAuditable,
    mockAccountUtils,
    template
  )

  "ViewEnrolmentsController" must {

    "show the View Enrolment page" in {
      setAuthMocks()
      val res = testViewEnrolmentController.showViewEnrolmentsPage().apply(SessionBuilder.buildRequestWithSession(userId))
      status(res) mustBe 200
    }

    "contain a button linking to the Business Tax Account page" in {
      setAuthMocks()

      val res     = testViewEnrolmentController.showViewEnrolmentsPage().apply(SessionBuilder.buildRequestWithSession(userId))
      val content = contentAsString(res)

      content must include(s"""<a href="" role="button" class="govuk-button" id="bta-redirect-link" data-module="govuk-button">""")
    }

  }

}
