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

package services

import _root_.models._
import connectors.{AWRSConnector, AuthenticatorConnector}
import exceptions.{InvalidStateException, ResubmissionException}
import forms.AWRSEnums.BooleanRadioEnum
import forms.AwrsFormFields
import org.joda.time.LocalDate
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Request}
import services.helper._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.AccountUtils
import utils.CacheUtil.cacheUtil

import controllers.util.convertBCAddressToAddress

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse, InternalServerException }

trait ApplicationService extends AccountUtils with AwrsAPI5Helper with DataCacheService {

  override val save4LaterService: Save4LaterService
  override val keyStoreService: KeyStoreService
  val enrolService: EnrolService
  val awrsConnector: AWRSConnector
  val authenticatorConnector: AuthenticatorConnector
  val emailService: EmailService
  val maxSuppliers = 5

  // util method for trading activity and products, this method is introduced to remove the matching of the `all`
  // checkboxes from the lists (AWRS-1278 and 1279).
  // The original `all` options have been removed from the lists in AwrsFormFields, and this method uses
  // these lists as a filter on the user data so they can then be compared ignoring any changes to the `all` option
  implicit class TAndPUtil(selections: List[String]) {
    def sortAndFilterWith(formField: Seq[(String, String)]) =
      selections.filter(key => formField.map { case (formFieldKey, y) => formFieldKey }.contains(key)).sorted
  }

  def trimSuppliers(suppliers: Option[Suppliers]): Option[Suppliers] =
    suppliers match {
      case Some(sup) if sup.suppliers.size > maxSuppliers => Some(Suppliers(suppliers = sup.suppliers.slice(0, 5)))
      case _ => suppliers
    }

