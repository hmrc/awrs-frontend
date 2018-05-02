/*
 * Copyright 2018 HM Revenue & Customs
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

import java.text.SimpleDateFormat

import forms.AWRSEnums.ApplicationStatusEnum
import forms.AwrsFormFields
import org.joda.time.format.DateTimeFormat
import org.joda.time.{LocalDate, LocalDateTime}
import play.api.data.format.Formats
import play.api.libs.json._
import utils.AwrsFieldConfig

trait ModelVersionControl {
  def modelVersion: String
}

case class ApplicationStatus(status: ApplicationStatusEnum.Value, updatedDate: LocalDateTime)

case class BusinessDirector(personOrCompany: Option[String] = None,
                            firstName: Option[String] = None,
                            lastName: Option[String] = None,
                            doTheyHaveNationalInsurance: Option[String] = None,
                            nino: Option[String] = None,
                            passportNumber: Option[String] = None,
                            nationalID: Option[String] = None,
                            companyNames: Option[CompanyNames] = None,
                            doYouHaveUTR: Option[String] = None,
                            utr: Option[String] = None,
                            doYouHaveCRN: Option[String] = None,
                            companyRegNumber: Option[String] = None,
                            doYouHaveVRN: Option[String] = None,
                            vrn: Option[String] = None,
                            directorsAndCompanySecretaries: Option[String] = None,
                            otherDirectors: Option[String] = None
                           )

case class BusinessDirectors(directors: List[BusinessDirector],
                             modelVersion: String = BusinessDirectors.latestModelVersion
                            ) extends ModelVersionControl


case class TradingActivity_old(wholesalerType: List[String],
                           otherWholesaler: Option[String],
                           typeOfAlcoholOrders: List[String],
                           otherTypeOfAlcoholOrders: Option[String],
                           doesBusinessImportAlcohol: Option[String],
                           doYouExportAlcohol: List[String],
                           thirdPartyStorage: Option[String]
                          )

case class TradingActivity(wholesalerType: List[String],
                           otherWholesaler: Option[String],
                           typeOfAlcoholOrders: List[String],
                           otherTypeOfAlcoholOrders: Option[String],
                           doesBusinessImportAlcohol: Option[String],
                           doYouExportAlcohol: Option[String],
                           exportLocation: Option[List[String]],
                           thirdPartyStorage: Option[String]
                          )

case class Products(mainCustomers: List[String],
                    otherMainCustomers: Option[String],
                    productType: List[String],
                    otherProductType: Option[String])

case class Supplier(alcoholSuppliers: Option[String],
                    supplierName: Option[String],
                    ukSupplier: Option[String],
                    supplierAddress: Option[Address],
                    vatRegistered: Option[String],
                    vatNumber: Option[String],
                    additionalSupplier: Option[String])

case class Partner(entityType: Option[String] = None,
                   firstName: Option[String] = None,
                   lastName: Option[String] = None,
                   companyNames: Option[CompanyNames] = None,
                   partnerAddress: Option[Address] = None,
                   doYouHaveNino: Option[String] = None,
                   nino: Option[String] = None,
                   doYouHaveUTR: Option[String] = None,
                   utr: Option[String] = None,
                   isBusinessIncorporated: Option[String] = None,
                   companyRegDetails: Option[CompanyRegDetails] = None,
                   doYouHaveVRN: Option[String] = None,
                   vrn: Option[String] = None,
                   otherPartners: Option[String] = None)

case class Partners(partners: List[Partner],
                    modelVersion: String = Partners.latestModelVersion
                   ) extends ModelVersionControl

case class Suppliers(suppliers: List[Supplier])

case class NewAWBusiness(newAWBusiness: String, proposedStartDate: Option[TupleDate])

case class TupleDate(day: String, month: String, year: String) {
  lazy val localDate = new LocalDate(year.toInt, month.toInt, day.toInt)

  lazy val date = localDate.toDate

  def toString(format: String) = new SimpleDateFormat(format).format(date)
}

case class AdditionalBusinessPremises(additionalPremises: Option[String], additionalAddress: Option[Address], addAnother: Option[String])

case class AdditionalBusinessPremisesList(premises: List[AdditionalBusinessPremises])

case class WithdrawalConfirmation(confirmation: Option[String])

case class WithdrawalReason(reason: Option[String], reasonOther: Option[String])

case class ReapplicationConfirmation(answer: Option[String])

case class GroupMember(companyNames: CompanyNames = CompanyNames(),
                       address: Option[Address] = None,
                       groupJoiningDate: Option[String] = None,
                       doYouHaveUTR: Option[String] = None,
                       utr: Option[String] = None,
                       isBusinessIncorporated: Option[String] = None,
                       companyRegDetails: Option[CompanyRegDetails] = None,
                       doYouHaveVRN: Option[String] = None,
                       vrn: Option[String] = None,
                       addAnotherGrpMember: Option[String] = None)

case class GroupMembers(members: List[GroupMember],
                        modelVersion: String = GroupMembers.latestModelVersion
                       ) extends ModelVersionControl

case class CompanyNames(businessName: Option[String] = None,
                        doYouHaveTradingName: Option[String] = None,
                        tradingName: Option[String] = None)

case class BusinessType(legalEntity: Option[String], isSaAccount: Option[Boolean], isOrgAccount: Option[Boolean])

case class CompanyRegDetails(companyRegistrationNumber: String, dateOfIncorporation: TupleDate)

case class DeleteConfirmation(deleteConfirmation: Option[String])

case class Address(
                    addressLine1: String,
                    addressLine2: String,
                    addressLine3: Option[String] = None,
                    addressLine4: Option[String] = None,
                    postcode: Option[String] = None,
                    addressCountry: Option[String] = None,
                    addressCountryCode: Option[String] = None
                  ) {

  override def toString = {
    val line3display = addressLine3.map(line3 => s"$line3, ").getOrElse("")
    val line4display = addressLine4.map(line4 => s"$line4, ").getOrElse("")
    val postcodeDisplay = postcode.map(postcode1 => s"$postcode1, ").getOrElse("")
    val countryDisplay = addressCountry.map(country => s"$country, ").getOrElse("")
    val countryCodeDisplay = addressCountryCode.map(countryCode => s"$countryCode").getOrElse("")
    s"$addressLine1, $addressLine2, $line3display$line4display$postcodeDisplay$countryDisplay$countryCodeDisplay"
  }

  override def equals(obj: Any): Boolean = obj match {
    case that: Address =>
      that.addressLine1.equals(addressLine1) &&
        that.addressLine2.equals(addressLine2) &&
        that.addressLine3.equals(addressLine3) &&
        that.addressLine4.equals(addressLine4) &&
        that.postcode.equals(postcode) &&
        that.addressCountry.equals(addressCountry)
    case _ => false
  }

  override def hashCode(): Int =
    (addressLine1, addressLine2, addressLine3, addressLine4, postcode, addressCountry).hashCode()
}

case class IndexStatus(soleTraderBusinessDetailsStatus: String,
                       corporateTraderBusinessDetailsStatus: String,
                       additionalBusinessPremisesStatus: String,
                       businessDirectorsStatus: String,
                       additionalInformationStatus: String,
                       suppliersStatus: String)

case class ApplicationDeclaration(declarationName: Option[String], declarationRole: Option[String], confirmation: Option[Boolean])

case class GroupDeclaration(groupRepConfirmation: Boolean)

case class BCAddress(
                      line_1: String,
                      line_2: String,
                      line_3: Option[String] = None,
                      line_4: Option[String] = None,
                      postcode: Option[String] = None,
                      country: Option[String] = None) {

  override def toString = {
    val line3display = line_3.map(line3 => s"$line3, ").getOrElse("")
    val line4display = line_4.map(line4 => s"$line4, ").getOrElse("")
    val postcodeDisplay = postcode.map(postcode1 => s"$postcode1, ").getOrElse("")
    s"$line_1, $line_2, $line3display$line4display$postcodeDisplay$country"
  }
}

case class AWRSFEModel(subscriptionTypeFrontEnd: SubscriptionTypeFrontEnd)

case class SubscriptionTypeFrontEnd(
                                     legalEntity: Option[BusinessType],
                                     businessPartnerName: Option[String],
                                     groupDeclaration: Option[GroupDeclaration],
                                     businessCustomerDetails: Option[BusinessCustomerDetails],
                                     businessDetails: Option[BusinessDetails],
                                     businessRegistrationDetails: Option[BusinessRegistrationDetails],
                                     businessContacts: Option[BusinessContacts],
                                     placeOfBusiness: Option[PlaceOfBusiness],
                                     partnership: Option[Partners],
                                     groupMembers: Option[GroupMembers],
                                     additionalPremises: Option[AdditionalBusinessPremisesList],
                                     businessDirectors: Option[BusinessDirectors],
                                     tradingActivity: Option[TradingActivity],
                                     products: Option[Products],
                                     suppliers: Option[Suppliers],
                                     applicationDeclaration: Option[ApplicationDeclaration],
                                     changeIndicators: Option[ChangeIndicators],
                                     modelVersion: String = SubscriptionTypeFrontEnd.latestModelVersion
                                   ) extends ModelVersionControl

/* TODO AWRS-1800 old model to be removed after 28 days */
case class SubscriptionTypeFrontEnd_old(
                                         legalEntity: Option[BusinessType],
                                         businessPartnerName: Option[String],
                                         groupDeclaration: Option[GroupDeclaration],
                                         businessCustomerDetails: Option[BusinessCustomerDetails],
                                         businessDetails: Option[BusinessDetails],
                                         businessRegistrationDetails: Option[BusinessRegistrationDetails],
                                         businessContacts: Option[BusinessContacts],
                                         placeOfBusiness: Option[PlaceOfBusiness],
                                         partnership: Option[Partners],
                                         groupMembers: Option[GroupMembers],
                                         additionalPremises: Option[AdditionalBusinessPremisesList],
                                         businessDirectors: Option[BusinessDirectors],
                                         tradingActivity: Option[TradingActivity_old],
                                         products: Option[Products],
                                         suppliers: Option[Suppliers],
                                         applicationDeclaration: Option[ApplicationDeclaration],
                                         changeIndicators: Option[ChangeIndicators],
                                         modelVersion: String = SubscriptionTypeFrontEnd_old.latestModelVersion
                                       ) extends ModelVersionControl

