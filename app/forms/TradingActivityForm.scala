/*
 * Copyright 2023 HM Revenue & Customs
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
import forms.validation.util.ConstraintUtil.{CompulsoryTextFieldMappingParameter, _}
import forms.validation.util.ErrorMessagesUtilAPI._
import forms.validation.util.MappingUtilAPI._
import forms.validation.util.NamedMappingAndUtil._
import models.TradingActivity
import play.api.data.Forms._
import play.api.data.validation.{Invalid, Valid}
import play.api.data.{Form, Forms, Mapping}
import utils.AwrsFieldConfig
import utils.AwrsValidator._

object TradingActivityForm extends AwrsFieldConfig {

  private val wholesalerTypeOtherIsSelected = whenListContainsAnswer(listId = "wholesalerType", answer = "99")

  private val otherWholesaler_compulsory: Mapping[Option[String]] = {
    val fieldId = "otherWholesaler"
    val fieldNameInErrorMessage = "wholesaler type other"
    val companyNameConstraintParameters =
      CompulsoryTextFieldMappingParameter(
        simpleFieldIsEmptyConstraintParameter(fieldId, "awrs.additional_information.error.other_wholesaler"),
        FieldMaxLengthConstraintParameter(otherWholesalerLen, Invalid("awrs.view_application.error.trading_activity.order.maxlength", fieldNameInErrorMessage, otherWholesalerLen)),
        FieldFormatConstraintParameter((name: String) => if (validAlphaNumeric(name)) Valid else Invalid("awrs.additional_information.error.wholesaler_validation", fieldNameInErrorMessage))
      )
    compulsoryText(companyNameConstraintParameters)
  }

  private val typeOfAlcoholOrdersOtherIsSelected = whenListContainsAnswer(listId = "typeOfAlcoholOrders", answer = "99")

  private val doYouExportAlcoholIsYes = (data: FormData) => data.getOrElse("doYouExportAlcohol", "").equals(BooleanRadioEnum.Yes.toString)

  private val typeOfAlcoholOrders_compulsory: Mapping[Option[String]] = {
    val fieldId = "otherTypeOfAlcoholOrders"
    val fieldNameInErrorMessage = "other orders"
    val companyNameConstraintParameters =
      CompulsoryTextFieldMappingParameter(
        simpleFieldIsEmptyConstraintParameter(fieldId, "awrs.additional_information.error.other_order"),
        FieldMaxLengthConstraintParameter(otherOrdersLen, Invalid("awrs.view_application.error.trading_activity.maxlength", fieldNameInErrorMessage, otherOrdersLen)),
        FieldFormatConstraintParameter((name: String) => if (validAlphaNumeric(name)) Valid else Invalid("awrs.additional_information.error.order_validation", fieldNameInErrorMessage))
      )
    compulsoryText(companyNameConstraintParameters)
  }

  val compulsoryOptStringList: (String, String) => Mapping[Option[List[String]]] = (fieldId: String, emptyErrorMsgId: String) =>
    compulsoryOptList(CompulsoryListMappingParameter[String](Forms.text, simpleErrorMessage(fieldId, emptyErrorMsgId)))

  val tradingActivityForm = Form(mapping(
    "wholesalerType" -> compulsoryStringList("wholesalerType", "awrs.additional_information.error.type_of_wholesaler"),
    "otherWholesaler" -> (otherWholesaler_compulsory iff wholesalerTypeOtherIsSelected),
    "typeOfAlcoholOrders" -> compulsoryStringList("typeOfAlcoholOrders", "awrs.additional_information.error.orders"),
    "otherTypeOfAlcoholOrders" -> (typeOfAlcoholOrders_compulsory iff typeOfAlcoholOrdersOtherIsSelected),
    "doesBusinessImportAlcohol" -> yesNoQuestion_compulsory("doesBusinessImportAlcohol", "awrs.additional_information.error.import_alcohol"),
    "doYouExportAlcohol" -> yesNoQuestion_compulsory("doYouExportAlcohol", "awrs.additional_information.error.do_you_export_alcohol"),
    "exportLocation" -> (compulsoryOptStringList("exportLocation", "awrs.additional_information.error.export_location") iff doYouExportAlcoholIsYes),
    "thirdPartyStorage" -> yesNoQuestion_compulsory("thirdPartyStorage", "awrs.additional_information.error.third_party_storage")
  )(TradingActivity.apply)(TradingActivity.unapply))

}
