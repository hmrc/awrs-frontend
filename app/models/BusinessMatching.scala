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

package models

import play.api.libs.json.{Json, OFormat}

case class Individual(firstName: String, lastName: String, dateOfBirth: Option[String])

case class Organisation(organisationName: String, organisationType: String)

case class OrganisationResponse(organisationName: String, isAGroup: Option[Boolean], organisationType: Option[String])

case class MatchBusinessData(acknowledgementReference: String,
                             utr: String,
                             requiresNameMatch: Boolean = false,
                             isAnAgent: Boolean = false,
                             individual: Option[Individual],
                             organisation: Option[Organisation])

object Individual {
  implicit val formats: OFormat[Individual] = Json.format[Individual]
}

object Organisation {
  implicit val formats: OFormat[Organisation] = Json.format[Organisation]
}

object OrganisationResponse {
  implicit val formats: OFormat[OrganisationResponse] = Json.format[OrganisationResponse]
}

object MatchBusinessData {
  implicit val formats: OFormat[MatchBusinessData] = Json.format[MatchBusinessData]
}

case class BCAddressApi3(addressLine1: String,
                      addressLine2: String,
                      addressLine3: Option[String] = None,
                      addressLine4: Option[String] = None,
                      postalCode: Option[String] = None,
                      countryCode: Option[String] = None)

object BCAddressApi3 {
  implicit val formats: OFormat[BCAddressApi3] = Json.format[BCAddressApi3]

  def apply(placeOfBusiness: PlaceOfBusiness): BCAddressApi3 = {
    val address = placeOfBusiness.mainAddress.get
    BCAddressApi3(
      address.addressLine1,
      address.addressLine2,
      address.addressLine3,
      address.addressLine4,
      address.postcode,
      if(address.postcode.isDefined) Some("GB") else address.addressCountryCode)
  }
}

case class MatchSuccessResponse(isAnIndividual: Boolean,
                                agentReferenceNumber: Option[String],
                                sapNumber: Option[String],
                                safeId: String,
                                address: BCAddressApi3,
                                organisation: Option[OrganisationResponse] = None,
                                individual: Option[Individual] = None)

object MatchSuccessResponse {
  implicit val format: OFormat[MatchSuccessResponse] = Json.format[MatchSuccessResponse]
}
