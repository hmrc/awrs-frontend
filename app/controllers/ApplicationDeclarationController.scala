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
import audit.Auditable
import config.ApplicationConfig
import connectors.AwrsDataCacheConnector
import controllers.auth.AwrsController
import exceptions._
import forms.ApplicationDeclarationForm._
import javax.inject.Inject
import models.FormBundleStatus
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import services._
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.AccountUtils

import scala.concurrent.{ExecutionContext, Future}

class ApplicationDeclarationController @Inject()(enrolService: EnrolService,
                                                 applicationService: ApplicationService,
                                                 mcc: MessagesControllerComponents,
                                                 val save4LaterService: Save4LaterService,
                                                 val keyStoreService: KeyStoreService,
                                                 val authConnector: DefaultAuthConnector,
                                                 val auditable: Auditable,
                                                 val accountUtils: AccountUtils,
                                                 val mainStoreSave4LaterConnector: AwrsDataCacheConnector,
                                                 implicit val applicationConfig: ApplicationConfig) extends FrontendController(mcc) with AwrsController with DataCacheService {

  val signInUrl: String = applicationConfig.signIn
  implicit val ec: ExecutionContext = mcc.executionContext

  def isEnrolledApplicant(implicit request: Request[AnyContent], messages: Messages): Boolean = {
    val validStatus: Set[FormBundleStatus] = Set(Pending, Approved, ApprovedWithConditions)
    getSessionStatus exists validStatus
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
      if (accountUtils.hasAwrs(ar.enrolments)) {
        applicationDeclarationForm.bindFromRequest.fold(
          formWithErrors => Future.successful(BadRequest(views.html.awrs_application_declaration(formWithErrors, isEnrolledApplicant))),
          applicationDeclarationData => {
            for {
              savedDeclaration <- save4LaterService.mainStore.saveApplicationDeclaration(ar, applicationDeclarationData)
              _ <- backUpSave4LaterInKeyStore(ar)
              successResponse <- applicationService.updateApplication(ar)
            } yield Redirect(controllers.routes.ConfirmationController.showApplicationUpdateConfirmation(false)).addAwrsRefToSession(successResponse.etmpFormBundleNumber)
          }.recover {
            case error: ResubmissionException => InternalServerError(views.html.error_template(request.messages("awrs.application_resubmission_error.title"), request.messages("awrs.application_resubmission_error.heading"), request.messages("awrs.application_resubmission_error.message")))
            case error: DESValidationException => InternalServerError(views.html.error_template(request.messages("awrs.application_des_validation.title"), request.messages("awrs.application_des_validation.heading"), request.messages("awrs.application_des_validation.message")))
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
            case error: DESValidationException => InternalServerError(views.html.error_template(request.messages("awrs.application_des_validation.title"), request.messages("awrs.application_des_validation.heading"), request.messages("awrs.application_des_validation.message")))
            case error: DuplicateSubscriptionException => InternalServerError(views.html.error_template(request.messages("awrs.application_duplicate_request.title"), request.messages("awrs.application_duplicate_request.heading"), request.messages("awrs.application_duplicate_request.message")))
            case error: PendingDeregistrationException => InternalServerError(views.html.error_template(request.messages("awrs.application_pending_deregistration.title"), request.messages("awrs.application_pending_deregistration.heading"), request.messages("awrs.application_pending_deregistration.message")))
            case error: GovernmentGatewayException => InternalServerError(views.html.error_template(request.messages("awrs.application_government_gateway_error.title"), request.messages("awrs.application_government_gateway_error.heading"), request.messages("awrs.application_government_gateway_error.message")))
            case error =>
              warn("Exception encountered in Application Declaration Controller :\n" + error.getStackTrace.mkString("\n"))
              throw error
          }
        )
      }
    }
  }


}
