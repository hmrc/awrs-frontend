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

import _root_.models.FormBundleStatus._
import audit.Auditable
import config.ApplicationConfig
import connectors.AwrsDataCacheConnector
import controllers.auth.{AwrsController, StandardAuthRetrievals}
import exceptions._
import forms.ApplicationDeclarationForm._
import javax.inject.Inject
import models.FormBundleStatus
import play.api.mvc._
import services._
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AccountUtils

import scala.concurrent.{ExecutionContext, Future}

class ApplicationDeclarationController @Inject()(enrolService: EnrolService,
                                                 applicationService: ApplicationService,
                                                 mcc: MessagesControllerComponents,
                                                 val save4LaterService: Save4LaterService,
                                                 val keyStoreService: KeyStoreService,
                                                 val deEnrolService: DeEnrolService,
                                                 val authConnector: DefaultAuthConnector,
                                                 val auditable: Auditable,
                                                 val accountUtils: AccountUtils,
                                                 val mainStoreSave4LaterConnector: AwrsDataCacheConnector,
                                                 implicit val applicationConfig: ApplicationConfig,
                                                 template: views.html.awrs_application_declaration) extends FrontendController(mcc) with AwrsController with DataCacheService {

  val signInUrl: String = applicationConfig.signIn
  implicit val ec: ExecutionContext = mcc.executionContext

  def isEnrolledApplicant(implicit request: Request[AnyContent]): Boolean = {
    val validStatus: Set[FormBundleStatus] = Set(Pending, Approved, ApprovedWithConditions)
    getSessionStatus exists validStatus
  }

  def showApplicationDeclaration: Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit ar =>
      restrictedAccessCheck {
        // Confirm cached application data available
        save4LaterService.mainStore.fetchAll(ar).flatMap{
          case Some(_) =>
            save4LaterService.mainStore.fetchApplicationDeclaration(ar) map {
              case Some(data) => Ok(template(applicationDeclarationForm.form.fill(data), isEnrolledApplicant))
              case _ => Ok(template(applicationDeclarationForm.form, isEnrolledApplicant))
            }
          case None =>
            logger.warn(s"Arrived at Application declaration page with no cached application data, possible browser back after application, redirect to logout page")
            Future.successful(Redirect(routes.ApplicationController.logout))
        }
      }
    }
  }

  private def api5Application(ar: StandardAuthRetrievals)(implicit request: MessagesRequest[AnyContent]) = {
    applicationDeclarationForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(template(formWithErrors, isEnrolledApplicant))),
      applicationDeclarationData => {
        for {
          _ <- save4LaterService.mainStore.saveApplicationDeclaration(ar, applicationDeclarationData)
          _ <- backUpSave4LaterInKeyStore(ar)
          successResponse <- applicationService.updateApplication(ar)
        } yield Redirect(controllers.routes.ConfirmationController.showApplicationUpdateConfirmation()).addAwrsRefToSession(successResponse.etmpFormBundleNumber)
      }.recover {
        case _: ResubmissionException => InternalServerError(applicationConfig.templateError(request.messages("awrs.application_resubmission_error.title"), request.messages("awrs.application_resubmission_error.heading"), request.messages("awrs.application_resubmission_error.message")))
        case _: DESValidationException => InternalServerError(applicationConfig.templateError(request.messages("awrs.application_des_validation.title"), request.messages("awrs.application_des_validation.heading"), request.messages("awrs.application_des_validation.message")))
        case error =>
          warn("Exception encountered in Application Declaration Controller :\n" + error.getStackTrace.mkString("\n"))
          throw error
      }
    )
  }

  def sendApplication(): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { ar =>
      if (accountUtils.hasAwrs(ar.enrolments)) {
        api5Application(ar)
      } else {
        val businessType = getBusinessType.getOrElse("")
        applicationDeclarationForm.bindFromRequest.fold(
          formWithErrors => Future.successful(BadRequest(template(formWithErrors, isEnrolledApplicant))),
          applicationDeclarationData => {
            for {
              _ <- save4LaterService.mainStore.saveApplicationDeclaration(ar, applicationDeclarationData)
              _ <- backUpSave4LaterInKeyStore(ar)

              businessPartnerDetails <- save4LaterService.mainStore.fetchBusinessCustomerDetails(ar)
              businessRegDetails <- save4LaterService.mainStore.fetchBusinessRegistrationDetails(ar)
              successResponse <- applicationService.sendApplication(ar)
              _ <- enrolService.enrolAWRS(applicationService.getRegistrationReferenceNumber(successResponse),
                businessPartnerDetails.get,
                businessType,
                businessRegDetails.get.utr) // Calls ES8
            } yield {
              successResponse match {
                case Left(_)         =>
                  Redirect(controllers.routes.ConfirmationController.showApplicationConfirmation(selfHeal = true))
                case Right(response) =>
                  Redirect(controllers.routes.ConfirmationController.showApplicationConfirmation())
                    .addAwrsRefToSession(response.etmpFormBundleNumber)
              }
            }

          }.recover {
            case _: DESValidationException => InternalServerError(applicationConfig.templateError(request.messages("awrs.application_des_validation.title"), request.messages("awrs.application_des_validation.heading"), request.messages("awrs.application_des_validation.message")))
            case _: DuplicateSubscriptionException => InternalServerError(applicationConfig.templateError(request.messages("awrs.application_duplicate_request.title"), request.messages("awrs.application_duplicate_request.heading"), request.messages("awrs.application_duplicate_request.message")))
            case _: PendingDeregistrationException => InternalServerError(applicationConfig.templateError(request.messages("awrs.application_pending_deregistration.title"), request.messages("awrs.application_pending_deregistration.heading"), request.messages("awrs.application_pending_deregistration.message")))
            case _: GovernmentGatewayException => InternalServerError(applicationConfig.templateError(request.messages("awrs.application_government_gateway_error.title"), request.messages("awrs.application_government_gateway_error.heading"), request.messages("awrs.application_government_gateway_error.message")))
            case error =>
              warn("Exception encountered in Application Declaration Controller :\n" + error.getStackTrace.mkString("\n"))
              throw error
          }
        )
      }
    }
  }


}
