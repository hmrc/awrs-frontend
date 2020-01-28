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

import config.ApplicationConfig
import forms.AWRSEnums.{BooleanRadioEnum, OperatingDurationEnum}
import forms.prevalidation._
import forms.submapping.AddressMapping._
import forms.validation.util.ConstraintUtil._
import forms.validation.util.ErrorMessagesUtilAPI._
import forms.validation.util.MappingUtilAPI._
import forms.validation.util.NamedMappingAndUtil._
import models.PlaceOfBusiness
import play.api.data.Form
import play.api.data.Forms._

object PlaceOfBusinessForm {

  private val principalAddress_compulsory = yesNoQuestion_compulsory("mainPlaceOfBusiness", "awrs.business_contacts.error.place_of_business_empty")
  private val whenPrincipalAddressNo = whenAnswerToFieldIs("mainPlaceOfBusiness", BooleanRadioEnum.No.toString)(_)
  private val previousAddress_compulsory = yesNoQuestion_compulsory("placeOfBusinessLast3Years", "awrs.business_contacts.error.place_of_business_changed_last_3_years_empty")
  private val whenPreviousAddressNo = whenAnswerToFieldIs("placeOfBusinessLast3Years", BooleanRadioEnum.No.toString)(_)
  private val operatingDuration_compulsory = {
    val question = CompulsoryEnumMappingParameter(
      simpleFieldIsEmptyConstraintParameter("operatingDuration", "awrs.business_contacts.error.operating_duration_empty"),
      OperatingDurationEnum
    )
    compulsoryEnum(question)
  }

  def placeOfBusinessValidationForm(implicit applicationConfig: ApplicationConfig) = Form(mapping(
    "mainPlaceOfBusiness" -> principalAddress_compulsory,
    "mainAddress" -> (ukAddress_compulsory(prefix = "mainAddress", prefixRefNameInErrorMessage = "principal place of business", applicationConfig.countryCodes).toOptionalAddressMapping iff whenPrincipalAddressNo),
    "placeOfBusinessLast3Years" -> previousAddress_compulsory,
    "placeOfBusinessAddressLast3Years" -> (ukAddress_compulsory(prefix = "placeOfBusinessAddressLast3Years", prefixRefNameInErrorMessage = "previous principal place of business", applicationConfig.countryCodes).toOptionalAddressMapping iff whenPreviousAddressNo),
    "operatingDuration" -> operatingDuration_compulsory,
    "modelVersion" -> ignored[String](PlaceOfBusiness.latestModelVersion)
  )(PlaceOfBusiness.apply)(PlaceOfBusiness.unapply))

  def placeOfBusinessForm(implicit applicationConfig: ApplicationConfig): PrevalidationAPI[PlaceOfBusiness] = PreprocessedForm(placeOfBusinessValidationForm)
}
