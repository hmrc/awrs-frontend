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
import caching.CacheMap
import play.api.mvc.Call
import controllers.auth.StandardAuthRetrievals
import forms.AWRSEnums
import forms.AWRSEnums.BooleanRadioEnum

import javax.inject.Inject
import services.DataCacheKeys._
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AccountUtils, CacheUtil}
import view_models._

import scala.concurrent.{ExecutionContext, Future}

class IndexService @Inject()(dataCacheService: Save4LaterService,
                             applicationService: ApplicationService,
                             accountUtils: AccountUtils) {
  implicit val cacheUtil: CacheMap => CacheUtil.CacheHelper = CacheUtil.cacheUtil

  implicit class OptionUtil[T](option: Option[T]) {
    // internal function used to determine if suppliers or premises has any entries or if the user has chosen "No" when
    // they answered
    // haveAny needs to be a call by name function so that it is not evaluated on input. otherwise if the list is empty
    // it will cause erroneous behaviours
    private def isAnswered(size: Int, haveAny: => Option[String]) =
      size match {
        case 1 => haveAny match {
          case Some(BooleanRadioEnum.YesString) => 1
          case _ => 0 // this will be transformed into None by the match at the end of this function
        }
        case size@_ => size
      }

    def getOrElseSize: Option[Int] = option.map{
      case d: BusinessDirectors => d.directors.size
      case g: GroupMembers => g.members.size
      case a: AdditionalBusinessPremisesList =>
        // for additional premises if they answered no to having any additional premises then we also do not
        // want to display (0)
        val p = a.premises
        isAnswered(p.size, p.head.additionalPremises)
      case s: Suppliers =>
        // for suppliers if they answered no to having any suppliers then we also do not want to display (0)
        val sup = s.suppliers
        isAnswered(sup.size, sup.head.alcoholSuppliers)
      case p: Partners => p.partners.size
      case _ => throw new RuntimeException("unsupported type for getOrElseSize")
    }.flatMap{
      case 0 => None
      case x => Some(x)
    }

  }

  def showContinueButton(indexViewModel: IndexViewModel): Boolean = {
    val listWithoutEdited = indexViewModel.sectionModels.filterNot(_.status.equals(SectionEdited))
    listWithoutEdited.forall(_.status.equals(SectionComplete))
  }

  def showOneViewLink(indexViewModel: IndexViewModel): Boolean =
    !indexViewModel.sectionModels.forall(_.status.equals(SectionNotStarted))


  def isLegalEntityNone(cache: Option[BusinessRegistrationDetails]): Boolean = cache.fold(false)(_.legalEntity.isEmpty)

  def isMainPlaceOfBusinessNone(cache: Option[PlaceOfBusiness]): Boolean = cache.fold(false)(_.mainPlaceOfBusiness.isEmpty)

  def isContactFirstNameNone(cache: Option[BusinessContacts]): Boolean = cache.fold(false)(_.contactFirstName.isEmpty)

  def getStatus(cacheMap: Option[CacheMap], businessType: String, authRetrievals: StandardAuthRetrievals)
               (implicit ec: ExecutionContext): Future[IndexViewModel] = {

    val sectionStatus = foldOverOptionWithDefault[CacheMap, view_models.IndexStatus](SectionNotStarted) _
    def sectionHref(cm: Option[CacheMap], noData: Call, block: CacheMap => Call): String = (cm.fold(noData)(block)).url

    import controllers.routes._

    applicationService.getApi5ChangeIndicators(cacheMap, authRetrievals) map { changeIndicators =>

        val businessDetailsStatus = sectionStatus(cacheMap, {cache =>
          (cache.getBusinessNameDetails, cache.getTradingStartDetails) match {
            case (Some(bnd), Some(gtsd)) => if (changeIndicators.businessDetailsChanged && gtsd.proposedStartDate.isDefined) {
              SectionEdited
            } else {
              if (bnd.doYouHaveTradingName.isDefined && isNewAWBusinessAnswered(gtsd.invertedBeforeMarch2016Question) && gtsd.proposedStartDate.isDefined) SectionComplete else SectionIncomplete
            }
            case (Some(bnd), None) if bnd.doYouHaveTradingName.isDefined => SectionIncomplete
            case _ => SectionNotStarted
          }
        })

        val businessRegistrationDetailsStatus = sectionStatus(cacheMap, {cache =>
          (cache.getBusinessRegistrationDetails, isLegalEntityNone(cache.getBusinessRegistrationDetails)) match {
            case (Some(businessRegistrationDetails), false) => if (changeIndicators.businessRegistrationDetailsChanged) {
              SectionEdited
            } else {
              if (displayCompleteForBusinessType(businessRegistrationDetails, Some(businessType))) SectionComplete else SectionIncomplete
            }
            case (_, _) => SectionNotStarted
          }
        })

        val businessDetailsHref = sectionHref(cacheMap, TradingNameController.showTradingName(true), cache =>
          (cache.getBusinessNameDetails, cache.getTradingStartDetails) match {
            case (Some(_), Some(td)) => ViewApplicationController.viewSection(businessDetailsName)
            case _ => TradingNameController.showTradingName(true)
          }
        )

        val businessRegistrationDetailsHref = sectionHref(cacheMap, BusinessRegistrationDetailsController.showBusinessRegistrationDetails(true), cache =>
          (cache.getBusinessRegistrationDetails, isLegalEntityNone(cache.getBusinessRegistrationDetails)) match {
            case (Some(_), false) => ViewApplicationController.viewSection(businessRegistrationDetailsName)
            case (_, _) => BusinessRegistrationDetailsController.showBusinessRegistrationDetails(true)
          }
        )

        val businessContactsStatus = sectionStatus(cacheMap, cache =>
          (cache.getBusinessContacts, isContactFirstNameNone(cache.getBusinessContacts)) match {
            case (Some(_), false) => if (changeIndicators.contactDetailsChanged) SectionEdited else SectionComplete
            case (_, _) => SectionNotStarted
          }
        )

        val placeOfBusinessStatus = sectionStatus(cacheMap, cache =>
          (cache.getPlaceOfBusiness, isMainPlaceOfBusinessNone(cache.getPlaceOfBusiness)) match {
            case (Some(placeOfBusiness), false) => if (changeIndicators.businessAddressChanged) SectionEdited
              else if (existsAndHasNoValue(placeOfBusiness.operatingDuration) ||
                       !placeOfBusiness.mainAddress.fold(false)(addr => existsAndHasValue(addr.postcode))) SectionIncomplete else SectionComplete
            case (_, _) => SectionNotStarted
          }
        )

        val businessContactsHref = sectionHref(cacheMap, BusinessContactsController.showBusinessContacts(true), cache =>
          (cache.getBusinessContacts, isContactFirstNameNone(cache.getBusinessContacts)) match {
            case (Some(_), false) => ViewApplicationController.viewSection(businessContactsName)
            case (_, _) => BusinessContactsController.showBusinessContacts(true)
          }
        )

        val placeOfBusinessHref = sectionHref(cacheMap, PlaceOfBusinessController.showPlaceOfBusiness(true), cache =>
          (cache.getPlaceOfBusiness, isMainPlaceOfBusinessNone(cache.getPlaceOfBusiness)) match {
            case (Some(_), false) => ViewApplicationController.viewSection(placeOfBusinessName)
            case (_, _) => PlaceOfBusinessController.showPlaceOfBusiness(true)
          }
        )

        val additionalBusinessPremisesStatus = sectionStatus(cacheMap,
          _.getAdditionalBusinessPremises.fold[view_models.IndexStatus](SectionNotStarted){_ =>
            if (changeIndicators.premisesChanged) SectionEdited else SectionComplete
          }
        )

        val additionalBusinessPremisesHref = sectionHref(cacheMap, AdditionalPremisesController.showPremisePage(1, true, true),
          _.getAdditionalBusinessPremises.fold(AdditionalPremisesController.showPremisePage(1, true, true)){_ =>
            ViewApplicationController.viewSection(additionalBusinessPremisesName)
          }
        )

        val businessDirectorsHref = sectionHref(cacheMap, BusinessDirectorsController.showBusinessDirectors(isLinearMode = true, isNewRecord = true),
          _.getBusinessDirectors.fold(BusinessDirectorsController.showBusinessDirectors(isLinearMode = true, isNewRecord = true)){_ =>
            ViewApplicationController.viewSection(businessDirectorsName)
          }
        )

        val businessDirectorsStatus = sectionStatus(cacheMap,
          _.getBusinessDirectors.fold[view_models.IndexStatus](SectionNotStarted){
            _.directors match {
              // need at least 1 director
              // this condition can only be reached if they have entered some directors and then deleted them
              case Nil => SectionIncomplete
              case _ => if (changeIndicators.coOfficialsChanged) SectionEdited else SectionComplete
            }
          }
        )

        val tradingActivityStatus = sectionStatus(cacheMap,
          _.getTradingActivity.fold[view_models.IndexStatus](SectionNotStarted)(_ =>
            if (changeIndicators.tradingActivityChanged) SectionEdited else SectionComplete)
        )

        val tradingActivityHref = sectionHref(cacheMap, TradingActivityController.showTradingActivity(isLinearMode = true),
          _.getTradingActivity.fold(TradingActivityController.showTradingActivity(isLinearMode = true)){_ =>
            ViewApplicationController.viewSection(tradingActivityName)
          }
        )

        val productsHref = sectionHref(cacheMap, ProductsController.showProducts(isLinearMode = true),
          _.getProducts.fold(ProductsController.showProducts(isLinearMode = true)){_=>
            ViewApplicationController.viewSection(productsName)
          }
        )

        val productsStatus = sectionStatus(cacheMap,
          _.getProducts.fold[view_models.IndexStatus](SectionNotStarted)(_ => if (changeIndicators.productsChanged) SectionEdited else SectionComplete)
        )

        val suppliersStatus = sectionStatus(cacheMap,
          _.getSuppliers.fold[view_models.IndexStatus](SectionNotStarted)(_ => if (changeIndicators.suppliersChanged) SectionEdited else SectionComplete)
        )

        val suppliersHref = sectionHref(cacheMap, SupplierAddressesController.showSupplierAddressesPage(1, true, true),
          _.getSuppliers.fold(SupplierAddressesController.showSupplierAddressesPage(1, true, true)){_ =>
            ViewApplicationController.viewSection(suppliersName)
          }
        )

        val businessPartnersStatus = sectionStatus(cacheMap,
          _.getPartners.fold[view_models.IndexStatus](SectionNotStarted){partnerDetails =>
            // Defensive coding for old LLP applications that didn't used to have partners on them
            // must have at least 2 partners
            if (partnerDetails.partners.size > 1){
              if (changeIndicators.partnersChanged) SectionEdited else SectionComplete
            } else SectionIncomplete
          }
        )

        val groupMembersStatus = sectionStatus(cacheMap,
          _.getGroupMembers.fold[view_models.IndexStatus](SectionNotStarted){ gm =>
            if (gm.members.nonEmpty) {if (changeIndicators.groupMembersChanged) SectionEdited else SectionComplete} else SectionIncomplete
          }
        )

        val businessPartnersHref = sectionHref(cacheMap, BusinessPartnersController.showPartnerMemberDetails(1, true, true),
          _.getPartners.fold(BusinessPartnersController.showPartnerMemberDetails(1, true, true))(_ =>
            ViewApplicationController.viewSection(partnersName)
          )
        )

        val groupMembersHref = sectionHref(cacheMap, GroupMemberController.showMemberDetails(1, true, true),
          _.getGroupMembers.fold(GroupMemberController.showMemberDetails(1, true, true))(_ => ViewApplicationController.viewSection(groupMembersName))
        )

        val (id, busTypeInMsg): (String, String) = businessType match {
          case "SOP" | "LTD" => ("business", "business")
          case "Partnership" | "LLP" | "LP" => ("partnership", "partnership")
          case "LTD_GRP" | "LLP_GRP" => ("groupBusiness", "group_business")
        }

        val sectionMessage = (page: String) => f"awrs.index_page.${busTypeInMsg}_${page}_text"

        val businessDetailsSection =
          SectionModel(id + "Details", businessDetailsHref, sectionMessage("details"), businessDetailsStatus)

        val businessRegistrationDetailsSection =
          SectionModel(id + "RegistrationDetails", businessRegistrationDetailsHref, sectionMessage("registration_details"), businessRegistrationDetailsStatus)

        val businessContactsSection =
          SectionModel(id + "Contacts", businessContactsHref, sectionMessage("contacts"), businessContactsStatus)

        val placeOfBusinessSection =
          SectionModel(id + "PlaceOfBusiness", placeOfBusinessHref, sectionMessage("place_of_business"), placeOfBusinessStatus)

        val additionalPremisesSection = SectionModel(
          "additionalPremises", additionalBusinessPremisesHref, "awrs.index_page.additional_premises_text", additionalBusinessPremisesStatus,
          size = cacheMap.fold[Option[Int]](None)(_.getAdditionalBusinessPremises.getOrElseSize)
        )
        val businessDirectorsSection = SectionModel(
          "directorsAndCompanySecretaries", businessDirectorsHref, "awrs.index_page.business_directors.index_text", businessDirectorsStatus,
          size = cacheMap.fold[Option[Int]](None)(_.getBusinessDirectors.getOrElseSize)
        )
        val tradingActivity = SectionModel(
          "tradingActivity", tradingActivityHref, "awrs.index_page.trading_activity_text", tradingActivityStatus
        )
        val products = SectionModel(
          "products", productsHref, "awrs.index_page.products_text", productsStatus
        )
        val suppliersSection = SectionModel(
          "aboutYourSuppliers", suppliersHref, "awrs.index_page.suppliers_text", suppliersStatus,
          size = cacheMap.fold[Option[Int]](None)(_.getSuppliers.getOrElseSize)
        )
        val businessPartnersSection = SectionModel(
          "businessPartners", businessPartnersHref, "awrs.index_page.business_partners_text", businessPartnersStatus,
          size = cacheMap.fold[Option[Int]](None)(_.getPartners.getOrElseSize)
        )
        val groupMemberDetailsSection = SectionModel(
          "groupMembers", groupMembersHref, "awrs.index_page.group_member_details_text", groupMembersStatus,
          size = cacheMap.fold[Option[Int]](None)(_.getGroupMembers.getOrElseSize) match {
            case Some(x) if accountUtils.hasAwrs(authRetrievals.enrolments) => Some(x - 1)
            case Some(x) => Some(x)
            case _ => Some(0)
          }
        )

        IndexViewModel({
          JourneyConstants.getJourney(businessType) map { x =>
            (x: @unchecked) match {
              case `businessDetailsName` => businessDetailsSection
              case `businessRegistrationDetailsName` => businessRegistrationDetailsSection
              case `businessContactsName` => businessContactsSection
              case `placeOfBusinessName` => placeOfBusinessSection
              case `groupMembersName` => groupMemberDetailsSection
              case `partnersName` => businessPartnersSection
              case `additionalBusinessPremisesName` => additionalPremisesSection
              case `businessDirectorsName` => businessDirectorsSection
              case `tradingActivityName` => tradingActivity
              case `productsName` => products
              case `suppliersName` => suppliersSection
            }
          }
        }.toList)
    }

  }

  def displayCompleteForBusinessType(businessRegistrationDetails: BusinessRegistrationDetails, legalEntity: Option[String]): Boolean =
    legalEntity.fold(false)(legal => businessRegistrationDetails.legalEntity.fold(false)(breLegal => isSameEntityIdCategory(breLegal, legal)))

  def isNewAWBusinessAnswered(businessDetails: NewAWBusiness): Boolean =
    (businessDetails.newAWBusiness : @unchecked) match {
        case AWRSEnums.BooleanRadioEnum.NoString => true
        // This is amended to also check the proposed date is present due to a bug by etmp as specified by AWRS-1413
        case AWRSEnums.BooleanRadioEnum.YesString =>
          businessDetails.proposedStartDate match {
            case Some(_) => true
            case _ => false
          }
      }

  // SOP and Partnership have their own combination of Identities required, all the rest are the same
  def isSameEntityIdCategory(previousLegalEntity: String, newLegalEntity: String): Boolean = previousLegalEntity match {
    case "SOP" | "Partnership" => previousLegalEntity.equalsIgnoreCase(newLegalEntity)
    case _ => !newLegalEntity.equalsIgnoreCase("SOP") && !newLegalEntity.equalsIgnoreCase("Partnership")
  }

}
