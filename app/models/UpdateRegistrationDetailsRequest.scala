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

import play.api.libs.json.Json

/**
  * Created by nikhilb on 18/05/17.
  */


case class ContactDetails(phoneNumber: Option[String] = None,
                          mobileNumber: Option[String] = None,
                          faxNumber: Option[String] = None,
                          emailAddress: Option[String] = None)

object ContactDetails {
  implicit val formats = Json.format[ContactDetails]
}


case class OrganisationName(organisationName: String)

object OrganisationName {
  implicit val formats = Json.format[OrganisationName]
}

case class UpdateRegistrationDetailsRequest(isAnIndividual: Boolean,
                                            organisationName: Option[OrganisationName], // TODO: could this just be a string?
                                            address: BCAddressApi3,
                                            contactDetails: ContactDetails,
                                            isAnAgent: Boolean,
                                            isAGroup: Boolean) {


}

object UpdateRegistrationDetailsRequest {
  implicit val formats = Json.format[UpdateRegistrationDetailsRequest]
}
