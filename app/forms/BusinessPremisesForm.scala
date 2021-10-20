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

import config.ApplicationConfig
import forms.AWRSEnums.BooleanRadioEnum
import forms.prevalidation._
import forms.submapping.AddressMapping._
import forms.validation.util.ConstraintUtil._
import forms.validation.util.MappingUtilAPI._
import forms.validation.util.NamedMappingAndUtil._
import models.AdditionalBusinessPremises
import play.api.data.Form
import play.api.data.Forms._

object BusinessPremisesForm {
  private val whenThereAreAdditionalTradingPremises = (data: FormData) => data.getOrElse("additionalPremises", "").equals(BooleanRadioEnum.Yes.toString)
  private val additionalPremises_compulsory = yesNoQuestion_compulsory("additionalPremises", "awrs.additional-premises.error.do_you_have_additional_premises")
  private val addAnother_compulsory = yesNoQuestion_compulsory("addAnother", "awrs.additional-premises.error.add_another")

  def businessPremisesValidationForm(implicit applicationConfig: ApplicationConfig): Form[AdditionalBusinessPremises] = Form(
    mapping(
      "additionalPremises" -> additionalPremises_compulsory,
      "additionalAddress" -> (ukAddress_compulsory(prefix = "additionalAddress", "", applicationConfig.countryCodes).toOptionalAddressMapping iff whenThereAreAdditionalTradingPremises),
      "addAnother" -> (addAnother_compulsory iff whenThereAreAdditionalTradingPremises)
    )
    (AdditionalBusinessPremises.apply)(AdditionalBusinessPremises.unapply)
  )

  def businessPremisesForm(implicit applicationConfig: ApplicationConfig): PrevalidationAPI[AdditionalBusinessPremises] = PreprocessedForm(businessPremisesValidationForm)
}
