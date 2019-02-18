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

import java.text.SimpleDateFormat
import java.util.Calendar

import config.FrontendAuthConnector
import controllers.auth.AwrsController
import forms.AWRSEnums.BooleanRadioEnum
import models.BusinessDetails
import services.{KeyStoreService, Save4LaterService}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, InternalServerException }

trait ConfirmationController extends AwrsController {

  val save4LaterService: Save4LaterService
  val keystoreService: KeyStoreService

  // because the nature of visiting this page deletes all data in save4later
  // this method caches the result of isNewBusiness into the keystore so that when this page is loaded within the session
  // it wouldn't error out
  def isNewBusiness(implicit user: AuthContext, hc: HeaderCarrier): Future[Boolean] = {

    val err = () => throw new InternalServerException("Unexpected error when evaluating if the application is a new business")

    lazy val evalIsNewBusiness = save4LaterService.mainStore.fetchBusinessDetails flatMap {
      case Some(data: BusinessDetails) => data.newAWBusiness.get.newAWBusiness match {
        case BooleanRadioEnum.YesString => Future.successful(true)
        case BooleanRadioEnum.NoString => Future.successful(false)
        case data@_ => err()
      }
      case _ => err()
    }

    keystoreService.fetchIsNewBusiness flatMap {
      case Some(bool) => Future.successful(bool)
      case None => evalIsNewBusiness flatMap {
        case bool => keystoreService.saveIsNewBusiness(bool) flatMap (_ => Future.successful(bool))
      }
    }
  }

  def showApplicationConfirmation(printFriendly: Boolean) = asyncRestrictedAccess {
    implicit user => implicit request =>
      isNewBusiness flatMap {
        isNewBusiness =>
          save4LaterService.mainStore.removeAll
          val format = new SimpleDateFormat("d MMMM y")
          val submissionDate = format.format(Calendar.getInstance().getTime)
          Future.successful(Ok(views.html.awrs_application_confirmation(submissionDate, isNewBusiness, printFriendly)) addLocation)
      }
  }

  def showApplicationUpdateConfirmation(printFriendly: Boolean) = asyncRestrictedAccess {
    implicit user => implicit request =>
      isNewBusiness flatMap {
        isNewBusiness =>
          save4LaterService.mainStore.removeAll
          save4LaterService.api.removeAll
          val format = new SimpleDateFormat("d MMMM y")
          val resubmissionDate = format.format(Calendar.getInstance().getTime)
          Future.successful(Ok(views.html.awrs_application_update_confirmation(resubmissionDate, isNewBusiness, printFriendly)) addLocation)
      }
  }
}

object ConfirmationController extends ConfirmationController {
  override val authConnector = FrontendAuthConnector
  override val save4LaterService = Save4LaterService
  override val keystoreService = KeyStoreService

}
