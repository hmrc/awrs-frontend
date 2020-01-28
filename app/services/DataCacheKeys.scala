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

package services

object DataCacheKeys {

  val indexName = "index"

  val businessTypeName = "legalEntity"
  val businessCustomerDetailsName = "businessCustomerDetails"
  val newApplicationTypeName = "newApplicationType"
  val groupDeclarationName = "groupDeclaration"

  val groupMembersName = "groupMembers"

  val extendedBusinessDetailsName = "extendedBusinessDetails"
  val businessDetailsName = "businessDetails"
  val businessRegistrationDetailsName = "businessRegistrationDetails"
  val businessContactsName = "businessContacts"
  val placeOfBusinessName = "placeOfBusiness"

  val businessCustomerAddressName = "businessCustomerAddress"

  val partnersName = "partnerDetails"

  val businessDirectorsName = "businessDirectors"
  val additionalBusinessPremisesName = "additionalBusinessPremises"

  val suppliersName = "suppliers"

  val tradingActivityName = "tradingActivity"
  val productsName = "products"

  val applicationDeclarationName = "applicationDeclaration"

  val subscriptionTypeFrontEndName = "subscriptionTypeFrontEnd"

  val deRegistrationDateName = "deRegistrationDate"
  val deRegistrationReasonName = "deRegistrationReason"

  val withdrawalReasonName = "withdrawalReason"

  val statusInfoTypeName = "statusInfoType"
  val statusNotificationName = "statusNotification"

  val subscriptionStatusTypeName = "subscriptionStatusType"
  val businessDetailsSupportName = "businessDetailsSupport"

  val isNewBusinessName = "isNewBusiness"

  val viewedStatusName = "viewedStatus"

  val applicationStatusName = "applicationStatus"

  // this key is used by the keystore service to backup save4later data in order to allow print application post submission
  val save4LaterBackupName = "awrs-frontend-backup"

  //TODO centralise the rest of the keys

}
