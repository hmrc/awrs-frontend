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
import controllers.auth.StandardAuthRetrievals
import controllers.util.{JourneyPage, RedirectParam, SaveAndRoutable, convertBCAddressToAddress}
import forms.PlaceOfBusinessForm._
import javax.inject.Inject
import models.{BCAddressApi3, PlaceOfBusiness}
import play.api.mvc._
import services.DataCacheKeys._
import services.{DeEnrolService, KeyStoreService, Save4LaterService}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AccountUtils
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, ViewApplicationType}

import scala.concurrent.{ExecutionContext, Future}

class PlaceOfBusinessController @Inject()(val mcc: MessagesControllerComponents,
                                          val save4LaterService: Save4LaterService,
                                          val keyStoreService: KeyStoreService,
                                          val authConnector: DefaultAuthConnector,
                                          val deEnrolService: DeEnrolService,
                                          val auditable: Auditable,
                                          val accountUtils: AccountUtils,
                                          implicit val applicationConfig: ApplicationConfig,
                                          template: views.html.awrs_principal_place_of_business) extends FrontendController(mcc) with JourneyPage with SaveAndRoutable {

  override implicit val ec: ExecutionContext = mcc.executionContext
  override val section: String = placeOfBusinessName
  val signInUrl: String = applicationConfig.signIn

  def showPlaceOfBusiness(isLinearMode: Boolean): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { implicit ar =>
      restrictedAccessCheck {
        implicit val viewApplicationType: ViewApplicationType = if (isLinearMode) {
          LinearViewMode
        } else {
          EditSectionOnlyMode
        }

        val businessType = request.getBusinessType

        // if this is the first time the user enters this form, then populate the main principal place of business with the address from business customers frontend
        save4LaterService.mainStore.fetchPlaceOfBusiness(ar).flatMap {
          case Some(data) => Future.successful(Ok(template(accountUtils.hasAwrs(ar.enrolments), businessType, placeOfBusinessForm.form.fill(data))))
          case _ =>
            save4LaterService.mainStore.fetchBusinessCustomerDetails(ar).flatMap {
              case Some(businessCustomerDetails) =>
                val firstTimeForm = placeOfBusinessForm.form.fill(PlaceOfBusiness(mainAddress = convertBCAddressToAddress(businessCustomerDetails.businessAddress)))
                Future.successful(Ok(template(accountUtils.hasAwrs(ar.enrolments), businessType, firstTimeForm)))
              case None => showErrorPage // given the user started the journey correctly from the home controller, this must never happen
            }
        }
      }
    }
  }

  def save(id: Int, redirectRoute: (Option[RedirectParam], Boolean) => Future[Result], viewApplicationType: ViewApplicationType, isNewRecord: Boolean, authRetrievals: StandardAuthRetrievals)
          (implicit request: Request[AnyContent]): Future[Result] = {
    implicit val viewMode = viewApplicationType
    placeOfBusinessForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(template(accountUtils.hasAwrs(authRetrievals.enrolments), request.getBusinessType, formWithErrors)))
      ,
      placeOfBusinessData => {
        keyStoreService.saveBusinessCustomerAddress(BCAddressApi3(placeOfBusinessData))
        save4LaterService.mainStore.savePlaceOfBusiness(authRetrievals, placeOfBusinessData) flatMap {
          _ => redirectRoute(Some(RedirectParam("No", id)), isNewRecord)
        }
      }
    )
  }
}
