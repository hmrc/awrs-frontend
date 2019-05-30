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
import controllers.auth.{AwrsController, ExternalUrls, StandardAuthRetrievals}
import models.{ApplicationStatus, BusinessCustomerDetails}
import org.joda.time.LocalDateTime
import play.api.Play
import play.api.i18n.Messages.Implicits._
import play.api.libs.json.JsResultException
import play.api.mvc.{Action, AnyContent, Request, Result}
import services._
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AccountUtils, AwrsSessionKeys}

import scala.concurrent.Future

trait HomeController extends AwrsController with AccountUtils {

  private final lazy val MinReturnHours = 24

  val businessCustomerService: BusinessCustomerService
  implicit val save4LaterService: Save4LaterService
  val modelUpdateService: ModelUpdateService
  implicit lazy val app = Play.current


  private def awrsIdentifier(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): String = {
    val awrsIdentifier = if (AccountUtils.hasAwrs(authRetrievals.enrolments)) {
      AccountUtils.getAwrsRefNo(authRetrievals.enrolments)
    } else {
      save4LaterService.mainStore.fetchBusinessCustomerDetails(authRetrievals).map(_.get.safeId)
    }
    awrsIdentifier.toString
  }

  def api5Journey(callerId: Option[String])(implicit request: Request[AnyContent]): Future[Result] = {
    debug("API5 journey triggered")
    gotoBusinessTypePage(callerId)
  }

  private def gotoBusinessTypePage(callerId: Option[String])(implicit request: Request[AnyContent]) = callerId match {
    case Some(id) => Future.successful(Redirect(controllers.routes.BusinessTypeController.showBusinessType(false)).addingToSession(AwrsSessionKeys.sessionCallerId -> id))
    case _ => Future.successful(Redirect(controllers.routes.BusinessTypeController.showBusinessType(false)))
  }

  def api4Journey(authRetrievals: StandardAuthRetrievals, callerId: Option[String])(implicit request: Request[AnyContent]): Future[Result] = {

    save4LaterService.mainStore.fetchBusinessCustomerDetails(authRetrievals) flatMap {
      case Some(data) =>
        if (data.safeId.isEmpty) {
          Future.successful(Redirect(ExternalUrls.businessCustomerStartPage))
        } else {
          gotoBusinessTypePage(callerId)
        }
      case _ =>
        businessCustomerService.getReviewBusinessDetails[BusinessCustomerDetails] flatMap {
          case Some(data) =>
            save4LaterService.mainStore.saveBusinessCustomerDetails(authRetrievals, data) flatMap {
              _ => gotoBusinessTypePage(callerId)
            }
          case _ => Future.successful(Redirect(ExternalUrls.businessCustomerStartPage))
        }
    }
  }

  def showOrRedirect(callerId: Option[String] = None): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { authRetrievals =>
      chooseScenario(callerId, authRetrievals) recoverWith {
        case _: JsResultException =>
          if (AccountUtils.hasAwrs(authRetrievals.enrolments)) {
            save4LaterService.mainStore.removeAll(authRetrievals)
            save4LaterService.api.removeAll(authRetrievals)
            chooseScenario(callerId, authRetrievals)
          } else {
            save4LaterService.mainStore.removeAll(authRetrievals)
            chooseScenario(callerId, authRetrievals)
          }
        case error@_ =>
          warn("Exception encountered in Home Controller: " + awrsIdentifier(authRetrievals) + " \nERROR: " + error)
          showErrorPage
      }
    }
  }

  private def chooseScenario(callerId: Option[String] = None, authRetrievals: StandardAuthRetrievals)(implicit request: Request[AnyContent]) = {
    save4LaterService.mainStore.fetchApplicationStatus(authRetrievals) flatMap {
      case Some(data) =>
        checkValidApplicationStatus(data, callerId, authRetrievals)
      case _ =>
        startJourney(callerId, authRetrievals)
    }
  }

  def checkValidApplicationStatus(applicationStatus: ApplicationStatus, callerId: Option[String], authRetrievals: StandardAuthRetrievals)
                                 (implicit request: Request[AnyContent]): Future[Result] =
  // check that the user has not returned within the specified amount of hours since de-registering or withdrawing their application
    if (applicationStatus.updatedDate.isBefore(LocalDateTime.now().minusHours(MinReturnHours))) {
      startJourney(callerId, authRetrievals)
    } else {
      Future.successful(InternalServerError(views.html.awrs_application_too_soon_error(applicationStatus)))
    }

  def startJourney(callerId: Option[String], authRetrievals: StandardAuthRetrievals)(implicit request: Request[AnyContent]): Future[Result] =
    modelUpdateService.ensureAllModelsAreUpToDate(authRetrievals).flatMap {
      case true =>
        if (AccountUtils.hasAwrs(authRetrievals.enrolments)) {
          api5Journey(callerId)
        } else {
          api4Journey(authRetrievals, callerId)
        }
      case _ => showErrorPage
    }

}

object HomeController extends HomeController {
  override val authConnector = FrontendAuthConnector
  override val businessCustomerService = BusinessCustomerService
  override val save4LaterService = Save4LaterService
  /* TODO save4later update for AWRS-1800 to be replaced by NoUpdatesRequired after 28 days*/
  override val modelUpdateService = NoUpdatesRequired //NoUpdatesRequired
  val signInUrl = ExternalUrls.signIn
}
