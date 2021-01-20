/*
 * Copyright 2021 HM Revenue & Customs
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

import forms.helper.FormHelper.{isDateAfterOrEqual, isDateBefore}
import forms.submapping.TupleDateMapping.{tupleDate_compulsory, _}
import forms.validation.util.ErrorMessagesUtilAPI.simpleErrorMessage
import models.TupleDate
import org.joda.time.LocalDate
import play.api.data.Forms._
import play.api.data.validation.Valid
import play.api.data.{Form, Mapping}

object TradingDateForm {

  override def hashCode(): Int = super.hashCode()

  private val isInThePast = (fieldKey: String) =>
    (date: TupleDate) => {
      val enteredDate = new LocalDate(date.year.trim.toInt,
                                      date.month.trim.toInt,
                                      date.day.trim.toInt).toDate
      if (enteredDate.before(LocalDate.now().toDate)) {
        Valid
      } else {
        simpleErrorMessage(
          fieldKey,
          "awrs.business_details.error.proposedDate_inTheFuture")
      }
  }

  private val cutOffAWBusinessStartDate = "01/04/2016"

  private val isTooEarly = (fieldKey: String) =>
    (date: TupleDate) => {
      val providedDate = new LocalDate(date.year.trim.toInt,
                                       date.month.trim.toInt,
                                       date.day.trim.toInt).toDate

      if (isDateAfterOrEqual(cutOffAWBusinessStartDate, providedDate)) {
        Valid
      } else {
        simpleErrorMessage(fieldKey,
                           "awrs.business_details.error.proposedDate_tooEarly")
      }
  }

  private val isTooLate = (fieldKey: String) =>
    (date: TupleDate) => {
      val providedDate = new LocalDate(date.year.trim.toInt,
                                       date.month.trim.toInt,
                                       date.day.trim.toInt).toDate

      if (isDateBefore(cutOffAWBusinessStartDate, providedDate)) {
        Valid
      } else {
        simpleErrorMessage(fieldKey,
                           "awrs.business_details.error.proposedDate_tooLate")
      }
  }

  private val daysInTheFuture = 45
  private val farEnoughInFuture = (fieldKey: String) =>
    (date: TupleDate) => {
      val enteredDate = new LocalDate(date.year.trim.toInt,
                                      date.month.trim.toInt,
                                      date.day.trim.toInt).toDate
      if (!enteredDate.before(LocalDate.now().plusDays(daysInTheFuture).toDate)) {
        Valid
      } else {
        simpleErrorMessage(
          fieldKey,
          "awrs.business_details.error.proposedDate_lessThan45DaysAhead")
      }
  }

  def proposedStartDate_compulsory: Mapping[Option[TupleDate]] =
    tupleDate_compulsory(
      isEmptyErrMessage =
        simpleErrorMessage(_,
                           "awrs.business_details.error.proposedDate_emptyP"),
      isInvalidErrMessage =
        simpleErrorMessage(_, "awrs.generic.error.invalid.date.summary"),
      dateRangeCheck = Some(farEnoughInFuture(_)),
      isTooEarlyCheck = None,
      isTooLateCheck = None
    ).toOptionalTupleDate

  def didYouStartDate_compulsoryNewAWBusiness: Mapping[Option[TupleDate]] =
    tupleDate_compulsory(
      isEmptyErrMessage =
        simpleErrorMessage(_, "awrs.business_details.error.proposedDate_pastP"),
      isInvalidErrMessage =
        simpleErrorMessage(_, "awrs.generic.error.invalid.date.summary"),
      dateRangeCheck = Some(isInThePast(_)),
      isTooEarlyCheck = None,
      isTooLateCheck = Some(isTooLate(_))
    ).toOptionalTupleDate

  def didYouStartDate_compulsoryNewBusiness: Mapping[Option[TupleDate]] =
    tupleDate_compulsory(
      isEmptyErrMessage =
        simpleErrorMessage(_, "awrs.business_details.error.proposedDate_pastP"),
      isInvalidErrMessage =
        simpleErrorMessage(_, "awrs.generic.error.invalid.date.summary"),
      dateRangeCheck = Some(isInThePast(_)),
      isTooEarlyCheck = Some(isTooEarly(_)),
      isTooLateCheck = None
    ).toOptionalTupleDate

  def tradingDateForm(past: Boolean,
                      newBusiness: Option[Boolean]): Form[TupleDate] = {

    val awMapping = if (newBusiness.getOrElse(false)) {
      didYouStartDate_compulsoryNewBusiness
    } else {
      didYouStartDate_compulsoryNewAWBusiness
    }

    Form(
      mapping(
        "tradingDate" -> (if (past) awMapping else proposedStartDate_compulsory)
      )((s: Option[TupleDate]) => s.get)((s: TupleDate) => Option(Option(s)))
    )
  }
}
