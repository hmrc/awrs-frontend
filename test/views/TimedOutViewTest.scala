/*
 * Copyright 2024 HM Revenue & Customs
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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.play.PlaySpec
import play.api.test.FakeRequest
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.twirl.api.HtmlFormat
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.mockito.Mockito._
import org.scalatest.{Assertion, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar
import config.ApplicationConfig

class TimedOutViewTest
    extends PlaySpec
    with MockitoSugar
    with BeforeAndAfterEach
    with GuiceOneAppPerSuite {

  val timedOutView = app.injector.instanceOf[views.html.timed_out]
  implicit val fakeRequest = FakeRequest("GET", "/")
  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val messages: Messages = MessagesImpl(Lang("en"), messagesApi)

  val htmlContent: HtmlFormat.Appendable =
    timedOutView.apply()(fakeRequest, messages, mockAppConfig)
  val htmlString = htmlContent.body
  val document: Document = Jsoup.parse(htmlString)

  "TimedOutView" should {

    "render the correct content" in {

      val heading = document.select("h1").text()
      val bodyText = document.select("p").text()
      val sign_in_btn = document.select("a.govuk-button").text()
      val sign_in_href = document.select("a.govuk-button").attr("href")

      heading mustBe "For your security, we signed you out"
      bodyText mustBe "We saved your answers. Sign in using your Government Gateway to return to your application summary page."
      sign_in_btn mustBe "Sign in"
      sign_in_href mustBe "/alcohol-wholesale-scheme/landing-page"
    }
  }
}
