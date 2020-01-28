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

import forms.validation.util.ConstraintUtil._
import forms.validation.util.ErrorMessagesUtilAPI._
import forms.validation.util.MappingUtilAPI._
import forms.validation.util.NamedMappingAndUtil._
import models.Products
import play.api.data.Forms._
import play.api.data.{Form, Mapping}
import utils.AwrsFieldConfig
import utils.AwrsValidator._

object ProductsForm extends AwrsFieldConfig {

  private val mainCustomerOtherIsSelected = whenListContainsAnswer(listId = "mainCustomers", answer = "99")

  private val otherMainCustomers_compulsory: Mapping[Option[String]] = {
    val fieldId = "otherMainCustomers"
    val fieldNameInErrorMessage = "other customers"
    val companyNameConstraintParameters =
      CompulsoryTextFieldMappingParameter(
        simpleFieldIsEmptyConstraintParameter(fieldId, "awrs.additional_information.error.other_mainCustomers"),
        genericFieldMaxLengthConstraintParameterForDifferentMessages(otherProductsLen, fieldId, fieldNameInErrorMessage,errorMsg = "awrs.additional_information.error.maximum_length.customer"),
        genericInvalidFormatConstraintParameter(validAlphaNumeric, fieldId, fieldNameInErrorMessage, errorMsg = "awrs.additional_information.error.other_mainCustomers_invalid_format")
      )
    compulsoryText(companyNameConstraintParameters)
  }

  private val productTypeIsSelected = whenListContainsAnswer(listId = "productType", answer = "99")

  private val otherProductType_compulsory: Mapping[Option[String]] = {
    val fieldId = "otherProductType"
    val fieldNameInErrorMessage = "other products"
    val companyNameConstraintParameters =
      CompulsoryTextFieldMappingParameter(
        simpleFieldIsEmptyConstraintParameter(fieldId, "awrs.additional_information.error.type_of_product_other"),
        genericFieldMaxLengthConstraintParameterForDifferentMessages(otherProductsLen, fieldId, fieldNameInErrorMessage,errorMsg = "awrs.additional_information.error.maximum_length.product"),
        genericInvalidFormatConstraintParameter(validAlphaNumeric, fieldId, fieldNameInErrorMessage, errorMsg = "awrs.additional_information.error.type_of_product_other_validation")
      )
    compulsoryText(companyNameConstraintParameters)
  }

  val productsForm = Form(mapping(
    "mainCustomers" -> compulsoryStringList("mainCustomers", "awrs.additional_information.error.main_customer"),
    "otherMainCustomers" -> (otherMainCustomers_compulsory iff mainCustomerOtherIsSelected),
    "productType" -> compulsoryStringList("productType", "awrs.additional_information.error.type_of_product"),
    "otherProductType" -> (otherProductType_compulsory iff productTypeIsSelected)
  )(Products.apply)(Products.unapply))

}