/* end old block */

case class BusinessCustomerDetails(businessName: String,
                                   businessType: Option[String],
                                   businessAddress: BCAddress,
                                   sapNumber: String,
                                   safeId: String,
                                   isAGroup: Boolean,
                                   agentReferenceNumber: Option[String],
                                   firstName: Option[String] = None,
                                   lastName: Option[String] = None,
                                   utr: Option[String] = None)

case class SectionChangeIndicators(businessDetailsChanged: Boolean,
                                   businessRegistrationDetailsChanged: Boolean,
                                   businessAddressChanged: Boolean,
                                   contactDetailsChanged: Boolean,
                                   tradingActivityChanged: Boolean,
                                   productsChanged: Boolean,
                                   partnersChanged: Boolean,
                                   coOfficialsChanged: Boolean,
                                   premisesChanged: Boolean,
                                   suppliersChanged: Boolean,
                                   groupMembersChanged: Boolean,
                                   declarationChanged: Boolean)

case class ChangeIndicators(businessDetailsChanged: Boolean,
                            businessAddressChanged: Boolean,
                            contactDetailsChanged: Boolean,
                            additionalBusinessInfoChanged: Boolean,
                            partnersChanged: Boolean,
                            coOfficialsChanged: Boolean,
                            premisesChanged: Boolean,
                            suppliersChanged: Boolean,
                            groupMembersChanged: Boolean,
                            declarationChanged: Boolean)

