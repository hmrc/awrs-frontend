/*
 * Copyright 2019 HM Revenue & Customs
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

import config.FrontendAuthConnector
import controllers.auth.{AwrsController, ExternalUrls, StandardAuthRetrievals}
import controllers.util.{JourneyPage, RedirectParam, SaveAndRoutable}
import forms.BusinessRegistrationDetailsForm._
import models._
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.DataCacheKeys._
import services.Save4LaterService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{AccountUtils, MatchingUtil}
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, ViewApplicationType}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.concurrent.Future

trait BusinessRegistrationDetailsController extends AwrsController with JourneyPage with AccountUtils with SaveAndRoutable {

  override val section = businessRegistrationDetailsName

  def matchingUtil: MatchingUtil

  def showBusinessRegistrationDetails(isLinearMode: Boolean): Action[AnyContent] = Action.async { implicit request : Request[AnyContent] =>
    authorisedAction { ar =>
      restrictedAccessCheck {
        implicit val viewApplicationType = isLinearMode match {
          case true => LinearViewMode
          case false => EditSectionOnlyMode
        }
        val businessType = request.getBusinessType
        save4LaterService.mainStore.fetchBusinessRegistrationDetails(ar).flatMap {
          case Some(data) => Future.successful(Ok(views.html.awrs_business_registration_details(businessType, businessRegistrationDetailsForm(businessType.get).form.fill(data))))
          case _ =>
            save4LaterService.mainStore.fetchBusinessCustomerDetails(ar).flatMap {
              businessCustomerDetails =>
                val data = BusinessRegistrationDetails(utr = businessCustomerDetails.get.utr)
                Future.successful(Ok(views.html.awrs_business_registration_details(businessType, businessRegistrationDetailsForm(businessType.get).form.fill(data))))
            }
        }
      }
    }
  }

  def save(id: Int,
           redirectRoute: (Option[RedirectParam], Boolean) => Future[Result],
           viewApplicationType: ViewApplicationType,
           isNewRecord: Boolean,
           authRetrievals: StandardAuthRetrievals)
          (implicit request: Request[AnyContent]): Future[Result] = {
    implicit val viewMode = viewApplicationType
    val businessType = request.getBusinessType
    businessRegistrationDetailsForm(businessType.get).bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(views.html.awrs_business_registration_details(businessType, formWithErrors))),
      success = businessRegistrationDetails =>
        businessType match {
          case Some("LTD_GRP" | "LLP_GRP") => businessRegistrationDetails.utr match {
            case Some(utr) => {
              matchingUtil.isValidMatchedGroupUtr(utr, authRetrievals) map {
                case true =>
                  save4LaterService.mainStore.saveBusinessRegistrationDetails(authRetrievals, businessRegistrationDetails) flatMap (_ => redirectRoute(Some(RedirectParam("No", id)), isNewRecord))
                case false =>
                  val errorMsg = "awrs.generic.error.utr_invalid_match"
                  val errorForm = businessRegistrationDetailsForm(businessType.get).form.withError(key = "utr", message = errorMsg).fill(businessRegistrationDetails)
                  Future.successful(BadRequest(views.html.awrs_business_registration_details(businessType, errorForm)))
              }
            }.flatMap(identity)
            case _ =>
              val errorMsg = "awrs.generic.error.utr_empty"
              val errorForm = businessRegistrationDetailsForm(businessType.get).form.withError(key = "utr", message = errorMsg).fill(businessRegistrationDetails)
              Future.successful(BadRequest(views.html.awrs_business_registration_details(businessType, errorForm)))
          }
          case _ => save4LaterService.mainStore.saveBusinessRegistrationDetails(authRetrievals, businessRegistrationDetails) flatMap (_ => redirectRoute(Some(RedirectParam("No", id)), isNewRecord))
        }
    )
  }
}

object BusinessRegistrationDetailsController extends BusinessRegistrationDetailsController {
  override val authConnector = FrontendAuthConnector
  override val save4LaterService = Save4LaterService
  override val matchingUtil = MatchingUtil
  val signInUrl = ExternalUrls.signIn
}
