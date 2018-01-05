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

import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import uk.gov.hmrc.play.test.UnitSpec
import views.view_application.ViewApplicationHelper._

class ViewApplicationHelperTest extends UnitSpec with MockitoSugar with BeforeAndAfterEach with OneServerPerSuite {

  type TestData = Seq[Option[String]]
  val dummyString = "dummyString"

  object TestData {
    def apply(input: Option[String]*): TestData = Seq(input: _*)
  }

  "One view helper content counter" should {

    case class Expectations(count: Int)
    case class TestCase(data: TestData, expecations: Expectations)



    def runTests(testCases: TestCase*): Unit = {
      val rowTitle = "does not matter"
      testCases.foreach(testCase => countContent(testCase.data) shouldBe testCase.expecations.count)
    }

    "return the count of strings when the sequence has some or none values " in {
      val testCases = Seq(
        TestCase(TestData(dummyString, dummyString), Expectations(2)),
        TestCase(TestData(dummyString, dummyString, dummyString), Expectations(3)),
        TestCase(TestData(dummyString, None), Expectations(1)),
        TestCase(TestData(dummyString, ""), Expectations(1))
      )

      runTests(testCases: _*)
    }

    "return the count as 0 when the sequence has all None or empty values" in {
      val testCases = Seq(
        TestCase(TestData("", None), Expectations(0)),
        TestCase(TestData("", ""), Expectations(0)),
        TestCase(TestData(), Expectations(0))
      )

      runTests(testCases: _*)
    }
  }

  "One view helper Option String Util " should {

    "concatenate the strings " in {
      Some(dummyString) + " " + Some(dummyString) shouldBe Some("dummyString dummyString")
      Some(dummyString) + "" shouldBe Some("dummyString")
      None + Some(dummyString) shouldBe Some("dummyString")
      //  " " + Some(dummyString) will return " Some(dummyString)". the x symbol is required if the latter is desired
      " " x Some(dummyString) shouldBe Some(" dummyString")
    }
  }

}