case class Survey(visitReason: Option[String],
                  easeOfAccess: Option[String],
                  easeOfUse: Option[String],
                  satisfactionRating: Option[String],
                  helpNeeded: Option[String],
                  howDidYouFindOut: Option[String],
                  comments: Option[String],
                  contactFullName: Option[String],
                  contactEmail: Option[String],
                  contactTelephone: Option[String]
                 )

case class Feedback(visitReason: Option[String],
                    satisfactionRating: Option[String],
                    comments: Option[String])

object ApplicationStatus {
  val dateFormat: String = "yyyy-MM-dd'T'HH:mm:ss"

  val jodaDateTimeReads = Reads[LocalDateTime](js =>
    js.validate[String].map[LocalDateTime](dtString =>
      LocalDateTime.parse(dtString, DateTimeFormat.forPattern(dateFormat))
    )
  )

  val enumReads = Reads[ApplicationStatusEnum.Value](js =>
    js.validate[String].map[ApplicationStatusEnum.Value](enum =>
      ApplicationStatusEnum.withName(enum)
    )
  )

  implicit val writer = new Writes[ApplicationStatus] {
    def writes(applicationStatus: ApplicationStatus): JsValue = {
      Json.obj()
        .++(Json.obj("status" -> applicationStatus.status.toString)
          .++(Json.obj("updatedDate" -> applicationStatus.updatedDate.toString(dateFormat))))
    }
  }

