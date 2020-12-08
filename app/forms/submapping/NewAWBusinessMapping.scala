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

import forms.AWRSEnums.BooleanRadioEnum
import forms.TradingDateForm.isTooEarly
import forms.helper.FormHelper._
import forms.submapping.TupleDateMapping._
import forms.validation.util.ConstraintUtil.CompulsoryEnumMappingParameter
import forms.validation.util.ErrorMessagesUtilAPI._
import forms.validation.util.MappingUtilAPI._
import forms.validation.util.NamedMappingAndUtil._
import models.{NewAWBusiness, TupleDate}
import org.joda.time.LocalDate
import play.api.data.Forms._
import play.api.data.Mapping
import play.api.data.validation.Valid


object NewAWBusinessMapping {

  private val cutOffAWBusinessStartDate = "01/04/2016"

  val newBusiness_compulsoryBoolean = (prefix: String) =>
    compulsoryEnum(CompulsoryEnumMappingParameter(
      simpleFieldIsEmptyConstraintParameter(
        prefix attach "newAWBusiness",
        "awrs.business_details.error.newAWBusiness_invalid"),
      BooleanRadioEnum
    )).toStringFormatter

  private val isTooEarly = (fieldKey: String) => (date: TupleDate) => isDateAfterOrEqual(cutOffAWBusinessStartDate,
    new LocalDate(date.year.trim.toInt, date.month.trim.toInt, date.day.trim.toInt).toDate) match {
    case true => Valid
    case false => simpleErrorMessage(fieldKey, "awrs.business_details.error.proposedDate_tooEarly")
  }

  def proposedStartDate_compulsory: Mapping[Option[TupleDate]] =
    tupleDate_compulsory(
      isEmptyErrMessage = simpleErrorMessage(_, "awrs.business_details.error.proposedDate_empty"),
      isInvalidErrMessage = simpleErrorMessage(_, "awrs.generic.error.invalid.date"),
      dateRangeCheck = Some(isTooEarly(_)),
      isTooEarlyCheck = None,
      isTooLateCheck = None).toOptionalTupleDate

  val whenNewBusinessIsAnsweredYes = (prefix: String) => answerGivenInFieldIs(prefix attach "newAWBusiness", "Yes")

  // Reusable NewAWBusiness mapping
  def newAWBusinessMapping(prefix: String) = mapping(
    "newAWBusiness" -> newBusiness_compulsoryBoolean(prefix), //conversion to boolean is currently done in the middle service
    "proposedStartDate" -> (proposedStartDate_compulsory iff whenNewBusinessIsAnsweredYes(prefix))
  )(NewAWBusiness.apply)(NewAWBusiness.unapply)
}
