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

import forms.AWRSEnums.BooleanRadioEnum
import forms.{AWRSEnums, DeleteConfirmationForm}
import models.BusinessDetailsEntityTypes._
import models.FormBundleStatus._
import models.StatusContactType.{MindedToReject, MindedToRevoke, NoLongerMindedToRevoke}
import models._
import org.joda.time.LocalDateTime
import org.jsoup.nodes.Document
import org.scalatest.Matchers._
import play.api.data.Form
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.test.FakeRequest
import services.DataCacheKeys._
import services.JourneyConstants
import uk.gov.hmrc.http.cache.client.CacheMap
import view_models.{IndexViewModel, SectionComplete, SectionModel}
import TestConstants._

import scala.annotation.tailrec
import scala.io.Source

object TestUtil {

  implicit def convertToOption[T](value: T): Option[T] = Some(value)

  final lazy val Yes = Some("Yes")
  final lazy val No = Some("No")
  final lazy val EmptyJsVal = None: Option[JsValue]

  val legalEntityList = List("SOP", "Partnership", "LTD", "LLP", "LP", "LLP_GRP", "LTD_GRP")
  val testBCAddress = BCAddress("line1", "line2", Option("line3"), Option("line4"), Option("postcode"), Option("country"))
  val testBusinessCustomerDetails = (legalEntity: String) => BusinessCustomerDetails("ACME", Some(legalEntity), testBCAddress, "sap123", "safe123", false, Some("agent123"), testUtr)

  val testSoleTraderBusinessRegistrationDetails = testBusinessRegistrationDetails(doYouHaveNino = Yes, nino = testNino, doYouHaveVRN = No, doYouHaveUTR = No)
  val testIsNewBusiness = newAWBusiness("Yes", Some(TupleDate("10", "10", "2016")))

  val testPlaceOfBusinessMainPlaceOfBusinessAddressNoPostcode = testPlaceOfBusinessDefault(mainAddress = Some(testAddress(postcode = None)))

  val testCorporateBodyBusinessDetails = testBusinessDetails()

  val testPlaceOfBusinessNoOpDuration = testPlaceOfBusinessDefault(operatingDuration = Some(""))

  val testBusinessDetailsNoNewAWFlag = testBusinessDetails(newBusiness = None)

  val testSoleTraderBusinessDetails = testBusinessDetails()

  val testNewApplicationType = NewApplicationType(Some(true))

  val testAddress = testAddressDefault(addressLine1 = "line1", addressLine2 = "line2", addressLine3 = Some("line3"), addressLine4 = Some("line4"), postcode = Some("NE3 2NG"))
  val testAddressInternational = testAddressDefault(addressLine1 = "line1", addressLine2 = "line2", addressLine3 = Some("line3"), addressLine4 = Some("line4"), addressCountryCode = Some("ES"))

  def testAddressDefault(addressLine1: String,
                         addressLine2: String,
                         addressLine3: Option[String] = None,
                         addressLine4: Option[String] = None,
                         postcode: Option[String] = None,
                         addressCountry: Option[String] = None,
                         addressCountryCode: Option[String] = None) = Address(
    addressLine1 = addressLine1,
    addressLine2 = addressLine2,
    addressLine3 = addressLine3,
    addressLine4 = addressLine4,
    postcode = postcode,
    addressCountry = addressCountry,
    addressCountryCode = addressCountryCode)

  def testAdditionalBusinessPremisesDefault(additionalPremises: Option[String] = None,
                                            additionalAddress: Option[Address] = None,
                                            addAnother: Option[String] = None) = AdditionalBusinessPremises(
    additionalPremises = additionalPremises,
    additionalAddress = additionalAddress,
    addAnother = addAnother)

  val testBusinessPremises = testAdditionalBusinessPremisesDefault(additionalPremises = Yes, Some(testAddress), addAnother = Option("No"))

  val testAdditionalBusinessPremises = AdditionalBusinessPremises(additionalPremises = Yes, Some(testAddress()), addAnother = Option("No"))

  val testAdditionalPremisesList = AdditionalBusinessPremisesList(List(testAdditionalBusinessPremises))

  def testBusinessDirectorDefault(directorsAndCompanySecretaries: Option[String] = None,
                                  personOrCompany: Option[String] = None,
                                  firstName: Option[String] = None,
                                  lastName: Option[String] = None,
                                  doTheyHaveNationalInsurance: Option[String] = None,
                                  nino: Option[String] = None,
                                  passportNumber: Option[String] = None,
                                  nationalID: Option[String] = None,
                                  isDirectorACompany: Option[String] = None,
                                  businessName: Option[String] = None,
                                  doYouHaveTradingName: Option[String] = None,
                                  tradingName: Option[String] = None,
                                  doYouHaveUTR: Option[String] = None,
                                  utr: Option[String] = None,
                                  doYouHaveCRN: Option[String] = None,
                                  companyRegNumber: Option[String] = None,
                                  doYouHaveVRN: Option[String] = None,
                                  vrn: Option[String] = None,
                                  otherDirectors: Option[String] = None
                                 ): BusinessDirector = {
    val companyNames =
      (businessName, doYouHaveTradingName, tradingName) match {
        case (None, None, None) => None
        case _ =>
          Some(CompanyNames(
            businessName = businessName,
            doYouHaveTradingName = doYouHaveTradingName,
            tradingName = tradingName))
      }

    BusinessDirector(personOrCompany = personOrCompany,
      firstName = firstName,
      lastName = lastName,
      doTheyHaveNationalInsurance = doTheyHaveNationalInsurance,
      nino = nino,
      passportNumber = passportNumber,
      nationalID = nationalID,
      companyNames = companyNames,
      doYouHaveUTR = doYouHaveUTR,
      utr = utr,
      doYouHaveCRN = doYouHaveCRN,
      companyRegNumber = companyRegNumber,
      doYouHaveVRN = doYouHaveVRN,
      vrn = vrn,
      directorsAndCompanySecretaries = directorsAndCompanySecretaries,
      otherDirectors = otherDirectors)
  }