  implicit val reader = new Reads[ApplicationStatus] {
    def reads(js: JsValue): JsResult[ApplicationStatus] = {
      for {
        status <- (js \ "status").validate[ApplicationStatusEnum.Value](enumReads)
        updatedDate <- (js \ "updatedDate").validate[LocalDateTime](jodaDateTimeReads)
      } yield {
        ApplicationStatus(status = status, updatedDate = updatedDate)
      }
    }
  }
}

object SectionChangeIndicators {
  implicit val formats = Json.format[SectionChangeIndicators]
}

object ChangeIndicators {
  implicit val formats = Json.format[ChangeIndicators]
}

object BCAddress {
  implicit val formats = Json.format[BCAddress]
}

object BusinessCustomerDetails {
  implicit val formats = Json.format[BusinessCustomerDetails]
}

object TupleDate {
  implicit val formats = Json.format[TupleDate]

  implicit def convert(date: LocalDate): TupleDate = TupleDate("%02d".format(date.getDayOfMonth), "%02d".format(date.getMonthOfYear), "%04d".format(date.getYear))
}

object CompanyRegDetails {

  private val pattern = "dd/MM/yyyy"
  implicit val writer = new Writes[CompanyRegDetails] {
    def writes(companyRegDetails: CompanyRegDetails): JsValue = {
      Json.obj()
        .++(Json.obj("companyRegistrationNumber" -> companyRegDetails.companyRegistrationNumber)
          .++(Json.obj("dateOfIncorporation" -> companyRegDetails.dateOfIncorporation.toString(pattern))))
    }
  }

  implicit val reader = new Reads[CompanyRegDetails] {
    def reads(js: JsValue): JsResult[CompanyRegDetails] = {
      for {
        companyRegistrationNumber <- (js \ "companyRegistrationNumber").validate[String]
        date <- (js \ "dateOfIncorporation").validate[LocalDate](Reads.jodaLocalDateReads(pattern))
      } yield {
        CompanyRegDetails(companyRegistrationNumber = companyRegistrationNumber, dateOfIncorporation = date)
      }
    }
  }
}

object NewAWBusiness {

  private val pattern = "dd/MM/yyyy"
  val dtf = DateTimeFormat.forPattern(pattern)

  implicit val writer = new Writes[NewAWBusiness] {
    def writes(newAWBusiness: NewAWBusiness): JsValue =
      Json.obj()
        .++(Json.obj("newAWBusiness" -> newAWBusiness.newAWBusiness)
          .++(newAWBusiness.proposedStartDate.fold(Json.obj())(x => Json.obj("proposedStartDate" -> newAWBusiness.proposedStartDate.fold(TupleDate("", "", ""))(x => x).toString(pattern)))))
  }

  implicit val reader = new Reads[NewAWBusiness] {

    def reads(js: JsValue): JsResult[NewAWBusiness] =
      for {
        newAWBusiness <- (js \ "newAWBusiness").validate[String]
        proposedStartDate <- (js \ "proposedStartDate").validateOpt[String]
      } yield {
        val parsedProposedStartDate: Option[TupleDate] =
          proposedStartDate match {
            case Some(dateString) => Some(dtf.parseLocalDate(dateString))
            case None => None
          }
        NewAWBusiness(newAWBusiness = newAWBusiness, proposedStartDate = parsedProposedStartDate)
      }

  }
}

