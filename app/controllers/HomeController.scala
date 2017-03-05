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
import controllers.auth.{AwrsController, ExternalUrls}
import models.{ApplicationStatus, BusinessCustomerDetails}
import org.joda.time.LocalDateTime
import play.api.mvc.{AnyContent, Request, Result}
import services._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{AccountUtils, AwrsSessionKeys}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.libs.json
import play.api.libs.json.JsResultException
import play.api.mvc.Action

import scala.concurrent.Future

trait HomeController extends AwrsController with AccountUtils {

  private final lazy val MinReturnHours = 24

  val businessCustomerService: BusinessCustomerService
  implicit val save4LaterService: Save4LaterService
  val modelUpdateService: ModelUpdateService

  def api5Journey(callerId: Option[String])(implicit user: AuthContext, request: Request[AnyContent]): Future[Result] = {
    debug("API5 journey triggered")
    gotoBusinessTypePage(callerId)
  }

  private def gotoBusinessTypePage(callerId: Option[String])(implicit user: AuthContext, request: Request[AnyContent]) = callerId match {
    case Some(id) => Future.successful(Redirect(controllers.routes.BusinessTypeController.showBusinessType(false)).addingToSession(AwrsSessionKeys.sessionCallerId -> id))
    case _ => Future.successful(Redirect(controllers.routes.BusinessTypeController.showBusinessType(false)))
  }

  def api4Journey(callerId: Option[String])(implicit user: AuthContext, request: Request[AnyContent]): Future[Result] = {

    save4LaterService.mainStore.fetchBusinessCustomerDetails flatMap {
      case Some(data) => {

        data.safeId.isEmpty match {
          case true => Future.successful(Redirect(ExternalUrls.businessCustomerStartPage))
          case _ => gotoBusinessTypePage(callerId)
        }
      }
      case _ =>
        businessCustomerService.getReviewBusinessDetails[BusinessCustomerDetails] flatMap {
          case Some(data) =>
            save4LaterService.mainStore.saveBusinessCustomerDetails(data) flatMap {
              _ => gotoBusinessTypePage(callerId)
            }
          case _ => Future.successful(Redirect(ExternalUrls.businessCustomerStartPage))
        }
    }
  }
  def showOrRedirect(callerId: Option[String] = None): Action[AnyContent] = async {
    implicit user => implicit request => {
      start(callerId)
    }.recoverWith {
      case e: JsResultException => {
        if (AccountUtils.hasAwrs) {
          save4LaterService.mainStore.removeAll
          save4LaterService.api.removeAll
          start(callerId)
        } else {
          save4LaterService.mainStore.removeAll
          start(callerId)
        }
      }
      case _ => {
        showErrorPage
      }
    }
  }

  private def start(callerId: Option[String] = None)(implicit user:AuthContext,request: Request[AnyContent]) = {

    save4LaterService.mainStore.fetchApplicationStatus flatMap {
      case Some(data) => {
        checkValidApplicationStatus(data, callerId)
      }
      case _ => {
        startJourney(callerId)
      }
    }
  }

  def checkValidApplicationStatus(applicationStatus: ApplicationStatus, callerId: Option[String])(implicit user: AuthContext, request: Request[AnyContent]): Future[Result] =
  // check that the user has not returned within the specified amount of hours since de-registering or withdrawing their application
    applicationStatus.updatedDate.isBefore(LocalDateTime.now().minusHours(MinReturnHours)) match {
      case true => startJourney(callerId)
      case _ => Future.successful(InternalServerError(views.html.awrs_application_too_soon_error(applicationStatus)))
    }

  def startJourney(callerId: Option[String])(implicit user: AuthContext, request: Request[AnyContent]): Future[Result] =
    modelUpdateService.ensureAllModelsAreUpToDate.flatMap {
      case true =>
        AccountUtils.hasAwrs match {
          case true => api5Journey(callerId)
          case false => api4Journey(callerId)
        }
      case _ => showErrorPage
    }

}

object HomeController extends HomeController {
  override val authConnector = FrontendAuthConnector
  override val businessCustomerService = BusinessCustomerService
  override val save4LaterService = Save4LaterService
  /* TODO save4later update for AWRS-1800 to be replaced by NoUpdatesRequired after 28 days*/
  override val modelUpdateService = UpdateRequired //NoUpdatesRequired
}
