/*
 * Copyright 2022 HM Revenue & Customs
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
import forms.validation.util.ErrorMessagesUtilAPI._
import forms.validation.util.MappingUtilAPI._
import forms.validation.util.NamedMappingAndUtil._
import models.Supplier
import play.api.data.Forms._
import play.api.data.validation.{Invalid, Valid}
import play.api.data.{Form, Mapping}
import utils.AwrsFieldConfig
import utils.AwrsValidator._

object SupplierAddressesForm extends AwrsFieldConfig {
  private val addressPrefix = "supplierAddress"
  private val addressErrorMessageArg = "supplier"
  private val vatNumber = "vatNumber"
  private val whenThereAreAlcoholSuppliers = (data: FormData) => data.getOrElse("alcoholSupplier", "").equals(BooleanRadioEnum.Yes.toString)
  private val whenThereAreUkAlcoholSuppliers = (data: FormData) => data.getOrElse("ukSupplier", "").equals(BooleanRadioEnum.Yes.toString)
  private val whenSupplierIsVatRegistered = (data: FormData) => data.getOrElse("vatRegistered", "").equals(BooleanRadioEnum.Yes.toString)
  private val whenIsUkSupplierAnswered = (data: FormData) => BooleanRadioEnum.isEnumValue(data.getOrElse("ukSupplier", ""))
  private val alcoholSupplier_compulsory = yesNoQuestion_compulsory("alcoholSupplier", "awrs.supplier-addresses.alcohol_supplier_empty")
  private val isUkSupplier_compulsory = yesNoQuestion_compulsory("ukSupplier", "awrs.supplier-addresses.error.uk_supplier_blank")
  private val additionalSupplier_compulsory = yesNoQuestion_compulsory("additionalSupplier", "awrs.supplier-addresses.add_supplier_empty")

  def supplierName_compulsory: Mapping[Option[String]] = {
    val fieldId = "supplierName"
    val fieldNameInErrorMessage = "supplier name"
    val supplierNameConstraintParameters =
      CompulsoryTextFieldMappingParameter(
        simpleFieldIsEmptyConstraintParameter(fieldId, "awrs.supplier-addresses.error.supplier_name_blank"),
        FieldMaxLengthConstraintParameter(supplierNameLen, Invalid("awrs.generic.error.maximum_length", fieldNameInErrorMessage, supplierNameLen)),
        FieldFormatConstraintParameter((name: String) => if (validText(name)) Valid else Invalid("awrs.generic.error.character_invalid.summary", fieldNameInErrorMessage))
      )
    compulsoryText(supplierNameConstraintParameters)
  }

  @inline def vatRegistered_compulsory(): Mapping[Option[String]] =
    yesNoQuestion_compulsory("vatRegistered", "awrs.generic.error.supplier.do_you_have_vat_reg_empty")

  private def vatNumber_compulsory: Mapping[Option[String]] =
    commonIdConstraints(
      fieldId = "vatNumber",
      isEmptyErrMessage = "awrs.generic.their.error.vrn_empty",
      regex = vatRegex,
      isInvalidErrMessage = "awrs.generic.error.vrn_invalid"
    )

  def supplierAddressesValidationForm(implicit applicationConfig: ApplicationConfig): Form[Supplier] = Form(
    mapping(
      "alcoholSupplier" -> alcoholSupplier_compulsory,
      "supplierName" -> (supplierName_compulsory iff whenThereAreAlcoholSuppliers),
      "ukSupplier" -> (isUkSupplier_compulsory iff whenThereAreAlcoholSuppliers),
      addressPrefix -> (ukOrForeignAddressMapping(addressPrefix, addressErrorMessageArg, applicationConfig.countryCodes).toOptionalAddressMapping iff (whenThereAreAlcoholSuppliers &&& whenIsUkSupplierAnswered)),
      "vatRegistered" -> (vatRegistered_compulsory iff whenThereAreUkAlcoholSuppliers),
      vatNumber -> (vatNumber_compulsory iff whenSupplierIsVatRegistered),
      "additionalSupplier" -> (additionalSupplier_compulsory iff (whenThereAreAlcoholSuppliers &&& whenIsUkSupplierAnswered))
    )(Supplier.apply)(Supplier.unapply)
  )

  def supplierAddressesForm(implicit applicationConfig: ApplicationConfig): PrevalidationAPI[Supplier] = {
    PreprocessedForm(supplierAddressesValidationForm)
  }
}