object Address {
  implicit val formats = Json.format[Address]
}

object CompanyNames {

  implicit val formats = Json.format[CompanyNames]

  implicit class CompanyNamesGetUtil(companyNames: Option[CompanyNames]) {
    def businessName: Option[String] = companyNames.fold(None: Option[String])(_.businessName)

    def tradingName: Option[String] = companyNames.fold(None: Option[String])(_.tradingName)
  }

}

/*
 *  This object is used as a utility factory for CompanyNames,
 *  this object is required since the Json.format[T] in play 2.3 only works when a single def apply exists in
 *  the companion object.
*/
object CompanyNamesFact {

  /*
  *  only specify an entityType if it can be a sole trader, since in that case doYouHaveTradingName needs to be populated
  */
  def apply(businessName: Option[String], tradingName: Option[String], entityType: Option[String] = None): Option[CompanyNames] =
  (businessName, tradingName) match {
    case (None, None) =>
      entityType match {
        // for sole traders only trading name is checked and it is an optional field
        // so return the inference of no based on its sole existence
        case Some("Sole Trader") =>
          Some(
            CompanyNames(
              businessName = None,
              doYouHaveTradingName = Some("No"),
              tradingName = None
            )
          )
        case _ => None
      }
    case _ => Some(
      CompanyNames(
        businessName = businessName,
        doYouHaveTradingName = Some(tradingName match {
          case Some(_) => "Yes"
          case None => "No"
        }),
        tradingName = tradingName
      )
    )
  }
}

object Partner {
  implicit val formats: Format[Partner] = Json.format[Partner]
}

object Partners {
  val latestModelVersion = "1.0"
  implicit val formats = Json.format[Partners]
}

object AdditionalBusinessPremises {
  implicit val formats = Json.format[AdditionalBusinessPremises]
}

object AdditionalBusinessPremisesList {
  implicit val formats = Json.format[AdditionalBusinessPremisesList]
}

object BusinessDirector {
  implicit val formats: Format[BusinessDirector] = Json.format[BusinessDirector]
}

object BusinessDirectors {
  val latestModelVersion = "1.0"
  implicit val formats: Format[BusinessDirectors] = Json.format[BusinessDirectors]
}

object Supplier {
  implicit val formats = Json.format[Supplier]
}

object Suppliers {
  implicit val formats = Json.format[Suppliers]
}

object BusinessType {
  implicit val formats = Json.format[BusinessType]
}

object ApplicationDeclaration {
  implicit val formats = Json.format[ApplicationDeclaration]
}

object GroupDeclaration {
  implicit val formats = Json.format[GroupDeclaration]
}

object GroupMember {
  implicit val formats = Json.format[GroupMember]
}

object GroupMembers {
  val latestModelVersion = "1.0"
  implicit val formats = Json.format[GroupMembers]
}

// This can be replaced by an implicit format if CapGemini update their schema
object TradingActivity_old {

  val Producer = ("Producer", "04")
  val Broker = ("Broker", "05")
  val Other = "99"
  val Delimiter = " "

