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

import config.ApplicationConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat

abstract class ViewTestFixture extends PlaySpec
  with MockitoSugar
  with GuiceOneAppPerSuite {
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/")
  implicit val messages: Messages = MessagesImpl(Lang("en"), messagesApi)

  val htmlContent:HtmlFormat.Appendable

  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]

  lazy val htmlString = htmlContent.body
  lazy val document: Document = Jsoup.parse(htmlString)

  lazy val heading = document.select("h1").text()

  lazy val bodyText = document.select("p").text()

  lazy val sign_in_btn = document.select("a.govuk-button").text()

  lazy val buttonText = document.select(".govuk-button").text()

  lazy val sign_in_href = document.select("a.govuk-button").attr("href")

  lazy val input_field = document.select("input.govuk-input")

  lazy val input_field_label = document.select("label.govuk-label").text()

  lazy val back_link = document.select(".govuk-width-container > a#back").text()

  lazy val back_link_href = document.select(".govuk-width-container > a#back").attr("href")


}
