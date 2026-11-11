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

import play.api.libs.json._

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

object BusinessDetailsEntityTypes extends Enumeration {
  val SoleTrader: BusinessDetailsEntityTypes.Value = Value("SoleTrader")
  val CorporateBody: BusinessDetailsEntityTypes.Value = Value("CorporateBody")
  val GroupRep: BusinessDetailsEntityTypes.Value = Value("GroupRep")
  val Llp: BusinessDetailsEntityTypes.Value = Value("Llp")
  val Lp: BusinessDetailsEntityTypes.Value = Value("Lp")
  val Partnership: BusinessDetailsEntityTypes.Value = Value("Partnership")

  given reader: Reads[BusinessDetailsEntityTypes.Value] = new Reads[BusinessDetailsEntityTypes.Value] {
    def reads(js: JsValue): JsResult[BusinessDetailsEntityTypes.Value] = js match {
      case JsString(s) =>
        Try(BusinessDetailsEntityTypes.withName(s)) match {
          case Success(value) => JsSuccess(value)
          case Failure(_) => JsError(s"Enumeration expected of type: '${BusinessDetailsEntityTypes.getClass}', but it does not appear to contain the value: '$s'")
        }
      case _ => JsError("String value expected")
    }
  }
  given writer: Writes[BusinessDetailsEntityTypes.Value] = new Writes[BusinessDetailsEntityTypes.Value] {
    def writes(entityType: BusinessDetailsEntityTypes.Value): JsValue = Json.toJson(entityType.toString)
  }
  
  given autoToString: Conversion[BusinessDetailsEntityTypes.Value, String] = _.toString
}

case class BusinessNameChangeConfirmation(businessNameChangeConfirmation: Option[String])

case class BusinessDetails(doYouHaveTradingName: Option[String],
                           tradingName: Option[String],
                           newAWBusiness: Option[NewAWBusiness])

case class ExtendedBusinessDetails(businessName: Option[String],
                                   doYouHaveTradingName: Option[String],
                                   tradingName: Option[String],
                                   newAWBusiness: Option[NewAWBusiness]) {
  def getBusinessDetails: BusinessDetails = BusinessDetails(doYouHaveTradingName, tradingName, newAWBusiness)
}

case class BusinessNameDetails(businessName: Option[String],
                               doYouHaveTradingName: Option[String],
                               tradingName: Option[String])

case class BusinessDetailSummaryModel(businessNameDetails: Option[BusinessNameDetails], newAWBusiness: Option[NewAWBusiness])

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


object BusinessNameChangeConfirmation {
  given formats: Format[BusinessNameChangeConfirmation] = Json.format[BusinessNameChangeConfirmation]
}

object BusinessDetails {
  given formats: Format[BusinessDetails] = Json.format[BusinessDetails]
}

object BusinessNameDetails {
  given formats: Format[BusinessNameDetails] = Json.format[BusinessNameDetails]
}

object ExtendedBusinessDetails {
  given formats: Format[ExtendedBusinessDetails] = Json.format[ExtendedBusinessDetails]
}

object BusinessRegistrationDetails {
  given formats: Format[BusinessRegistrationDetails] = Json.format[BusinessRegistrationDetails]
  given optionFormats: Format[Option[BusinessRegistrationDetails]] = Format.optionWithNull[BusinessRegistrationDetails]
}

object BusinessContacts {

  val latestModelVersion = "1.1"

  given formats: Format[BusinessContacts] = Json.format[BusinessContacts]
  given optionFormats: Format[Option[BusinessContacts]] = Format.optionWithNull[BusinessContacts]

}

object PlaceOfBusiness {

  val latestModelVersion = "1.0"

  given formats: Format[PlaceOfBusiness] = Json.format[PlaceOfBusiness]
  given optionFormats: Format[Option[PlaceOfBusiness]] = Format.optionWithNull[PlaceOfBusiness]

}