  val testBusinessDirector = testBusinessDirectorDefault(directorsAndCompanySecretaries = Some("Director and Company Secretary"), personOrCompany = Some("person"), firstName = Some("John"), lastName = Some("Smith"), doTheyHaveNationalInsurance = Option("Yes"), nino = testNino, otherDirectors = No)

  val testBusinessDirectorPerson = testBusinessDirector

  val testBusinessDirectorCompany = testBusinessDirectorDefault(directorsAndCompanySecretaries = Some("Director and Company Secretary"), personOrCompany = Some("company"), businessName = Some("Acme"), doYouHaveTradingName = Some("No"), doYouHaveUTR = Some("No"), doYouHaveCRN = Some("No"), doYouHaveVRN = Some("Yes"), vrn = testVrn, otherDirectors = No)
  val testBusinessDirectors = BusinessDirectors(List(testBusinessDirector))

  def testTradingActivity(wholesalerType: List[String] = List("02", "05", "99"),
                          otherWholesaler: Option[String] = Option("Supermarket"),
                          typeOfAlcoholOrders: List[String] = List("02", "04"),
                          otherTypeOfAlcoholOrders: Option[String] = Option("Post"),
                          doesBusinessImportAlcohol: Option[String] = Option("Yes"),
                          thirdPartyStorage: Option[String] = Option("Yes"),
                          doYouExportAlcohol: Option[String] = Some("No"),
                          exportLocation: Option[List[String]] = None) =
    TradingActivity(
      wholesalerType = wholesalerType,
      otherWholesaler,
      typeOfAlcoholOrders,
      otherTypeOfAlcoholOrders,
      doesBusinessImportAlcohol,
      doYouExportAlcohol = doYouExportAlcohol,
      exportLocation = exportLocation,
      thirdPartyStorage)

  def testProducts(mainCustomers: List[String] = List("02"),
                   otherMainCustomers: Option[String] = Option("Off_License"),
                   productType: List[String] = List("02"),
                   otherProductType: Option[String] = None) =
    Products(
      mainCustomers,
      otherMainCustomers,
      productType,
      otherProductType)

  val testApplicationDeclaration = testApplicationDeclarationDefault(declarationName = Some("Mark Smith"), declarationRole = Some("Owner"), None)
  val testApplicationDeclarationTrue = testApplicationDeclarationDefault(Option("John Doe"), Option("Senior Manager"), Option(true))

  def testApplicationDeclarationDefault(declarationName: Option[String] = None,
                                        declarationRole: Option[String] = None,
                                        confirmation: Option[Boolean] = None) = ApplicationDeclaration(declarationName, declarationRole, confirmation)

  val testPartnerDetails = Partners(List(testPartner(), testPartner()))

  val testReviewDetails = BusinessCustomerDetails("ACME", Some("SOP"), BCAddress("additional line 1", "additional line 2", Option("line3"), Option("line4"), Option("postcode"), None), "sap123", "safe123", false, Some("agent123"))

  val testSubscriptionStatusTypePending = SubscriptionStatusType("2001-12-17T09:30:47Z", Pending, None, false, None)
  val testSubscriptionStatusTypeApproved = SubscriptionStatusType("2001-12-17T09:30:47Z", Approved, Some("2016-04-1T09:30:00Z"), false, None)
  val testSubscriptionStatusTypeApprovedWithConditions = SubscriptionStatusType("10 December 2014", ApprovedWithConditions, Some("2016-04-1T09:30:00Z"), false, Some("123456789012"))
  val testStatusInfoTypeApprovedWithConditions = StatusInfoType(Some(StatusInfoSuccessResponseType("2001-12-17T09:30:47Z", "conditions for approval")))
  val testSubscriptionStatusTypeRejected = SubscriptionStatusType("2001-12-17T09:30:47Z", Rejected, Some("2016-04-1T09:30:00Z"), false, Some("123456789012"))
  val testSubscriptionStatusTypeRejectedUnderReviewOrAppeal = SubscriptionStatusType("2001-12-17T09:30:47Z", RejectedUnderReviewOrAppeal, Some("2016-04-1T09:30:00Z"), false, Some("123456789012"))
  val testSubscriptionStatusTypeRevokedUnderReviewOrAppeal = SubscriptionStatusType("2001-12-17T09:30:47Z", RevokedUnderReviewOrAppeal, Some("2016-04-1T09:30:00Z"), false, Some("123456789012"))
  val testSubscriptionStatusTypeRevoked = SubscriptionStatusType("2001-12-17T09:30:47Z", Revoked, Some("2016-04-1T09:30:00Z"), false, Some("123456789012"))
  val testStatusInfoTypeRejected = StatusInfoType(Some(StatusInfoSuccessResponseType("2001-12-17T09:30:47Z", "reasons for rejection")))
  val testStatusInfoTypeRejectedUnderReviewOrAppeal = StatusInfoType(Some(StatusInfoSuccessResponseType("2001-12-17T09:30:47Z", "reasons for rejection under review")))
  val testStatusInfoTypeRevoked = StatusInfoType(Some(StatusInfoSuccessResponseType("2001-12-17T09:30:47Z", "reasons for revoked")))
  val testStatusInfoTypeRevokedUnderReviewOrAppeal = StatusInfoType(Some(StatusInfoSuccessResponseType("2001-12-17T09:30:47Z", "reasons for revoked under review")))

  val testSubscriptionStatusTypePendingGroup = SubscriptionStatusType("2001-12-17T09:30:47Z", Pending, Some("2001-12-17T09:30:47Z"), true, None)

