/*
 * Copyright 2019 HM Revenue & Customs
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
import forms.validation.util.ErrorMessagesUtilAPI._
import models.TupleDate
import play.api.data.Forms._
import play.api.data.format.Formatter
import play.api.data.validation.{Invalid, Valid, ValidationResult}
import play.api.data.{FormError, Mapping}

object TupleDateMapping {

  private val tupleToOptionalTuple = (value: TupleDate) => Some(value)
  private val optionalTupleToTuple = (value: Option[TupleDate]) => value.getOrElse(TupleDate("", "", ""))

  implicit class TupleDateUtil(tupleMapping: Mapping[TupleDate]) {
    def toOptionalTupleDate: Mapping[Option[TupleDate]] =
      tupleMapping.transform[Option[TupleDate]](
        tupleToOptionalTuple,
        optionalTupleToTuple)
  }

  implicit class TupleDateReverseUtil(tupleMapping: Mapping[Option[TupleDate]]) {
    def toTupleDate: Mapping[TupleDate] =
      tupleMapping.transform[TupleDate](
        optionalTupleToTuple,
        tupleToOptionalTuple)
  }

  val yearMustBe4Digits = (fieldKey: String) => (date: TupleDate) => date.year.matches("""^\d{4}$""") match {
    case true => Valid
    case _ => simpleErrorMessage(fieldKey, "awrs.business_details.error.year_toSmall")
  }

  def CompulsoryTupleDateFormatter(isEmptyErrMessage: (String) => Invalid,
                                   isInvalidErrMessage: (String) => Invalid,
                                   dateRangeCheck: Option[(String) => (TupleDate) => ValidationResult] = Some(yearMustBe4Digits)) = new Formatter[TupleDate] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], TupleDate] = {
      val day = data.getOrElse(f"$key.day", "").trim()
      val month = data.getOrElse(f"$key.month", "").trim()
      val year = data.getOrElse(f"$key.year", "").trim()
      val date = TupleDate(day, month, year)

      def err(err: Invalid): Either[Seq[FormError], TupleDate] = Left(err.errors.map(ve => FormError(key, ve.message, ve.args)))

      date match {
        case TupleDate("", "", "") => err(isEmptyErrMessage(key))
        case _ if isInvalidDate(date) => err(isInvalidErrMessage(key))
        case _ => dateRangeCheck match {
          case Some(dateRangeFunction) => dateRangeFunction(key)(date) match {
            case Valid => Right(date)
            case invalid: Invalid => err(invalid)
          }
          case _ => Right(date)
        }
      }
    }

    override def unbind(key: String, value: TupleDate): Map[String, String] =
      Map(f"$key.day" -> value.day,
        f"$key.month" -> value.month,
        f"$key.year" -> value.year
      )
  }


  def tupleDate_compulsory(isEmptyErrMessage: (String) => Invalid,
                           isInvalidErrMessage: (String) => Invalid,
                           dateRangeCheck: Option[(String) => (TupleDate) => ValidationResult] = Some(TupleDateMapping.yearMustBe4Digits)) =
    of(CompulsoryTupleDateFormatter(isEmptyErrMessage, isInvalidErrMessage, dateRangeCheck))


}
