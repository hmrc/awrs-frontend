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

package models

import exceptions.InvalidStateException
import models.BusinessDetailsEntityTypes._

object Util {

  def getBusinessDetailsEntityType(businessType: BusinessType): BusinessDetailsEntityTypes.Value =
    businessType match {
      case BusinessType(Some("SOP"), _, _) => SoleTrader
      case BusinessType(Some("LTD"), _, _) => CorporateBody
      case BusinessType(Some("LTD_GRP" | "LLP_GRP"), _, _) => GroupRep
      case BusinessType(Some("LLP" | "LP"), _, _) => Llp
      case BusinessType(Some("Partnership"), _, _) => Partnership
      case _ => throw InvalidStateException("Invalid Legal entity")
    }

}