  val testStatusNotificationNoAlert: Option[StatusNotification] = None
  val testStatusNotificationMindedToReject: Option[StatusNotification] = Some(StatusNotification(Some("XXAW000001234560"), Some("123456789012"), Some(MindedToReject), Some(Pending)))
  val testStatusInfoTypeMindedToReject = StatusInfoType(Some(StatusInfoSuccessResponseType("2001-12-17T09:30:47Z", "reasons for minded to reject")))
  val testStatusInfoTypeNoLongerMindedToReject = StatusInfoType(Some(StatusInfoSuccessResponseType("2001-12-17T09:30:47Z", "reasons for no longer minded to reject")))
  val testStatusNotificationMindedToRevoke: Option[StatusNotification] = Some(StatusNotification(Some("XXAW000001234560"), Some("123456789012"), Some(MindedToRevoke), Some(Approved)))
  val testStatusNotificationNoLongerMindedToRevoke: Option[StatusNotification] = Some(StatusNotification(Some("XXAW000001234560"), Some("123456789012"), Some(NoLongerMindedToRevoke), Some(Approved)))
  val testStatusInfoTypeMindedToRevoke = StatusInfoType(Some(StatusInfoSuccessResponseType("2001-12-17T09:30:47Z", "reasons for minded to revoke")))
  val testStatusInfoTypeNoLongerMindedToRevoke = StatusInfoType(Some(StatusInfoSuccessResponseType("2001-12-17T09:30:47Z", "reasons for no longer minded to revoke")))

  val testLegalEntity = BusinessType(Some("LTD"), None, Some(true))

  val testBusinessPartnerName = "BusinessPartner"

  def dynamicLegalEntity(legalEntity: String) = legalEntity match {
    case "SOP" => BusinessType(Some(legalEntity), Some(true), None)
    case _ => BusinessType(Some(legalEntity), None, Some(true))
  }

  def defaultTradingCompanyName(id: Int = 1) = CompanyNames(Some("ACME"), Some("Yes"), Some(s"Business$id"))

  def testGroupMemberDefault(names: CompanyNames = defaultTradingCompanyName(),
                             address: Option[Address] = Some(testAddress),
                             groupJoiningDate: Option[String] = None,
                             doYouHaveVRN: Option[String] = No,
                             vrn: Option[String] = None,
                             companyRegDetails: Option[CompanyRegDetails] = None,
                             isBusinessIncorporated: Option[String] = No,
                             doYouHaveUTR: Option[String] = Yes,
                             utr: Option[String] = testUtr,
                             addAnotherGrpMember: Option[String] = No) =
    GroupMember(companyNames = names, address = address, groupJoiningDate = groupJoiningDate, doYouHaveUTR = doYouHaveUTR, utr = utr, isBusinessIncorporated = isBusinessIncorporated, companyRegDetails = companyRegDetails, doYouHaveVRN = doYouHaveVRN, vrn = vrn, addAnotherGrpMember = addAnotherGrpMember)

  val testGroupMember = testGroupMemberDefault()
  val testGroupMemberDetails = GroupMembers(List(testGroupMember))

  val testGroupMemberDetailsAddAnother = GroupMembers(List(GroupMember(defaultTradingCompanyName(), Some(Address("line1", "line2", Option("line3"), Option("line4"), Option("NE28 6LZ"), None, None)), None, Yes, testUtr, No, None, No, None, No)))
  val testGroupMemberDetails2Members = GroupMembers(List(GroupMember(defaultTradingCompanyName(), Some(Address("line1", "line2", Option("line3"), Option("line4"), Option("NE28 6LZ"), None, None)), None, Yes, testUtr, No, None, No, None, Yes),
    GroupMember(defaultTradingCompanyName(2), Some(Address("line1", "line2", Option("line3"), Option("line4"), Option("NE28 6LZ"), None, None)), None, Yes, testUtr, No, None, No, None, No)))
  val testGroupDeclaration = GroupDeclaration(true)
  val testBusinessCustomerDetailsOrig = BusinessCustomerDetails("ACME", Some("SOP"), BCAddress("line1", "line2", Option("line3"), Option("line4"), Option("postcode"), Option("country")), "sap123", "safe123", true, Some("agent123"))

  val testAddressOrig = Address(postcode = Some("postcode"), addressLine1 = "line1", addressLine2 = "line2", addressLine3 = Some("line3"), addressLine4 = Some("line4"), addressCountryCode = None)
  val testAdditionalBusinessPremisesOrig1 = AdditionalBusinessPremises(additionalPremises = Yes, Some(testAddressOrig), addAnother = Option("No"))
  val testAdditionalBusinessPremisesOrig2 = AdditionalBusinessPremises(additionalPremises = Yes, Some(testAddress()), addAnother = Option("No"))
  val testAdditionalPremisesListOrig = AdditionalBusinessPremisesList(List(testAdditionalBusinessPremisesOrig1, testAdditionalBusinessPremisesOrig2))
  val testAdditionalPremisesListCache = AdditionalBusinessPremisesList(List(testAdditionalBusinessPremisesOrig2))

  val testBusinessDirectorOrig =
    BusinessDirector(Some("Person"),
      firstName = Some("John"),
      lastName = Some("Smith"),
      doTheyHaveNationalInsurance = Option("Yes"),
      nino = testNino,
      passportNumber = None,
      nationalID = None,
      companyNames = None,
      doYouHaveUTR = None,
      utr = None,
      doYouHaveCRN = None,
      companyRegNumber = None,
      doYouHaveVRN = None,
      vrn = None,
      directorsAndCompanySecretaries = Some("Director and Company Secretary"),
      otherDirectors = No
    )

  val testBusinessDirectorsOrig = BusinessDirectors(List(testBusinessDirector))

  val testApplicationDeclarationOrig = ApplicationDeclaration(declarationName = Some("Mark Smith"), declarationRole = Some("Owner"), None)

  def testSupplierDefault(alcoholSuppliers: Option[String] = No,
                          supplierName: Option[String] = None,
                          ukSupplier: Option[String] = None,
                          vatRegistered: Option[String] = None,
                          vatNumber: Option[String] = None,
                          supplierAddress: Option[Address] = None,
                          additionalSupplier: Option[String] = None) = Supplier(alcoholSuppliers = alcoholSuppliers,
    supplierName = supplierName,
    vatRegistered = vatRegistered,
    vatNumber = vatNumber,
    supplierAddress = supplierAddress,
    additionalSupplier = additionalSupplier,
    ukSupplier = ukSupplier)

  def testSupplier(isUK: Boolean = true) = {
    val address = isUK match {
      case true => testAddress
      case _ => testAddressInternational
    }
    testSupplierDefault(alcoholSuppliers = Yes,
      supplierName = Some("Smith and co"),
      vatRegistered = Yes,
      vatNumber = testVrn,
      supplierAddress = Some(address),
      additionalSupplier = No,
      ukSupplier = Yes)
  }

