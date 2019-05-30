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
import forms.BusinessContactsForm._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.DataCacheKeys._
import services.{EmailVerificationService, Save4LaterService}
import utils.AccountUtils
import utils.AwrsConfig._
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, ViewApplicationType}

import scala.concurrent.Future

trait BusinessContactsController extends AwrsController with JourneyPage with AccountUtils with SaveAndRoutable {

  val emailVerificationService: EmailVerificationService

  override val section = businessContactsName

  def showBusinessContacts(isLinearMode: Boolean): Action[AnyContent] = Action.async { implicit request =>
    restrictedAccessCheck {
      authorisedAction { ar =>
        implicit val viewApplicationType = if (isLinearMode) {
          LinearViewMode
        } else {
          EditSectionOnlyMode
        }

        val businessType = request.getBusinessType
        save4LaterService.mainStore.fetchBusinessCustomerDetails(ar).flatMap {
          case Some(businessCustomerDetails) =>
            save4LaterService.mainStore.fetchBusinessContacts(ar).flatMap {
              case Some(data) => Future.successful(Ok(views.html.awrs_business_contacts(AccountUtils.hasAwrs(ar.enrolments),
                businessType,
                businessCustomerDetails.businessAddress,
                businessContactsForm.form.fill(data))))
              case _ =>
                val firstTimeForm = businessContactsForm.form
                Future.successful(Ok(views.html.awrs_business_contacts(AccountUtils.hasAwrs(ar.enrolments),
                  businessType,
                  businessCustomerDetails.businessAddress,
                  firstTimeForm)))
            }
          case None => showErrorPage
        }
      }
    }
  }

  def save(id: Int, redirectRoute: (Option[RedirectParam], Boolean) => Future[Result], viewApplicationType: ViewApplicationType, isNewRecord: Boolean, authRetrievals: StandardAuthRetrievals)
          (implicit request: Request[AnyContent]): Future[Result] = {
    implicit val viewMode = viewApplicationType
    businessContactsForm.bindFromRequest.fold(
      formWithErrors =>
        save4LaterService.mainStore.fetchBusinessCustomerDetails(authRetrievals).flatMap {
          case Some(businessCustomerDetails) =>
            Future.successful(BadRequest(views.html.awrs_business_contacts(AccountUtils.hasAwrs(authRetrievals.enrolments), request.getBusinessType, businessCustomerDetails.businessAddress, formWithErrors)))
          case _ => showErrorPage
        }
      ,
      addressesAndContactDetailsData =>
        save4LaterService.mainStore.saveBusinessContacts(authRetrievals, addressesAndContactDetailsData) flatMap {
          _ =>
            if (emailVerificationEnabled) {
              emailVerificationService.sendVerificationEmail(addressesAndContactDetailsData.email.get)
            }
            redirectRoute(Some(RedirectParam("No", id)), isNewRecord)
        }
    )
  }

}

object BusinessContactsController extends BusinessContactsController {
  override val authConnector = FrontendAuthConnector
  override val save4LaterService = Save4LaterService
  override val emailVerificationService = EmailVerificationService
  val signInUrl = ExternalUrls.signIn
}
