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

import models._
import play.api.data.Form
import play.api.data.Forms._

object BusinessTypeForm {

  def optionalStringToBoolean(t: Option[String]): Option[Boolean] =
    t match {
      case Some(x) if x == "" => None
      case Some(x) => Some(x.toBoolean)
      case _ => None
    }

  def validateBusinessType(model: Form[BusinessType]): Form[BusinessType] = {

    val isSaAccount = optionalStringToBoolean(model.data.get("isSaAccount"))
    val isOrgAccount = optionalStringToBoolean(model.data.get("isOrgAccount"))
    val legalEntity = model.data.get("legalEntity") map (_.trim) filterNot (_.isEmpty)

    (legalEntity.nonEmpty, legalEntity.getOrElse(""), isSaAccount, isOrgAccount) match {
      case (true, "SOP", None, Some(true)) => model.withError(key = "legalEntity", message = "awrs.business_verification.error.type_of_business_organisation_invalid")
      case (true, "SOP", Some(true), None) => model
      case (true, _, Some(true), None) => model.withError(key = "legalEntity", message = "awrs.business_verification.error.type_of_business_individual_invalid")
      case _ => model
    }
  }

  val businessTypeForm: Form[BusinessType] = Form(mapping(
    "legalEntity" -> optional(text).verifying("awrs.business_verification.error.type_of_business_empty", x => x.isDefined),
    "isSaAccount" -> optional(boolean),
    "isOrgAccount" -> optional(boolean)
  )(BusinessType.apply)(BusinessType.unapply)
  )
}