  def testSupplierOthersYes(isUK: Boolean = true) = {
    val address = isUK match {
      case true => testAddress
      case _ => testAddressInternational
    }
    testSupplierDefault(alcoholSuppliers = Yes,
      supplierName = Some("Smith and co"),
      vatRegistered = Yes,
      vatNumber = testVrn,
      supplierAddress = Some(address),
      additionalSupplier = Yes,
      ukSupplier = Yes)
  }

  lazy val testSuppliers = Suppliers(List(testSupplier()))
  lazy val testSuppliersInternational = Suppliers(List(testSupplier(isUK = false)))
  lazy val testSupplierAddressList = Suppliers(List(testSupplierOthersYes(isUK = true).copy(supplierAddress = Some(testAddress.copy(addressCountry = Some("United Kingdom")))),
    testSupplier(isUK = false).copy(supplierAddress = Some(testAddressInternational.copy(addressCountry = Some("Spain"), addressCountryCode = Some("ES"))))))
  lazy val testSupplierAddressListOrig = Suppliers(List(testSupplierOthersYes(isUK = true), testSupplier(isUK = false)))

  val reviewDetails: BusinessCustomerDetails = BusinessCustomerDetails("ACME", Some("SOP"), BCAddress("line1", "line2", Option("line3"), Option("line4"), Option("postcode"), Option("country")), "sap123", "safe123", true, Some("agent123"))
  val returnedCacheMap: CacheMap = CacheMap("data", Map("BC_Business_Details" -> Json.toJson(reviewDetails), "Supplier" -> Json.toJson(testSuppliers)))
  val returnedKeystoreCacheMap: CacheMap = CacheMap("data", Map("1097172564" -> Json.toJson(testSubscriptionStatusTypePendingGroup)))

  def createIndexViewModel(legalEntity: String,
                           businessDetails: view_models.IndexStatus = SectionComplete,
                           businessRegistrationDetails: view_models.IndexStatus = SectionComplete,
                           placeOfBusiness: view_models.IndexStatus = SectionComplete,
                           businessContacts: view_models.IndexStatus = SectionComplete,
                           groupMemberDetails: view_models.IndexStatus = SectionComplete,
                           additionalBusinessPremises: view_models.IndexStatus = SectionComplete,
                           partnerDetails: view_models.IndexStatus = SectionComplete,
                           businessDirectors: view_models.IndexStatus = SectionComplete,
                           tradingActivity: view_models.IndexStatus = SectionComplete,
                           products: view_models.IndexStatus = SectionComplete,
                           suppliers: view_models.IndexStatus = SectionComplete,
                           additionalPremSize: Int = 1,
                           partnerSize: Int = 2,
                           groupSize: Int = 1,
                           directorSize: Int = 1,
                           supplierSize: Int = 2) = {
    def addTestData(legalEntity: String, section: String): SectionModel = section match {
      case `businessDetailsName` => getDynamicSectionModel(legalEntity, section, businessDetails)
      case `businessRegistrationDetailsName` => getDynamicSectionModel(legalEntity, section, businessRegistrationDetails)
      case `placeOfBusinessName` => getDynamicSectionModel(legalEntity, section, placeOfBusiness)
      case `businessContactsName` => getDynamicSectionModel(legalEntity, section, businessContacts)
      case `additionalBusinessPremisesName` => SectionModel(s"additionalPremises", s"/alcohol-wholesale-scheme/view-section/$additionalBusinessPremisesName", "awrs.index_page.additional_premises_text", additionalBusinessPremises, additionalPremSize)
      case `partnersName` => SectionModel("businessPartners", s"/alcohol-wholesale-scheme/view-section/$partnersName", "awrs.index_page.business_partners_text", partnerDetails, partnerSize)
      case `groupMembersName` => SectionModel("groupMembers", s"/alcohol-wholesale-scheme/view-section/$groupMembersName", "awrs.index_page.group_member_details_text", groupMemberDetails, groupSize)
      case `businessDirectorsName` => SectionModel("directorsAndCompanySecretaries", s"/alcohol-wholesale-scheme/view-section/$businessDirectorsName", "awrs.index_page.business_directors.index_text", businessDirectors, directorSize)
      case `tradingActivityName` => SectionModel(tradingActivityName, s"/alcohol-wholesale-scheme/view-section/$tradingActivityName", "awrs.index_page.trading_activity_text", tradingActivity)
      case `productsName` => SectionModel(productsName, s"/alcohol-wholesale-scheme/view-section/$productsName", "awrs.index_page.products_text", products)
      case `suppliersName` => SectionModel("aboutYourSuppliers", s"/alcohol-wholesale-scheme/view-section/$suppliersName", "awrs.index_page.suppliers_text", suppliers, supplierSize)
      case _ => throw new Exception("Unknown section")
    }
    def getDynamicSectionModel(legalEntity: String, sectionName: String, sectionStatus: view_models.IndexStatus) = {
      val (sectionId, busTypeInMsg): (String, String) = legalEntity match {
        case "SOP" | "LTD" => ("business", "business")
        case "Partnership" | "LLP" | "LP" => ("partnership", "partnership")
        case "LTD_GRP" | "LLP_GRP" => ("groupBusiness", "group_business")
      }
      val sectionMessage = (page: String) => f"awrs.index_page.${busTypeInMsg}_${page}_text"
      sectionName match {
        case `businessDetailsName` => SectionModel(sectionId + "Details", "/alcohol-wholesale-scheme/view-section/businessDetails", sectionMessage("details"), sectionStatus)
        case `businessRegistrationDetailsName` => SectionModel(sectionId + "RegistrationDetails", "/alcohol-wholesale-scheme/view-section/businessRegistrationDetails", sectionMessage("registration_details"), sectionStatus)
        case `businessContactsName` => SectionModel(sectionId + "Contacts", "/alcohol-wholesale-scheme/view-section/businessContacts", sectionMessage("contacts"), sectionStatus)
        case `placeOfBusinessName` => SectionModel(sectionId + "PlaceOfBusiness", "/alcohol-wholesale-scheme/view-section/placeOfBusiness", sectionMessage("place_of_business"), sectionStatus)
      }
    }

    @tailrec
    def addSectionToList(journey: Seq[String], sections: List[SectionModel]): List[SectionModel] = journey match {
      case head :: tail => addSectionToList(tail, sections :+ addTestData(legalEntity, head))
      case _ => sections
    }
    IndexViewModel(addSectionToList(JourneyConstants.getJourney(legalEntity), List()))
  }


