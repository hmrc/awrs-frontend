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
import forms.submapping.AddressMapping._
import forms.prevalidation._
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

  val businessPremisesValidationForm = Form(
    mapping(
      "additionalPremises" -> additionalPremises_compulsory,
      "additionalAddress" -> (ukAddress_compulsory(prefix = "additionalAddress").toOptionalAddressMapping iff whenThereAreAdditionalTradingPremises),
      "addAnother" -> (addAnother_compulsory iff whenThereAreAdditionalTradingPremises)
    )
    (AdditionalBusinessPremises.apply)(AdditionalBusinessPremises.unapply)
  )

  val businessPremisesForm = PreprocessedForm(businessPremisesValidationForm)
}
