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

import forms.prevalidation._
import forms.validation.util.MappingUtilAPI._
import forms.validation.util.NamedMappingAndUtil._
import models._
import play.api.data.Form
import play.api.data.Forms._

object TradingNameForm {

  val businessName = "companyName"
  val doYouHaveTradingName = "doYouHaveTradingName"
  val tradingName = "tradingName"

  private val groupIds = Seq[String](businessName, doYouHaveTradingName, tradingName)
  private val otherIds = Seq[String](doYouHaveTradingName, tradingName)

  private val entityIds = (entityType: String, hasAwrs: Boolean) => entityType match {
    case "LLP_GRP" | "LTD_GRP" =>
      if (hasAwrs) {
        groupIds
      } else {
        otherIds
      }
    case _ => otherIds
  }

  val tradingNameFormValidation: (String, Boolean) => Form[BusinessNameDetails] = (entityType: String, hasAwrs: Boolean) => {
    val ids = entityIds(entityType, hasAwrs)
    Form(
      mapping(
        businessName -> (companyName_compulsory() iff ids.contains(businessName)),
        doYouHaveTradingName -> doYouHaveTradingName_compulsory,
        tradingName -> (tradingName_compulsory iff whenDoYouHaveTradingNameIsAnsweredYes)
      )(BusinessNameDetails.apply)(BusinessNameDetails.unapply)
    )
  }

  val tradingNameForm: (String, Boolean) => PrevalidationAPI[BusinessNameDetails] =
    (entityType: String, hasAwrs: Boolean) => PreprocessedForm(tradingNameFormValidation(entityType, hasAwrs))
}