  def createCacheMap(legalEntity: String,
                     businessDetails: Option[String => BusinessDetails] = (entity: String) => testBusinessDetails(Some(entity)),
                     businessRegistrationDetails: Option[String => BusinessRegistrationDetails] = (entity: String) => testBusinessRegistrationDetails(Some(entity)),
                     placeOfBusiness: Option[PlaceOfBusiness] = testPlaceOfBusinessDefault(),
                     businessContacts: Option[BusinessContacts] = testBusinessContactsDefault(),
                     groupMemberDetails: Option[GroupMembers] = testGroupMemberDetails,
                     groupDeclaration: Option[GroupDeclaration] = testGroupDeclaration,
                     additionalBusinessPremises: Option[AdditionalBusinessPremisesList] = testAdditionalPremisesList,
                     partnerDetails: Option[Partners] = testPartnerDetails,
                     businessDirectors: Option[BusinessDirectors] = testBusinessDirectors,
                     tradingActivity: Option[TradingActivity] = testTradingActivity(),
                     products: Option[Products] = testProducts(),
                     suppliers: Option[Suppliers] = testSupplierAddressList,
                     businessType: BusinessDetailsEntityTypes.Value = SoleTrader) = {
    def addTestData(legalEntity: String, section: String): Option[JsValue] = section match {
      case `businessDetailsName` => businessDetails.fold(EmptyJsVal)(f => Json.toJson(f(legalEntity)))
      case `businessRegistrationDetailsName` => businessRegistrationDetails.fold(EmptyJsVal)(f => Json.toJson(f(legalEntity)))
      case `placeOfBusinessName` => placeOfBusiness.fold(EmptyJsVal)(x => Some(Json.toJson(x)))
      case `businessContactsName` => businessContacts.fold(EmptyJsVal)(x => Some(Json.toJson(x)))
      case `additionalBusinessPremisesName` => additionalBusinessPremises.fold(EmptyJsVal)(x => Some(Json.toJson(x)))
      case `partnersName` => partnerDetails.fold(EmptyJsVal)(x => Some(Json.toJson(x)))
      case `groupMembersName` => groupMemberDetails.fold(EmptyJsVal)(x => Some(Json.toJson(x)))
      case `businessDirectorsName` => businessDirectors.fold(EmptyJsVal)(x => Some(Json.toJson(x)))
      case `tradingActivityName` => tradingActivity.fold(EmptyJsVal)(x => Some(Json.toJson(x)))
      case `productsName` => products.fold(EmptyJsVal)(x => Some(Json.toJson(x)))
      case `suppliersName` => suppliers.fold(EmptyJsVal)(x => Some(Json.toJson(x)))
      case _ => throw new Exception("Unknown section")
    }
    @tailrec
    def addSectionToMap(journey: Seq[String], cacheMap: Map[String, JsValue]): Map[String, JsValue] = journey match {
      case head :: tail => addTestData(legalEntity, head) match {
        case Some(data) => addSectionToMap(tail, cacheMap + (head -> data))
        case _ => addSectionToMap(tail, cacheMap)
      }
      case _ => cacheMap
    }
    CacheMap(testUtr, addSectionToMap(JourneyConstants.getJourney(legalEntity),
      groupDeclaration.fold(Map(businessTypeName -> Json.toJson(testBusinessDetailsEntityTypes(businessType))))(x =>
        Map(businessTypeName -> Json.toJson(testBusinessDetailsEntityTypes(businessType)), groupDeclarationName -> Json.toJson(x)))))
  }

  lazy val emptyCachemap = CacheMap(testUtr, Map())

  // this test data is used to simulate a bug from ETMP as specified by AWRS-1413
  // sometimes the proposed start date is not returned from API 5
  // if the date is missing the data is still valid if isNewBusiness is false
  // however the data is incomplete if isNewBusiness is true
  def testBusinessDetailsWithMissingStartDate(legalEntity: String = "SOP", isNewBusiness: Boolean) = createCacheMap(legalEntity = legalEntity, businessDetails = (x: String) => testBusinessDetails(newBusiness =
    Some(NewAWBusiness(newAWBusiness =
      isNewBusiness match {
        case true => BooleanRadioEnum.YesString
        case false => BooleanRadioEnum.NoString
      }, None))))

  def testApplicationStatus(status: AWRSEnums.ApplicationStatusEnum.Value = AWRSEnums.ApplicationStatusEnum.Withdrawn, updatedDate: LocalDateTime = LocalDateTime.now()) = ApplicationStatus(status = status, updatedDate = updatedDate)

  val testBusinessDetailsEntityTypes = (entityType: BusinessDetailsEntityTypes.Value) => entityType match {
    case SoleTrader => BusinessType(Some("SOP"), Some(true), None)
    case CorporateBody => BusinessType(Some("LTD"), None, Some(true))
    case GroupRep => BusinessType(Some("LTD_GRP"), None, Some(true))
    case Llp => BusinessType(Some("LLP"), None, Some(true))
    case Lp => BusinessType(Some("LP"), None, Some(true))
    case Partnership => BusinessType(Some("Partnership"), None, Some(true))
  }

  def testBusinessDetails(doYouHaveTradingName: Option[String] = Some("Yes"),
                          tradingName: Option[String] = Some("Simple Wines"),
                          newBusiness: Option[NewAWBusiness] = Some(newAWBusiness())) =
    BusinessDetails(
      doYouHaveTradingName = doYouHaveTradingName,
      tradingName = tradingName,
      newAWBusiness = newBusiness
    )

