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
import controllers.auth.{AwrsController, ExternalUrls}
import forms.BusinessNameChangeConfirmationForm._
import models._
import play.api.mvc.{Action, AnyContent}
import services.{IndexService, KeyStoreService, Save4LaterService}
import utils.AccountUtils
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import services.DataCacheKeys._

import scala.concurrent.Future
import uk.gov.hmrc.http.InternalServerException

trait BusinessNameChangeController extends AwrsController with AccountUtils {

  val keyStoreService: KeyStoreService
  val save4LaterService: Save4LaterService
  val indexService: IndexService

  def showConfirm: Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { _ =>
      val businessType = request.getBusinessType
      Future.successful(Ok(views.html.awrs_group_representative_change_confirm(businessNameChangeConfirmationForm, businessType)))
    }
  }

  def callToAction: Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { ar =>
      businessNameChangeConfirmationForm.bindFromRequest.fold(
        formWithErrors => {
          val businessType = request.getBusinessType
          Future.successful(BadRequest(views.html.awrs_group_representative_change_confirm(formWithErrors, businessType)))
        },
        businessNameChangeDetails =>
          businessNameChangeDetails.businessNameChangeConfirmation match {
            case Some("Yes") =>
              keyStoreService.fetchExtendedBusinessDetails flatMap {
                extendedBusinessDetailsData =>
                  save4LaterService.mainStore.fetchBusinessCustomerDetails(ar) flatMap {
                    case Some(businessCustomerDetails) => {
                      save4LaterService.mainStore.saveBusinessCustomerDetails(ar, businessCustomerDetails.copy(businessName = extendedBusinessDetailsData.get.businessName.get)) flatMap {
                        _ =>
                          save4LaterService.mainStore.saveBusinessDetails(ar, extendedBusinessDetailsData.get.getBusinessDetails) flatMap {
                            _ =>
                              save4LaterService.mainStore.saveBusinessRegistrationDetails(ar, BusinessRegistrationDetails()) flatMap {
                                _ =>
                                  save4LaterService.mainStore.savePlaceOfBusiness(ar, PlaceOfBusiness()) flatMap {
                                    _ =>
                                      save4LaterService.mainStore.saveBusinessContacts(ar, BusinessContacts()) flatMap {
                                        _ =>
                                          Future.successful(Redirect(controllers.routes.ViewApplicationController.viewSection(businessDetailsName)).addBusinessNameToSession(extendedBusinessDetailsData.get.businessName.get))
                                      }
                                  }
                              }
                          }
                      }
                    }
                    case _ => throw new InternalServerException("Business name change, businessCustomerDetails not found")
                  }
              }
            case _ =>
              Future.successful(Redirect(routes.BusinessDetailsController.showBusinessDetails(false)))
          }
      )
    }
  }

}

object BusinessNameChangeController extends BusinessNameChangeController {
  override val authConnector = FrontendAuthConnector
  override val keyStoreService = KeyStoreService
  override val save4LaterService = Save4LaterService
  override val indexService = IndexService
  val signInUrl = ExternalUrls.signIn
}
