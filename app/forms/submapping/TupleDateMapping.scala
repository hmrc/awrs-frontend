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

package forms.submapping

import forms.helper.FormHelper._
import forms.validation.util.ErrorMessagesUtilAPI._
import forms.validation.util.TargetFieldIds
import models.TupleDate
import play.api.data.Forms._
import play.api.data.format.Formatter
import play.api.data.validation.{Invalid, Valid, ValidationResult}
import play.api.data.{FieldMapping, FormError, Mapping}

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

  val yearMustBe4Digits: String => TupleDate => ValidationResult = (fieldKey: String) => (date: TupleDate) => date.year.matches("""^\d{4}$""") match {
    case true => Valid
    case _ => simpleErrorMessage(fieldKey, "awrs.business_details.error.year_toSmall")
  }

  private def missingDateField(data: Map[String, String], date: TupleDate): Either[List[String], TupleDate] = {
    val missingFields: List[String] = data
      .withFilter(_._2.isEmpty)
      .map(_._1)
      .toList

    missingFields.length match {
      case 1 | 2 => Left(missingFields)
      case _ => Right(date)
    }
  }
  val dateFieldNames = List("day", "month", "year")

  private def extractFieldNames(listOfFieldIds: List[String]): List[String] = {
    listOfFieldIds.map(fieldId => fieldId.split('.')).flatten.filter(dateFieldNames.contains(_))
  }

  def CompulsoryTupleDateFormatter(isEmptyErrMessage: (String) => Invalid,
                                   keyRequired: List[String]=> Invalid,
                                   twoKeysRequired: List[String] => Invalid,
                                   isInvalidErrMessage: (String) => Invalid,
                                   dateRangeCheck: Option[(String) => (TupleDate) => ValidationResult] = Some(yearMustBe4Digits),
                                   isTooEarlyCheck: Option[(String) => (TupleDate) => ValidationResult],
                                   isTooLateCheck: Option[(String) => (TupleDate) => ValidationResult]): Formatter[TupleDate] = new Formatter[TupleDate] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], TupleDate] = {
        val day = data.getOrElse(f"$key.day", "").trim()
        val month = data.getOrElse(f"$key.month", "").trim()
        val year = data.getOrElse(f"$key.year", "").trim()
        val date = TupleDate(day, month, year)

        def err(err: Invalid): Either[Seq[FormError], TupleDate] = Left(err.errors.map(ve => FormError(key, ve.message, ve.args)))

        def dateFieldsMissingError(err: Invalid, missingFields: List[String]): Either[Seq[FormError], TupleDate] =
          Left(err.errors.map(ve => FormError(missingFields.head, ve.message, ve.args ++ missingFields)))

        missingDateField(data, date) match {
          case Left(missingDateFields) if missingDateFields.length.equals(1) => dateFieldsMissingError(keyRequired(extractFieldNames(missingDateFields)), missingDateFields)
          case Left(missingDateFields) => dateFieldsMissingError(twoKeysRequired(extractFieldNames(missingDateFields)), missingDateFields)
          case Right(date) =>
            date match {
              case TupleDate("", "", "")    => err(isEmptyErrMessage(key))
              case _ if isInvalidDate(date) => err(isInvalidErrMessage(key))
              case _ =>
                dateRangeCheck match {
                  case Some(dateRangeFunction) =>
                    dateRangeFunction(key)(date) match {
                      case Valid =>
                        isTooEarlyCheck match {
                          case Some(tooEarlyFunction) =>
                            tooEarlyFunction(key)(date) match {
                              case Valid            => Right(date)
                              case invalid: Invalid => err(invalid)
                            }
                          case None =>
                            isTooLateCheck match {
                              case Some(tooLateFunction) =>
                                tooLateFunction(key)(date) match {
                                  case Valid            => Right(date)
                                  case invalid: Invalid => err(invalid)
                                }
                              case None => Right(date)
                            }
                        }
                      case invalid: Invalid => err(invalid)
                    }
                  case _ => Right(date)
                }
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
                           //keyRequired: String => Invalid  = simpleCrossFieldErrorMessage(TargetFieldIds("month", "year"), "key required"),
                           keyRequired: List[String] => Invalid  = dateFieldMissingErrorMessage(_, ("awrs.generic.error.date.fieldMissing")),
                           twoKeysRequired: List[String] => Invalid  = dateFieldMissingErrorMessage(_, ("awrs.generic.error.date.twoFieldsMissing")),
                           isInvalidErrMessage: (String) => Invalid,
                           dateRangeCheck: Option[(String) => TupleDate => ValidationResult] = Some(TupleDateMapping.yearMustBe4Digits),
                           isTooEarlyCheck: Option[(String) => TupleDate => ValidationResult],
                           isTooLateCheck: Option[(String) => TupleDate => ValidationResult]): FieldMapping[TupleDate] =
    of(CompulsoryTupleDateFormatter(isEmptyErrMessage, keyRequired, twoKeysRequired, isInvalidErrMessage, dateRangeCheck, isTooEarlyCheck, isTooLateCheck))

}