  def testBusinessRegistrationDetails(legalEntity: String) = legalEntity match {
    case "Partnership" => defaultBusinessRegistrationDetails(legalEntity = legalEntity)
    case "SOP" => defaultBusinessRegistrationDetails(doYouHaveNino = Yes, nino = testNino)
    case _ => defaultBusinessRegistrationDetails(legalEntity = legalEntity, isBusinessIncorporated = Yes, companyRegDetails = Some(testCompanyRegDetails()))
  }

  def defaultBusinessRegistrationDetails(legalEntity: Option[String] = "SOP",
                                         doYouHaveNino: Option[String] = No,
                                         nino: Option[String] = None,
                                         isBusinessIncorporated: Option[String] = No,
                                         companyRegDetails: Option[CompanyRegDetails] = None,
                                         doYouHaveVRN: Option[String] = Yes,
                                         vrn: Option[String] = testVrn,
                                         doYouHaveUTR: Option[String] = Yes,
                                         utr: Option[String] = testUtr) = BusinessRegistrationDetails(
    legalEntity = legalEntity,
    doYouHaveNino = doYouHaveNino,
    nino = nino,
    isBusinessIncorporated = isBusinessIncorporated,
    companyRegDetails = companyRegDetails,
    doYouHaveVRN = doYouHaveVRN,
    vrn = vrn,
    doYouHaveUTR = doYouHaveUTR,
    utr = utr
  )

  def testBusinessRegistrationDetails(legalEntity: Option[String] = "SOP",
                                      doYouHaveNino: Option[String] = Yes,
                                      nino: Option[String] = testNino,
                                      isBusinessIncorporated: Option[String] = Yes,
                                      companyRegDetails: Option[CompanyRegDetails] = Some(testCompanyRegDetails()),
                                      doYouHaveVRN: Option[String] = No,
                                      vrn: Option[String] = None,
                                      doYouHaveUTR: Option[String] = No,
                                      utr: Option[String] = None) = BusinessRegistrationDetails(
    legalEntity = legalEntity,
    doYouHaveNino = doYouHaveNino,
    nino = nino,
    isBusinessIncorporated = isBusinessIncorporated,
    companyRegDetails = companyRegDetails,
    doYouHaveVRN = doYouHaveVRN,
    vrn = vrn,
    doYouHaveUTR = doYouHaveUTR,
    utr = utr
  )

  def testAddress(addressLine1: Option[String] = Some("address Line1"),
                  addressLine2: Option[String] = Some("address Line2"),
                  addressLine3: Option[String] = Some("address Line3"),
                  addressLine4: Option[String] = Some("address Line4"),
                  postcode: Option[String] = Some("NE28 8ER"),
                  addressCountry: Option[String] = None,
                  addressCountryCode: Option[String] = None) =
    Address(addressLine1 = addressLine1.get,
      addressLine2 = addressLine2.get,
      addressLine3 = addressLine3,
      addressLine4 = addressLine4,
      postcode = postcode,
      addressCountry = addressCountry,
      addressCountryCode = addressCountryCode)

  def testBusinessContactsDefault(

                                   contactAddressSame: Option[String] = No,
                                   contactAddress: Option[Address] = Some(testAddress()),
                                   contactFirstName: Option[String] = Some("John"),
                                   contactLastName: Option[String] = Some("Walker"),
                                   email: Option[String] = Some(testEmail),
                                   confirmEmail: Option[String] = Some(testEmail),
                                   telephone: Option[String] = Some("123456789")) =
    BusinessContacts(
      contactAddressSame = contactAddressSame,
      contactAddress = contactAddress,
      contactFirstName = contactFirstName,
      contactLastName = contactLastName,
      email = email,
      confirmEmail = confirmEmail,
      telephone = telephone
    )

  def testPlaceOfBusinessDefault(mainPlaceOfBusiness: Option[String] = No,
                                 mainAddress: Option[Address] = Some(testAddress()),
                                 placeOfBusinessLast3Years: Option[String] = No,
                                 placeOfBusinessAddressLast3Years: Option[Address] = Some(testAddress()),
                                 operatingDuration: Option[String] = Some("0 to 2 years")
                                ) =
    PlaceOfBusiness(
      mainPlaceOfBusiness = mainPlaceOfBusiness,
      mainAddress = mainAddress,
      placeOfBusinessLast3Years = placeOfBusinessLast3Years,
      placeOfBusinessAddressLast3Years = placeOfBusinessAddressLast3Years,
      operatingDuration = operatingDuration
    )

  // used to test business details model conversions, these data are created because the default data set used for other tests have very different business details data
  // between the entities
  lazy val defaultTestBusinessDetails = testBusinessDetails()
  lazy val defaultTestBusinessContacts = testBusinessContactsDefault()

  // used to test business details model conversions, this function is created because the default data set used for other tests have very different business details data
  // between the entities
  def testSubscriptionTypeFrontEnd(legalEntity: Option[BusinessType] = Some(testBusinessDetailsEntityTypes(SoleTrader)),
                                   businessPartnerName: String = testBusinessPartnerName,
                                   groupDeclaration: Option[GroupDeclaration] = Some(testGroupDeclaration),
                                   businessCustomerDetails: Option[BusinessCustomerDetails] = Some(testBusinessCustomerDetailsOrig),
                                   businessDetails: Option[BusinessDetails] = Some(testSoleTraderBusinessDetails),
                                   businessRegistrationDetails: Option[BusinessRegistrationDetails] = Some(testSoleTraderBusinessRegistrationDetails),
                                   placeOfBusiness: Option[PlaceOfBusiness] = Some(testPlaceOfBusinessDefault()),
                                   businessContacts: Option[BusinessContacts] = Some(testBusinessContactsDefault()),
                                   businessPartnerDetails: Option[Partners] = Some(testPartnerDetails),
                                   groupMemberDetails: Option[GroupMembers] = Some(testGroupMemberDetails),
                                   additionalPremises: Option[AdditionalBusinessPremisesList] = Some(testAdditionalPremisesListOrig),
                                   businessDirectors: Option[BusinessDirectors] = Some(testBusinessDirectorsOrig),
                                   tradingActivity: Option[TradingActivity] = Some(testTradingActivity()),
                                   products: Option[Products] = Some(testProducts()),
                                   suppliers: Option[Suppliers] = Some(testSupplierAddressListOrig),
                                   applicationDeclaration: Option[ApplicationDeclaration] = Some(testApplicationDeclarationOrig),
                                   changeIndicators: Option[ChangeIndicators] = None) =
  SubscriptionTypeFrontEnd(
    legalEntity = legalEntity,
    businessPartnerName = Some(businessPartnerName),
    groupDeclaration = groupDeclaration,
    businessCustomerDetails = businessCustomerDetails,
    businessDetails = businessDetails,
    businessRegistrationDetails = businessRegistrationDetails,
    placeOfBusiness = placeOfBusiness,
    businessContacts = businessContacts,
    partnership = businessPartnerDetails,
    groupMembers = groupMemberDetails,
    additionalPremises = additionalPremises,
    businessDirectors = businessDirectors,
    tradingActivity = tradingActivity,
    products = products,
    suppliers = suppliers,
    applicationDeclaration = applicationDeclaration,
    changeIndicators = changeIndicators
  )

