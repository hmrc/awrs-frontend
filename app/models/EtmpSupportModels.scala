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

package models

import forms.AWRSEnums.BooleanRadioEnum
import play.api.libs.json.Json

// models in this file are used to indicate if certain actions are required due to issues from etmp post submission
// e.g. if data is missing when api 5 is called


case class BusinessDetailsSupport(missingProposedStartDate: Boolean)

object BusinessDetailsSupport {

  implicit val format = Json.format[BusinessDetailsSupport]

  def evaluate(newAWBusiness: NewAWBusiness): BusinessDetailsSupport = {
    val missing: Boolean =
      newAWBusiness.newAWBusiness match {
        case BooleanRadioEnum.YesString =>
          newAWBusiness.proposedStartDate match {
            case None => true
            case _ => false
          }
        case BooleanRadioEnum.NoString => false
      }
    BusinessDetailsSupport(missingProposedStartDate = missing)
  }

  def evaluate(subscriptionTypeFrontEnd: SubscriptionTypeFrontEnd): BusinessDetailsSupport =
    evaluate(subscriptionTypeFrontEnd.businessDetails.get.newAWBusiness.get)

}
