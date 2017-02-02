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

import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid}

object BusinessTypeForm {

  val TypeOfBusinessConstraint: Constraint[BusinessType] = Constraint("awrs.business_verification.error.type_of_business_empty")({ model =>
    model.legalEntity.nonEmpty match {
      case true => Valid
      case false => Invalid("awrs.business_verification.error.type_of_business_empty", "legalEntity")
    }
  })

  val ValidBusinessTypeConstraint = Constraint[BusinessType] { model: BusinessType =>
    (model.legalEntity.nonEmpty, model.legalEntity.getOrElse(""), model.isSaAccount, model.isOrgAccount) match {
      case (true, "SOP", None, Some(true)) => Invalid("awrs.business_verification.error.type_of_business_organisation_invalid", "legalEntity")
      case (true, "SOP", Some(true), None) => Valid
      case (true, _, Some(true), None) => Invalid("awrs.business_verification.error.type_of_business_individual_invalid", "legalEntity")
      case _ => Valid
    }
  }

  val businessTypeForm = Form(mapping(
    "legalEntity" -> optional(text),
    "isSaAccount" -> optional(boolean),
    "isOrgAccount" -> optional(boolean)
  )(BusinessType.apply)(BusinessType.unapply)
    .verifying(TypeOfBusinessConstraint)
    .verifying(ValidBusinessTypeConstraint)
  )
}
