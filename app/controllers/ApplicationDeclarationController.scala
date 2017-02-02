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

import _root_.models.FormBundleStatus._
import config.FrontendAuthConnector
import controllers.auth.AwrsController
import exceptions._
import forms.ApplicationDeclarationForm._
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.mvc.{AnyContent, Request}
import services._
import utils.AccountUtils

import scala.concurrent.Future

trait ApplicationDeclarationController extends AwrsController with AccountUtils with DataCacheService{

  val save4LaterService: Save4LaterService
  val keyStoreService: KeyStoreService

  val enrolService: EnrolService
  val applicationService: ApplicationService

  def isEnrolledApplicant(implicit request: Request[AnyContent], messages: Messages ): Boolean = {
    getSessionStatus exists (result => if (result == Pending || result == Approved || result == ApprovedWithConditions) true else false)
  }

  def showApplicationDeclaration = asyncRestrictedAccess {
    implicit user => implicit request =>
      save4LaterService.mainStore.fetchApplicationDeclaration map {
        case Some(data) => Ok(views.html.awrs_application_declaration(applicationDeclarationForm.form.fill(data), isEnrolledApplicant))
        case _ => Ok(views.html.awrs_application_declaration(applicationDeclarationForm.form, isEnrolledApplicant))
      }
  }

  def sendApplication = async {
    implicit user => implicit request =>
      hasAwrs match {
        case true => applicationDeclarationForm.bindFromRequest.fold(
          formWithErrors => Future.successful(BadRequest(views.html.awrs_application_declaration(formWithErrors, isEnrolledApplicant))),
          applicationDeclarationData => {
            for {
              savedDeclaration <- save4LaterService.mainStore.saveApplicationDeclaration(applicationDeclarationData)
              _ <- backUpSave4LaterInKeyStore
              successResponse <- applicationService.updateApplication()
            } yield Redirect(controllers.routes.ConfirmationController.showApplicationUpdateConfirmation(false)).addAwrsRefToSession(successResponse.etmpFormBundleNumber)
          }.recover {
            case error: ResubmissionException => InternalServerError(views.html.error_template(Messages("awrs.application_resubmission_error.title"), Messages("awrs.application_resubmission_error.heading"), Messages("awrs.application_resubmission_error.message")))
            case error: DESValidationException => InternalServerError(views.html.error_template(Messages("awrs.application_des_validation.title"), Messages("awrs.application_des_validation.heading"), Messages("awrs.application_des_validation.message")))
            case error =>
              warn("Exception encountered in Application Declaration Controller :\n" + error)
              throw error
          }
        )
        case _ =>
          val businessType = getBusinessType.getOrElse("")

          applicationDeclarationForm.bindFromRequest.fold(
            formWithErrors => Future.successful(BadRequest(views.html.awrs_application_declaration(formWithErrors, isEnrolledApplicant))),
            applicationDeclarationData => {
              for {
                savedDeclaration <- save4LaterService.mainStore.saveApplicationDeclaration(applicationDeclarationData)
                _ <- backUpSave4LaterInKeyStore
                businessPartnerDetails <- save4LaterService.mainStore.fetchBusinessCustomerDetails
                businessRegDetails <- save4LaterService.mainStore.fetchBusinessRegistrationDetails
                successResponse <- applicationService.sendApplication()
                enrolAWRS <- enrolService.enrolAWRS(successResponse, businessPartnerDetails.get, businessType, businessRegDetails.get.utr)
                refreshResp <- applicationService.refreshProfile
              } yield refreshResp match {
                case _ =>
                  Redirect(controllers.routes.ConfirmationController.showApplicationConfirmation(false)).addAwrsRefToSession(successResponse.etmpFormBundleNumber)
              }
            }.recover {
              case error: DESValidationException => InternalServerError(views.html.error_template(Messages("awrs.application_des_validation.title"), Messages("awrs.application_des_validation.heading"), Messages("awrs.application_des_validation.message")))
              case error: DuplicateSubscriptionException => InternalServerError(views.html.error_template(Messages("awrs.application_duplicate_request.title"), Messages("awrs.application_duplicate_request.heading"), Messages("awrs.application_duplicate_request.message")))
              case error: PendingDeregistrationException => InternalServerError(views.html.error_template(Messages("awrs.application_pending_deregistration.title"), Messages("awrs.application_pending_deregistration.heading"), Messages("awrs.application_pending_deregistration.message")))
              case error: GovernmentGatewayException => InternalServerError(views.html.error_template(Messages("awrs.application_government_gateway_error.title"), Messages("awrs.application_government_gateway_error.heading"), Messages("awrs.application_government_gateway_error.message")))
              case error =>
                warn("Exception encountered in Application Declaration Controller :\n" + error)
                throw error
            }
          )

      }
  }


}

object ApplicationDeclarationController extends ApplicationDeclarationController {

  override val authConnector = FrontendAuthConnector
  override val save4LaterService = Save4LaterService
  override val enrolService = EnrolService
  override val applicationService = ApplicationService
  override val keyStoreService = KeyStoreService

}
