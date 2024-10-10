package views

import config.ApplicationConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat

abstract class ViewTestFixture extends PlaySpec
  with MockitoSugar
  with BeforeAndAfterEach
  with GuiceOneAppPerSuite {
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  implicit val fakeRequest = FakeRequest("GET", "/")
  implicit val messages: Messages = MessagesImpl(Lang("en"), messagesApi)

  val htmlContent:HtmlFormat.Appendable

  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]

  lazy val htmlString = htmlContent.body
  lazy val document: Document = Jsoup.parse(htmlString)

  lazy val heading = document.select("h1").text()

  lazy val bodyText = document.select("p").text()

  lazy val sign_in_btn = document.select("a.govuk-button").text()

  lazy val sign_in_href = document.select("a.govuk-button").attr("href")
}
