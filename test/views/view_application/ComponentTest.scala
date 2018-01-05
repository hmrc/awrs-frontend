/*
 * Copyright 2018 HM Revenue & Customs
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

package views.view_application

import java.util.UUID

import connectors.mock.MockAuthConnector
import controllers.auth.AwrsController
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.{AwrsUtr, Nino}
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{ConfidenceLevel, _}
import uk.gov.hmrc.play.test._
import utils.TestConstants._

import scala.concurrent.Future
import uk.gov.hmrc.http.SessionKeys

class ComponentTest extends UnitSpec with MockitoSugar with BeforeAndAfterEach with OneServerPerSuite with MockAuthConnector {

  trait TestController extends AwrsController {
    def show(rowTitle: String, content: Option[String]*) = asyncRestrictedAccess {
      implicit user => implicit request =>
        // Run sbt test in terminal to compile tests and generate TableRowTestPage, otherwise it will show up red here
        Future.successful(Ok(views.html.view_application.TableRowTestPage(rowTitle, content: _*)))
    }
  }

  object TestController extends TestController {
    override val authConnector = mockAuthConnector
  }

  // implicit parameters required by the save4later calls, the actual values are not important as these calls are mocked
  implicit lazy val fakeRequest = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId,
      "businessType" -> "SOP",
      "businessName" -> "North East Wines"
    )
  }

  implicit def conv(str: String): Option[String] = Some(str)

  import scala.collection.JavaConversions._

  "row helper" should {

    case class Expectations(lines: String*)
    case class TestCase(data: Seq[Option[String]], expecations: Expectations)

    def testExpectations(result: Future[Result], expectations: Expectations) {
      val document = Jsoup.parse(contentAsString(result))
      val trs = document.getElementsByTag("tr")
      val trSize = trs.size()

      val expPSize = expectations.lines.size
      // the expected tr size is 1 if expSize > 0 or 0 otherwise
      val expTrSize =
        expectations.lines.size match {
          case 0 => return
          case _ => 1
        }
      trSize shouldBe expTrSize
      // each tr has exactly 2 tds
      // the first is the rowTitle which we are ignoring
      // the second is the contents specified with each element wrappined in a p tag
      val ps = trs.get(0).getElementsByTag("td").get(1).getElementsByTag("p")
      ps.size() shouldBe expPSize

      ps.map(x => x.text()) shouldBe expectations.lines
    }

    def runTests(testCases: TestCase*): Unit = {
      val rowTitle = "does not matter"
      testCases.foreach(testCase => testExpectations(TestController.show(rowTitle, testCase.data: _*)(fakeRequest), testCase.expecations))
    }

    val line1 = "line 1"
    val line2 = "line 2"
    val line3 = "line 3"

    "When no data is provided then the row should not be displayed" in {
      val testCases = Seq(
        TestCase(Seq(), Expectations()),
        TestCase(Seq(None, None), Expectations())
      )
      runTests(testCases: _*)
    }

    "When all rows are presented then they should all be displayed" in {
      val testCases = Seq(
        TestCase(Seq(line1), Expectations(line1)),
        TestCase(Seq(line1, line2), Expectations(line1, line2))
      )
      runTests(testCases: _*)
    }

    "When not all rows are presented then only the defined row should all be displayed" in {
      val testCases = Seq(
        TestCase(Seq(line1, None, None), Expectations(line1)),
        TestCase(Seq(line1, None, line3), Expectations(line1, line3)),
        TestCase(Seq(None, None, line3), Expectations(line3))
      )
      runTests(testCases: _*)
    }
  }

}
