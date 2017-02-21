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

package controllers

import config.FrontendAuthConnector
import controllers.auth.AwrsController
import controllers.util.{JourneyPage, RedirectParam, SaveAndRoutable, convertBCAddressToAddress}
import forms.PlaceOfBusinessForm._
import models.PlaceOfBusiness
import play.api.mvc.{AnyContent, Request, Result}
import services.DataCacheKeys._
import services.Save4LaterService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.AccountUtils
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, ViewApplicationType}

import scala.concurrent.Future

trait PlaceOfBusinessController extends AwrsController with JourneyPage with AccountUtils with SaveAndRoutable {

  override val section = placeOfBusinessName

  def showPlaceOfBusiness(isLinearMode: Boolean) = asyncRestrictedAccess {
    implicit user => implicit request =>
      implicit val viewApplicationType = isLinearMode match {
        case true => LinearViewMode
        case false => EditSectionOnlyMode
      }

      val businessType = request.getBusinessType

      // if this is the first time the user enters this form, then populate the main principal place of business with the address from business customers frontend
      save4LaterService.mainStore.fetchPlaceOfBusiness.flatMap {
        case Some(data) => Future.successful(Ok(views.html.awrs_principal_place_of_business(AccountUtils.hasAwrs, businessType, placeOfBusinessForm.form.fill(data))))
        case _ =>
          save4LaterService.mainStore.fetchBusinessCustomerDetails.flatMap {
            case Some(businessCustomerDetails) =>
              val firstTimeForm = placeOfBusinessForm.form.fill(PlaceOfBusiness(mainAddress = convertBCAddressToAddress(businessCustomerDetails.businessAddress)))
              Future.successful(Ok(views.html.awrs_principal_place_of_business(AccountUtils.hasAwrs, businessType, firstTimeForm)))
            case None => showErrorPage // given the user started the journey correctly from the home controller, this should never happen
          }
      }
  }

  def save(id: Int, redirectRoute: (Option[RedirectParam], Boolean) => Future[Result], viewApplicationType: ViewApplicationType, isNewRecord: Boolean)(implicit request: Request[AnyContent], user: AuthContext): Future[Result] = {
    implicit val viewMode = viewApplicationType
    placeOfBusinessForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(views.html.awrs_principal_place_of_business(AccountUtils.hasAwrs, request.getBusinessType, formWithErrors)))
      ,
      placeOfBusinessData =>
        save4LaterService.mainStore.savePlaceOfBusiness(placeOfBusinessData) flatMap {
          _ => redirectRoute(Some(RedirectParam("No", id)), isNewRecord)
        }
    )
  }

}

object PlaceOfBusinessController extends PlaceOfBusinessController {
  override val authConnector = FrontendAuthConnector
  override val save4LaterService = Save4LaterService
}