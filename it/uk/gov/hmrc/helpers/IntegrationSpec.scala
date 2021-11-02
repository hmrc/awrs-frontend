
package uk.gov.hmrc.helpers

import akka.util.Timeout
import org.scalatest._
import play.api.libs.ws.WSRequest
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.helpers.application.IntegrationApplication
import uk.gov.hmrc.helpers.http.StubbedBasicHttpCalls
import uk.gov.hmrc.helpers.wiremock.WireMockSetup
import uk.gov.hmrc.http.HeaderCarrier
import org.scalatest.wordspec.AnyWordSpecLike
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
    with StubbedBasicHttpCalls {

  override implicit def defaultAwaitTimeout: Timeout = 5.seconds

  implicit val hc: HeaderCarrier = HeaderCarrier()

  def client(path: String): WSRequest = ws.url(s"http://localhost:$port$path")
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