  implicit val writer = new Writes[TradingActivity_old] {

    // remove the Producer and Broker wholesaler codes if required as they will be sent via the 'Other' field from now on
    // and add the 'Other' code to the wholesaler type if either Producer or Broker are selected
    def updateWholesaler(wholesalerType: List[String]): List[String] =
    wholesalerType.contains(Producer._2) || wholesalerType.contains(Broker._2) match {
      case true => wholesalerType.filterNot(x => x == Producer._2 || x == Broker._2 || x == Other).::(Other)
      case _ => wholesalerType
    }

    val trimOtherWholesaler = (otherField: String) => otherField.length > AwrsFieldConfig.otherWholesalerLen match {
      case true => otherField.substring(0, AwrsFieldConfig.otherWholesalerLen)
      case _ => otherField
    }

    // add the Producer and Broker to the other field if they have been selected
    def augmentOtherWholesaler(wholesalerType: List[String], otherWholesaler: Option[String]): JsObject =
      (wholesalerType.contains(Producer._2), wholesalerType.contains(Broker._2)) match {
        case (true, true) => Json.obj("otherWholesaler" -> trimOtherWholesaler(Producer._1 + Delimiter + Broker._1 + Delimiter + otherWholesaler.fold("")(x => x)))
        case (false, true) => Json.obj("otherWholesaler" -> trimOtherWholesaler(Broker._1 + Delimiter + otherWholesaler.fold("")(x => x)))
        case (true, false) => Json.obj("otherWholesaler" -> trimOtherWholesaler(Producer._1 + Delimiter + otherWholesaler.fold("")(x => x)))
        case _ => otherWholesaler.fold(Json.obj())(x => Json.obj("otherWholesaler" -> x))
      }

    def writes(tradingActivity: TradingActivity_old): JsValue =
      Json.obj()
        .++(Json.obj("wholesalerType" -> updateWholesaler(tradingActivity.wholesalerType)))
        .++(tradingActivity.otherWholesaler.fold(augmentOtherWholesaler(tradingActivity.wholesalerType, tradingActivity.otherWholesaler))(x => augmentOtherWholesaler(tradingActivity.wholesalerType, tradingActivity.otherWholesaler)))
        .++(Json.obj("typeOfAlcoholOrders" -> tradingActivity.typeOfAlcoholOrders))
        .++(tradingActivity.otherTypeOfAlcoholOrders.fold(Json.obj())(x => Json.obj("otherTypeOfAlcoholOrders" -> x)))
        .++(tradingActivity.doesBusinessImportAlcohol.fold(Json.obj())(x => Json.obj("doesBusinessImportAlcohol" -> x)))
        .++(tradingActivity.thirdPartyStorage.fold(Json.obj())(x => Json.obj("thirdPartyStorage" -> x)))
        .++(Json.obj("doYouExportAlcohol" -> tradingActivity.doYouExportAlcohol))
  }

  implicit val reader = new Reads[TradingActivity_old] {

    // remove the 'Other' wholesaler type if it only contained Producer and Broker so it is not selected on the screen
    // add the Producer and Broker wholesaler codes if required so they are selected on the screen
    def augmentWholesaler(wholesalerType: List[String], otherWholesaler: Option[String]): List[String] = otherWholesaler match {
      case Some(other) =>
        wholesalerType.filterNot(x => x == Other) ++ {
          other.split(Delimiter).toSet[String].map {
            case Producer._1 => Producer._2
            case Broker._1 => Broker._2
            case _ => Other
          }
        }
      case _ => wholesalerType
    }

    // Remove the Producer and Broker text from the free text 'Other' field.
    // Check for the String plus delimiter first then just the String, this caters for spaces being used as the delimiter
    // as these will be trimmed if there is no genuine 'otherWholesaler' string after the 'Producer' or 'Broker' String.
    def cleanseOtherWholesaler(wholesalerType: List[String], otherWholesaler: Option[String]): Option[String] = otherWholesaler match {
      case Some(other) =>
        val trimmed = other.split(Delimiter).filterNot(x => x == Producer._1 || x == Broker._1).mkString(Delimiter)
        trimmed.nonEmpty match {
          case true => Some(trimmed)
          case false => None
        }
      case _ => None
    }

    def reads(js: JsValue): JsResult[TradingActivity_old] =
      for {
        wholesalerType <- (js \ "wholesalerType").validate[List[String]]
        otherWholesaler <- (js \ "otherWholesaler").validateOpt[String]
        typeOfAlcoholOrders <- (js \ "typeOfAlcoholOrders").validate[List[String]]
        otherTypeOfAlcoholOrders <- (js \ "otherTypeOfAlcoholOrders").validateOpt[String]
        doesBusinessImportAlcohol <- (js \ "doesBusinessImportAlcohol").validateOpt[String]
        thirdPartyStorage <- (js \ "thirdPartyStorage").validateOpt[String]
        doYouExportAlcohol <- (js \ "doYouExportAlcohol").validate[List[String]]
      } yield {
        val other = cleanseOtherWholesaler(wholesalerType, otherWholesaler)
        TradingActivity_old(wholesalerType = augmentWholesaler(wholesalerType, otherWholesaler),
          otherWholesaler = other,
          typeOfAlcoholOrders = typeOfAlcoholOrders,
          otherTypeOfAlcoholOrders = otherTypeOfAlcoholOrders,
          doesBusinessImportAlcohol = doesBusinessImportAlcohol,
          thirdPartyStorage = thirdPartyStorage,
          doYouExportAlcohol = doYouExportAlcohol)
      }
  }
}

