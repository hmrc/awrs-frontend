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
import org.scalatestplus.play.PlaySpec
import play.api.data.Forms._
import play.api.data.validation.Valid
import play.api.data.{Form, Mapping}


class TupleDateMappingTest extends PlaySpec with MockitoSugar  with AwrsFormTestUtils {

  import TupleDateMapping._

  private val earliestDate = "01/04/2016"

  private val isTooEarly = (fieldKey: String) => (date: TupleDate) => isDateAfterOrEqual(earliestDate,
    new LocalDate(date.year.trim.toInt, date.month.trim.toInt, date.day.trim.toInt).toDate) match {
    case true => Valid
    case false => simpleErrorMessage(fieldKey, "testkey.tooEarly")
  }

  private val latestDate = "01/04/2016"

  private val isTooLate = (fieldKey: String) => (date: TupleDate) => isDateBefore(latestDate,
    new LocalDate(date.year.trim.toInt, date.month.trim.toInt, date.day.trim.toInt).toDate) match {
    case true => Valid
    case false => simpleErrorMessage(fieldKey, "testkey.tooLate")
  }

  private case class TestForm(sub: TupleDate, sub2: TupleDate)

  private def testDate_compulsory(newBusiness: Boolean): Mapping[TupleDate] =
    tupleDate_compulsory(
      isEmptyErrMessage = simpleErrorMessage(_, "testkey.emptyDate"),
      isInvalidErrMessage = simpleErrorMessage(_, "testkey.invalidDate"),
      dateRangeCheck = Some(yearMustBe4Digits),
      isTooEarlyCheck = if(newBusiness) Some(isTooEarly) else None,
      isTooLateCheck = if(!newBusiness) Some(isTooLate) else None
    )

  private implicit val testForm = Form(mapping(
    "newBusTrue" -> testDate_compulsory(true),
    "newBusFalse" -> testDate_compulsory(false)
  )(TestForm.apply)(TestForm.unapply))

  "tupleDate_compulsory sub mapping" must {
    "Correctly validate 'tupleDate' for new business (started trading after 31st March 2016)" in {
      val fieldId = "newBusTrue"
      val expectations = CompulsoryDateValidationExpectations(
        ExpectedFieldIsEmpty(fieldId, FieldError("testkey.emptyDate")),
        ExpectedDateFormat(
          List(
            ExpectedInvalidDateFormat(
              TupleDate("31", "03", "16"),
              fieldId,
              FieldError("awrs.business_details.error.year_toSmall")),
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

    "Correctly validate 'tupleDate' for old business (started trading before 1st April 2016)" in {
      val fieldId = "newBusFalse"
      val expectations = CompulsoryDateValidationExpectations(
        ExpectedFieldIsEmpty(fieldId, FieldError("testkey.emptyDate")),
        ExpectedDateFormat(
          List(
            ExpectedInvalidDateFormat(
              TupleDate("01", "04", "2016"),
              fieldId,
              FieldError("testkey.tooLate")
            )
          ),
          List(
            ExpectedValidDateFormat(TupleDate("31", "03", "2016"))
          )
        )
      )
      fieldId assertDateFieldIsCompulsory expectations
    }
  }
}
