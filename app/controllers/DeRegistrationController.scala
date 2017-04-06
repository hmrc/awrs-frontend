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
import exceptions.DeEnrollException
import forms.AWRSEnums.BooleanRadioEnum._
import forms.AWRSEnums._
import forms.DeRegistrationConfirmationForm._
import forms.DeRegistrationForm._
import forms.DeRegistrationReasonForm._
import models.FormBundleStatus._
import models._
import org.joda.time.LocalDateTime
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.apis.AwrsAPI10
import services.{DeEnrolService, EmailService, KeyStoreService, Save4LaterService}
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.AccountUtils
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.CacheUtil.cacheUtil

import scala.concurrent.Future

trait DeRegistrationController extends AwrsController with AccountUtils {

  val keyStoreService: KeyStoreService
  val permittedStatusTypes: Set[FormBundleStatus] = Set[FormBundleStatus](Approved, ApprovedWithConditions)
  val save4LaterService: Save4LaterService

  val api10: AwrsAPI10
  val deEnrolService: DeEnrolService
  val emailService: EmailService


  private def statusPermission(permittedResult: Future[Result])(implicit request: Request[AnyContent]): Future[Result] =
    permittedStatusTypes.contains(getSessionStatus.getOrElse(models.FormBundleStatus.NotFound(""))) match {
      case true => permittedResult
      case false => showNotFoundPage
    }

  private def returnToIndexSubroutine(implicit hc: HeaderCarrier) =
    keyStoreService.deleteDeRegistrationDate flatMap {
      _ => keyStoreService.deleteDeRegistrationReason flatMap {
        _ => Future.successful(Redirect(routes.IndexController.showIndex()))
      }
    }

  def showReason: Action[AnyContent] = asyncRestrictedAccess {
    implicit user => implicit request =>
      statusPermission(
        // if reason exists then fill the form with the data
        keyStoreService.fetchDeRegistrationReason flatMap {
          case Some(data) => Future.successful(Ok(views.html.awrs_de_registration_reason(deRegistrationReasonForm.form.fill(data))))
          case _ => Future.successful(Ok(views.html.awrs_de_registration_reason(deRegistrationReasonForm.form)))
        }
      )
  }

  def submitReason: Action[AnyContent] = async {
    implicit user => implicit request =>
      deRegistrationReasonForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(views.html.awrs_de_registration_reason(formWithErrors)))
        ,
        deRegistrationReasonForm =>
          keyStoreService.saveDeRegistrationReason(deRegistrationReasonForm) flatMap {
            _ => Future.successful(Redirect(routes.DeRegistrationController.showDate()))
          }
      )
  }

  def showDate: Action[AnyContent] = asyncRestrictedAccess {
    implicit user => implicit request =>
      statusPermission(
        keyStoreService.fetchDeRegistrationReason flatMap {
          case Some(reason) =>
            keyStoreService.fetchDeRegistrationDate flatMap {
              case Some(data) => Future.successful(Ok(views.html.awrs_de_registration(deRegistrationForm.fill(data))))
              case _ => Future.successful(Ok(views.html.awrs_de_registration(deRegistrationForm)))
            }
          case _ => Future.successful(Redirect(routes.DeRegistrationController.showReason()))
        }
      )
  }

  def submitDate: Action[AnyContent] = async {
    implicit user => implicit request =>
      deRegistrationForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(views.html.awrs_de_registration(formWithErrors)))
        ,
        deRegistrationFormData =>
          keyStoreService.saveDeRegistrationDate(deRegistrationFormData) flatMap {
            _ => Future.successful(Redirect(routes.DeRegistrationController.showConfirm()))
          }
      )
  }

  def confirmationJourneyPrerequisiteCheck(callToAction: (TupleDate) => Future[Result])
                                          (implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] =
    statusPermission(
      keyStoreService.fetchDeRegistrationReason flatMap {
        // if de-registration date has not been given then redirect back to the date page
        case None => Future.successful(Redirect(routes.DeRegistrationController.showReason()))
        case Some(reason) =>
          keyStoreService.fetchDeRegistrationDate flatMap {
            case None => Future.successful(Redirect(routes.DeRegistrationController.showDate()))
            // otherwise continue
            case Some(date) => callToAction(date.proposedEndDate)
          }
      })

  def showConfirm: Action[AnyContent] = asyncRestrictedAccess {
    implicit user => implicit request =>
      confirmationJourneyPrerequisiteCheck((proposedEndDate: TupleDate) =>
        Future.successful(Ok(views.html.awrs_de_registration_confirm(deRegistrationConfirmationForm, proposedEndDate))))
  }

  def callToAction: Action[AnyContent] = async {
    implicit user => implicit request =>
      confirmationJourneyPrerequisiteCheck((proposedEndDate: TupleDate) =>
        deRegistrationConfirmationForm.bindFromRequest.fold(
          formWithErrors =>
            Future.successful(BadRequest(views.html.awrs_de_registration_confirm(formWithErrors, proposedEndDate)))
          ,
          confirmationData => {
            val awrsRef = AccountUtils.getUtrOrName()
            confirmationData.deRegistrationConfirmation match {
              case Some(YesString) =>

                // the calls are abstracted out to make it easier for rewiring and replacement of de-enroll call
                def success(): Future[Result] =
                  for {
                    _ <- deEnrolService.refreshProfile
                    _ <- save4LaterService.mainStore.removeAll
                    _ <- save4LaterService.api.removeAll
                    _ <- save4LaterService.mainStore.saveApplicationStatus(ApplicationStatus(ApplicationStatusEnum.DeRegistered,
                      LocalDateTime.now()))
                    cache <- save4LaterService.mainStore.fetchAll
                    _ <- emailService.sendCancellationEmail(cache.get.getBusinessContacts.get.email.get)
                  } yield Redirect(routes.DeRegistrationController.showConfirmation())

                def callAPI10(success: () => Future[Result]): Future[Result] = api10.deRegistration() flatMap {
                  case Some(DeRegistrationType(Some(DeRegistrationSuccessResponseType(_)))) => success()
                  case _ =>
                    err("call to API 10 failed")
                    showErrorPage
                }: Future[Result]

                def callDeEnrol(success: () => Future[Result]) = {
                  val businessName = getBusinessName.fold("")(x => x)
                  val businessType = getBusinessType.fold("")(x => x)
                  deEnrolService.deEnrolAWRS(awrsRef, businessName, businessType) flatMap {
                    case (true) => success()
                    case _ =>
                      err("call to government gateway de-enrol failed")
                      Future.failed(DeEnrollException("call to government gateway de-enrol failed"))
                  }
                }.recover {
                  case error: NoSuchElementException => showErrorPageRaw
                  case error => throw error
                }

                // call routing
                callAPI10(() => callDeEnrol(success))
              case _ => returnToIndexSubroutine
            }
          }
        )
      )
  }

  def showConfirmation(printFriendly: Boolean): Action[AnyContent] = asyncRestrictedAccess {
    implicit user => implicit request =>
      confirmationJourneyPrerequisiteCheck((proposedEndDate: TupleDate) => Future.successful
      (Ok(views.html.awrs_de_registration_confirmation_evidence(proposedEndDate, printFriendly))))
  }

  def returnToIndex: Action[AnyContent] = async {
    implicit user => implicit request => returnToIndexSubroutine
  }

}

object DeRegistrationController extends DeRegistrationController {
  override val authConnector = FrontendAuthConnector
  override val keyStoreService = KeyStoreService
  override val api10 = AwrsAPI10
  override val deEnrolService = DeEnrolService
  override val save4LaterService = Save4LaterService
  override val emailService = EmailService
}
