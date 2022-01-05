/*
 * Copyright 2022 HM Revenue & Customs
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
import controllers.auth.AwrsController
import forms.BusinessNameChangeConfirmationForm._
import javax.inject.Inject
import models._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.DataCacheKeys._
import services.{DeEnrolService, IndexService, KeyStoreService, Save4LaterService}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AccountUtils

import scala.concurrent.{ExecutionContext, Future}

class BusinessNameChangeController @Inject()(mcc: MessagesControllerComponents,
                                             val keyStoreService: KeyStoreService,
                                             val save4LaterService: Save4LaterService,
                                             val deEnrolService: DeEnrolService,
                                             val indexService: IndexService,
                                             val authConnector: DefaultAuthConnector,
                                             val auditable: Auditable,
                                             val accountUtils: AccountUtils,
                                             implicit val applicationConfig: ApplicationConfig,
                                             template: views.html.awrs_group_representative_change_confirm) extends FrontendController(mcc) with AwrsController {

  implicit val ec: ExecutionContext = mcc.executionContext
  val signInUrl: String = applicationConfig.signIn

  def showConfirm(): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { _ =>
      val businessType = request.getBusinessType
      Future.successful(Ok(template(businessNameChangeConfirmationForm, businessType)))
    }
  }

  def callToAction(): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { ar =>
      businessNameChangeConfirmationForm.bindFromRequest.fold(
        formWithErrors => {
          val businessType = request.getBusinessType
          Future.successful(BadRequest(template(formWithErrors, businessType)))
        },
        success = businessNameChangeDetails =>
          businessNameChangeDetails.businessNameChangeConfirmation match {
            case Some("Yes") =>
              keyStoreService.fetchBusinessNameChange flatMap {
                case Some(businessNameDetails) =>
                  save4LaterService.mainStore.fetchBusinessCustomerDetails(ar) flatMap {
                    case Some(businessCustomerDetails) =>
                      save4LaterService.mainStore.saveBusinessCustomerDetails(ar, businessCustomerDetails.copy(businessName = businessNameDetails.businessName.get)) flatMap {
                        _ =>
                          save4LaterService.mainStore.saveBusinessNameDetails(ar, businessNameDetails) flatMap {
                            _ =>
                              save4LaterService.mainStore.saveBusinessRegistrationDetails(ar, BusinessRegistrationDetails()) flatMap {
                                _ =>
                                  save4LaterService.mainStore.savePlaceOfBusiness(ar, PlaceOfBusiness()) flatMap {
                                    _ =>
                                      save4LaterService.mainStore.saveBusinessContacts(ar, BusinessContacts()) flatMap {
                                        _ =>
                                          Future.successful(Redirect(controllers.routes.ViewApplicationController.viewSection(businessDetailsName)).addBusinessNameToSession(businessNameDetails.businessName.get))
                                      }
                                  }
                              }
                          }
                      }
                    case _ => throw new InternalServerException("Business name change, businessCustomerDetails not found")
                  }
                case _ => throw new InternalServerException("businessNameDetails not found")
              }
            case _ =>
              Future.successful(Redirect(routes.TradingNameController.showTradingName(false)))
          }
      )
    }
  }
}
