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
import forms.BusinessNameChangeConfirmationForm._
import models._
import play.api.mvc.{Action, AnyContent}
import services.{KeyStoreService, Save4LaterService}
import utils.AccountUtils
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.concurrent.Future

trait BusinessNameChangeController extends AwrsController with AccountUtils {

  val keyStoreService: KeyStoreService
  val save4LaterService: Save4LaterService


  def showConfirm: Action[AnyContent] = async {
    implicit user => implicit request =>
      Future.successful(Ok(views.html.awrs_group_representative_change_confirm(businessNameChangeConfirmationForm)))
  }

  def callToAction: Action[AnyContent] = async {
    implicit user => implicit request =>
      businessNameChangeConfirmationForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(views.html.awrs_group_representative_change_confirm(formWithErrors)))
        ,
        businessNameChangeDetails =>
          businessNameChangeDetails.businessNameChangeConfirmation match {
            case Some("Yes") =>

              // Update business name with new business name from temp store
//              val extendedBusinessDetails = save4LaterService.mainStore.fetchExtendedBusinessDetails
//              save4LaterService.mainStore.saveBusinessCustomerDetails(extendedBusinessDetails.updateBusinessCustomerDetails(businessCustomerDetails))
//              save4LaterService.mainStore.saveBusinessDetails(extendedBusinessDetails.getBusinessDetails)

              // Clear business sections
              save4LaterService.mainStore.saveBusinessRegistrationDetails(BusinessRegistrationDetails()) flatMap {
                _ => save4LaterService.mainStore.savePlaceOfBusiness(PlaceOfBusiness()) flatMap {
                  _ => save4LaterService.mainStore.saveBusinessContacts(BusinessContacts()) flatMap {
                    _ =>
                      // set businessRegistrationDetailsStatus, placeOfBusinessStatus and businessContactsStatus to Incomplete?
                      Future.successful(Redirect(routes.IndexController.showIndex()))
                  }
                }
              }

            case _ =>
              Future.successful(Redirect(routes.BusinessDetailsController.showBusinessDetails(true)))
          }
      )
  }

}

object BusinessNameChangeController extends BusinessNameChangeController {
  override val authConnector = FrontendAuthConnector
  override val keyStoreService = KeyStoreService
  override val save4LaterService = Save4LaterService
}
