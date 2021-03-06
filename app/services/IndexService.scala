/*
 * Copyright 2021 HM Revenue & Customs
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


  def isLegalEntityNone(cache: Option[BusinessRegistrationDetails]): Boolean = {
    try {
      cache.get.legalEntity.isEmpty
    }
    catch {
      case _: Throwable =>
        false
    }
  }

  def isMainPlaceOfBusinessNone(cache: Option[PlaceOfBusiness]): Boolean = {
    try {
      cache.get.mainPlaceOfBusiness.isEmpty
    }
    catch {
      case _: Throwable =>
        false
    }
  }

  def isContactFirstNameNone(cache: Option[BusinessContacts]): Boolean = {
    try {
      cache.get.contactFirstName.isEmpty
    }
    catch {
      case _: Throwable =>
        false
    }
  }


  def getStatus(cacheMap: Option[CacheMap], businessType: String, authRetrievals: StandardAuthRetrievals)
               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[IndexViewModel] = {

    applicationService.getApi5ChangeIndicators(cacheMap, authRetrievals) map {
      changeIndicators =>
        val someBusinessType = Some(businessType)
        val cache = cacheMap.get

        val businessDetailsStatus = cacheMap match {
          case Some(cache) => (cache.getBusinessNameDetails, cache.getTradingStartDetails) match {
            case (Some(bnd), Some(gtsd)) => if (changeIndicators.businessDetailsChanged) {
              SectionEdited
            } else {
              if (bnd.doYouHaveTradingName.isDefined && isNewAWBusinessAnswered(gtsd.invertedBeforeMarch2016Question)) {
                SectionComplete
              } else {
                SectionIncomplete
              }
            }
            case _ => SectionNotStarted
          }
          case _ => SectionNotStarted
        }

        val businessRegistrationDetailsStatus = cacheMap match {
          case Some(cache) => (cache.getBusinessRegistrationDetails, isLegalEntityNone(cache.getBusinessRegistrationDetails)) match {
            case (Some(businessRegistrationDetails), false) => if (changeIndicators.businessRegistrationDetailsChanged) {
              SectionEdited
            } else {
              if (displayCompleteForBusinessType(businessRegistrationDetails, someBusinessType)) {
                SectionComplete
              } else {
                SectionIncomplete
              }
            }
            case (_, _) => SectionNotStarted
          }
          case _ => SectionNotStarted
        }

        val businessDetailsHref = cacheMap match {
          case Some(cache) => (cache.getBusinessNameDetails, cache.getTradingStartDetails) match {
            case (Some(bnd), Some(td)) =>
              controllers.routes.ViewApplicationController.viewSection(businessDetailsName).url
            case _ => controllers.routes.TradingNameController.showTradingName(true).url
          }
          case _ => controllers.routes.TradingNameController.showTradingName(true).url
        }

        val businessRegistrationDetailsHref = cacheMap match {
          case Some(cache) => (cache.getBusinessRegistrationDetails, isLegalEntityNone(cache.getBusinessRegistrationDetails)) match {
            case (Some(_), false) => controllers.routes.ViewApplicationController.viewSection(businessRegistrationDetailsName).url
            case (_, _) => controllers.routes.BusinessRegistrationDetailsController.showBusinessRegistrationDetails(true).url
          }
          case _ => controllers.routes.BusinessRegistrationDetailsController.showBusinessRegistrationDetails(true).url
        }

        val businessContactsStatus = cacheMap match {
          case Some(cache) => (cache.getBusinessContacts, isContactFirstNameNone(cache.getBusinessContacts)) match {
            case (Some(businessContacts), false) => changeIndicators.contactDetailsChanged match {
              case true => SectionEdited
              case false => SectionComplete
            }
            case (_, _) => SectionNotStarted
          }
          case _ => SectionNotStarted
        }

        val placeOfBusinessStatus = cacheMap match {
          case Some(cache) => (cache.getPlaceOfBusiness, isMainPlaceOfBusinessNone(cache.getPlaceOfBusiness)) match {
            case (Some(placeOfBusiness), false) => changeIndicators.businessAddressChanged match {
              case false => placeOfBusiness.operatingDuration.exists(_.isEmpty) || !placeOfBusiness.mainAddress.get.postcode.exists(_.nonEmpty) match {
                case true => SectionIncomplete
                case _ => SectionComplete
              }
              case true => SectionEdited
            }
            case (_, _) => SectionNotStarted
          }
          case _ => SectionNotStarted
        }

        val businessContactsHref = cacheMap match {
          case Some(cache) => (cache.getBusinessContacts, isContactFirstNameNone(cache.getBusinessContacts)) match {
            case (Some(_), false) => controllers.routes.ViewApplicationController.viewSection(businessContactsName).url
            case (_, _) => controllers.routes.BusinessContactsController.showBusinessContacts(true).url
          }
          case _ => controllers.routes.BusinessContactsController.showBusinessContacts(true).url
        }

        val placeOfBusinessHref = cacheMap match {
          case Some(cache) => (cache.getPlaceOfBusiness, isMainPlaceOfBusinessNone(cache.getPlaceOfBusiness)) match {
            case (Some(_), false) => controllers.routes.ViewApplicationController.viewSection(placeOfBusinessName).url
            case (_, _) => controllers.routes.PlaceOfBusinessController.showPlaceOfBusiness(true).url
          }
          case _ => controllers.routes.PlaceOfBusinessController.showPlaceOfBusiness(true).url
        }

        val additionalBusinessPremisesStatus = cacheMap match {
          case Some(cache) => cache.getAdditionalBusinessPremises.isDefined match {
            case true => changeIndicators.premisesChanged match {
              case false => SectionComplete
              case true => SectionEdited
            }
            case _ => SectionNotStarted
          }
          case _ => SectionNotStarted
        }

        val additionalBusinessPremisesHref = cacheMap match {
          case Some(cache) => cache.getAdditionalBusinessPremises.isDefined match {
            case true => controllers.routes.ViewApplicationController.viewSection(additionalBusinessPremisesName).url
            case false => controllers.routes.AdditionalPremisesController.showPremisePage(id = 1, isLinearMode = true, isNewRecord = true).url
          }
          case _ => controllers.routes.AdditionalPremisesController.showPremisePage(id = 1, isLinearMode = true, isNewRecord = true).url
        }

        val businessDirectorsHref = cacheMap match {
          case Some(cache) => cache.getBusinessDirectors.isDefined match {
            case true => controllers.routes.ViewApplicationController.viewSection(businessDirectorsName).url
            case false => controllers.routes.BusinessDirectorsController.showBusinessDirectors(isLinearMode = true, isNewRecord = true).url
          }
          case _ => controllers.routes.BusinessDirectorsController.showBusinessDirectors(isLinearMode = true, isNewRecord = true).url
        }

        val businessDirectorsStatus = cacheMap match {
          case Some(cache) => cache.getBusinessDirectors match {
            case Some(directors) =>
              directors.directors match {
                // need at least 1 director
                // this condition can only be reached if they have entered some directors and then deleted them
                case Nil => SectionIncomplete
                case _ =>
                  changeIndicators.coOfficialsChanged match {
                    case true => SectionEdited
                    case _ => SectionComplete
                  }
              }
            case None => SectionNotStarted
          }
          case _ => SectionNotStarted
        }

        val tradingActivityStatus = cacheMap match {
          case Some(cache) => cache.getTradingActivity match {
            case Some(_) => changeIndicators.tradingActivityChanged match {
              case false => SectionComplete
              case true => SectionEdited
            }
            case _ => SectionNotStarted
          }
          case _ => SectionNotStarted
        }

        val tradingActivityHref = cacheMap match {
          case Some(cache) => cache.getTradingActivity match {
            case Some(_) => controllers.routes.ViewApplicationController.viewSection(tradingActivityName).url
            case _ => controllers.routes.TradingActivityController.showTradingActivity(isLinearMode = true).url
          }
          case _ => controllers.routes.TradingActivityController.showTradingActivity(isLinearMode = true).url
        }

        val productsHref = cacheMap match {
          case Some(cache) => cache.getProducts match {
            case Some(_) => controllers.routes.ViewApplicationController.viewSection(productsName).url
            case _ => controllers.routes.ProductsController.showProducts(isLinearMode = true).url
          }
          case _ => controllers.routes.ProductsController.showProducts(isLinearMode = true).url
        }

        val productsStatus = cacheMap match {
          case Some(cache) => cache.getProducts match {
            case Some(_) => changeIndicators.productsChanged match {
              case false => SectionComplete
              case true => SectionEdited
            }
            case _ => SectionNotStarted
          }
          case _ => SectionNotStarted
        }

        val suppliersStatus = cacheMap match {
          case Some(cache) => cache.getSuppliers match {
            case Some(_) => changeIndicators.suppliersChanged match {
              case false => SectionComplete
              case true => SectionEdited
            }
            case _ => SectionNotStarted
          }
          case _ => SectionNotStarted
        }

        val suppliersHref = cacheMap match {
          case Some(cache) => cache.getSuppliers match {
            case Some(_) => controllers.routes.ViewApplicationController.viewSection(suppliersName).url
            case _ => controllers.routes.SupplierAddressesController.showSupplierAddressesPage(id = 1, isLinearMode = true, isNewRecord = true).url
          }
          case _ => controllers.routes.SupplierAddressesController.showSupplierAddressesPage(id = 1, isLinearMode = true, isNewRecord = true).url
        }

        val businessPartnersStatus = cacheMap match {
          case Some(cache) => cache.getPartners match {
            // Defensive coding for old LLP applications that didn't used to have partners on them
            // must have at least 2 partners
            case Some(partnerDetails) if partnerDetails.partners.size > 1 =>
              changeIndicators.partnersChanged match {
                case true => SectionEdited
                case _ => SectionComplete
              }
            case Some(_) => SectionIncomplete
            case None => SectionNotStarted
          }
          case _ => SectionNotStarted
        }

        val groupMembersStatus = cacheMap match {
          case Some(cache) => cache.getGroupMembers.isDefined match {
            // must have at least 1 entry
            case true => cache.getGroupMembers.get.members.nonEmpty match {
              case true => changeIndicators.groupMembersChanged match {
                case true => SectionEdited
                case _ => SectionComplete
              }
              case false => SectionIncomplete
            }
            case _ => SectionNotStarted
          }
          case _ => SectionNotStarted
        }

        val businessPartnersHref = cacheMap match {
          case Some(cache) => cache.getPartners match {
            case Some(_) => controllers.routes.ViewApplicationController.viewSection(partnersName).url
            case _ => controllers.routes.BusinessPartnersController.showPartnerMemberDetails(id = 1, isLinearMode = true, isNewRecord = true).url
          }
          case _ => controllers.routes.BusinessPartnersController.showPartnerMemberDetails(id = 1, isLinearMode = true, isNewRecord = true).url
        }

        val groupMembersHref = cacheMap match {
          case Some(cache) => cache.getGroupMembers match {
            case Some(_) => controllers.routes.ViewApplicationController.viewSection(groupMembersName).url
            case _ => controllers.routes.GroupMemberController.showMemberDetails(id = 1, isLinearMode = true, isNewRecord = true).url
          }
          case _ => controllers.routes.GroupMemberController.showMemberDetails(id = 1, isLinearMode = true, isNewRecord = true).url
        }

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
          size = cache.getAdditionalBusinessPremises.getOrElseSize
        )
        val businessDirectorsSection = SectionModel(
          "directorsAndCompanySecretaries", businessDirectorsHref, "awrs.index_page.business_directors.index_text", businessDirectorsStatus,
          size = cache.getBusinessDirectors.getOrElseSize
        )
        val tradingActivity = SectionModel(
          "tradingActivity", tradingActivityHref, "awrs.index_page.trading_activity_text", tradingActivityStatus
        )
        val products = SectionModel(
          "products", productsHref, "awrs.index_page.products_text", productsStatus
        )
        val suppliersSection = SectionModel(
          "aboutYourSuppliers", suppliersHref, "awrs.index_page.suppliers_text", suppliersStatus,
          size = cache.getSuppliers.getOrElseSize
        )
        val businessPartnersSection = SectionModel(
          "businessPartners", businessPartnersHref, "awrs.index_page.business_partners_text", businessPartnersStatus,
          size = cache.getPartners.getOrElseSize
        )
        val groupMemberDetailsSection = SectionModel(
          "groupMembers", groupMembersHref, "awrs.index_page.group_member_details_text", groupMembersStatus,
          size = cache.getGroupMembers.getOrElseSize match {
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

  def displayCompleteForBusinessType(businessRegistrationDetails: BusinessRegistrationDetails, legalEntity: Option[String]): Boolean = legalEntity match {
    case Some(legal) => businessRegistrationDetails.legalEntity.isDefined && isSameEntityIdCategory(businessRegistrationDetails.legalEntity.get, legal)
    case _ => false
  }

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