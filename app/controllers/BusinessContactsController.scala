/*
 * Copyright 2018 HM Revenue & Customs
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
import controllers.util.{JourneyPage, RedirectParam, SaveAndRoutable}
import forms.BusinessContactsForm._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{AnyContent, Request, Result}
import services.DataCacheKeys._
import services.{EmailVerificationService, Save4LaterService}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.AccountUtils
import utils.AwrsConfig._
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, ViewApplicationType}

import scala.concurrent.Future

trait BusinessContactsController extends AwrsController with JourneyPage with AccountUtils with SaveAndRoutable {

  val emailVerificationService: EmailVerificationService

  override val section = businessContactsName

  def showBusinessContacts(isLinearMode: Boolean) = asyncRestrictedAccess {
    implicit user => implicit request =>
      implicit val viewApplicationType = isLinearMode match {
        case true => LinearViewMode
        case false => EditSectionOnlyMode
      }

      val businessType = request.getBusinessType
      save4LaterService.mainStore.fetchBusinessCustomerDetails.flatMap {
        case Some(businessCustomerDetails) =>
          // if this is the first time the user enters this form, then populate the main principal place of business with the address from business customers frontend
          save4LaterService.mainStore.fetchBusinessContacts.flatMap {
            case Some(data) => Future.successful(Ok(views.html.awrs_business_contacts(AccountUtils.hasAwrs,
              businessType,
              businessCustomerDetails.businessAddress,
              businessContactsForm.form.fill(data))))
            case _ =>
              val firstTimeForm = businessContactsForm.form
              Future.successful(Ok(views.html.awrs_business_contacts(AccountUtils.hasAwrs,
                businessType,
                businessCustomerDetails.businessAddress,
                firstTimeForm)))
          }
        case None => showErrorPage // given the user started the journey correctly from the home controller, this should never happen
      }
  }

  def save(id: Int, redirectRoute: (Option[RedirectParam], Boolean) => Future[Result], viewApplicationType: ViewApplicationType, isNewRecord: Boolean)(implicit request: Request[AnyContent], user: AuthContext): Future[Result] = {
    implicit val viewMode = viewApplicationType
    businessContactsForm.bindFromRequest.fold(
      formWithErrors =>
        save4LaterService.mainStore.fetchBusinessCustomerDetails.flatMap {
          case Some(businessCustomerDetails) =>
            Future.successful(BadRequest(views.html.awrs_business_contacts(AccountUtils.hasAwrs, request.getBusinessType, businessCustomerDetails.businessAddress, formWithErrors)))
          case _ => showErrorPage
        }
      ,
      addressesAndContactDetailsData =>
        save4LaterService.mainStore.saveBusinessContacts(addressesAndContactDetailsData) flatMap {
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
}