  val defaultTestSubscriptionTypeFrontEnd = testSubscriptionTypeFrontEnd()

  val soleTraderSubscriptionTypeFrontEnd = testSubscriptionTypeFrontEnd(groupDeclaration = None, businessPartnerDetails = None, groupMemberDetails = None, businessDirectors = None)

  val differentSoleTraderSubscriptionTypeFrontEnd = testSubscriptionTypeFrontEnd(businessDetails = Some(testBusinessDetails(tradingName = Some("Complex Wines"))), groupDeclaration = None, businessPartnerDetails = None, groupMemberDetails = None, businessDirectors = None)

  val matchingSoleTraderCacheMap = CacheMap(testUtr, Map("businessCustomerDetails" -> Json.toJson(testBusinessCustomerDetailsOrig),
    "businessDetails" -> Json.toJson(testSoleTraderBusinessDetails),
    "businessRegistrationDetails" -> Json.toJson(testSoleTraderBusinessRegistrationDetails),
    placeOfBusinessName -> Json.toJson(testPlaceOfBusinessDefault()),
    "businessContacts" -> Json.toJson(testBusinessContactsDefault()),
    "additionalBusinessPremises" -> Json.toJson(testAdditionalPremisesListCache),
    "tradingActivity" -> Json.toJson(testTradingActivity()),
    "products" -> Json.toJson(testProducts()),
    "suppliers" -> Json.toJson(testSupplierAddressListOrig),
    "legalEntity" -> Json.toJson(testBusinessDetailsEntityTypes(SoleTrader)),
    "applicationDeclaration" -> Json.toJson(testApplicationDeclarationOrig)
  ))

  def testCompanyRegDetails(companyRegistrationNumber: String = "12345678", dateOfIncorporation: TupleDate = TupleDate("10", "10", "2016")): CompanyRegDetails =
    CompanyRegDetails(companyRegistrationNumber, dateOfIncorporation)

  def newAWBusiness(newAWBusiness: String = "No", proposedStartDate: Option[TupleDate] = None): NewAWBusiness = NewAWBusiness(newAWBusiness, proposedStartDate)

  def testBusinessDetailsSupport(isBugged: Boolean) = BusinessDetailsSupport(isBugged)

  def testBusinessDetailsSupport(newAWBusiness: NewAWBusiness) = BusinessDetailsSupport.evaluate(newAWBusiness)

  def testBusinessDetailsSupport(subscriptionTypeFrontEnd: SubscriptionTypeFrontEnd) = BusinessDetailsSupport.evaluate(subscriptionTypeFrontEnd)


  def loadAndParseJson(path: String): JsValue = {
    val source = Source.fromURL(getClass.getResource(path)).mkString
    Json.parse(source)
  }

  def getChangeIndicators(businessDetailsChanged: Boolean = false,
                          businessAddressChanged: Boolean = false,
                          contactDetailsChanged: Boolean = false,
                          additionalBusinessInfoChanged: Boolean = false,
                          partnersChanged: Boolean = false,
                          coOfficialsChanged: Boolean = false,
                          premisesChanged: Boolean = false,
                          suppliersChanged: Boolean = false,
                          groupMembersChanged: Boolean = false,
                          declarationChanged: Boolean = false): ChangeIndicators = {
    ChangeIndicators(businessDetailsChanged, businessAddressChanged, contactDetailsChanged,
      additionalBusinessInfoChanged, partnersChanged, coOfficialsChanged,
      premisesChanged, suppliersChanged, groupMembersChanged, declarationChanged)
  }

  def getSectionChangeIndicators(businessDetailsChanged: Boolean = false,
                                 businessRegistrationDetailsChanged: Boolean = false,
                                 businessAddressChanged: Boolean = false,
                                 contactDetailsChanged: Boolean = false,
                                 tradingActivityChanged: Boolean = false,
                                 productsChanged: Boolean = false,
                                 partnersChanged: Boolean = false,
                                 coOfficialsChanged: Boolean = false,
                                 premisesChanged: Boolean = false,
                                 suppliersChanged: Boolean = false,
                                 groupMembersChanged: Boolean = false,
                                 declarationChanged: Boolean = false): SectionChangeIndicators = {
    SectionChangeIndicators(businessDetailsChanged, businessRegistrationDetailsChanged, businessAddressChanged, contactDetailsChanged,
      tradingActivityChanged, productsChanged, partnersChanged, coOfficialsChanged,
      premisesChanged, suppliersChanged, groupMembersChanged, declarationChanged)
  }

  def getSectionChangeIndicatorsAllTrue = getSectionChangeIndicators(true, true, true, true, true, true, true, true, true, true, true)

