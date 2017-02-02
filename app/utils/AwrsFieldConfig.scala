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

package utils

import play.api.Play._

trait AwrsFieldConfig {
  lazy val applicationDeclarationNameLen = configuration.getInt("application-declaration.declarationName.maxLength").getOrElse(0)
  lazy val applicationDeclarationRoleLen = configuration.getInt("application-declaration.declarationRole.maxLength").getOrElse(0)
  lazy val companyNameLen = configuration.getInt("business-details.companyName.maxLength").getOrElse(0)
  lazy val tradingNameLen = configuration.getInt("business-details.tradingName.maxLength").getOrElse(0)
  lazy val addressPostcodeLen = configuration.getInt("business-details.postcode.maxLength").getOrElse(0)
  lazy val addressLine1Len = configuration.getInt("business-details.addressLine1.maxLength").getOrElse(0)
  lazy val addressLine2Len = configuration.getInt("business-details.addressLine2.maxLength").getOrElse(0)
  lazy val addressLine3Len = configuration.getInt("business-details.addressLine3.maxLength").getOrElse(0)
  lazy val addressLine4Len = configuration.getInt("business-details.addressLine4.maxLength").getOrElse(0)
  lazy val firstNameLen = configuration.getInt("contact.firstName.maxLength").getOrElse(0)
  lazy val lastNameLen = configuration.getInt("contact.lastName.maxLength").getOrElse(0)
  lazy val emailLen = configuration.getInt("contact.email.maxLength").getOrElse(0)
  lazy val telephoneLen = configuration.getInt("contact.telephone.maxLength").getOrElse(0)
  lazy val nationalIdNumberLen = configuration.getInt("nationalIdNumber.maxLength").getOrElse(0)
  lazy val otherWholesalerLen = configuration.getInt("additional-information.otherWholesaler.maxLength").getOrElse(0)
  lazy val otherOrdersLen = configuration.getInt("additional-information.otherOrders.maxLength").getOrElse(0)
  lazy val otherCustomersLen = configuration.getInt("additional-information.otherCustomers.maxLength").getOrElse(0)
  lazy val otherProductsLen = configuration.getInt("additional-information.otherProducts.maxLength").getOrElse(0)
  lazy val supplierNameLen = configuration.getInt("supplier-address.supplierName.maxLength").getOrElse(0)
  lazy val deRegistrationOtherReasonsLen = configuration.getInt("de-registration.otherReasons.maxLength").getOrElse(0)
  lazy val withdrawalOtherReasonsLen = configuration.getInt("withdrawal.otherReasons.maxLength").getOrElse(0)
  lazy val frontendDefaultLen = configuration.getInt("frontend-default.maxLength").getOrElse(0)
}
object AwrsFieldConfig extends AwrsFieldConfig