// This can be replaced by an implicit format if CapGemini update their schema
object TradingActivity {

  val Producer = ("Producer", "04")
  val Broker = ("Broker", "05")
  val Other = "99"
  val Delimiter = " "

  implicit val writer = new Writes[TradingActivity] {

    // remove the Producer and Broker wholesaler codes if required as they will be sent via the 'Other' field from now on
    // and add the 'Other' code to the wholesaler type if either Producer or Broker are selected
    def updateWholesaler(wholesalerType: List[String]): List[String] =
    wholesalerType.contains(Producer._2) || wholesalerType.contains(Broker._2) match {
      case true => wholesalerType.filterNot(x => x == Producer._2 || x == Broker._2 || x == Other).::(Other)
      case _ => wholesalerType
    }

    val trimOtherWholesaler = (otherField: String) => otherField.length > AwrsFieldConfig.otherWholesalerLen match {
      case true => otherField.substring(0, AwrsFieldConfig.otherWholesalerLen)
      case _ => otherField
    }

    // add the Producer and Broker to the other field if they have been selected
    def augmentOtherWholesaler(wholesalerType: List[String], otherWholesaler: Option[String]): JsObject =
    (wholesalerType.contains(Producer._2), wholesalerType.contains(Broker._2)) match {
      case (true, true) => Json.obj("otherWholesaler" -> trimOtherWholesaler(Producer._1 + Delimiter + Broker._1 + Delimiter + otherWholesaler.fold("")(x => x)))
      case (false, true) => Json.obj("otherWholesaler" -> trimOtherWholesaler(Broker._1 + Delimiter + otherWholesaler.fold("")(x => x)))
      case (true, false) => Json.obj("otherWholesaler" -> trimOtherWholesaler(Producer._1 + Delimiter + otherWholesaler.fold("")(x => x)))
      case _ => otherWholesaler.fold(Json.obj())(x => Json.obj("otherWholesaler" -> x))
    }

    def writes(tradingActivity: TradingActivity): JsValue =
      Json.obj()
        .++(Json.obj("wholesalerType" -> updateWholesaler(tradingActivity.wholesalerType)))
        .++(tradingActivity.otherWholesaler.fold(augmentOtherWholesaler(tradingActivity.wholesalerType, tradingActivity.otherWholesaler))(x => augmentOtherWholesaler(tradingActivity.wholesalerType, tradingActivity.otherWholesaler)))
        .++(Json.obj("typeOfAlcoholOrders" -> tradingActivity.typeOfAlcoholOrders))
        .++(tradingActivity.otherTypeOfAlcoholOrders.fold(Json.obj())(x => Json.obj("otherTypeOfAlcoholOrders" -> x)))
        .++(tradingActivity.doesBusinessImportAlcohol.fold(Json.obj())(x => Json.obj("doesBusinessImportAlcohol" -> x)))
        .++(tradingActivity.thirdPartyStorage.fold(Json.obj())(x => Json.obj("thirdPartyStorage" -> x)))
        .++(Json.obj("doYouExportAlcohol" -> tradingActivity.doYouExportAlcohol))
        .++(tradingActivity.exportLocation.fold(Json.obj())(x => Json.obj("exportLocation" -> x)))
  }