  def getSections(cacheID: String)(implicit user: AuthContext, hc: HeaderCarrier, ec: ExecutionContext) =
    save4LaterService.mainStore.fetchAll.map {
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
      businessDetails = cached.get.getBusinessDetails,
      businessRegistrationDetails = cached.get.getBusinessRegistrationDetails,
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

  private def isNewBusiness(cacheMap: Option[CacheMap]): Future[Boolean] = cacheMap match {
    case Some(cached) =>
      val isNewBusiness: Boolean = cached.getBusinessDetails match {
        case Some(BusinessDetails(_, _, Some(NewAWBusiness(BooleanRadioEnum.YesString, _)))) => true
        case _ => false
      }
      Future.successful(isNewBusiness)
    case None => Future.failed(new InternalServerException("No cache map found"))
  }

  def createGroupRep(cached: Option[CacheMap]): GroupMember = {
    val businessName = cached.get.getBusinessCustomerDetails.get.businessName
    val businessDetails = cached.get.getBusinessDetails.get
    val businessRegistrationDetails = cached.get.getBusinessRegistrationDetails.get
    val placeOfBusiness = cached.get.getPlaceOfBusiness.get

    GroupMember(CompanyNames(Some(businessName),businessDetails.doYouHaveTradingName,businessDetails.tradingName),
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

  def replaceGroupRepInGroupMembers(cached: Option[CacheMap]) : Option[GroupMembers] =
    Some(GroupMembers(cached.get.getGroupMembers.get.members.patch(0, Seq(createGroupRep(cached)), 1), GroupMembers.latestModelVersion))

  def isGrpRepChanged(cached: Option[CacheMap], cachedSubscription: Option[SubscriptionTypeFrontEnd]): Boolean = {
    cached.get.getBusinessType.get.legalEntity match {
      case Some("LTD_GRP") | Some("LLP_GRP") => cached.get.getBusinessCustomerDetails.get.businessName != cachedSubscription.get.businessPartnerName.get
      case _ => false
    }
  }

  def sendApplication()(implicit user: AuthContext, request: Request[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Future[SuccessfulSubscriptionResponse] = {
    for {
      cached <- save4LaterService.mainStore.fetchAll
      businessCustomerDetails = cached.get.getBusinessCustomerDetails
      sections <- getSections(AccountUtils.getUtrOrName())
      awrsData <- {
        val schema = assembleAWRSFEModel(cached, businessCustomerDetails, sections)
        awrsConnector.submitAWRSData(Json.toJson(schema))
      }
      isNewBusiness <- isNewBusiness(cached)
      _ <- emailService.sendConfirmationEmail(email = cached.get.getBusinessContacts.get.email.get, reference = awrsData.etmpFormBundleNumber, isNewBusiness = isNewBusiness)
    } yield awrsData
  }

  def hasAPI5ApplicationChanged(cacheID: String)(implicit user: AuthContext, hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    AccountUtils.hasAwrs match {
      case true => for {
        cached <- fetchMainStore
        cachedSubscription <- save4LaterService.api.fetchSubscriptionTypeFrontEnd
        hasAppChanged <- isApplicationDifferent(cached, cachedSubscription)
      } yield {
        hasAppChanged
      }
      case _ => Future.successful(false)
    }

  def updateApplication()(implicit user: AuthContext, request: Request[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Future[SuccessfulUpdateSubscriptionResponse] =
    for {
      cached <- save4LaterService.mainStore.fetchAll
      cachedSubscription <- save4LaterService.api.fetchSubscriptionTypeFrontEnd
      subscriptionStatus <- keyStoreService.fetchSubscriptionStatus
      _ <- if(isGrpRepChanged(cached,cachedSubscription)) callUpdateGroupBusinessPartner(cached, cachedSubscription, subscriptionStatus) else Future("OK")
      awrsData <- awrsConnector.updateAWRSData(Json.toJson(AWRSFEModel(getModifiedSubscriptionType(cached, cachedSubscription))))
      isNewBusiness <- isNewBusiness(cached)
      _ <- emailService.sendConfirmationEmail(email = cached.get.getBusinessContacts.get.email.get, reference = awrsData.etmpFormBundleNumber, isNewBusiness = isNewBusiness)
    } yield {
      awrsData
    }

  def callUpdateGroupBusinessPartner(cached: Option[CacheMap],
                                     cachedSubscription: Option[SubscriptionTypeFrontEnd],
                                     subscriptionStatus: Option[SubscriptionStatusType])
                                    (implicit user: AuthContext, request: Request[AnyContent], hc: HeaderCarrier, ec: ExecutionContext)
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
      case Some(businessCustomerAddress) => {
        awrsConnector.updateGroupBusinessPartner(
          cachedSubscription.get.businessPartnerName.get,
          cached.get.getBusinessType.get.legalEntity.get,
          subscriptionStatus.get.safeId.get,
          createUpdateRegistrationDetailsRequest(businessCustomerAddress))
      }
    }

  }

  def refreshProfile(implicit hc: HeaderCarrier): Future[HttpResponse] = authenticatorConnector.refreshProfile

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
    val groupMemberDetails = cached.get.getGroupMembers

    val newChangeInds = toChangeIndicators(getChangeIndicators(cached, cachedSubscription))
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
      businessDetails = cached.get.getBusinessDetails,
      businessRegistrationDetails = cached.get.getBusinessRegistrationDetails,
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

  def isApplicationDifferent(cached: Option[CacheMap], cachedSubscription: Option[SubscriptionTypeFrontEnd]): Future[Boolean] = {
    getChangeIndicators(cached, cachedSubscription) match {
      case Some(SectionChangeIndicators(false, false, false, false, false, false, false, false, false, false, false, false)) => Future.successful(false)
      case _ => Future.successful(true)
    }
  }

  def getApi5ChangeIndicators(cached: Option[CacheMap])(implicit user: AuthContext, hc: HeaderCarrier, ec: ExecutionContext): Future[SectionChangeIndicators] = {
    AccountUtils.hasAwrs match {
      case true =>
        for {
          cachedSubscription <- save4LaterService.api.fetchSubscriptionTypeFrontEnd
        } yield {
          getChangeIndicators(cached, cachedSubscription).fold(SectionChangeIndicators(false, false, false, false, false, false, false, false, false, false, false, false))(x => x)
        }
      case false => Future.successful(SectionChangeIndicators(false, false, false, false, false, false, false, false, false, false, false, false))
    }
  }

  def getChangeIndicators(cached: Option[CacheMap], cachedSubscription: Option[SubscriptionTypeFrontEnd]): Option[SectionChangeIndicators] = {
    val suppliers = cached.get.getSuppliers

    info(s" Cached Data from AWRS-FRONTEND Cache*** ")

    val additionalPremises = cached.get.getAdditionalBusinessPremises
    val partnership = cached.get.getPartners
    val tradingActivity = cached.get.getTradingActivity
    val products = cached.get.getProducts
    val applicationDeclaration = cached.get.getApplicationDeclaration
    val legalEntity = cached.get.getBusinessType
    val businessDirectors = cached.get.getBusinessDirectors
    val groupMemberDetails = cached.get.getGroupMembers

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
      } else false

      val suppliersChanged: Boolean = if (data.suppliers.isDefined) {
        val suppliersLst = suppliers.get.suppliers

        val formAddressSupplier = suppliersLst match {
          case supplierList :: supplierLst => suppliersLst.zipWithIndex.map {
            case (supplier, index) =>
              supplier.copy(supplierAddress = removeCountry(supplier.supplierAddress))
            case _ => supplierList
          }
          case _ => List()
        }
        !data.suppliers.get.suppliers.equals(formAddressSupplier)
      } else false

      val declarationChanged: Boolean = if (data.applicationDeclaration.isDefined) {
        val appDeclaration = applicationDeclaration.map(appDec => appDec.copy(confirmation = None))
        !data.applicationDeclaration.equals(appDeclaration)
      } else false

      val coOfficialsChanged: Boolean = if (data.businessDirectors.isDefined) !data.businessDirectors.equals(businessDirectors) else false

      val tradingActivityChanged: Boolean = if (data.tradingActivity.isDefined) {
        val formTradingActivity = tradingActivity.map(
          addDetails => addDetails.copy(wholesalerType = addDetails.wholesalerType.sortAndFilterWith(AwrsFormFields.wholesaler),
            typeOfAlcoholOrders = addDetails.typeOfAlcoholOrders.sortAndFilterWith(AwrsFormFields.orders)))

        val savedTradingActivity = data.tradingActivity.map(addDetails => addDetails.copy(wholesalerType = addDetails.wholesalerType.sortAndFilterWith(AwrsFormFields.wholesaler),
          typeOfAlcoholOrders = addDetails.typeOfAlcoholOrders.sortAndFilterWith(AwrsFormFields.orders)))

        !formTradingActivity.equals(savedTradingActivity)
      } else false

      val productsChanged: Boolean = if (data.products.isDefined) {

        val formProducts = products.map(addDetails => addDetails.copy(mainCustomers = addDetails.mainCustomers.sortAndFilterWith(AwrsFormFields.mainCustomerOptions),
          productType = addDetails.productType.sortAndFilterWith(AwrsFormFields.products)))

        val savedProducts = data.products.map(addDetails => addDetails.copy(mainCustomers = addDetails.mainCustomers.sortAndFilterWith(AwrsFormFields.mainCustomerOptions),
          productType = addDetails.productType.sortAndFilterWith(AwrsFormFields.products)))

        !formProducts.equals(savedProducts)
      } else false

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

      // to here <<<<<<<<<<<<<<<<<<

      newChangeInds

    }

    changeIndicator
  }

}

object ApplicationService extends ApplicationService {
  override val save4LaterService = Save4LaterService
  override val keyStoreService = KeyStoreService
  override val enrolService = EnrolService
  override val awrsConnector = AWRSConnector
  override val authenticatorConnector = AuthenticatorConnector
  override val emailService = EmailService
}
