/*
 * Copyright 2018 HM Revenue & Customs
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
import forms.validation.util.ConstraintUtil._
import forms.validation.util.ErrorMessageFactory._
import forms.validation.util.ErrorMessagesUtilAPI._
import forms.validation.util.MappingUtilAPI.{MappingUtil, _}
import forms.validation.util.NamedMappingAndUtil._
import forms.validation.util.{FieldErrorConfig, MessageArguments, SummaryErrorConfig, TargetFieldIds}
import play.api.data.Forms._
import play.api.data.Mapping
import play.api.data.validation.Valid
import utils.AwrsValidator._
import utils.{AwrsFieldConfig, CountryCodes}

object AddressMapping {

  import models.Address

  private def addressLinex_compulsory(line: Int, prefix: String, prefixRefNameInErrorMessage: String): Mapping[Option[String]] = {
    val fieldId = prefix attach f"addressLine$line"
    val fieldNameInErrorMessage = f"$prefixRefNameInErrorMessage address line $line"

    val emptyErrorMessage = createErrorMessage(
      TargetFieldIds(fieldId),
      FieldErrorConfig(f"awrs.generic.error.addressLine${line}_empty", MessageArguments(prefixRefNameInErrorMessage)),
      SummaryErrorConfig(MessageArguments(prefixRefNameInErrorMessage))
    )

    val addresslineConstraintParameters =
      CompulsoryTextFieldMappingParameter(
        FieldIsEmptyConstraintParameter(emptyErrorMessage),
        genericFieldMaxLengthConstraintParameter(AwrsFieldConfig.addressLine1Len, fieldId, fieldNameInErrorMessage),
        genericInvalidFormatConstraintParameter(validText, fieldId, fieldNameInErrorMessage)
      )
    compulsoryText(addresslineConstraintParameters)
  }

  private def addressLinex_optional(line: Int, prefix: String, prefixRefNameInErrorMessage: String): Mapping[Option[String]] = {
    val fieldId = prefix attach f"addressLine$line"
    val fieldNameInErrorMessage = f"$prefixRefNameInErrorMessage address line $line"
    val addresslineConstraintParameters =
      OptionalTextFieldMappingParameter(
        genericFieldMaxLengthConstraintParameter(AwrsFieldConfig.addressLine1Len, fieldId, fieldNameInErrorMessage),
        genericInvalidFormatConstraintParameter(validText, fieldId, fieldNameInErrorMessage)
      )
    optionalText(addresslineConstraintParameters)
  }

  // The type of mapping returned depends on if the form of the address mapping can accept foreign addresses
  private def postcode_compulsory(prefix: String, prefixRefNameInErrorMessage: String, canBeForeignAddress: Boolean): Mapping[Option[String]] = {
    val fieldId = prefix attach "postcode"

    val emptyErrorMessage = createErrorMessage(
      TargetFieldIds(fieldId),
      FieldErrorConfig("awrs.generic.error.postcode_empty", MessageArguments(prefixRefNameInErrorMessage)),
      SummaryErrorConfig(MessageArguments(prefixRefNameInErrorMessage))
    )

    val invalidPostcodeErrorMessage = Seq[FieldFormatConstraintParameter](
      FieldFormatConstraintParameter(
        (postcode: String) => postcode.matches(postcodeRegex) match {
          case true =>
            Valid
          case false =>
            createErrorMessage(
              TargetFieldIds(fieldId),
              FieldErrorConfig("awrs.generic.error.postcode_invalid", MessageArguments(prefixRefNameInErrorMessage)),
              SummaryErrorConfig(MessageArguments(prefixRefNameInErrorMessage)))
        }
      )
    )

    val postcodeConstraintParameters =
      CompulsoryTextFieldMappingParameter(
        FieldIsEmptyConstraintParameter(emptyErrorMessage),
        MaxLengthConstraintIsHandledByTheRegEx(),
        invalidPostcodeErrorMessage
      )

    val postcodeMapping = compulsoryText(postcodeConstraintParameters)

    canBeForeignAddress match {
      case true => postcodeMapping iff whenThisIsUKAddress
      case false => postcodeMapping
    }
  }

  private def addressCountry_compulsory(prefix: String, prefixRefNameInErrorMessage: String, canBeForeignAddress: Boolean): Mapping[Option[String]] = {
    canBeForeignAddress match {
      case false => optional(text)
      case true =>
        val fieldId = prefix attach "addressCountry"

        val invalidCountryCodeErrorMessage = Seq[FieldFormatConstraintParameter](
          FieldFormatConstraintParameter(
            (addressCountry: String) => CountryCodes.getCountryCode(addressCountry) match {
              case Some(code) => Valid
              case _ =>
                simpleErrorMessage(fieldId, "awrs.supplier-addresses.error.supplier_address_country_invalid")
            }
          )
        )

        val addressCountryParameters =
          CompulsoryTextFieldMappingParameter(
            simpleFieldIsEmptyConstraintParameter(fieldId, "awrs.supplier-addresses.error.supplier_address_country_blank"),
            MaxLengthConstraintIsHandledByTheRegEx(),
            invalidCountryCodeErrorMessage
          )

        val addressCountryMapping = compulsoryText(addressCountryParameters)

        addressCountryMapping iff whenThisIsForeignAddress
    }
  }

  private def addressCountryCode_compulsory(prefix: String, prefixRefNameInErrorMessage: String, canBeForeignAddress: Boolean): Mapping[Option[String]] = {
    canBeForeignAddress match {
      case false => optional(text)
      case true =>
        //TODO find out if we're using this field at the moment
        optional(text) iff whenThisIsForeignAddress
    }
  }

  private val isUkAddressFieldId = "ukSupplier"

  private val whenUkSupplierIsAnsweredWith = (answer: BooleanRadioEnum.Value) =>
    (data: Map[String, String]) =>
      whenAnswerToIdTypeIs(isUkAddressFieldId, answer)(data: Map[String, String])

  private val whenThisIsUKAddress =
    whenUkSupplierIsAnsweredWith(BooleanRadioEnum.Yes)

  private val whenThisIsForeignAddress =
    whenUkSupplierIsAnsweredWith(BooleanRadioEnum.No)


  // Reusable address mapping
  private def addressMapping(prefix: String, prefixRefNameInErrorMessage: String, canBeForeignAddress: Boolean = false): Mapping[Address] = mapping(
    "addressLine1" -> addressLinex_compulsory(1, prefix, prefixRefNameInErrorMessage).toStringFormatter,
    "addressLine2" -> addressLinex_compulsory(2, prefix, prefixRefNameInErrorMessage).toStringFormatter,
    "addressLine3" -> addressLinex_optional(3, prefix, prefixRefNameInErrorMessage),
    "addressLine4" -> addressLinex_optional(4, prefix, prefixRefNameInErrorMessage),
    "postcode" -> postcode_compulsory(prefix, prefixRefNameInErrorMessage, canBeForeignAddress),
    "addressCountry" -> addressCountry_compulsory(prefix, prefixRefNameInErrorMessage, canBeForeignAddress),
    "addressCountryCode" -> addressCountryCode_compulsory(prefix, prefixRefNameInErrorMessage, canBeForeignAddress)
  )(Address.apply)(Address.unapply)

  def ukAddress_compulsory(prefix: String = "", prefixRefNameInErrorMessage: String = " "): Mapping[Address] =
    addressMapping(prefix, prefixRefNameInErrorMessage)

  def ukOrForeignAddressMapping(prefix: String = "", prefixRefNameInErrorMessage: String = " "): Mapping[Address] =
    addressMapping(prefix, prefixRefNameInErrorMessage, canBeForeignAddress = true)

  implicit class AddressUtil(addressMapping: Mapping[Address]) {
    def toOptionalAddressMapping: Mapping[Option[Address]] =
      addressMapping.transform[Option[Address]](
        (value: Address) => Some(value),
        (value: Option[Address]) => value.getOrElse(Address("", "", Some(""), Some(""), Some(""), Some(""), Some(""))))
  }

}
