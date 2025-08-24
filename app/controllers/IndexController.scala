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
import controllers.auth.AwrsController
import javax.inject.Inject
import models.FormBundleStatus._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import services.apis.AwrsAPI9
import services.{ApplicationService, DeEnrolService, IndexService, Save4LaterService}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{AccountUtils, AwrsSessionKeys}

import scala.language.postfixOps
import scala.concurrent.{ExecutionContext, Future}

class IndexController @Inject()(mcc: MessagesControllerComponents,
                                indexService: IndexService,
                                api9: AwrsAPI9,
                                applicationService: ApplicationService,
                                val save4LaterService: Save4LaterService,
                                val deEnrolService: DeEnrolService,
                                val authConnector: DefaultAuthConnector,
                                val auditable: Auditable,
                                val accountUtils: AccountUtils,
                                implicit val applicationConfig: ApplicationConfig,
                                template: views.html.awrs_index) extends FrontendController(mcc) with AwrsController {

  implicit val ec: ExecutionContext = mcc.executionContext
  val signInUrl: String = applicationConfig.signIn

  def showIndex(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { implicit ar =>
      restrictedAccessCheck {
        request.session.get("businessType").fold("")(x => x) match {
          case "" =>
            debug("No business Type found")
            Future.successful(Redirect(controllers.routes.HomeController.showOrRedirect(request.session.get(AwrsSessionKeys.sessionCallerId))) removeJouneyStartLocationFromSession)
          case businessType =>
            debug(s"Business Type : $businessType")
            for {
              businessPartnerDetails <- save4LaterService.mainStore.fetchBusinessCustomerDetails(ar)
              subscriptionStatus <- api9.getSubscriptionStatusFromCache
              awrsDataMap <- save4LaterService.mainStore.fetchAll(ar)
              applicationChangeFlag <- applicationService.hasAPI5ApplicationChanged(accountUtils.getUtr(ar), ar)
              sectionStatus <- indexService.getStatus(awrsDataMap, businessType, ar)
            } yield {
              val allSectionCompletedFlag = indexService.showContinueButton(sectionStatus)
              val showOneViewLink = indexService.showOneViewLink(sectionStatus)
              val isHappyPathEnrolment: Boolean = subscriptionStatus exists (result => if (result.formBundleStatus == Pending || result.formBundleStatus == Approved || result.formBundleStatus == ApprovedWithConditions) true else false)
              val businessName: Option[String] = businessPartnerDetails.map(b => b.businessName)
              if (businessName.isDefined) {
                Ok(template(
                  awrsRef = {
                    if (accountUtils.hasAwrs(ar.enrolments)) {
                      Some(accountUtils.getAwrsRefNo(ar.enrolments))
                    } else {
                      None
                    }
                  },
                  hasApplicationChanged = applicationChangeFlag,
                  allSectionComplete = allSectionCompletedFlag,
                  showOneViewLink = showOneViewLink,
                  businessName = businessName,
                  sectionStatus,
                  subscriptionStatus,
                  isHappyPathEnrolment
                )).addIndexBusinessNameToSession(businessName).removeJouneyStartLocationFromSession.addSectionStatusToSession(sectionStatus)
              } else {
                debug("No business Name found")
                Redirect(controllers.routes.HomeController.showOrRedirect(request.session.get(AwrsSessionKeys.sessionCallerId))) removeJouneyStartLocationFromSession
                }
            }
        }
      }
    }
  }

  def showLastLocation(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { implicit ar =>
      restrictedAccessCheck {
        Future.successful(Redirect(sessionUtil(request).getPreviousLocation.fold("/alcohol-wholesale-scheme/index")(x => x)))
      }
    }
  }

  def unauthorised(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction {
      _ => Future.successful(Unauthorized(applicationConfig.templateUnauthorised()))
    }
  }
}
