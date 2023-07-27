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

import audit.Auditable
import config.ApplicationConfig
import controllers.auth.{AwrsController, StandardAuthRetrievals}
import models.{ApplicationStatus, BusinessCustomerDetails}
import org.joda.time.LocalDateTime
import play.api.libs.json.JsResultException
import play.api.mvc._
import services._
import uk.gov.hmrc.auth.core.Assistant
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{AccountUtils, AwrsSessionKeys}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HomeController @Inject()(mcc: MessagesControllerComponents,
                               businessCustomerService: BusinessCustomerService,
                               val deEnrolService: DeEnrolService,
                               val checkEtmpService: CheckEtmpService,
                               val authConnector: DefaultAuthConnector,
                               val auditable: Auditable,
                               val accountUtils: AccountUtils,
                               implicit val save4LaterService: Save4LaterService,
                               implicit val applicationConfig: ApplicationConfig,
                               templateTooSoon: views.html.awrs_application_too_soon_error,
                               templateAssistantKickout: views.html.assistant_kickout) extends FrontendController(mcc) with AwrsController {

  private final lazy val MinReturnHours = 24

  implicit val ec: ExecutionContext = mcc.executionContext
  val signInUrl: String = applicationConfig.signIn

  private def awrsIdentifier(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): String = {
    val awrsIdentifier = if (accountUtils.hasAwrs(authRetrievals.enrolments)) {
      accountUtils.getAwrsRefNo(authRetrievals.enrolments)
    } else {
      save4LaterService.mainStore.fetchBusinessCustomerDetails(authRetrievals).map(_.get.safeId)
    }
    awrsIdentifier.toString
  }

  def api5Journey(callerId: Option[String])(implicit request: Request[AnyContent]): Future[Result] = {
    debug("API5 journey triggered")
    gotoBusinessTypePage(callerId)
  }

  private def gotoBusinessTypePage(callerId: Option[String])(implicit request: Request[AnyContent]): Future[Result] = {
    callerId match {
      case Some(id) => Future.successful(Redirect(controllers.routes.BusinessTypeController.showBusinessType(false)).addingToSession(AwrsSessionKeys.sessionCallerId -> id))
      case _ => Future.successful(Redirect(controllers.routes.BusinessTypeController.showBusinessType(false)))
    }
  }

  def businessCustomerDetails(authRetrievals: StandardAuthRetrievals, callerId: Option[String])(implicit request: Request[AnyContent]): Future[Option[BusinessCustomerDetails]] =
    save4LaterService.mainStore.fetchBusinessCustomerDetails(authRetrievals) flatMap {
      case Some(data) if data.safeId.isEmpty => Future.successful(None)
      case Some(data) => Future.successful(Some(data))
      case None =>
        businessCustomerService.getReviewBusinessDetails[BusinessCustomerDetails].flatMap {
          case Some(data) => save4LaterService.mainStore.saveBusinessCustomerDetails(authRetrievals, data).map(_ => Some(data))
          case _ => Future.successful(None)
        }
    }

  def api4Journey(authRetrievals: StandardAuthRetrievals, callerId: Option[String])(implicit request: Request[AnyContent]): Future[Result] =
    businessCustomerDetails(authRetrievals, callerId).flatMap {
      _.fold(Future.successful(Redirect(applicationConfig.businessCustomerStartPage))) { details =>
        checkEtmpService.checkUsersEnrolments(details, authRetrievals.plainTextCredId) flatMap { result =>
          result match {
            case Some(true) =>
              logger.info(s"User business already has AWRS enrolment")
              Future.successful(Redirect(controllers.routes.WrongAccountController.showWrongAccountPage(Some(details.businessName))))
            case _ =>
              gotoBusinessTypePage(callerId)
          }
        }
      }
    }

  def showOrRedirect(callerId: Option[String] = None): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { authRetrievals =>
      chooseScenario(callerId, authRetrievals) recoverWith {
        case _: JsResultException =>
          if (accountUtils.hasAwrs(authRetrievals.enrolments)) {
            save4LaterService.mainStore.removeAll(authRetrievals)
            save4LaterService.api.removeAll(authRetrievals)
            chooseScenario(callerId, authRetrievals)
          } else {
            save4LaterService.mainStore.removeAll(authRetrievals)
            chooseScenario(callerId, authRetrievals)
          }
        case error =>
          warn("Exception encountered in Home Controller: " + awrsIdentifier(authRetrievals) + " \nERROR: " + error.getMessage)
          showErrorPage
      }
    }
  }

  private def chooseScenario(callerId: Option[String], authRetrievals: StandardAuthRetrievals)
                            (implicit request: Request[AnyContent]): Future[Result] = {
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
      Future.successful(InternalServerError(templateTooSoon(applicationStatus)))
    }

  def startJourney(callerId: Option[String], authRetrievals: StandardAuthRetrievals)
                  (implicit request: Request[AnyContent]): Future[Result] =
    if (accountUtils.hasAwrs(authRetrievals.enrolments)) {
      api5Journey(callerId)(request)
    } else if (authRetrievals.role.equals(Some(Assistant))) {
      logger.warn(s"Assistant attempting to use AWRS without AWRS enrolment")
      Future.successful(Forbidden(templateAssistantKickout()))
    } else {
      api4Journey(authRetrievals, callerId)
    }
}
