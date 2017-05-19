/*
 * Copyright 2017 HM Revenue & Customs
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

import forms.prevalidation._
import forms.submapping.NewAWBusinessMapping._
import forms.validation.util.ConstraintUtil.{CompulsoryEnumMappingParameter, CompulsoryTextFieldMappingParameter}
import forms.validation.util.MappingUtilAPI._
import forms.validation.util.NamedMappingAndUtil._
import models._
import play.api.data.Forms._
import play.api.data.{Form}

object BusinessDetailsForm {

  val doYouHaveTradingName = "doYouHaveTradingName"
  val tradingName = "tradingName"
  val newAWBusiness = "newAWBusiness"

  private val nbToOptional = (business: NewAWBusiness) => Some(business): Option[NewAWBusiness]
  private val nbFromOptional = (business: Option[NewAWBusiness]) => business.fold(NewAWBusiness("", None))(x => x): NewAWBusiness

  val businessDetailsValidationForm = (entityType: String) => Form(
    mapping(
      doYouHaveTradingName -> doYouHaveTradingName_compulsory,
      tradingName -> (tradingName_compulsory iff whenDoYouHaveTradingNameIsAnsweredYes),
      newAWBusiness -> newAWBusinessMapping(newAWBusiness).transform(nbToOptional, nbFromOptional)
    )(BusinessDetails.apply)(BusinessDetails.unapply))

  val businessDetailsForm = (entityType: String) => PreprocessedForm(businessDetailsValidationForm(entityType))
}

object BusinessNameChangeConfirmationForm {

  val businessNameChangeConfirmationForm = Form(mapping(
    "businessNameChangeConfirmation" -> yesNoQuestion_compulsory(fieldId = "businessNameChangeConfirmation", errorMessageId = "awrs.business_name_change.error.empty")
  )(BusinessNameChangeConfirmation.apply)(BusinessNameChangeConfirmation.unapply))

}

