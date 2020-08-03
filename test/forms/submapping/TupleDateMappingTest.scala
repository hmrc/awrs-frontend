/*
 * Copyright 2020 HM Revenue & Customs
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

import forms.helper.FormHelper._
import forms.test.util._
import forms.validation.util.ErrorMessagesUtilAPI._
import forms.validation.util.FieldError
import models.TupleDate
import org.joda.time.LocalDate
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.api.data.Forms._
import play.api.data.validation.{Valid, ValidationResult}
import play.api.data.{Form, Mapping}
import org.scalatestplus.play.PlaySpec


class TupleDateMappingTest extends PlaySpec with MockitoSugar  with AwrsFormTestUtils {

  import TupleDateMapping._

  private val earliestDate = "01/04/2016"

  private val isTooEarly = (fieldKey: String) => (date: TupleDate) => isDateAfterOrEqual(earliestDate,
    new LocalDate(date.year.trim.toInt, date.month.trim.toInt, date.day.trim.toInt).toDate) match {
    case true => Valid
    case false => simpleErrorMessage(fieldKey, "testkey.tooEarly")
  }

  private case class TestForm(sub: TupleDate, sub2: TupleDate)

  private def testDate_compulsory(dateRangeCheck: (String) => (TupleDate) => ValidationResult): Mapping[TupleDate] =
    tupleDate_compulsory(
      isEmptyErrMessage = simpleErrorMessage(_, "testkey.emptyDate"),
      isInvalidErrMessage = simpleErrorMessage(_, "testkey.invalidDate"),
      dateRangeCheck = Some(dateRangeCheck)
    )

  private implicit val testForm = Form(mapping(
    "prefix" -> testDate_compulsory(isTooEarly),
    "prefix2" -> testDate_compulsory(yearMustBe4Digits)
  )(TestForm.apply)(TestForm.unapply))


  "tupleDate_compulsory sub mapping" must {
    "Correctly validate 'tupleDate'" in {
      val fieldId = "prefix"
      val expectations = CompulsoryDateValidationExpectations(
        ExpectedFieldIsEmpty(fieldId, FieldError("testkey.emptyDate")),
        ExpectedDateFormat(
          List(
            ExpectedInvalidDateFormat(
              TupleDate("01", "01", ""),
              fieldId,
              FieldError("testkey.invalidDate")),
            ExpectedInvalidDateFormat(
              TupleDate("01", "13", "2000"),
              fieldId,
              FieldError("testkey.invalidDate")),
            ExpectedInvalidDateFormat(
              TupleDate("00", "01", "2000"),
              fieldId,
              FieldError("testkey.invalidDate")),
            ExpectedInvalidDateFormat(
              TupleDate("30", "02", "2000"),
              fieldId,
              FieldError("testkey.invalidDate")),
            // tests the too early range check function
            ExpectedInvalidDateFormat(
              TupleDate("31", "03", "2016"),
              fieldId,
              FieldError("testkey.tooEarly"))
          ),
          List(
            // tests the too early range check function
            ExpectedValidDateFormat(TupleDate("01", "04", "2016")),
            ExpectedValidDateFormat(TupleDate("29", "02", "2020"))
          )
        )
      )
      fieldId assertDateFieldIsCompulsory expectations
    }

    "yearMustBe4Digits must enforce users to enter 4 digits in the year field" in {
      val fieldId = "prefix2"
      val expectations = CompulsoryDateValidationExpectations(
        ExpectedFieldIsEmpty(fieldId, FieldError("testkey.emptyDate")),
        ExpectedDateFormat(
          List(
            ExpectedInvalidDateFormat(
              TupleDate("31", "03", "16"),
              fieldId,
              FieldError("awrs.business_details.error.year_toSmall"))
          ),
          List(
            ExpectedValidDateFormat(TupleDate("01", "04", "0016")),
            ExpectedValidDateFormat(TupleDate("29", "02", "2020"))
          )
        )
      )
      fieldId assertDateFieldIsCompulsory expectations
    }
  }
}