  implicit val reader = new Reads[TradingActivity] {

    // remove the 'Other' wholesaler type if it only contained Producer and Broker so it is not selected on the screen
    // add the Producer and Broker wholesaler codes if required so they are selected on the screen
    def augmentWholesaler(wholesalerType: List[String], otherWholesaler: Option[String]): List[String] = otherWholesaler match {
      case Some(other) =>
        wholesalerType.filterNot(x => x == Other) ++ {
          other.split(Delimiter).toSet[String].map {
            case Producer._1 => Producer._2
            case Broker._1 => Broker._2
            case _ => Other
          }
        }
      case _ => wholesalerType
    }

    // Remove the Producer and Broker text from the free text 'Other' field.
    // Check for the String plus delimiter first then just the String, this caters for spaces being used as the delimiter
    // as these will be trimmed if there is no genuine 'otherWholesaler' string after the 'Producer' or 'Broker' String.
    def cleanseOtherWholesaler(wholesalerType: List[String], otherWholesaler: Option[String]): Option[String] = otherWholesaler match {
      case Some(other) =>
        val trimmed = other.split(Delimiter).filterNot(x => x == Producer._1 || x == Broker._1).mkString(Delimiter)
        trimmed.nonEmpty match {
          case true => Some(trimmed)
          case false => None
        }
      case _ => None
    }

    def getOrderedWholesalers(orderedWholesalers: Seq[(String, String)], unorderedWholesalers: List[String]): List[String] =
      orderedWholesalers.map(_._1).toList.filter(unorderedWholesalers.toSet)

    def reads(js: JsValue): JsResult[TradingActivity] =
      for {
        wholesalerType <- (js \ "wholesalerType").validate[List[String]]
        otherWholesaler <- (js \ "otherWholesaler").validateOpt[String]
        typeOfAlcoholOrders <- (js \ "typeOfAlcoholOrders").validate[List[String]]
        otherTypeOfAlcoholOrders <- (js \ "otherTypeOfAlcoholOrders").validateOpt[String]
        doesBusinessImportAlcohol <- (js \ "doesBusinessImportAlcohol").validateOpt[String]
        thirdPartyStorage <- (js \ "thirdPartyStorage").validateOpt[String]
        doYouExportAlcohol <- (js \ "doYouExportAlcohol").validateOpt[String]
        exportLocation <- (js \ "exportLocation").validateOpt[List[String]]
      } yield {
        val other = cleanseOtherWholesaler(wholesalerType, otherWholesaler)
        TradingActivity(wholesalerType = getOrderedWholesalers(AwrsFormFields.wholesaler, augmentWholesaler(wholesalerType, otherWholesaler)),
          otherWholesaler = other,
          typeOfAlcoholOrders = typeOfAlcoholOrders,
          otherTypeOfAlcoholOrders = otherTypeOfAlcoholOrders,
          doesBusinessImportAlcohol = doesBusinessImportAlcohol,
          thirdPartyStorage = thirdPartyStorage,
          doYouExportAlcohol = doYouExportAlcohol,
          exportLocation = exportLocation)
      }
  }

  implicit val optFormats = Format.optionWithNull[TradingActivity]
}

object Products {
  implicit val formats = Json.format[Products]
}

object SubscriptionTypeFrontEnd {
  val latestModelVersion = "1.1"

  implicit val formats = Json.format[SubscriptionTypeFrontEnd]
}

object AWRSFEModel {
  implicit val formats = Json.format[AWRSFEModel]
}

object Survey {
  implicit val formats = Json.format[Survey]
}

object Feedback {
  implicit val formats = Json.format[Feedback]
}

object WithdrawalConfirmation {
  implicit val formats = Json.format[WithdrawalConfirmation]
}

object WithdrawalReason {
  implicit val formats = Json.format[WithdrawalReason]
  implicit val optionFormats = Format.optionWithNull[WithdrawalReason]
}

object ReapplicationConfirmation {
  implicit val formats = Json.format[ReapplicationConfirmation]
}

object DeleteConfirmation {
  implicit val formats: Format[DeleteConfirmation] = Json.format[DeleteConfirmation]
}

/* TODO AWRS-1800 old model to be removed after 28 days */
object SubscriptionTypeFrontEnd_old {
  val latestModelVersion = "1.0"

  implicit val format: Format[SubscriptionTypeFrontEnd_old] = Json.format[SubscriptionTypeFrontEnd_old]
}

/* end old block */
