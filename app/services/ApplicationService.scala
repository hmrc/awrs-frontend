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

package services

import _root_.models._
import audit.Auditable
import connectors.{AWRSConnector, AwrsDataCacheConnector}
import controllers.auth.StandardAuthRetrievals
import exceptions.{InvalidStateException, ResubmissionException}
import forms.AwrsFormFields
import javax.inject.Inject
import java.time.LocalDate
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Request}
import services.helper._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utils.CacheUtil.cacheUtil
import utils.{AccountUtils, LoggingUtils}

import scala.concurrent.{ExecutionContext, Future}

class ApplicationService @Inject()(awrsConnector: AWRSConnector,
                                   emailService: EmailService,
                                   val save4LaterService: Save4LaterService,
                                   val keyStoreService: KeyStoreService,
                                   val auditable: Auditable,
                                   val accountUtils: AccountUtils,
                                   val mainStoreSave4LaterConnector: AwrsDataCacheConnector) extends AwrsAPI5Helper with DataCacheService with LoggingUtils {

  val maxSuppliers = 5

  // util method for trading activity and products, this method is introduced to remove the matching of the `all`
  // checkboxes from the lists (AWRS-1278 and 1279).
  // The original `all` options have been removed from the lists in AwrsFormFields, and this method uses
  // these lists as a filter on the user data so they can then be compared ignoring any changes to the `all` option
  implicit class TAndPUtil(selections: List[String]) {
    def sortAndFilterWith(formField: Seq[(String, String)]): List[String] =
      selections.filter(key => formField.map { case (formFieldKey, y) => formFieldKey }.contains(key)).sorted
  }

  def trimSuppliers(suppliers: Option[Suppliers]): Option[Suppliers] =
    suppliers match {
      case Some(sup) if sup.suppliers.size > maxSuppliers => Some(Suppliers(suppliers = sup.suppliers.slice(0, 5)))
      case _ => suppliers
    }

  def getSections(cacheID: String, authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Sections] =
    save4LaterService.mainStore.fetchAll(authRetrievals).map {
      res => res.get.getEntry[BusinessType]("legalEntity") match {
        case Some(BusinessType(Some("SOP"), _, _)) => Sections(soleTraderBusinessDetails = true)
        case Some(BusinessType(Some("LTD"), _, _)) => Sections(corporateBodyBusinessDetails = true, businessDirectors = true)
        case Some(BusinessType(Some("Partnership"), _, _)) => Sections(partnershipBusinessDetails = true, partnership = true)
        case Some(BusinessType(Some("LLP" | "LP"), _, _)) => Sections(llpBusinessDetails = true, partnership = true)
        case Some(BusinessType(Some("LTD_GRP"), _, _)) => Sections(groupRepBusinessDetails = true, groupMemberDetails = true, businessDirectors = true)
        case Some(BusinessType(Some("LLP_GRP"), _, _)) => Sections(groupRepBusinessDetails = true, groupMemberDetails = true, partnership = true)
        case _ => throw InvalidStateException("Invalid Legal entity")
      }
    }

  def assembleSubscriptionTypeFrontEnd(cached: Option[CacheMap],
                                       businessCustomerDetails: Option[BusinessCustomerDetails],
                                       sections: Sections): SubscriptionTypeFrontEnd =
    SubscriptionTypeFrontEnd(
      legalEntity = cached.get.getBusinessType,
      businessPartnerName = Some(businessCustomerDetails.get.businessName),
      groupDeclaration = cached.get.getGroupDeclaration,
      businessCustomerDetails = businessCustomerDetails,
      businessDetails =
        cached.get.getBusinessNameDetails map { businessNameDetails =>
          BusinessDetails(
            businessNameDetails.doYouHaveTradingName,
            businessNameDetails.tradingName,
            cached.get.getTradingStartDetails map {_.invertedBeforeMarch2016Question}
          )
        },
      businessRegistrationDetails = cached.get.getBusinessRegistrationDetails.map{regDetails =>
        regDetails.copy(utr = regDetails.utr.map(_.replaceAll("\\s+", "")))
      },
      businessContacts = cached.get.getBusinessContacts,
      placeOfBusiness = cached.get.getPlaceOfBusiness,
      groupMembers = if (sections.groupMemberDetails) addGroupRepToGroupMembers(cached) else None,
      partnership = if (sections.partnership) cached.get.getPartners else None,
      additionalPremises = cached.get.getAdditionalBusinessPremises,
      businessDirectors = if (sections.businessDirectors) cached.get.getBusinessDirectors else None,
      tradingActivity = cached.get.getTradingActivity,
      products = cached.get.getProducts,
      suppliers = trimSuppliers(cached.get.getSuppliers),
      applicationDeclaration = cached.get.getApplicationDeclaration,
      changeIndicators = None)

  def assembleAWRSFEModel(cached: Option[CacheMap], businessCustomerDetails: Option[BusinessCustomerDetails], sections: Sections): AWRSFEModel =
    AWRSFEModel(assembleSubscriptionTypeFrontEnd(cached, businessCustomerDetails, sections))

  private[services] def isNewBusiness(cacheMap: Option[CacheMap]): Future[Boolean] = cacheMap match {
    case Some(cached) =>
      cached.getTradingStartDetails match {
        case Some(dets) => Future.successful(dets.isNewAWBusiness)
        case _ => Future.successful(false)
      }
    case None => Future.failed(new InternalServerException("No cache map found"))
  }

  def createGroupRep(cached: Option[CacheMap]): GroupMember = {
    val businessName = cached.get.getBusinessCustomerDetails.get.businessName
    val businessNameDetails = cached.get.getBusinessNameDetails.get
    val businessRegistrationDetails = cached.get.getBusinessRegistrationDetails.get
    val placeOfBusiness = cached.get.getPlaceOfBusiness.get

    GroupMember(CompanyNames(Some(businessName),businessNameDetails.doYouHaveTradingName,businessNameDetails.tradingName),
      placeOfBusiness.mainAddress,
      Some(LocalDate.now().toString),
      businessRegistrationDetails.doYouHaveUTR,businessRegistrationDetails.utr,
      businessRegistrationDetails.isBusinessIncorporated,
      businessRegistrationDetails.companyRegDetails,
      businessRegistrationDetails.doYouHaveVRN,
      businessRegistrationDetails.vrn,
      Some("No"))
  }

  def addGroupRepToGroupMembers(cached: Option[CacheMap]) : Option[GroupMembers] =
    Some(GroupMembers(createGroupRep(cached) :: cached.get.getGroupMembers.get.members, GroupMembers.latestModelVersion))

  def replaceGroupRepInGroupMembers(cached: Option[CacheMap]) : Option[GroupMembers] = {
    Some(GroupMembers(cached.get.getGroupMembers.get.members.patch(0, Seq(createGroupRep(cached)), 1), GroupMembers.latestModelVersion))
  }

  def isGrpRepChanged(cached: Option[CacheMap], cachedSubscription: Option[SubscriptionTypeFrontEnd]): Boolean = {
    cached.get.getBusinessType.get.legalEntity match {
      case Some("LTD_GRP") | Some("LLP_GRP") => {
        (cached.get.getBusinessCustomerDetails.get.businessName != cachedSubscription.get.businessPartnerName.get) ||
        (cached.get.getPlaceOfBusiness.get.mainAddress != cachedSubscription.get.placeOfBusiness.get.mainAddress)
      }
      case _ => false
    }
  }

  type AwrsData = Either[SelfHealSubscriptionResponse, SuccessfulSubscriptionResponse]

  def getRegistrationReferenceNumber(either: AwrsData): String = {
    either match {
      case Left(selfHealSubscriptionResponse)    => selfHealSubscriptionResponse.regimeRefNumber
      case Right(successfulSubscriptionResponse) => successfulSubscriptionResponse.awrsRegistrationNumber
    }
  }

  def handleAWRSData(either: AwrsData, authRetrievals: StandardAuthRetrievals, cached: Option[CacheMap])
                    (implicit ec: ExecutionContext, request: Request[AnyContent], hc: HeaderCarrier): Future[AwrsData] = {
    either match {
      case Left(_)              => Future.successful(either)
      case Right(successfulSub) =>
        for {
          isNewBusiness <- isNewBusiness(cached)
          _ <- emailService.sendConfirmationEmail(
            email = cached.get.getBusinessContacts.get.email.get,
            reference = successfulSub.etmpFormBundleNumber,
            isNewBusiness = isNewBusiness,
            authRetrievals = authRetrievals
          )
        } yield {
          either
        }
    }
  }

  def sendApplication(authRetrievals: StandardAuthRetrievals)
                     (implicit request: Request[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Future[AwrsData] = {
    for {
      cached <- save4LaterService.mainStore.fetchAll(authRetrievals)
      businessCustomerDetails = cached.get.getBusinessCustomerDetails
      sections <- getSections(accountUtils.getUtr(authRetrievals), authRetrievals)
      awrsData <- {
        val schema = assembleAWRSFEModel(cached, businessCustomerDetails, sections)
        awrsConnector.submitAWRSData(Json.toJson(schema), authRetrievals)
      }
      processedData <- handleAWRSData(awrsData, authRetrievals, cached)
    } yield {
      processedData
    }
  }

  def hasAPI5ApplicationChanged(cacheID: String, authRetrievals: StandardAuthRetrievals)
                               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    if (accountUtils.hasAwrs(authRetrievals.enrolments)) {
      for {
        cached <- fetchMainStore(authRetrievals)
        cachedSubscription <- save4LaterService.api.fetchSubscriptionTypeFrontEnd(authRetrievals)
        hasAppChanged <- isApplicationDifferent(cached, cachedSubscription)
      } yield {
        hasAppChanged
      }
    } else {
      Future.successful(false)
    }

  def updateApplication(authRetrievals: StandardAuthRetrievals)
                       (implicit request: Request[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Future[SuccessfulUpdateSubscriptionResponse] =
    for {
      cached <- save4LaterService.mainStore.fetchAll(authRetrievals)
      cachedSubscription <- save4LaterService.api.fetchSubscriptionTypeFrontEnd(authRetrievals)
      subscriptionStatus <- keyStoreService.fetchSubscriptionStatus
      _ <- if(isGrpRepChanged(cached,cachedSubscription)) callUpdateGroupBusinessPartner(cached, cachedSubscription, subscriptionStatus, authRetrievals) else Future("OK")
      awrsData <- awrsConnector.updateAWRSData(Json.toJson(AWRSFEModel(getModifiedSubscriptionType(cached, cachedSubscription))), authRetrievals)
      isNewBusiness <- isNewBusiness(cached)
      _ <- emailService.sendConfirmationEmail(
        email = cached.get.getBusinessContacts.get.email.get,
        reference = awrsData.etmpFormBundleNumber,
        isNewBusiness = isNewBusiness,
        authRetrievals = authRetrievals
      )
    } yield {
      awrsData
    }

  def callUpdateGroupBusinessPartner(cached: Option[CacheMap],
                                     cachedSubscription: Option[SubscriptionTypeFrontEnd],
                                     subscriptionStatus: Option[SubscriptionStatusType],
                                     authRetrievals: StandardAuthRetrievals)
                                    (implicit hc: HeaderCarrier, ec: ExecutionContext)
  : Future[SuccessfulUpdateGroupBusinessPartnerResponse] = {
    def createUpdateRegistrationDetailsRequest(businessCustomerAddress: BCAddressApi3): UpdateRegistrationDetailsRequest = {
      val businessContacts = cached.get.getBusinessContacts.get
      UpdateRegistrationDetailsRequest(
        isAnIndividual = false,
        organisation = Some(OrganisationName(cached.get.getBusinessCustomerDetails.get.businessName)),
        address = businessCustomerAddress,
        contactDetails = ContactDetails(phoneNumber = businessContacts.telephone, emailAddress = businessContacts.email),
        isAnAgent = false,
        isAGroup = true
      )
    }

    keyStoreService.fetchBusinessCustomerAddresss flatMap {
      case Some(businessCustomerAddress) =>
        awrsConnector.updateGroupBusinessPartner(
          cachedSubscription.get.businessPartnerName.get,
          cached.get.getBusinessType.get.legalEntity.get,
          subscriptionStatus.get.safeId.get,
          createUpdateRegistrationDetailsRequest(businessCustomerAddress),
          authRetrievals
        )
      case _ =>
        warn("[callUpdateGroupBusinessPartner] - Address not found in keystore, attempting to fetch from save4Later")
        cached.fold(throw new RuntimeException("[callUpdateGroupBusinessPartner] Could not fetch cached changes")){
          cache => cache.getPlaceOfBusiness.fold(throw new RuntimeException("[callUpdateGroupBusinessPartner] Could not fetch business customer address")){
            address =>
              awrsConnector.updateGroupBusinessPartner(
                cachedSubscription.get.businessPartnerName.get,
                cached.get.getBusinessType.get.legalEntity.get,
                subscriptionStatus.get.safeId.get,
                createUpdateRegistrationDetailsRequest(BCAddressApi3(address)),
                authRetrievals
              )
          }
        }
    }
  }

  def removeCountry(someAddress: Option[Address]): Option[Address] =
    someAddress match {
      case Some(address) => Some(address.copy(addressCountry = None,
        addressCountryCode = if (address.addressCountryCode.fold("")(x => x).equals("GB")) None else address.addressCountryCode))
      case _ => None
    }

  def getModifiedSubscriptionType(cached: Option[CacheMap], cachedSubscription: Option[SubscriptionTypeFrontEnd]): SubscriptionTypeFrontEnd = {

    val suppliers = cached.get.getSuppliers

    val additionalPremises = cached.get.getAdditionalBusinessPremises
    val partnership = cached.get.getPartners
    val tradingActivity = cached.get.getTradingActivity
    val products = cached.get.getProducts
    val applicationDeclaration = cached.get.getApplicationDeclaration
    val legalEntity = cached.get.getBusinessType
    val businessPartnerName: String = cached.get.getBusinessCustomerDetails.get.businessName
    val businessDirectors = cached.get.getBusinessDirectors

    val newChangeInds = toChangeIndicators(getChangeIndicators(cached.get, cachedSubscription))
    newChangeInds match {
      case Some(ChangeIndicators(false, false, false, false, false, false, false, false, false, false)) => throw ResubmissionException(ResubmissionException.resubmissionMessage)
      case _ =>
    }

    info(s" CHANGED INDICATOR after population*** ")

    SubscriptionTypeFrontEnd(
      legalEntity = legalEntity,
      businessPartnerName = Some(businessPartnerName),
      groupDeclaration = cached.get.getGroupDeclaration,
      businessCustomerDetails = cached.get.getBusinessCustomerDetails,
      businessDetails =
        cached.get.getBusinessNameDetails map { businessNameDetails =>
          BusinessDetails(
            businessNameDetails.doYouHaveTradingName,
            businessNameDetails.tradingName,
            cached.get.getTradingStartDetails map {_.invertedBeforeMarch2016Question}
          )
        },
      businessRegistrationDetails = cached.get.getBusinessRegistrationDetails.map{regDetails =>
        regDetails.copy(utr = regDetails.utr.map(_.replaceAll("\\s+", "")))
      },
      businessContacts = cached.get.getBusinessContacts,
      placeOfBusiness = cached.get.getPlaceOfBusiness,
      groupMembers = if (isGrpRepChanged(cached, cachedSubscription)) replaceGroupRepInGroupMembers(cached) else cached.get.getGroupMembers,
      partnership = partnership,
      additionalPremises = additionalPremises,
      businessDirectors = businessDirectors,
      tradingActivity = tradingActivity,
      products = products,
      suppliers = suppliers,
      applicationDeclaration = applicationDeclaration,
      changeIndicators = newChangeInds)
  }

  def toChangeIndicators(changeIndicators: Option[SectionChangeIndicators]): Option[ChangeIndicators] = changeIndicators match {
    case Some(sectionChangeIndicators) =>
      Some(ChangeIndicators(businessDetailsChanged = sectionChangeIndicators.businessDetailsChanged || sectionChangeIndicators.businessRegistrationDetailsChanged,
        businessAddressChanged = sectionChangeIndicators.businessAddressChanged,
        contactDetailsChanged = sectionChangeIndicators.contactDetailsChanged,
        additionalBusinessInfoChanged = sectionChangeIndicators.tradingActivityChanged || sectionChangeIndicators.productsChanged,
        partnersChanged = sectionChangeIndicators.partnersChanged,
        coOfficialsChanged = sectionChangeIndicators.coOfficialsChanged,
        premisesChanged = sectionChangeIndicators.premisesChanged,
        suppliersChanged = sectionChangeIndicators.suppliersChanged,
        groupMembersChanged = sectionChangeIndicators.groupMembersChanged,
        declarationChanged = sectionChangeIndicators.declarationChanged))
    case _ => None
  }

  def isApplicationDifferent(cachedData: Option[CacheMap], cachedSubscription: Option[SubscriptionTypeFrontEnd]): Future[Boolean] = {
    cachedData.fold(Future.successful(false)){ cached =>
      getChangeIndicators(cached, cachedSubscription) match {
        case Some(SectionChangeIndicators(false, false, false, false, false, false, false, false, false, false, false, false)) => Future.successful(false)
        case _ => Future.successful(true)
      }
    }
  }

  def getApi5ChangeIndicators(cached: Option[CacheMap], authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[SectionChangeIndicators] = {
    if (accountUtils.hasAwrs(authRetrievals.enrolments) && cached.isDefined) {
      for {
        cachedSubscription <- save4LaterService.api.fetchSubscriptionTypeFrontEnd(authRetrievals)
      } yield {
        val default = SectionChangeIndicators(false, false, false, false, false, false, false, false, false, false, false, false)
        cached.fold(default)(getChangeIndicators(_, cachedSubscription).fold(default)(x => x))
      }
    } else {
      Future.successful(SectionChangeIndicators(false, false, false, false, false, false, false, false, false, false, false, false))
    }
  }

  def getChangeIndicators(cached: CacheMap, cachedSubscription: Option[SubscriptionTypeFrontEnd]): Option[SectionChangeIndicators] = {
    info(s" Cached Data from AWRS-FRONTEND Cache*** ")

    val additionalPremises = cached.getAdditionalBusinessPremises
    val partnership = cached.getPartners
    val tradingActivity = cached.getTradingActivity
    val products = cached.getProducts
    val applicationDeclaration = cached.getApplicationDeclaration
    val legalEntity = cached.getBusinessType


    val businessDirectors = cached.getBusinessDirectors
    val groupMemberDetails = cached.getGroupMembers

    info(s" cachedSubscription Data from AWRS-FRONTEND-API Cache*** ")

    val changeIndicator = cachedSubscription.map { data =>

      info(s" Cached Data from AWRS-FRONTEND-API Cache*** ")

      val changedBusinessDetailsIndicator: (Boolean, Boolean, Boolean, Boolean) = BusinessDetailsAndContactsComparator.compare(data, cached)

      val partnersChanged: Boolean = (data.partnership.isDefined, partnership.isDefined) match {
        case (true, true) => !data.partnership.equals(partnership)
        case (true, false) | (false, true) => true
        case _ => false
      }

      val premisesChanged: Boolean = if (data.additionalPremises.isDefined) {
        val dataAdditionalPremises = Some(AdditionalBusinessPremisesList(data.additionalPremises.get.premises.drop(1)))
        !dataAdditionalPremises.equals(additionalPremises)
      } else {
        false
      }

      val suppliersChanged: Boolean = data.suppliers.fold(false){suppliers =>
        val suppliersLst = cached.getSuppliers.fold(List[Supplier]())(_.suppliers)

        val formAddressSupplier = suppliersLst match {
          case supplierList :: supplierLst => suppliersLst.zipWithIndex.map {
            case (supplier, index) =>
              supplier.copy(supplierAddress = removeCountry(supplier.supplierAddress))
            case _ => supplierList
          }
          case _ => List()
        }
        !suppliers.suppliers.equals(formAddressSupplier)
      }

      val declarationChanged: Boolean = if (data.applicationDeclaration.isDefined) {
        val appDeclaration = applicationDeclaration.map(appDec => appDec.copy(confirmation = None))
        !data.applicationDeclaration.equals(appDeclaration)
      } else {
          false
        }

      val coOfficialsChanged: Boolean = if (data.businessDirectors.isDefined) !data.businessDirectors.equals(businessDirectors) else false

      val tradingActivityChanged: Boolean = if (data.tradingActivity.isDefined) {
        val formTradingActivity = tradingActivity.map(
          addDetails => addDetails.copy(wholesalerType = addDetails.wholesalerType.sortAndFilterWith(AwrsFormFields.wholesaler),
            typeOfAlcoholOrders = addDetails.typeOfAlcoholOrders.sortAndFilterWith(AwrsFormFields.orders)))

        val savedTradingActivity = data.tradingActivity.map(addDetails => addDetails.copy(wholesalerType = addDetails.wholesalerType.sortAndFilterWith(AwrsFormFields.wholesaler),
          typeOfAlcoholOrders = addDetails.typeOfAlcoholOrders.sortAndFilterWith(AwrsFormFields.orders)))

        !formTradingActivity.equals(savedTradingActivity)
      } else {
          false
        }

      val productsChanged: Boolean = if (data.products.isDefined) {

        val formProducts = products.map(addDetails => addDetails.copy(mainCustomers = addDetails.mainCustomers.sortAndFilterWith(AwrsFormFields.mainCustomerOptions),
          productType = addDetails.productType.sortAndFilterWith(AwrsFormFields.products)))

        val savedProducts = data.products.map(addDetails => addDetails.copy(mainCustomers = addDetails.mainCustomers.sortAndFilterWith(AwrsFormFields.mainCustomerOptions),
          productType = addDetails.productType.sortAndFilterWith(AwrsFormFields.products)))

        !formProducts.equals(savedProducts)
      } else {
          false
        }

      val groupMemberDetailsChanged: Boolean = if (data.groupMembers.isDefined) !data.groupMembers.equals(groupMemberDetails) else false

      val changedIndicators = SectionChangeIndicators(businessDetailsChanged = changedBusinessDetailsIndicator._1,
        businessRegistrationDetailsChanged = changedBusinessDetailsIndicator._2,
        businessAddressChanged = changedBusinessDetailsIndicator._3,
        contactDetailsChanged = changedBusinessDetailsIndicator._4,
        tradingActivityChanged = tradingActivityChanged,
        productsChanged = productsChanged,
        partnersChanged = partnersChanged,
        coOfficialsChanged = coOfficialsChanged,
        premisesChanged = premisesChanged,
        suppliersChanged = suppliersChanged,
        groupMembersChanged = groupMemberDetailsChanged,
        declarationChanged = declarationChanged)

      // TEMP code from here >>>>>>  Also remove /amend route and the 'showBusinessType' flag from the BusinessTypeController
      val legalEntityChanged: Boolean = if (data.legalEntity.isDefined) !data.legalEntity.equals(legalEntity) else false

      val newChangeInds = legalEntityChanged match {
        case true => (data.legalEntity.get.legalEntity, legalEntity.get.legalEntity) match {
          case (Some("SOP"), Some("LLP" | "LP" | "Partnership")) => changedIndicators.copy(partnersChanged = true)
          case (Some("SOP"), Some("LTD")) => changedIndicators.copy(coOfficialsChanged = true)
          case (Some("SOP"), Some("LTD_GRP")) => changedIndicators.copy(groupMembersChanged = true, coOfficialsChanged = true)
          case (Some("SOP"), Some("LLP_GRP")) => changedIndicators.copy(groupMembersChanged = true, partnersChanged = true)
          case (Some("LTD"), Some("LLP" | "LP" | "Partnership")) => changedIndicators.copy(partnersChanged = true, coOfficialsChanged = true)
          case (Some("LTD"), Some("SOP")) => changedIndicators.copy(coOfficialsChanged = true)
          case (Some("LTD"), Some("LTD_GRP")) => changedIndicators.copy(groupMembersChanged = true)
          case (Some("LTD"), Some("LLP_GRP")) => changedIndicators.copy(groupMembersChanged = true, partnersChanged = true, coOfficialsChanged = true)
          case (Some("LLP" | "LP" | "Partnership"), Some("LTD")) => changedIndicators.copy(partnersChanged = true, coOfficialsChanged = true)
          case (Some("LLP" | "LP" | "Partnership"), Some("SOP")) => changedIndicators.copy(partnersChanged = true)
          case (Some("LLP" | "LP" | "Partnership"), Some("LTD_GRP")) => changedIndicators.copy(groupMembersChanged = true, partnersChanged = true, coOfficialsChanged = true)
          case (Some("LLP" | "LP" | "Partnership"), Some("LLP_GRP")) => changedIndicators.copy(groupMembersChanged = true, coOfficialsChanged = true)
          case (Some("LTD_GRP"), Some("LTD")) => changedIndicators.copy(groupMembersChanged = true)
          case (Some("LTD_GRP"), Some("SOP")) => changedIndicators.copy(groupMembersChanged = true, coOfficialsChanged = true)
          case (Some("LTD_GRP"), Some("LLP" | "LP" | "Partnership")) => changedIndicators.copy(groupMembersChanged = true, partnersChanged = true, coOfficialsChanged = true)
          case (Some("LTD_GRP"), Some("LLP_GRP")) => changedIndicators.copy(partnersChanged = true, coOfficialsChanged = true)
          case (Some("LLP_GRP"), Some("LTD")) => changedIndicators.copy(groupMembersChanged = true, partnersChanged = true, coOfficialsChanged = true)
          case (Some("LLP_GRP"), Some("SOP")) => changedIndicators.copy(groupMembersChanged = true, partnersChanged = true)
          case (Some("LLP_GRP"), Some("LLP" | "LP" | "Partnership")) => changedIndicators.copy(groupMembersChanged = true, coOfficialsChanged = true)
          case (Some("LLP_GRP"), Some("LTD_GRP")) => changedIndicators.copy(partnersChanged = true, coOfficialsChanged = true)
          case _ => changedIndicators
        }
        case false => changedIndicators
      }

      newChangeInds

    }

    changeIndicator
  }

}
