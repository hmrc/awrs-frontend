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
import exceptions.DeEnrollException
import forms.AWRSEnums.ApplicationStatusEnum
import forms.WithdrawalConfirmationForm._
import forms.WithdrawalReasonForm._
import models.FormBundleStatus.Pending
import models.{ApplicationStatus, WithdrawalResponse}
import org.joda.time.LocalDateTime
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request}
import services.apis.{AwrsAPI8, AwrsAPI9}
import services.{DeEnrolService, EmailService, KeyStoreService, Save4LaterService}
import utils.CacheUtil.cacheUtil
import utils.{AccountUtils, LoggingUtils}

import scala.concurrent.Future


object WithdrawalController extends WithdrawalController {
  override val save4LaterService = Save4LaterService
  override val authConnector = FrontendAuthConnector
  override val keyStoreService = KeyStoreService
  override val deEnrolService = DeEnrolService
  override val api8 = AwrsAPI8
  override val api9 = AwrsAPI9
  override val emailService = EmailService
  val signInUrl = ExternalUrls.signIn
}

trait WithdrawalController extends AwrsController with LoggingUtils {
  val api9: AwrsAPI9
  val api8: AwrsAPI8
  val keyStoreService: KeyStoreService
  val deEnrolService: DeEnrolService
  val save4LaterService: Save4LaterService
  val emailService: EmailService

  def showWithdrawalReasons: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { _ =>
      for {
        successResponse <- api9.getSubscriptionStatusFromCache
        keyStoreResponse <- keyStoreService.fetchWithdrawalReason
      } yield {
        (successResponse.exists(_.formBundleStatus == Pending), keyStoreResponse) match {
          case (true, Some(data)) =>
            Ok(views.html.awrs_withdrawal_reasons(withdrawalReasonForm.form.fill(data)))
          case (true, _) =>
            Ok(views.html.awrs_withdrawal_reasons(withdrawalReasonForm.form))
          case _ =>
            showErrorPageRaw
        }
      }
    }
  }

  def submitWithdrawalReasons: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { _ =>
      withdrawalReasonForm.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(views.html.awrs_withdrawal_reasons(formWithErrors)))
        },
        surveyDetails => {
          keyStoreService.saveWithdrawalReason(surveyDetails) map {
            _ => Redirect(controllers.routes.WithdrawalController.showConfirmWithdrawal())
          }
        }
      )
    }
  }

  def showConfirmWithdrawal: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { _ =>
      Future.successful(Ok(views.html.awrs_withdrawal_confirmation(withdrawalConfirmation)))
    }
  }

  def submitConfirmWithdrawal: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { ar =>
      withdrawalConfirmation.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(views.html.awrs_withdrawal_confirmation(formWithErrors)))
        ,
        withdrawalDetails =>
          withdrawalDetails.confirmation match {
            case Some("Yes") =>
              lazy val deEnrol = () => {
                val awrsRef = AccountUtils.getUtr(ar)
                val businessName = getBusinessName.fold("")(x => x)
                val businessType = getBusinessType.fold("")(x => x)
                deEnrolService.deEnrolAWRS(awrsRef, businessName, businessType)
              }
              val denrolResult = (for {
                reason <- keyStoreService.fetchWithdrawalReason
                api8Response <- api8.withdrawApplication(reason, ar)
                deleteReason <- keyStoreService.deleteWithdrawalReason
                deEnrolSuccess <- deEnrol()
              } yield (deEnrolSuccess, api8Response)).recover {
                case error: NoSuchElementException => showErrorPageRaw
                case error => throw error
              }
              denrolResult flatMap {
                case (true, api8Response: WithdrawalResponse) =>
                  for {
                    cache <- save4LaterService.mainStore.fetchAll(ar)
                    _ <- emailService.sendWithdrawnEmail(cache.get.getBusinessContacts.get.email.get)
                    _ <- save4LaterService.mainStore.removeAll(ar)
                    _ <- save4LaterService.mainStore.saveApplicationStatus(ar, ApplicationStatus(ApplicationStatusEnum.Withdrawn, LocalDateTime.now()))
                  } yield Redirect(controllers.routes.WithdrawalController.showWithdrawalConfirmation()) addProcessingDateToSession api8Response.processingDate
                case _ =>
                  err("call to government gateway de-enrol failed")
                  throw DeEnrollException("call to government gateway de-enrol failed")
              }
            case _ =>
              keyStoreService.deleteWithdrawalReason map {
                _ => Redirect(controllers.routes.IndexController.showIndex)
              }
          }
      )
    }
  }

  def showWithdrawalConfirmation(printFriendly: Boolean): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { _ =>
      Future.successful(Ok(views.html.awrs_withdrawal_confirmation_status(request.getProcessingDate.fold("")(x => x), printFriendly)))
    }
  }
}
