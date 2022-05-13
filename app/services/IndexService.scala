/*
 * Copyright 2022 HM Revenue & Customs
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
import controllers.auth.StandardAuthRetrievals
import forms.AWRSEnums
import forms.AWRSEnums.BooleanRadioEnum
import javax.inject.Inject
import services.DataCacheKeys._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
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

    def getOrElseSize: Option[Int] = {
      option match {
        case Some(dataType) => Some(dataType match {
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
        })
        case None => None
      }
    } match {
      case Some(0) => None
      case x@_ => x
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
               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[IndexViewModel] = {

    val sectionStatus = foldOverOption[CacheMap, view_models.IndexStatus](SectionNotStarted) _
    def href(cm: Option[CacheMap], noDataUrl: String, block: CacheMap => String): String = cm.fold(noDataUrl)(block)

    applicationService.getApi5ChangeIndicators(cacheMap, authRetrievals) map { changeIndicators =>

        val businessDetailsStatus = sectionStatus(cacheMap, {cache =>
          (cache.getBusinessNameDetails, cache.getTradingStartDetails) match {
            case (Some(bnd), Some(gtsd)) => if (changeIndicators.businessDetailsChanged) {
              SectionEdited
            } else {
              if (bnd.doYouHaveTradingName.isDefined && isNewAWBusinessAnswered(gtsd.invertedBeforeMarch2016Question)) SectionComplete else SectionIncomplete
            }
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

        val businessDetailsHref = href(cacheMap, controllers.routes.TradingNameController.showTradingName(true).url, cache =>
          (cache.getBusinessNameDetails, cache.getTradingStartDetails) match {
            case (Some(_), Some(td)) => controllers.routes.ViewApplicationController.viewSection(businessDetailsName).url
            case _ => controllers.routes.TradingNameController.showTradingName(true).url
          }
        )

        val businessRegistrationDetailsHref = href(cacheMap, controllers.routes.BusinessRegistrationDetailsController.showBusinessRegistrationDetails(true).url, cache =>
          (cache.getBusinessRegistrationDetails, isLegalEntityNone(cache.getBusinessRegistrationDetails)) match {
            case (Some(_), false) => controllers.routes.ViewApplicationController.viewSection(businessRegistrationDetailsName).url
            case (_, _) => controllers.routes.BusinessRegistrationDetailsController.showBusinessRegistrationDetails(true).url
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
            case (Some(placeOfBusiness), false) =>
              if (changeIndicators.businessAddressChanged) SectionEdited
              else if (placeOfBusiness.operatingDuration.exists(_.isEmpty) || !placeOfBusiness.mainAddress.fold(false)(_.postcode.exists(_.nonEmpty)))
                SectionIncomplete else SectionComplete
            case (_, _) => SectionNotStarted
          }
        )

        val businessContactsHref = href(cacheMap, controllers.routes.BusinessContactsController.showBusinessContacts(true).url, cache =>
          (cache.getBusinessContacts, isContactFirstNameNone(cache.getBusinessContacts)) match {
            case (Some(_), false) => controllers.routes.ViewApplicationController.viewSection(businessContactsName).url
            case (_, _) => controllers.routes.BusinessContactsController.showBusinessContacts(true).url
          }
        )

        val placeOfBusinessHref = href(cacheMap, controllers.routes.PlaceOfBusinessController.showPlaceOfBusiness(true).url, cache =>
          (cache.getPlaceOfBusiness, isMainPlaceOfBusinessNone(cache.getPlaceOfBusiness)) match {
            case (Some(_), false) => controllers.routes.ViewApplicationController.viewSection(placeOfBusinessName).url
            case (_, _) => controllers.routes.PlaceOfBusinessController.showPlaceOfBusiness(true).url
          }
        )

        val additionalBusinessPremisesStatus = sectionStatus(cacheMap,
          _.getAdditionalBusinessPremises.fold[view_models.IndexStatus](SectionNotStarted){_ =>
            if (changeIndicators.premisesChanged) SectionEdited else SectionComplete
          }
        )

        val additionalBusinessPremisesHref = href(cacheMap, controllers.routes.AdditionalPremisesController.showPremisePage(1, true, true).url,
          _.getAdditionalBusinessPremises.fold(controllers.routes.AdditionalPremisesController.showPremisePage(1, true, true).url){_ =>
            controllers.routes.ViewApplicationController.viewSection(additionalBusinessPremisesName).url
          }
        )

        val businessDirectorsHref = href(cacheMap, controllers.routes.BusinessDirectorsController.showBusinessDirectors(isLinearMode = true, isNewRecord = true).url,
          _.getBusinessDirectors.fold(controllers.routes.BusinessDirectorsController.showBusinessDirectors(isLinearMode = true, isNewRecord = true).url){_ =>
            controllers.routes.ViewApplicationController.viewSection(businessDirectorsName).url
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
          _.getTradingActivity.fold[view_models.IndexStatus](SectionNotStarted)(_ => if (changeIndicators.tradingActivityChanged) SectionEdited else SectionComplete)
        )

        val tradingActivityHref = href(cacheMap, controllers.routes.TradingActivityController.showTradingActivity(isLinearMode = true).url,
          _.getTradingActivity.fold(controllers.routes.TradingActivityController.showTradingActivity(isLinearMode = true).url){_ =>
            controllers.routes.ViewApplicationController.viewSection(tradingActivityName).url
          }
        )

        val productsHref = href(cacheMap, controllers.routes.ProductsController.showProducts(isLinearMode = true).url,
          _.getProducts.fold(controllers.routes.ProductsController.showProducts(isLinearMode = true).url){_=>
            controllers.routes.ViewApplicationController.viewSection(productsName).url
          }
        )

        val productsStatus = sectionStatus(cacheMap,
          _.getProducts.fold[view_models.IndexStatus](SectionNotStarted)(_ => if (changeIndicators.productsChanged) SectionEdited else SectionComplete)
        )

        val suppliersStatus = sectionStatus(cacheMap,
          _.getSuppliers.fold[view_models.IndexStatus](SectionNotStarted)(_ => if (changeIndicators.suppliersChanged) SectionEdited else SectionComplete)
        )

        val suppliersHref = href(cacheMap, controllers.routes.SupplierAddressesController.showSupplierAddressesPage(1, true, true).url,
          _.getSuppliers.fold(controllers.routes.SupplierAddressesController.showSupplierAddressesPage(1, true, true).url){_ =>
            controllers.routes.ViewApplicationController.viewSection(suppliersName).url
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

        val businessPartnersHref = href(cacheMap, controllers.routes.BusinessPartnersController.showPartnerMemberDetails(1, true, true).url,
          _.getPartners.fold(controllers.routes.BusinessPartnersController.showPartnerMemberDetails(1, true, true).url)(_ =>
            controllers.routes.ViewApplicationController.viewSection(partnersName).url
          )
        )

        val groupMembersHref = href(cacheMap, controllers.routes.GroupMemberController.showMemberDetails(1, true, true).url,
          _.getGroupMembers.fold(controllers.routes.GroupMemberController.showMemberDetails(1, true, true).url)(_ =>
            controllers.routes.ViewApplicationController.viewSection(groupMembersName).url
          )
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
          JourneyConstants.getJourney(businessType) map {
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
        }.toList)
    }

  }

  def displayCompleteForBusinessType(businessRegistrationDetails: BusinessRegistrationDetails, legalEntity: Option[String]): Boolean =
    legalEntity.fold(false)(legal => businessRegistrationDetails.legalEntity.fold(false)(breLegal => isSameEntityIdCategory(breLegal, legal)))

  def isNewAWBusinessAnswered(businessDetails: NewAWBusiness): Boolean =
      businessDetails.newAWBusiness match {
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
