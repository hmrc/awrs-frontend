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

package utils

trait AwrsFieldConfig {
  val applicationDeclarationNameLen: Int = 140
  val applicationDeclarationRoleLen: Int = 40
  val companyNameLen: Int = 140
  val tradingNameLen: Int = 120
  val addressPostcodeLen: Int = 20
  val addressLineLen: Int = 35 //originally had 1-4 but they are all the same
  val firstNameLen: Int = 35
  val lastNameLen: Int = 35
  val emailLen: Int = 100
  val telephoneLen: Int = 24
  val nationalIdNumberLen: Int = 20
  val otherWholesalerLen: Int = 40
  val otherOrdersLen: Int = 40
  val otherCustomersLen: Int = 40
  val otherProductsLen: Int = 40
  val supplierNameLen: Int = 140
  val deRegistrationOtherReasonsLen: Int = 40
  val withdrawalOtherReasonsLen: Int = 40
}