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

import forms.AWRSEnums.BooleanRadioEnum
import forms.prevalidation._
import forms.submapping.NewAWBusinessMapping._
import forms.validation.util.ConstraintUtil.{CompulsoryEnumMappingParameter, CompulsoryTextFieldMappingParameter}
import forms.validation.util.ErrorMessagesUtilAPI._
import forms.validation.util.MappingUtilAPI._
import forms.validation.util.NamedMappingAndUtil._
import models._
import play.api.data.Forms._
import play.api.data.{Form, Mapping}
import utils.{AccountUtils, AwrsFieldConfig}
import utils.AwrsValidator._

object BusinessDetailsForm {

  val businessName = "businessName"
  val doYouHaveTradingName = "doYouHaveTradingName"
  val tradingName = "tradingName"
  val newAWBusiness = "newAWBusiness"
  val businessNameUpdated = "businessNameUpdated"

  private val groupIds = Seq[String](businessName, doYouHaveTradingName, tradingName, newAWBusiness)
  private val otherIds = Seq[String](doYouHaveTradingName, tradingName, newAWBusiness)

  private val entityIds = (entityType: String, hasAwrs: Boolean) => entityType match {
    case "LLP_GRP" | "LTD_GRP" => {
      hasAwrs match {
        case true => groupIds
        case _ => otherIds
      }
    }
    case _ => otherIds
  }

  private val nbToOptional = (business: NewAWBusiness) => Some(business): Option[NewAWBusiness]
  private val nbFromOptional = (business: Option[NewAWBusiness]) => business.fold(NewAWBusiness("", None))(x => x): NewAWBusiness

  val businessDetailsValidationForm = (entityType: String, hasAwrs: Boolean) => {
    val ids = entityIds(entityType, hasAwrs)
    Form(
      mapping(
        businessName -> (companyName_compulsory() iff ids.contains(businessName)),
        doYouHaveTradingName -> doYouHaveTradingName_compulsory,
        tradingName -> (tradingName_compulsory iff whenDoYouHaveTradingNameIsAnsweredYes),
        newAWBusiness -> newAWBusinessMapping(newAWBusiness).transform(nbToOptional, nbFromOptional),
        businessNameUpdated -> ignored(false)
      )(ExtendedBusinessDetails.apply)(ExtendedBusinessDetails.unapply)
    )
  }

  val businessDetailsForm = (entityType: String, hasAwrs: Boolean) => PreprocessedForm(businessDetailsValidationForm(entityType, hasAwrs))
}