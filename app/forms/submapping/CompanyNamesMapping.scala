/*
 * Copyright 2019 HM Revenue & Customs
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
import forms.validation.util.ConstraintUtil.{CompulsoryTextFieldMappingParameter, FormData, FormQuery}
import forms.validation.util.ErrorMessagesUtilAPI._
import forms.validation.util.MappingUtilAPI._
import forms.validation.util.NamedMappingAndUtil._
import models.CompanyNames
import play.api.data.Forms._
import play.api.data.Mapping
import utils.AwrsFieldConfig
import utils.AwrsValidator._

object CompanyNamesMapping extends AwrsFieldConfig {

  val businessName = "businessName"
  val doYouHaveTradingName = "doYouHaveTradingName"
  val tradingName = "tradingName"

  def companyName_compulsory(prefix: String): Mapping[Option[String]] = {
    val fieldId = prefix attach businessName
    val fieldNameInErrorMessage: String = "business name"

    val companyNameConstraintParameters =
      CompulsoryTextFieldMappingParameter(
        simpleFieldIsEmptyConstraintParameter(fieldId, "awrs.generic.error.businessName_empty"),
        genericFieldMaxLengthConstraintParameter(companyNameLen, fieldId, fieldNameInErrorMessage),
        genericInvalidFormatConstraintParameter(validText, fieldId, fieldNameInErrorMessage)
      )
    compulsoryText(companyNameConstraintParameters)
  }

  def doYouHaveTradingName_compulsory(prefix: String): Mapping[Option[String]] =
    yesNoQuestion_compulsory(prefix attach doYouHaveTradingName, "awrs.generic.error.do_you_have_trading_name_empty")

  def tradingName_compulsory(prefix: String): Mapping[Option[String]] = {
    val fieldId = prefix attach tradingName
    val fieldNameInErrorMessage = "trading name"

    val companyNameConstraintParameters =
      CompulsoryTextFieldMappingParameter(
        simpleFieldIsEmptyConstraintParameter(fieldId, "awrs.generic.error.tradingName_empty"),
        genericFieldMaxLengthConstraintParameter(tradingNameLen, fieldId, fieldNameInErrorMessage),
        genericInvalidFormatConstraintParameter(validText, fieldId, fieldNameInErrorMessage)
      )
    compulsoryText(companyNameConstraintParameters)
  }

  val whenDoYouHaveTradingNameIsAnsweredYes: String => FormData => Boolean = (prefix: String) => answerGivenInFieldIs(prefix attach doYouHaveTradingName, BooleanRadioEnum.YesString)

  private val alwaysValidate = (_: FormData) => true

  def companyNamesMapping(prefix: String, validateBusinessName: FormQuery = alwaysValidate): Mapping[CompanyNames] =
    mapping(
      businessName -> (companyName_compulsory(prefix) iff validateBusinessName),
      doYouHaveTradingName -> doYouHaveTradingName_compulsory(prefix),
      tradingName -> (tradingName_compulsory(prefix) iff whenDoYouHaveTradingNameIsAnsweredYes(prefix))
    )(CompanyNames.apply)(CompanyNames.unapply)

  def companyNamesMapping(prefix: String, isBusinessNameRequired: Boolean): Mapping[CompanyNames] = companyNamesMapping(prefix, (data: FormData) => isBusinessNameRequired)

  private val cnToOptional = (companyNames: CompanyNames) => Some(companyNames): Option[CompanyNames]
  private val cnFromOptional = (companyNames: Option[CompanyNames]) => companyNames.fold(CompanyNames(None, None, None))(x => x): CompanyNames

  implicit class CompanyNamesOptionUtil(mapping: Mapping[CompanyNames]) {
    def toOptional: Mapping[Option[CompanyNames]] = mapping.transform(cnToOptional, cnFromOptional)
  }

  implicit class CompanyNamesOptionUtil2(mapping: Mapping[Option[CompanyNames]]) {
    def toCompulsory: Mapping[CompanyNames] = mapping.transform(cnFromOptional, cnToOptional)
  }


}
