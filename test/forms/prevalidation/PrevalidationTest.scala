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

package forms.prevalidation

import forms.prevalidation.CaseOption.CaseOption
import forms.prevalidation.TrimOption.TrimOption
import play.api.data.Form
import play.api.data.Forms._
import utils.AwrsUnitTestTraits

class PrevalidationTest extends AwrsUnitTestTraits {

  case class DummyData(string1: String)

  object DummyForm {

    val dummyForm = Form[DummyData](
      mapping(
        "string1" -> text
      )(DummyData.apply)(DummyData.unapply)
    )

    def preprocessedForm(trims: Map[String, TrimOption] = Map(), caseRules: Map[String, CaseOption] = Map()) = PreprocessedForm(dummyForm, trims, caseRules)

  }

  def testData(data: String): Map[String, String] = Map[String, String]("string1" -> data)

  "Form submission " must {

    "remove all whitespace if it exists for option 'all'" in {
      val trimAll = Map[String, TrimOption](
        "string1" -> TrimOption.all
      )
      val form = DummyForm.preprocessedForm(trimAll)

      val result = form.bind(testData(" Vinnie and the \t    grenades    \t")).get
      result.string1 mustBe "Vinnieandthegrenades"
    }

    "trim any text strings at both ends and compress when additional whitespace exists for option 'bothAndCompress'" in {
      val trimBothAndCompress = Map[String, TrimOption](
        "string1" -> TrimOption.bothAndCompress
      )
      val form = DummyForm.preprocessedForm(trimBothAndCompress)

      val result = form.bind(testData(" Vinnie and the \t    grenades    \t")).get
      result.string1 mustBe "Vinnie and the grenades"
    }

    "not trim any text strings when additional whitespace exists for option 'none'" in {
      val trimNone = Map[String, TrimOption](
        "string1" -> TrimOption.none
      )
      val form = DummyForm.preprocessedForm(trimNone)

      val result = form.bind(testData(" Vinnie and the \t    grenades    \t")).get
      result.string1 mustBe " Vinnie and the \t    grenades    \t"
    }

    "amend the case of any text strings to uppercase for option 'upper'" in {
      val caseUpper = Map[String, CaseOption](
        "string1" -> CaseOption.upper
      )
      val form = DummyForm.preprocessedForm(caseRules = caseUpper)

      val result = form.bind(testData("Vinnie and the grenades")).get
      result.string1 mustBe "VINNIE AND THE GRENADES"
    }

    "amend the case of any text strings to lowercase for option 'lower'" in {
      val caseLower = Map[String, CaseOption](
        "string1" -> CaseOption.lower
      )
      val form = DummyForm.preprocessedForm(caseRules = caseLower)

      val result = form.bind(testData("Vinnie and the grenades")).get
      result.string1 mustBe "vinnie and the grenades"
    }

    "leave the case of any text strings for option 'none'" in {
      val caseNone = Map[String, CaseOption](
        "string1" -> CaseOption.none
      )
      val form = DummyForm.preprocessedForm(caseRules = caseNone)

      val result = form.bind(testData("Vinnie and the grenades")).get
      result.string1 mustBe "Vinnie and the grenades"
    }
  }
}
