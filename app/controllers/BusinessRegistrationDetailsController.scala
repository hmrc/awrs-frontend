/*
 * Copyright 2020 HM Revenue & Customs
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
import controllers.util.{JourneyPage, RedirectParam, SaveAndRoutable}
import forms.BusinessRegistrationDetailsForm._
import javax.inject.Inject
import models._
import play.api.mvc._
import services.DataCacheKeys._
import services.{BusinessDetailsService, BusinessMatchingService, DeEnrolService, Save4LaterService}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.AccountUtils
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, ViewApplicationType}

import scala.concurrent.{ExecutionContext, Future}

class BusinessRegistrationDetailsController @Inject()(val mcc: MessagesControllerComponents,
                                                      val businessMatchingService: BusinessMatchingService,
                                                      val businessDetailsService: BusinessDetailsService,
                                                      val save4LaterService: Save4LaterService,
                                                      val deEnrolService: DeEnrolService,
                                                      val authConnector: DefaultAuthConnector,
                                                      val auditable: Auditable,
                                                      val accountUtils: AccountUtils,
                                                      implicit val applicationConfig: ApplicationConfig) extends FrontendController(mcc) with JourneyPage with SaveAndRoutable {

  override implicit val ec: ExecutionContext = mcc.executionContext
  override val section: String = businessRegistrationDetailsName
  val signInUrl: String = applicationConfig.signIn

  def showBusinessRegistrationDetails(isLinearMode: Boolean): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { implicit ar =>
      restrictedAccessCheck {
        implicit val viewApplicationType: ViewApplicationType = if (isLinearMode) {
          LinearViewMode
        } else {
          EditSectionOnlyMode
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

  override def save(id: Int,
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
              businessMatchingService.isValidMatchedGroupUtr(utr, authRetrievals) map {
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
