
package uk.gov.hmrc.helpers

import akka.util.Timeout
import org.scalatest._
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.ws.{WSCookie, WSRequest}
import play.api.mvc.{Cookie, Session, SessionCookieBaker}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits, Injecting}
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.helpers.application.IntegrationApplication
import uk.gov.hmrc.helpers.http.StubbedBasicHttpCalls
import uk.gov.hmrc.helpers.wiremock.WireMockSetup
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCrypto

import scala.concurrent.Future
import scala.concurrent.duration._

trait IntegrationSpec
  extends AnyWordSpecLike
    with OptionValues
    with FutureAwaits
    with DefaultAwaitTimeout
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with IntegrationApplication
    with WireMockSetup
    with StubbedBasicHttpCalls with Injecting {

  override implicit def defaultAwaitTimeout: Timeout = 5.seconds

  implicit val hc: HeaderCarrier = HeaderCarrier()

  def mockSessionCookie = {

    def makeSessionCookie(session:Session): Cookie = {
      val cookieCrypto = inject[SessionCookieCrypto]
      val cookieBaker = inject[SessionCookieBaker]
      val sessionCookie = cookieBaker.encodeAsCookie(session)
      val encryptedValue = cookieCrypto.crypto.encrypt(PlainText(sessionCookie.value))
      sessionCookie.copy(value = encryptedValue.value)
    }

    val mockSession = Session(Map(
      SessionKeys.lastRequestTimestamp -> System.currentTimeMillis().toString,
      SessionKeys.authToken -> "mock-bearer-token",
      SessionKeys.sessionId -> "mock-sessionid"
    ))

    val cookie = makeSessionCookie(mockSession)

    new WSCookie() {
      override def name: String = cookie.name
      override def value: String = cookie.value
      override def domain: Option[String] = cookie.domain
      override def path: Option[String] = Some(cookie.path)
      override def maxAge: Option[Long] = cookie.maxAge.map(_.toLong)
      override def secure: Boolean = cookie.secure
      override def httpOnly: Boolean = cookie.httpOnly
    }
  }

  def client(path: String): WSRequest = ws.url(s"http://localhost:$port$path")
    .withCookies(mockSessionCookie)
    .withFollowRedirects(false)

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWmServer()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    resetWmServer()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    stopWmServer()
  }

  def awaitAndAssert[T](methodUnderTest: => Future[T])(assertions: T => Assertion): Assertion = {
    assertions(await(methodUnderTest))
  }
}
