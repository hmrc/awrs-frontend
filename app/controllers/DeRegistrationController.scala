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

import audit.Auditable
import config.ApplicationConfig
import controllers.auth.AwrsController
import exceptions.DeEnrollException
import forms.AWRSEnums.BooleanRadioEnum._
import forms.AWRSEnums._
import forms.DeRegistrationConfirmationForm._
import forms.DeRegistrationForm._
import forms.DeRegistrationReasonForm._
import javax.inject.Inject
import models.FormBundleStatus._
import models._
import org.joda.time.LocalDateTime
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import services.apis.AwrsAPI10
import services.{DeEnrolService, EmailService, KeyStoreService, Save4LaterService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.AccountUtils
import utils.CacheUtil.cacheUtil

import scala.concurrent.{ExecutionContext, Future}

class DeRegistrationController @Inject()(mcc: MessagesControllerComponents,
                                         api10: AwrsAPI10,
                                         deEnrolService: DeEnrolService,
                                         emailService: EmailService,
                                         val keyStoreService: KeyStoreService,
                                         val save4LaterService: Save4LaterService,
                                         val authConnector: DefaultAuthConnector,
                                         val auditable: Auditable,
                                         val accountUtils: AccountUtils,
                                         implicit val applicationConfig: ApplicationConfig) extends FrontendController(mcc) with AwrsController {

  implicit val ec: ExecutionContext = mcc.executionContext
  val permittedStatusTypes: Set[FormBundleStatus] = Set[FormBundleStatus](Approved, ApprovedWithConditions)
  val signInUrl: String = applicationConfig.signIn

  private def statusPermission(permittedResult: Future[Result])(implicit request: Request[AnyContent]): Future[Result] =
    if (permittedStatusTypes.contains(getSessionStatus.getOrElse(models.FormBundleStatus.NotFound("")))) {
      permittedResult
    } else {
      showNotFoundPage
    }

  private def returnToIndexSubroutine(implicit hc: HeaderCarrier) =
    keyStoreService.deleteDeRegistrationDate flatMap {
      _ =>
        keyStoreService.deleteDeRegistrationReason flatMap {
          _ => Future.successful(Redirect(routes.IndexController.showIndex()))
        }
    }

  def showReason(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    restrictedAccessCheck {
      authorisedAction { ar =>
        statusPermission(
          // if reason exists then fill the form with the data
          keyStoreService.fetchDeRegistrationReason flatMap {
            case Some(data) => Future.successful(Ok(views.html.awrs_de_registration_reason(deRegistrationReasonForm.form.fill(data))))
            case _ => Future.successful(Ok(views.html.awrs_de_registration_reason(deRegistrationReasonForm.form)))
          }
        )
      }
    }
  }

  def submitReason(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { ar =>
      deRegistrationReasonForm.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(views.html.awrs_de_registration_reason(formWithErrors)))
        },
        deRegistrationReasonForm =>
          keyStoreService.saveDeRegistrationReason(deRegistrationReasonForm) flatMap {
            _ => Future.successful(Redirect(routes.DeRegistrationController.showDate()))
          }
      )
    }
  }

  def showDate(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    restrictedAccessCheck {
      authorisedAction { ar =>
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
    }
  }

  def submitDate(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { ar =>
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
  }

  def confirmationJourneyPrerequisiteCheck(callToAction: TupleDate => Future[Result])
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

  def showConfirm(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    restrictedAccessCheck {
      authorisedAction { ar =>
        confirmationJourneyPrerequisiteCheck((proposedEndDate: TupleDate) =>
          Future.successful(Ok(views.html.awrs_de_registration_confirm(deRegistrationConfirmationForm, proposedEndDate))))
      }
    }
  }

  def callToAction(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { ar =>
      confirmationJourneyPrerequisiteCheck((proposedEndDate: TupleDate) =>

        deRegistrationConfirmationForm.bindFromRequest.fold(
          formWithErrors =>
            Future.successful(BadRequest(views.html.awrs_de_registration_confirm(formWithErrors, proposedEndDate)))
          ,
          confirmationData => {
            val awrsRef = accountUtils.getAwrsRefNo(ar.enrolments)
            confirmationData.deRegistrationConfirmation match {
              case Some(YesString) =>

                // the calls are abstracted out to make it easier for rewiring and replacement of de-enroll call
                def success(): Future[Result] =
                  for {
                    deristrationDate <- keyStoreService.fetchDeRegistrationDate
                    cache <- save4LaterService.mainStore.fetchAll(ar)
                    _ <- emailService.sendCancellationEmail(cache.get.getBusinessContacts.get.email.get, deristrationDate)
                    _ <- save4LaterService.mainStore.removeAll(ar)
                    _ <- save4LaterService.api.removeAll(ar)
                    _ <- save4LaterService.mainStore.saveApplicationStatus(ar, ApplicationStatus(ApplicationStatusEnum.DeRegistered,
                      LocalDateTime.now()))
                  } yield Redirect(routes.DeRegistrationController.showConfirmation())

                def callAPI10(success: () => Future[Result]): Future[Result] = api10.deRegistration(ar) flatMap {
                  case Some(DeRegistrationType(Some(DeRegistrationSuccessResponseType(_)))) => success()
                  case _ =>
                    err("call to API 10 failed")
                    showErrorPage
                }: Future[Result]

                def callDeEnrol(success: () => Future[Result]) = {
                  val businessName = getBusinessName.fold("")(x => x)
                  val businessType = getBusinessType.fold("")(x => x)
                  deEnrolService.deEnrolAWRS(awrsRef, businessName, businessType) flatMap {
                    case true =>
                      success()
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
  }

  def showConfirmation(printFriendly: Boolean): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    restrictedAccessCheck {
      authorisedAction { ar =>
        confirmationJourneyPrerequisiteCheck((proposedEndDate: TupleDate) => Future.successful
        (Ok(views.html.awrs_de_registration_confirmation_evidence(proposedEndDate, printFriendly))))
      }
    }
  }

  def returnToIndex: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { ar =>
      returnToIndexSubroutine
    }
  }
}