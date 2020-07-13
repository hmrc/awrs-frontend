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

package forms

import forms.test.util._
import forms.validation.util.FieldError
import models.TupleDate
import models.TupleDate._
import org.joda.time.LocalDate
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import uk.gov.hmrc.play.test.UnitSpec

class DeRegistrationFormTest extends UnitSpec with MockitoSugar with OneServerPerSuite {

  implicit val testForm = DeRegistrationForm.deRegistrationForm

  "De-Registration Form" should {
    "Correctly validate 'tupleDate'" in {
      val fieldId = "proposedEndDate"
      val expectations = CompulsoryDateValidationExpectations(
        ExpectedFieldIsEmpty(fieldId, FieldError("awrs.de_registration.error.date_empty")),
        ExpectedDateFormat(
          List(
            ExpectedInvalidDateFormat(
              TupleDate("01", "01", ""),
              fieldId,
              FieldError("awrs.de_registration.error.date_valid")),
            ExpectedInvalidDateFormat(
              TupleDate("01", "13", "2000"),
              fieldId,
              FieldError("awrs.de_registration.error.date_valid")),
            ExpectedInvalidDateFormat(
              TupleDate("00", "01", "2000"),
              fieldId,
              FieldError("awrs.de_registration.error.date_valid")),
            ExpectedInvalidDateFormat(
              TupleDate("30", "02", "2000"),
              fieldId,
              FieldError("awrs.de_registration.error.date_valid")),
            ExpectedInvalidDateFormat(
              LocalDate.now().minusDays(1),
              fieldId,
              FieldError("awrs.de_registration.error.proposedDate_toEarly")),
            ExpectedInvalidDateFormat(
              LocalDate.now().plusYears(100).plusDays(1),
              fieldId,
              FieldError("awrs.de_registration.error.date_valid"))
          ),
          List(
            ExpectedValidDateFormat(
              LocalDate.now()
            ),
            ExpectedValidDateFormat(
              LocalDate.now().plusYears(100).minusDays(1)
            )
          )
        )
      )
      fieldId assertDateFieldIsCompulsory expectations
    }
  }
}