  def testPartner(entityType: Option[String] = Some("Individual"),
                  firstName: Option[String] = None,
                  lastName: Option[String] = None,
                  businessName: Option[String] = None,
                  tradingName: Option[String] = None,
                  partnerAddress: Option[Address] = Some(testAddress()),
                  doYouHaveNino: Option[String] = No,
                  nino: Option[String] = None,
                  isBusinessIncorporated: Option[String] = None,
                  companyRegDetails: Option[CompanyRegDetails] = None,
                  doYouHaveVRN: Option[String] = None,
                  vrn: Option[String] = None,
                  doYouHaveUTR: Option[String] = None,
                  utr: Option[String] = None,
                  otherPartners: Option[String] = No): Partner = {

    Partner(entityType, firstName, lastName, CompanyNames(businessName, doYouHaveTradingName = tradingName match {
      case Some(_) => "Yes"
      case _ => "No"
    }, tradingName), partnerAddress, doYouHaveNino, nino, doYouHaveUTR, utr, isBusinessIncorporated, companyRegDetails, doYouHaveVRN, vrn, otherPartners)
  }

  def deRegistrationDate(proposedEndDate: Option[TupleDate] = Some(TupleDate("12", "7", "2017"))): DeRegistrationDate =
    DeRegistrationDate(proposedEndDate.get)

  def deRegistrationReason(deregistrationReason: Option[String] = Some("Ceases to be registerable for the scheme"), deregReasonOther: Option[String] = Some("Blah")): DeRegistrationReason =
    DeRegistrationReason(deregistrationReason, deregReasonOther)

  def deRegistrationType(successful: Boolean = true): DeRegistrationType = successful match {
    case true => DeRegistrationType(Some(DeRegistrationSuccessResponseType("""{"processingDate": "2001-12-17T09:30:47Z"}""")))
    case false => DeRegistrationType(Some(DeRegistrationFailureResponseType("""{"reason":"Resource not found"}""")))
  }

  // this function replaces duplicated whitespaces with a single whitespace, this mimics the behaviour of the message displayed in html
  @inline private def trimmedMessage(key: String, args: Any*) = Messages(key, args: _*).trim().replaceAll(" +", " ")

  @inline private def messageAssertion(caller: String)(id: String, errorKey: String, expectedErrorKey: String, errorArgs: Any*) = {
    errorKey shouldBe trimmedMessage(expectedErrorKey, errorArgs: _*)

    // This test checks if there is a valid message in the messages file associated with the key
    // This test relies on the play framework propery: if a key doesn't exists in the messages file then the key itself is displayed
    errorKey should not be expectedErrorKey

    assert(!errorKey.equals(""), ", The %s error associated to the field #%s is currently unassigned in the Messages file".format(caller, id))

    assert(!errorKey.matches("^(.*?)\\{\\d+\\}(.*?)"), ", The %s error key %s associated to the field #%s contains unassigned parameters specified in the Message file".format(caller, expectedErrorKey, id))
  }

  @inline def errorSummaryValidation(document: Document, id: String, expectedErrorKey: String, expectedhref: String, errorArgs: Any*) = {
    val expectedSummaryErrorKey: String = expectedErrorKey + ".summary"
    val associatedSummaryErrMsg = document.getElementById(id + "_errorLink")
    if (associatedSummaryErrMsg == null) {
      println(f"id=${id + "_errorLink"}, associatedSummaryErrMsg=$associatedSummaryErrMsg")
    }
    val errorKey = associatedSummaryErrMsg.text

    assert(associatedSummaryErrMsg != null, ", No error summary associated to the field #%s can be found in the given document".format(id))

    errorKey should include(trimmedMessage(expectedSummaryErrorKey, errorArgs: _*))

    messageAssertion("summary")(id, errorKey, expectedSummaryErrorKey, errorArgs: _*)

    // This test validates the link from the summary error message to the correct field.
    associatedSummaryErrMsg.attr("href") shouldBe expectedhref
  }

  @inline def errorNotificationValidation(document: Document, id: String, expectedErrorKey: String, errorArgs: Any*) = {
    val associatedErrMsg = document.getElementById(id + "-error-0")
    assert(associatedErrMsg != null, ", No error message associated to the field #%s can be found in the given document".format(id))

    val errorKey = associatedErrMsg.text

    // only a single error should be displayed per field
    document.getElementById(id + "-error-1") shouldBe null

    messageAssertion("field")(id, errorKey, expectedErrorKey, errorArgs: _*)
  }

  @inline def testErrorMessageValidation(document: Document, id: String, expectedErrorKey: String, errorArgs: Any*) = {
    errorSummaryValidation(document, id, expectedErrorKey, "#" + id + "_field", errorArgs: _*)
    errorNotificationValidation(document, id, expectedErrorKey, errorArgs: _*)
  }

  @inline def noValidationErrors(document: Document, id: String) = {
    document.getElementById(id + "_errorLink") shouldBe null
    document.getElementById(id + "-error-0") shouldBe null
  }


  def testReviewDetailsAny(businessType: String) = BusinessCustomerDetails("ACME", Some(businessType), BCAddress("additional line 1", "additional line 2", Option("line3"), Option("line4"), Option("postcode"), None), "sap123", "safe123", businessType.contains("GRP"), Some("agent123"))

  def updateMockRequest(requestMap: Map[String, String], listOfUpdates: Seq[(String, String)]): Seq[(String, String)] = {
    listOfUpdates match {
      case list if list.isEmpty || requestMap.size < list.length =>
        requestMap.toSeq
      case _ =>
        updateMockRequest(requestMap.updated(listOfUpdates.head._1, listOfUpdates.head._2), listOfUpdates.tail)
    }
  }

  def populateFakeRequest[T](fakeRequest: FakeRequest[_], form: Form[T], data: T): FakeRequest[AnyContentAsFormUrlEncoded] =
    fakeRequest.withFormUrlEncodedBody(form.fill(data).data.toSeq: _*)

  private def testDeleteRequest(delete: DeleteConfirmation) =
    TestUtil.populateFakeRequest[DeleteConfirmation](FakeRequest(), DeleteConfirmationForm.deleteConfirmationForm, delete)

  val deleteConfirmation_No = testDeleteRequest(DeleteConfirmation(No))
  val deleteConfirmation_Yes = testDeleteRequest(DeleteConfirmation(Yes))
  val deleteConfirmation_None = testDeleteRequest(DeleteConfirmation(None))

}