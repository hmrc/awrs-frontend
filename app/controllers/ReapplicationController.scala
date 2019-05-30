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
import connectors.AWRSNotificationConnector
import controllers.auth.{AwrsController, ExternalUrls}
import forms.AWRSEnums.ApplicationStatusEnum
import forms.ReapplicationForm._
import models.ApplicationStatus
import org.joda.time.LocalDateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import play.api.Play
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.{DeEnrolService, KeyStoreService, Save4LaterService}
import utils.{AccountUtils, AwrsSessionKeys, LoggingUtils}

import scala.concurrent.Future

object ReapplicationController extends ReapplicationController {
  override val save4LaterService = Save4LaterService
  override val authConnector = FrontendAuthConnector
  override val deEnrolService = DeEnrolService
  override val keyStoreService = KeyStoreService
  override val awrsNotificationConnector = AWRSNotificationConnector
  val signInUrl = ExternalUrls.signIn
}

trait ReapplicationController extends AwrsController with LoggingUtils {
  val deEnrolService: DeEnrolService
  val save4LaterService: Save4LaterService
  val keyStoreService: KeyStoreService
  val awrsNotificationConnector: AWRSNotificationConnector
  implicit lazy val app = Play.current

  private final lazy val MinReturnHours = 24

  def show(): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { ar =>
      val applicationStatus = ApplicationStatus(ApplicationStatusEnum.blankString, LocalDateTime.now())
      // check that the user has not returned within the specified amount of hours since being revoked or rejected

      val fmt: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss")
      awrsNotificationConnector.fetchNotificationCache(ar) flatMap {
        case Some(notification) =>
          val storedDateString: String = notification.storageDatetime.getOrElse("")
          if (storedDateString.isEmpty) {
            Future.successful(Ok(views.html.awrs_reapplication_confirmation(reapplicationForm)))
          }
          else {
            val storedDate: LocalDateTime = fmt.parseLocalDateTime(storedDateString)
            if (storedDate.isBefore(LocalDateTime.now().minusHours(MinReturnHours))) {
              Future.successful(Ok(views.html.awrs_reapplication_confirmation(reapplicationForm)))
            } else {
              Future.successful(InternalServerError(views.html.awrs_application_too_soon_error(applicationStatus)))
            }
          }
        case _ => Future.successful(Ok(views.html.awrs_reapplication_confirmation(reapplicationForm)))
      }
    }
  }

  def submit(): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { ar =>
      reapplicationForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(views.html.awrs_reapplication_confirmation(formWithErrors)))
        ,
        success => {
          success.answer.get match {
            case "Yes" =>
              for {
                _ <- deEnrolService.deEnrolAWRS(AccountUtils.getAwrsRefNo(ar.enrolments), getBusinessName.get, getBusinessType.get)
                _ <- save4LaterService.mainStore.removeAll(ar)
                _ <- save4LaterService.api.removeAll(ar)
                _ <- keyStoreService.removeAll
              } yield Redirect(controllers.routes.HomeController.showOrRedirect(request.session.get(AwrsSessionKeys.sessionCallerId))).removingFromSession(
                AwrsSessionKeys.sessionBusinessType,
                AwrsSessionKeys.sessionBusinessName,
                AwrsSessionKeys.sessionPreviousLocation,
                AwrsSessionKeys.sessionCurrentLocation,
                AwrsSessionKeys.sessionStatusType,
                AwrsSessionKeys.sessionAwrsRefNo,
                AwrsSessionKeys.sessionJouneyStartLocation,
                AwrsSessionKeys.sessionSectionStatus
              )
            case _ =>
              Future.successful(Redirect(controllers.routes.HomeController.showOrRedirect(request.session.get(AwrsSessionKeys.sessionCallerId))))
          }
        }
      )
    }
  }
}
