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

package views

import builders.SessionBuilder
import config.ApplicationConfig
import connectors.mock.MockAuthConnector
import controllers.AdditionalPremisesController
import forms.AwrsEnrollmentUrnForm.awrsEnrolmentUrnForm
import models._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.DataCacheKeys._
import services.{EmailVerificationService, JourneyConstants, ServicesUnitTestFixture}
import services.mocks.MockSave4LaterService
import utils.AwrsUnitTestTraits
import utils.TestUtil._
import views.html.{awrs_additional_premises, awrs_urn}

import scala.concurrent.Future

class AwrsUrnViewTest extends ViewTestFixture  {

  val template: awrs_urn = app.injector.instanceOf[views.html.awrs_urn]

  override val htmlContent: HtmlFormat.Appendable = template.apply(awrsEnrolmentUrnForm.form)(fakeRequest, messages, mockAppConfig)

  val businessCustomerDetailsFormId = "businessCustomerDetails"
  val mockEmailVerificationService: EmailVerificationService = mock[EmailVerificationService]

  implicit val mockConfig: ApplicationConfig = mockAppConfig

  "Awrs Urn Template" must {

      "Display all fields correctly" in {
        heading mustBe "What is your Alcohol Wholesaler Registration Scheme (AWRS) Unique Reference Number (URN)?"



      }




  }


}
