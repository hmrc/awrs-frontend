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

package controllers

import audit.Auditable
import config.ApplicationConfig
import connectors.AwrsDataCacheConnector
import controllers.auth.{AwrsController, StandardAuthRetrievals}
import controllers.util.UnSubmittedBannerUtil
import javax.inject.Inject
import models.BusinessDetailSummaryModel
import play.api.i18n.Messages
import play.api.mvc._
import play.twirl.api.{Html, HtmlFormat}
import services.DataCacheKeys._
import services.JourneyConstants._
import services._
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{AccountUtils, CacheUtil}
import views.view_application.ViewApplicationHelper._
import views.view_application.helpers._
import scala.language.postfixOps

import scala.concurrent.{ExecutionContext, Future}

class ViewApplicationController @Inject()(mcc: MessagesControllerComponents,
                                          val applicationService: ApplicationService,
                                          val indexService: IndexService,
                                          val keyStoreService: KeyStoreService,
                                          val save4LaterService: Save4LaterService,
                                          val deEnrolService: DeEnrolService,
                                          val authConnector: DefaultAuthConnector,
                                          val auditable: Auditable,
                                          val accountUtils: AccountUtils,
                                          val mainStoreSave4LaterConnector: AwrsDataCacheConnector,
                                          implicit val applicationConfig: ApplicationConfig,
                                          templateError: views.html.awrs_application_error,
                                          templateViewApp: views.html.view_application.awrs_view_application) extends FrontendController(mcc) with AwrsController with UnSubmittedBannerUtil with DataCacheService {

  implicit val ec: ExecutionContext = mcc.executionContext
  implicit val cacheUtil: CacheMap => CacheUtil.CacheHelper = CacheUtil.cacheUtil
  val signInUrl: String = applicationConfig.signIn

  def viewApplicationContent(dataCache: CacheMap, status: String, enrolments: Set[Enrolment])(implicit request: Request[AnyContent]): Boolean => HtmlFormat.Appendable =
    (printFriendly: Boolean) =>
      if (printFriendly) {
        views.html.view_application.awrs_view_application_core(dataCache, status, enrolments, accountUtils)(viewApplicationType = PrintFriendlyMode, implicitly, implicitly, implicitly)
      } else {
        views.html.view_application.awrs_view_application_core(dataCache, status, enrolments, accountUtils)(viewApplicationType = OneViewMode, implicitly, implicitly, implicitly)
      }

  def show(printFriendly: Boolean): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { ar =>
      for {
        subscriptionData <- fetchMainStore(ar)
      } yield {
        lazy val status = getSessionStatusStr.fold(Messages("awrs.index_page.draft"))(x => x)
        (subscriptionData, printFriendly) match {
          case (Some(dataCache), false) => Ok(
            templateViewApp(
              viewApplicationContent(dataCache, status.toLowerCase.capitalize.replace("/", " / "), ar.enrolments),
              printFriendly = false,
              sectionText = None,
              // Un-submitted changes banner is not required on the one view page and it would always appear on the confirmation page version as the change indicator data has been cleared down
              unSubmittedChangesParam = None
            )
          ) addLocation
          case (Some(dataCache), true) =>
            // Don't use AWRSController OK helper as we don't want to add the thank you view to the session location history
            OkNoLocation(
              templateViewApp(
                viewApplicationContent(dataCache, status.toLowerCase.capitalize.replace("/", " / "), ar.enrolments),
                printFriendly = true,
                sectionText = None,
                unSubmittedChangesParam = None
              )
            ) addLocation
          case _ =>
            BadRequest(templateError())
        }
      }
    }
  }

  def viewSection(sectionName: String, printFriendly: Boolean): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { ar =>
      for {
        businessCustomerDetails <- save4LaterService.mainStore.fetchBusinessCustomerDetails(ar)
        subscriptionData <- save4LaterService.mainStore.fetchAll(ar)
        unSubmittedChangesParam <- unSubmittedChangesBanner(subscriptionData, ar)
      } yield {
        def showPage(content: Html, legalEntity: String) = Ok(
          templateViewApp(_ =>
            content,
            printFriendly,
            Some(getSectionDisplayName(sectionName, legalEntity)),
            unSubmittedChangesParam = unSubmittedChangesParam
          )
        )

        subscriptionData match {
          case Some(cacheMap) =>
            val legalEntity = cacheMap.getBusinessType match {
              case Some(businessTypeData) => businessTypeData.legalEntity.fold("")(x => x)
              case None => ""
            }
            val displayNameKey = getSectionDisplayName(sectionName, legalEntity)
            sectionName match {
              case `businessDetailsName` => showPage(views.html.view_application.subviews.subview_business_details(
                displayNameKey, BusinessDetailSummaryModel(cacheMap.getBusinessNameDetails, cacheMap.getTradingStartDetails), Some(businessCustomerDetails.fold("")(x => x.businessName)))(viewApplicationType = EditRecordOnlyMode, implicitly, implicitly), legalEntity)
              case `businessRegistrationDetailsName` => showPage(views.html.view_application.subviews.subview_business_registration_details(
                displayNameKey, cacheMap.getBusinessRegistrationDetails, Some(legalEntity))(viewApplicationType = EditRecordOnlyMode, implicitly, implicitly), legalEntity)
              case `placeOfBusinessName` => showPage(views.html.view_application.subviews.subview_place_of_business(
                displayNameKey, cacheMap.getPlaceOfBusiness)(viewApplicationType = EditRecordOnlyMode, implicitly, implicitly, implicitly), legalEntity)
              case `businessContactsName` => showPage(views.html.view_application.subviews.subview_business_contacts(
                displayNameKey, cacheMap.getBusinessContacts, businessCustomerDetails)(viewApplicationType = EditRecordOnlyMode, implicitly, implicitly, implicitly), legalEntity)
              case `partnersName` => showPage(views.html.view_application.subviews.subview_partner_details(
                displayNameKey, cacheMap.getPartners)(viewApplicationType = EditSectionOnlyMode, implicitly, implicitly, implicitly), legalEntity)
              case `groupMembersName` => showPage(views.html.view_application.subviews.subview_group_member_details(
                displayNameKey, cacheMap.getGroupMembers, legalEntity, ar.enrolments, accountUtils)(viewApplicationType = EditSectionOnlyMode, implicitly, implicitly, implicitly), legalEntity)
              case `additionalBusinessPremisesName` => showPage(views.html.view_application.subviews.subview_additional_premises(
                displayNameKey, cacheMap.getAdditionalBusinessPremises)(viewApplicationType = EditSectionOnlyMode, implicitly, implicitly, implicitly), legalEntity)
              case `businessDirectorsName` => showPage(views.html.view_application.subviews.subview_business_directors(
                displayNameKey, cacheMap.getBusinessDirectors, legalEntity)(viewApplicationType = EditSectionOnlyMode, implicitly, implicitly), legalEntity)
              case `tradingActivityName` => showPage(views.html.view_application.subviews.subview_trading_activity(
                displayNameKey, cacheMap.getTradingActivity)(viewApplicationType = EditRecordOnlyMode, implicitly, implicitly), legalEntity)
              case `productsName` => showPage(views.html.view_application.subviews.subview_products(
                displayNameKey, cacheMap.getProducts)(viewApplicationType = EditRecordOnlyMode, implicitly, implicitly), legalEntity)
              case `suppliersName` => showPage(views.html.view_application.subviews.subview_suppliers(
                displayNameKey, cacheMap.getSuppliers)(viewApplicationType = EditSectionOnlyMode, implicitly, implicitly, implicitly), legalEntity)
              case _ => NotFound(applicationConfig.templateNotFound())
            }
          case _ => NotFound(applicationConfig.templateNotFound())
        }
      }
    }
  }

  private def findLastId(authRetrievals: StandardAuthRetrievals, sectionName: String)(implicit request: Request[AnyContent]): Future[Int] = {
    sectionName match {
      case `partnersName` => save4LaterService.mainStore.fetchPartnerDetails(authRetrievals).map {
        case Some(partners) => partners.partners.length
        case None => 1
      }
      case `groupMembersName` => save4LaterService.mainStore.fetchGroupMembers(authRetrievals).map {
        case Some(groupMemebers) => groupMemebers.members.length
        case None => 1
      }
      case `additionalBusinessPremisesName` => save4LaterService.mainStore.fetchAdditionalBusinessPremisesList(authRetrievals).map {
        case Some(premises) => premises.premises.length
        case None => 1
      }
      case `businessDirectorsName` => save4LaterService.mainStore.fetchBusinessDirectors(authRetrievals).map {
        case Some(directors) => directors.directors.length
        case None => 1
      }
      case `suppliersName` => save4LaterService.mainStore.fetchSuppliers(authRetrievals).map {
        case Some(suppliers) => suppliers.suppliers.length
        case None => 1
      }
      case _ => Future.failed(new InternalServerException(s"findLastId for section: $sectionName is not supported"))
    }
  }

  /*
   * this internal method is designed to be solely used by the backFrom method to display the correct page
   */
  private def displayPage(sectionName: String, id: Option[Int], authRetrievals: StandardAuthRetrievals)
                         (implicit request: Request[AnyContent]): Future[Result] = {
    // sub function to goto a page which has subsections
    val pageWithSubsection = (show: (Int) => Call) =>
      id match {
        case Some(entryId) => Future.successful(Redirect(show(entryId)))
        // if the Id is not supplied then find the last page it needs to goto
        case _ => findLastId(authRetrievals, sectionName) map (id => Redirect(show(id)))
      }
    sectionName match {
      case `businessDetailsName` => Future.successful(Redirect(controllers.routes.TradingNameController.showTradingName(true)))
      case `businessRegistrationDetailsName` => Future.successful(Redirect(controllers.routes.BusinessRegistrationDetailsController.showBusinessRegistrationDetails(isLinearMode = true)))
      case `placeOfBusinessName` => Future.successful(Redirect(controllers.routes.PlaceOfBusinessController.showPlaceOfBusiness(isLinearMode = true)))
      case `businessContactsName` => Future.successful(Redirect(controllers.routes.BusinessContactsController.showBusinessContacts(isLinearMode = true)))
      case `partnersName` => pageWithSubsection(controllers.routes.BusinessPartnersController.showPartnerMemberDetails(_, isLinearMode = true, isNewRecord = true))
      case `groupMembersName` => pageWithSubsection(controllers.routes.GroupMemberController.showMemberDetails(_, isLinearMode = true, isNewRecord = true))
      case `additionalBusinessPremisesName` => pageWithSubsection(controllers.routes.AdditionalPremisesController.showPremisePage(_, isLinearMode = true, isNewRecord = true))
      case `businessDirectorsName` => pageWithSubsection(controllers.routes.BusinessDirectorsController.showBusinessDirectors(_, isLinearMode = true, isNewRecord = true))
      case `tradingActivityName` => Future.successful(Redirect(controllers.routes.TradingActivityController.showTradingActivity(isLinearMode = true)))
      case `productsName` => Future.successful(Redirect(controllers.routes.ProductsController.showProducts(isLinearMode = true)))
      case `suppliersName` => pageWithSubsection(controllers.routes.SupplierAddressesController.showSupplierAddressesPage(_, isLinearMode = true, isNewRecord = true))
      case _ => Future.successful(NotFound(applicationConfig.templateNotFound()))
    }
  }


  /*
   * this method is designed only to be used by the back button of the main form entry pages during the linear journey
   * once the user has entered something then the back button must goto the one-view version using the viewSection method
   *
   */
  def backFrom(sectionName: String, id: Option[Int] = None): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { ar =>
      lazy val gotoIndex = Future.successful(Redirect(controllers.routes.IndexController.showIndex))
      id match {
        // if id is > 1 then simply goto the id - 1 version of the page
        case Some(entryId) if entryId > 1 => displayPage(sectionName, Some(entryId - 1), ar)
        case _ =>
          // otherwise find the correct previous page to goto depending on their customer jouney
          // the journeys are dependent on the business type, so if it doens't exists in session then display the error page
          getBusinessType match {
            case Some(businessType) =>
              val journey = getJourney(businessType)
              val indexForThisSection = journey.indexOf(sectionName)
              indexForThisSection - 1 match {
                case -1 => gotoIndex // if this is the first entry in the journey then goto index
                case ind =>
                  // check where the journey began,
                  // if we are already at the point where the journey began then return to index
                  // otherwise goto the previous section in this journey
                  val jstart = request.getJourneyStartLocation.fold("")(x => x)
                  val indexForJourneyStartLocation = journey.indexOf(jstart)
                  jstart match {
                    case `sectionName` => gotoIndex
                    // if some how the current page is already past the journey start location then go back to index
                    // this can only happen if the user did not have a controlled flow, e.g. jumped to a previously bookmarked URL
                    case _ if indexForJourneyStartLocation > indexForThisSection => gotoIndex
                    case _ => displayPage(journey(ind), None, ar) //goto the previous section in this journey
                  }
              }
            case _ => showErrorPage
          }
      }
    }
  }

}
