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

package forms.submapping

import forms.AWRSEnums.BooleanRadioEnum
import forms.test.util._
import forms.validation.util.ConstraintUtil._
import forms.validation.util.FieldError
import models.{TupleDate, NewAWBusiness}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.play.test.UnitSpec

class NewAWBusinessMappingTest extends UnitSpec with MockitoSugar with OneServerPerSuite {

  import NewAWBusinessMapping._

  private case class TestForm(sub: NewAWBusiness)

  private implicit val testForm = Form(mapping(
    "prefix" -> newAWBusinessMapping("prefix")
  )(TestForm.apply)(TestForm.unapply))

  "newAWBusinessMapping sub mapping" should {
    "Correctly validate 'newAWBusiness'" in {
      // empty validation is correct, Yes and No are both valid entries
      val fieldId = "prefix" attach "newAWBusiness"
      val emptyError = ExpectedFieldIsEmpty(fieldId, FieldError("awrs.business_details.error.newAWBusiness_invalid"))
      val expectations = CompulsoryEnumValidationExpectations(emptyError, BooleanRadioEnum)
      fieldId assertEnumFieldIsCompulsory expectations

      // check that "true" and "false" are returned
      val dateId = "prefix" attach "proposedStartDate"
      val validDate = Map(f"$dateId.day" -> "01", f"$dateId.month" -> "01", f"$dateId.year" -> "2017")
      val testDataYes: FormData = FormData(fieldId -> "Yes") ++ validDate
      val testDataNo: FormData = FormData(fieldId -> "No") ++ validDate

      def testConvertedValues(data: FormData, expectedValue: String) = {
        val boundForm = testForm.bind(data)
        val caseClassValue = boundForm.value
        withClue(f"generated an error when it is unexpected\ntest data=$data\nerrors=${boundForm.errors}\n") {
          caseClassValue.isDefined shouldBe true
        }
        withClue(f"value did not match expected\ntest data=$data\nexpected=$expectedValue\n") {
          caseClassValue.get.sub.newAWBusiness shouldBe expectedValue
        }
      }
      testConvertedValues(testDataYes, "Yes")
      testConvertedValues(testDataNo, "No")
    }

    "Correctly validate 'tupleDate'" in {
      val newBusinessId = "prefix" attach "newAWBusiness"
      val testDataYes: FormData = FormData(newBusinessId -> "Yes")

      val dateId = "prefix" attach "proposedStartDate"
      val expectations = CompulsoryDateValidationExpectations(
        ExpectedFieldIsEmpty(dateId, FieldError("awrs.business_details.error.proposedDate_empty")),
        ExpectedDateFormat(
          List(
            ExpectedInvalidDateFormat(
              TupleDate("00", "00", "0000"),
              dateId,
              FieldError("awrs.generic.error.invalid.date")),
            ExpectedInvalidDateFormat(
              TupleDate("31", "03", "2016"),
              dateId,
              FieldError("awrs.business_details.error.proposedDate_toEarly"))
          ),
          List(
            // tests the too early range check function
            ExpectedValidDateFormat(TupleDate("01", "04", "2016"))
          )
        )
      )
      dateId assertDateFieldIsCompulsoryWhen(testDataYes, expectations)
    }
  }
}
