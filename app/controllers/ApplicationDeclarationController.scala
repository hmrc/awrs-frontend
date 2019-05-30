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

import _root_.models.FormBundleStatus._
import config.FrontendAuthConnector
import controllers.auth.{AwrsController, ExternalUrls, StandardAuthRetrievals}
import exceptions._
import forms.ApplicationDeclarationForm._
import play.api.Mode.Mode
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request}
import play.api.{Configuration, Play}
import services._
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.play.config.RunMode
import utils.AccountUtils

import scala.concurrent.Future

trait ApplicationDeclarationController extends AwrsController with AccountUtils with DataCacheService with RunMode {

  val save4LaterService: Save4LaterService
  val keyStoreService: KeyStoreService

  val enrolService: EnrolService
  val applicationService: ApplicationService

  def isEnrolledApplicant(implicit request: Request[AnyContent], messages: Messages): Boolean = {
    getSessionStatus exists (result => if (result == Pending || result == Approved || result == ApprovedWithConditions) true else false)
  }

  def showApplicationDeclaration: Action[AnyContent] = Action.async { implicit request =>
    restrictedAccessCheck {
      authorisedAction { ar =>
        save4LaterService.mainStore.fetchApplicationDeclaration(ar) map {
          case Some(data) => Ok(views.html.awrs_application_declaration(applicationDeclarationForm.form.fill(data), isEnrolledApplicant))
          case _ => Ok(views.html.awrs_application_declaration(applicationDeclarationForm.form, isEnrolledApplicant))
        }
      }
    }
  }

  def sendApplication(): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { ar =>
      if (hasAwrs(ar.enrolments)) {
        applicationDeclarationForm.bindFromRequest.fold(
          formWithErrors => Future.successful(BadRequest(views.html.awrs_application_declaration(formWithErrors, isEnrolledApplicant))),
          applicationDeclarationData => {
            for {
              savedDeclaration <- save4LaterService.mainStore.saveApplicationDeclaration(ar, applicationDeclarationData)
              _ <- backUpSave4LaterInKeyStore(ar)
              successResponse <- applicationService.updateApplication(ar)
            } yield Redirect(controllers.routes.ConfirmationController.showApplicationUpdateConfirmation(false)).addAwrsRefToSession(successResponse.etmpFormBundleNumber)
          }.recover {
            case error: ResubmissionException => InternalServerError(views.html.error_template(Messages("awrs.application_resubmission_error.title"), Messages("awrs.application_resubmission_error.heading"), Messages("awrs.application_resubmission_error.message")))
            case error: DESValidationException => InternalServerError(views.html.error_template(Messages("awrs.application_des_validation.title"), Messages("awrs.application_des_validation.heading"), Messages("awrs.application_des_validation.message")))
            case error =>
              warn("Exception encountered in Application Declaration Controller :\n" + error.getStackTrace.mkString("\n"))
              throw error
          }
        )
      } else {
        val businessType = getBusinessType.getOrElse("")
        applicationDeclarationForm.bindFromRequest.fold(
          formWithErrors => Future.successful(BadRequest(views.html.awrs_application_declaration(formWithErrors, isEnrolledApplicant))),
          applicationDeclarationData => {
            for {
              savedDeclaration <- save4LaterService.mainStore.saveApplicationDeclaration(ar, applicationDeclarationData)
              _ <- backUpSave4LaterInKeyStore(ar)
              businessPartnerDetails <- save4LaterService.mainStore.fetchBusinessCustomerDetails(ar)
              businessRegDetails <- save4LaterService.mainStore.fetchBusinessRegistrationDetails(ar)
              successResponse <- applicationService.sendApplication(ar)
              _ <- enrolService.enrolAWRS(successResponse,
                businessPartnerDetails.get,
                businessType,
                businessRegDetails.get.utr) // Calls ES8
            } yield {
              Redirect(controllers.routes.ConfirmationController.showApplicationConfirmation(false))
                .addAwrsRefToSession(successResponse.etmpFormBundleNumber)
            }

          }.recover {
            case error: DESValidationException => InternalServerError(views.html.error_template(Messages("awrs.application_des_validation.title"), Messages("awrs.application_des_validation.heading"), Messages("awrs.application_des_validation.message")))
            case error: DuplicateSubscriptionException => InternalServerError(views.html.error_template(Messages("awrs.application_duplicate_request.title"), Messages("awrs.application_duplicate_request.heading"), Messages("awrs.application_duplicate_request.message")))
            case error: PendingDeregistrationException => InternalServerError(views.html.error_template(Messages("awrs.application_pending_deregistration.title"), Messages("awrs.application_pending_deregistration.heading"), Messages("awrs.application_pending_deregistration.message")))
            case error: GovernmentGatewayException => InternalServerError(views.html.error_template(Messages("awrs.application_government_gateway_error.title"), Messages("awrs.application_government_gateway_error.heading"), Messages("awrs.application_government_gateway_error.message")))
            case error =>
              warn("Exception encountered in Application Declaration Controller :\n" + error.getStackTrace.mkString("\n"))
              throw error
          }
        )
      }
    }
  }


}

object ApplicationDeclarationController extends ApplicationDeclarationController {

  override val authConnector = FrontendAuthConnector
  override val save4LaterService = Save4LaterService
  override val enrolService = EnrolService
  override val applicationService = ApplicationService
  override val keyStoreService = KeyStoreService
  val signInUrl = ExternalUrls.signIn

  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration
}
