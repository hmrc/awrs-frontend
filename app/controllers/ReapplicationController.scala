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
import connectors.AWRSNotificationConnector
import controllers.auth.AwrsController
import forms.AWRSEnums.ApplicationStatusEnum
import forms.ReapplicationForm._
import models.{ApplicationStatus, StatusNotification}
import org.joda.time.LocalDateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import services.{DeEnrolService, KeyStoreService, Save4LaterService}
import utils.{AccountUtils, AwrsSessionKeys, LoggingUtils}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.Future

object ReapplicationController extends ReapplicationController {
  override val save4LaterService = Save4LaterService
  override val authConnector = FrontendAuthConnector
  override val deEnrolService = DeEnrolService
  override val keyStoreService = KeyStoreService
  override val awrsNotificationConnector = AWRSNotificationConnector
}

trait ReapplicationController extends AwrsController with LoggingUtils {
  val deEnrolService: DeEnrolService
  val save4LaterService: Save4LaterService
  val keyStoreService: KeyStoreService
  val awrsNotificationConnector: AWRSNotificationConnector

  private final lazy val MinReturnHours = 24

  def show = async {
    implicit user =>
      implicit request =>
        val applicationStatus = ApplicationStatus(ApplicationStatusEnum.blankString, LocalDateTime.now())
        // check that the user has not returned within the specified amount of hours since being revoked or rejected

        val fmt: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss")
        awrsNotificationConnector.fetchNotificationCache flatMap {
          case Some(notification) => {
            val storedDateString: String = notification.storageDatetime.getOrElse("")
            if (storedDateString.isEmpty()) {
              Future.successful(Ok(views.html.awrs_reapplication_confirmation(reapplicationForm)))
            }
            else {
              val storedDate: LocalDateTime = fmt.parseLocalDateTime(storedDateString)
              storedDate.isBefore(LocalDateTime.now().minusHours(MinReturnHours)) match {
                case true => Future.successful(Ok(views.html.awrs_reapplication_confirmation(reapplicationForm)))
                case _ => Future.successful(InternalServerError(views.html.awrs_application_too_soon_error(applicationStatus)))
              }
            }
          }
          case _ => Future.successful(Ok(views.html.awrs_reapplication_confirmation(reapplicationForm)))
        }
  }

  def submit = async {
    implicit user =>
      implicit request =>
        reapplicationForm.bindFromRequest.fold(
          formWithErrors =>
            Future.successful(BadRequest(views.html.awrs_reapplication_confirmation(formWithErrors)))
          ,
          success => {
            success.answer.get match {
              case "Yes" => {
                for {
                  _ <- deEnrolService.deEnrolAWRS(AccountUtils.getAwrsRefNo.toString(), getBusinessName.get, getBusinessType.get)
                  _ <- deEnrolService.refreshProfile
                  _ <- save4LaterService.mainStore.removeAll
                  _ <- save4LaterService.api.removeAll
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
              }
              case _ => {
                Future.successful(Redirect(controllers.routes.HomeController.showOrRedirect(request.session.get(AwrsSessionKeys.sessionCallerId))))
              }
            }
          }
        )
  }
}
