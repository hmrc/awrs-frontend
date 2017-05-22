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

import play.api.libs.json._

import scala.util.{Failure, Success, Try}

object BusinessDetailsEntityTypes extends Enumeration {
  val SoleTrader = Value("SoleTrader")
  val CorporateBody = Value("CorporateBody")
  val GroupRep = Value("GroupRep")
  val Llp = Value("Llp")
  val Lp = Value("Lp")
  val Partnership = Value("Partnership")

  implicit val reader = new Reads[BusinessDetailsEntityTypes.Value] {
    def reads(js: JsValue): JsResult[BusinessDetailsEntityTypes.Value] = js match {
      case JsString(s) =>
        Try(BusinessDetailsEntityTypes.withName(s)) match {
          case Success(value) => JsSuccess(value)
          case Failure(e) => JsError(s"Enumeration expected of type: '${BusinessDetailsEntityTypes.getClass}', but it does not appear to contain the value: '$s'")
        }
      case _ => JsError("String value expected")
    }
  }
  implicit val writer = new Writes[BusinessDetailsEntityTypes.Value] {
    def writes(entityType: BusinessDetailsEntityTypes.Value): JsValue = Json.toJson(entityType.toString)
  }

  implicit def autoToString(businessEntityType: BusinessDetailsEntityTypes.Value): String = businessEntityType.toString
}

case class BusinessDetails(doYouHaveTradingName: Option[String],
                           tradingName: Option[String],
                           newAWBusiness: Option[NewAWBusiness])

case class ExtendedBusinessDetails(businessName: Option[String],
                                   doYouHaveTradingName: Option[String],
                                   tradingName: Option[String],
                                   newAWBusiness: Option[NewAWBusiness],
                                   businessNameUpdated: Boolean = false) {
  def getBusinessDetails: BusinessDetails = BusinessDetails(doYouHaveTradingName, tradingName, newAWBusiness)

  def updateBusinessCustomerDetails(businessCustomerDetails: BusinessCustomerDetails): BusinessCustomerDetails = businessCustomerDetails.copy(businessName = businessName.get)
}

case class BusinessRegistrationDetails(legalEntity: Option[String] = None,
                                       doYouHaveUTR: Option[String] = None,
                                       utr: Option[String] = None,
                                       doYouHaveNino: Option[String] = None,
                                       nino: Option[String] = None,
                                       isBusinessIncorporated: Option[String] = None,
                                       companyRegDetails: Option[CompanyRegDetails] = None,
                                       doYouHaveVRN: Option[String] = None,
                                       vrn: Option[String] = None)

case class PlaceOfBusiness(mainPlaceOfBusiness: Option[String] = None,
                           mainAddress: Option[Address] = None,
                           placeOfBusinessLast3Years: Option[String] = None,
                           placeOfBusinessAddressLast3Years: Option[Address] = None,
                           operatingDuration: Option[String] = None,
                           modelVersion: String = PlaceOfBusiness.latestModelVersion
                          ) extends ModelVersionControl

case class BusinessContacts(contactFirstName: Option[String] = None,
                            contactLastName: Option[String] = None,
                            telephone: Option[String] = None,
                            email: Option[String] = None,
                            contactAddressSame: Option[String] = None,
                            contactAddress: Option[Address] = None,
                            modelVersion: String = BusinessContacts.latestModelVersion
                           ) extends ModelVersionControl

object BusinessDetails {
  implicit val formats: Format[BusinessDetails] = Json.format[BusinessDetails]
}

object ExtendedBusinessDetails {
  implicit val formats: Format[ExtendedBusinessDetails] = Json.format[ExtendedBusinessDetails]
}

object BusinessRegistrationDetails {
  implicit val formats: Format[BusinessRegistrationDetails] = Json.format[BusinessRegistrationDetails]
}

object BusinessContacts {

  val latestModelVersion = "1.1"

  implicit val formats: Format[BusinessContacts] = Json.format[BusinessContacts]

}

object PlaceOfBusiness {

  val latestModelVersion = "1.0"

  implicit val formats: Format[PlaceOfBusiness] = Json.format[PlaceOfBusiness]

}