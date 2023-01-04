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

package views.view_application

import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.PlaySpec
import views.view_application.ViewApplicationHelper._

class ViewApplicationHelperTest extends PlaySpec with MockitoSugar with BeforeAndAfterEach with GuiceOneServerPerSuite {

  type TestData = Seq[Option[String]]
  val dummyString = "dummyString"

  object TestData {
    def apply(input: Option[String]*): TestData = Seq(input: _*)
  }

  "One view helper content counter" must {

    case class Expectations(count: Int)
    case class TestCase(data: TestData, expecations: Expectations)



    def runTests(testCases: TestCase*): Unit = {
      val rowTitle = "does not matter"
      testCases.foreach(testCase => countContent(testCase.data) mustBe testCase.expecations.count)
    }

    "return the count of strings when the sequence has some or none values " in {
      val testCases = Seq(
        TestCase(TestData(Some(dummyString), Some(dummyString)), Expectations(2)),
        TestCase(TestData(Some(dummyString), Some(dummyString), Some(dummyString)), Expectations(3)),
        TestCase(TestData(Some(dummyString), None), Expectations(1)),
        TestCase(TestData(Some(dummyString), Some("")), Expectations(1))
      )

      runTests(testCases: _*)
    }

    "return the count as 0 when the sequence has all None or empty values" in {
      val testCases = Seq(
        TestCase(TestData(Some(""), None), Expectations(0)),
        TestCase(TestData(Some(""), Some("")), Expectations(0)),
        TestCase(TestData(), Expectations(0))
      )

      runTests(testCases: _*)
    }
  }

  "One view helper Option String Util " must {

    "concatenate the strings " in {
      Some(Some(dummyString).getOrElse("") + " " + Some(dummyString).getOrElse("")) mustBe Some("dummyString dummyString")
      Some(Some(dummyString).getOrElse("") + "") mustBe Some("dummyString")
      None + Some(dummyString) mustBe Some("dummyString")
      //  " " + Some(dummyString) will return " Some(dummyString)". the x symbol is required if the latter is desired
      " " x Some(dummyString) mustBe Some(" dummyString")
    }
  }

}
