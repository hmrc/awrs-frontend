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

package controllers

import audit.Auditable
import config.ApplicationConfig
import connectors.AwrsDataCacheConnector
import controllers.auth.StandardAuthRetrievals
import controllers.util.{JourneyPage, RedirectParam, SaveAndRoutable}
import forms.TradingDateForm._
import javax.inject.Inject
import models._
import play.api.mvc._
import services.DataCacheKeys._
import services._
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AccountUtils
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, ViewApplicationType}
import forms.AWRSEnums.BooleanRadioEnum
import scala.concurrent.{ExecutionContext, Future}

class TradingDateController @Inject()(val mcc: MessagesControllerComponents,
                                      val save4LaterService: Save4LaterService,
                                      val businessDetailsService: BusinessDetailsService,
                                      val keyStoreService: KeyStoreService,
                                      val deEnrolService: DeEnrolService,
                                      val authConnector: DefaultAuthConnector,
                                      val auditable: Auditable,
                                      val accountUtils: AccountUtils,
                                      val mainStoreSave4LaterConnector: AwrsDataCacheConnector,
                                      implicit val applicationConfig: ApplicationConfig,
                                      template: views.html.awrs_trading_date) extends FrontendController(mcc) with JourneyPage with SaveAndRoutable with DataCacheService {

  override val section: String = businessDetailsName
  override implicit val ec: ExecutionContext = mcc.executionContext
  val signInUrl: String = applicationConfig.signIn

  def showBusinessDetails(isLinearMode: Boolean): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { implicit ar =>
      restrictedAccessCheck {
        businessDetailsService.businessDetailsPageRenderMode(ar) flatMap {_ =>
          implicit val viewApplicationType: ViewApplicationType = if (isLinearMode) LinearViewMode else EditSectionOnlyMode
          val businessType = request.getBusinessType
          for {
            savedQuestionType <- keyStoreService.fetchAlreadyTrading
            maybeAWBusiness <- save4LaterService.mainStore.fetchTradingStartDetails(ar)
            tradingState <- updateTradingStateInDatabaseIfNeeded(savedQuestionType, maybeAWBusiness)
          } yield {
            (maybeAWBusiness, tradingState) match {
              case (Some(nab@NewAWBusiness(_, date)), Some(savedQ)) =>
                val form = tradingDateForm(savedQ, Some(nab.isNewAWBusiness))
                Ok(template(date.fold(form)(dt => form.fill(dt)), businessType, savedQ))
              case _ => Redirect(routes.TradingLegislationDateController.showBusinessDetails(isLinearMode))
            }
          }
        }
      }
    }
  }

  private def updateTradingStateInDatabaseIfNeeded(databaseTradingState: Option[Boolean], newAwBusiness: Option[NewAWBusiness])
                                                  (implicit requestHeader: RequestHeader): Future[Option[Boolean]] =
    (databaseTradingState, newAwBusiness) match {
      case (None, Some(NewAWBusiness(BooleanRadioEnum.YesString, _))) => keyStoreService.saveAlreadyTrading(true).map(_ => Some(true))
      case _ => Future.successful(databaseTradingState)
    }

  def saveBusinessDetails(id: Int,
                          redirectRoute: (Option[RedirectParam], Boolean) => Future[Result],
                          isNewRecord: Boolean,
                          businessDetails: NewAWBusiness,
                          authRetrievals: StandardAuthRetrievals):Future[Result] = {
    save4LaterService.mainStore.saveTradingStartDetails(authRetrievals, businessDetails) flatMap (_ => redirectRoute(Some(RedirectParam("No", id)), isNewRecord))
  }

  override def save(id: Int,
           redirectRoute: (Option[RedirectParam], Boolean) => Future[Result],
           viewApplicationType: ViewApplicationType,
           isNewRecord: Boolean,
           authRetrievals: StandardAuthRetrievals)
          (implicit request: Request[AnyContent]): Future[Result] = {
    businessDetailsService.businessDetailsPageRenderMode(authRetrievals) flatMap {_ =>
      implicit val viewMode: ViewApplicationType = viewApplicationType
      val businessType = request.getBusinessType
      keyStoreService.fetchAlreadyTrading flatMap {
        case Some(alreadyTrading) =>
          save4LaterService.mainStore.fetchTradingStartDetails(authRetrievals) flatMap { fetchedAW =>
            tradingDateForm(alreadyTrading, fetchedAW.map(_.isNewAWBusiness)).bindFromRequest().fold(
              formWithErrors => Future.successful(BadRequest(template(formWithErrors, businessType, alreadyTrading))),
              formData => {
                val awToSave = fetchedAW match {
                  case Some(NewAWBusiness(newAWBusiness, _)) => NewAWBusiness(newAWBusiness, Some(formData))
                  case _ => throw new RuntimeException("Missing already started trading answer")
                }

                saveBusinessDetails(id, redirectRoute, isNewRecord, awToSave, authRetrievals)
              }
            )
          }

        case _ => Future.successful(Redirect(routes.AlreadyStartingTradingController.showBusinessDetails(viewMode == LinearViewMode)))
      }
    }
  }
}
