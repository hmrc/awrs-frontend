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

package controllers.util

import controllers.auth.{AwrsController, StandardAuthRetrievals}
import forms.AWRSEnums.BooleanRadioEnum
import play.api.mvc._
import services.DataCacheKeys._
import services.{JourneyConstants, Save4LaterService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionSectionHashUtil
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, ViewApplicationType}

import scala.concurrent.{ExecutionContext, Future}

trait SaveAndRoutable extends FrontendController with AwrsController {

  val ErrorString = "error"

  val save4LaterService: Save4LaterService
  val mcc: MessagesControllerComponents
  implicit val ec: ExecutionContext = mcc.executionContext

  val section: String

  def save(id: Int,
           redirectRoute: (Option[RedirectParam], Boolean) => Future[Result],
           viewApplicationType: ViewApplicationType,
           isNewRecord: Boolean,
           authRetrievals: StandardAuthRetrievals
          )(implicit request: Request[AnyContent]): Future[Result]

  // shortcut methods for single record pages as the id is not relevant
  def saveAndContinue(): Action[AnyContent] = saveAndContinue(1, isNewRecord = true)

  def saveAndReturn(): Action[AnyContent] = saveAndReturn(1, isNewRecord = true)

  def saveAndContinue(id: Int, isNewRecord: Boolean): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { ar =>
      save(id, doRedirect, LinearViewMode, isNewRecord, ar)
    }
  }

  def saveAndReturn(id: Int, isNewRecord: Boolean): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { ar =>
      save(id, doRedirectReturn, EditSectionOnlyMode, isNewRecord, ar)
    }
  }

  private def doRedirect(param: Option[RedirectParam], isNewRecord: Boolean)(implicit request: Request[AnyContent]) =
    param match {
      case Some(RedirectParam(BooleanRadioEnum.YesString, id)) => nextInSection(id, isNewRecord)
      case _ => nextSection(isNewRecord)
    }

  private def doRedirectReturn(param: Option[RedirectParam], isNewRecord: Boolean) =
    Future.successful(Redirect(controllers.routes.ViewApplicationController.viewSection(section)))

  type MultiRecordSectionURL = (Int, Boolean, Boolean) => Call
  type SingleRecordSectionURL = (Boolean) => Call
  type SectionURL = Either[SingleRecordSectionURL, MultiRecordSectionURL]

  val multiRecordSectionMap: Map[String, SectionURL] = Map[String, SectionURL](
    (additionalBusinessPremisesName, Right(controllers.routes.AdditionalPremisesController.showPremisePage)),
    (businessDirectorsName, Right(controllers.routes.BusinessDirectorsController.showBusinessDirectors)),
    (groupMembersName, Right(controllers.routes.GroupMemberController.showMemberDetails)),
    (partnersName, Right(controllers.routes.BusinessPartnersController.showPartnerMemberDetails)),
    (suppliersName, Right(controllers.routes.SupplierAddressesController.showSupplierAddressesPage))
  )

  val allSectionMap: Map[String, SectionURL] = Map[String, SectionURL](
    (businessRegistrationDetailsName, Left(controllers.routes.BusinessRegistrationDetailsController.showBusinessRegistrationDetails)),
    (placeOfBusinessName, Left(controllers.routes.PlaceOfBusinessController.showPlaceOfBusiness)),
    (businessContactsName, Left(controllers.routes.BusinessContactsController.showBusinessContacts)),
    (tradingActivityName, Left(controllers.routes.TradingActivityController.showTradingActivity)),
    (productsName, Left(controllers.routes.ProductsController.showProducts))
  ) ++ multiRecordSectionMap

  private def nextInSection(id: Int, isNewRecord: Boolean) =
    multiRecordSectionMap.get(section) match {
      case Some(Right(route)) => Future.successful(Redirect(route(id + 1, true, isNewRecord)))
      // all the routes must be defined in multiRecordSectionMap and this must never happen in live
      case _ => throw new RuntimeException("Invalid section key")
    }

  type NextSectionForBusinessType = (Option[String]) => String

  val sectionRouteMap: Map[String, NextSectionForBusinessType] = Map(
    (businessDetailsName, (businessType: Option[String]) => businessRegistrationDetailsName),
    (businessRegistrationDetailsName, (businessType: Option[String]) => placeOfBusinessName),
    (placeOfBusinessName, (businessType: Option[String]) => businessContactsName),
    (businessContactsName, {
      case Some("LTD_GRP") | Some("LLP_GRP") => groupMembersName
      case Some("Partnership") | Some("LP") | Some("LLP") => partnersName
      case Some(_) => additionalBusinessPremisesName
      case _ => ErrorString
    }),
    (partnersName, (businessType: Option[String]) => additionalBusinessPremisesName),
    (additionalBusinessPremisesName, {
      case Some("LTD") | Some("LTD_GRP") => businessDirectorsName
      case Some(_) => tradingActivityName
      case _ => ErrorString
    }),
    (groupMembersName, {
      case Some("LTD_GRP") => additionalBusinessPremisesName
      case Some(_) => partnersName
      case _ => ErrorString
    }),
    (businessDirectorsName, (businessType: Option[String]) => tradingActivityName),
    (tradingActivityName, (businessType: Option[String]) => productsName),
    (productsName, (businessType: Option[String]) => suppliersName),
    (suppliersName, (businessType: Option[String]) => indexName)
  )

  private def whereToNext(implicit request: Request[AnyContent]): String =
    sectionRouteMap.get(section) match {
      case Some(sectionRoutingFunction) =>
        sectionRoutingFunction(request.getBusinessType)
      case _ => throw new RuntimeException("Invalid section key")
    }

  private def goToSection(section: String, isNewRecord: Boolean): Call =
    allSectionMap.get(section) match {
      case Some(Left(route)) => route(true)
      case Some(Right(route)) => route(1, true, isNewRecord)
      case _ => throw new RuntimeException(s"Invalid section key: $section")
    }

  /*
   *  This method is designed to determine if the save and continue call in a linear journey must continue on to the
   *  next section in the journey or return back to the index page.
   *
   *  It utilises the section hash which is updated every time the index page is visited, to determine if the section
   *  was completed when the current journey started.
   *
   *  The method returns true if the redirection must be back to the index page and false otherwise.
   */
  private[util] def redirectToIndex(sectionName: String)(implicit request: Request[AnyContent]): Future[Boolean] =
  Future.successful(
    sectionName match {
      case `indexName` => true
      case _ =>
        val sectionStatus = request.getSectionStatus.fold("0")(x => x)
        val journeyPage = JourneyConstants.getJourney(request.getBusinessType.fold("")(x => x))
        val index = journeyPage.indexOf(sectionName)
        SessionSectionHashUtil.isCompleted(index, sectionStatus)
    }
  )

  private def nextSection(isNewRecord: Boolean)(implicit request: Request[AnyContent]): Future[Result] =
    whereToNext match {
      case ErrorString => showErrorPage
      case nextSection@_ =>
        redirectToIndex(nextSection) map {
          case true => Redirect(controllers.routes.IndexController.showIndex())
          case false => Redirect(goToSection(nextSection, isNewRecord))
        }
    }
}
