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
import controllers.auth.{AwrsController, AwrsRegistrationGovernmentGateway}
import models.FormBundleStatus._
import services.apis.AwrsAPI9
import services.{ApplicationService, IndexService, Save4LaterService}
import utils.{AccountUtils, AwrsSessionKeys}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.concurrent.Future

object IndexController extends IndexController {
  override val authConnector = FrontendAuthConnector
  override val save4LaterService = Save4LaterService
  override val indexService = IndexService
  override val api9 = AwrsAPI9
  override val applicationService = ApplicationService
}

trait IndexController extends AwrsController {

  val save4LaterService: Save4LaterService
  val indexService: IndexService
  val api9: AwrsAPI9
  val applicationService: ApplicationService

  def showIndex = asyncRestrictedAccess {
    implicit user => implicit request =>
      request.session.get("businessType").fold("")(x => x) match {
        case "" =>
          debug("No business Type found")
          Future.successful(Redirect(controllers.routes.HomeController.showOrRedirect(request.session.get(AwrsSessionKeys.sessionCallerId))) removeJouneyStartLocationFromSession)
        case businessType =>
          debug(s"Business Type : $businessType")
          println(s"\n\n\n\nBusiness Type : $businessType\n\n\n\n")
          for {
            businessPartnerDetails <- save4LaterService.mainStore.fetchBusinessCustomerDetails
            subscriptionStatus <- api9.getSubscriptionStatusFromCache
            awrsDataMap <- save4LaterService.mainStore.fetchAll
            applicationChangeFlag <- applicationService.hasAPI5ApplicationChanged(AccountUtils.getUtrOrName())
            sectionStatus <- indexService.getStatus(awrsDataMap, businessType)
          } yield {
            val allSectionCompletedFlag = indexService.showContinueButton(sectionStatus)
            val showOneViewLink = indexService.showOneViewLink(sectionStatus)
            val isHappyPathEnrollment: Boolean = subscriptionStatus exists (result => if (result.formBundleStatus == Pending || result.formBundleStatus == Approved || result.formBundleStatus == ApprovedWithConditions) true else false)
            println(s"\n\n\n\nScubscription Status Type : $subscriptionStatus\n\n\n\n")
            Ok(views.html.awrs_index(
              awrsRef = {AccountUtils.hasAwrs match {
                case true => Some(AccountUtils.getAwrsRefNo.toString())
                case _ => None
                }
              },
              hasApplicationChanged = applicationChangeFlag,
              allSectionComplete = allSectionCompletedFlag,
              showOneViewLink = showOneViewLink,
              businessPartnerDetails.get.businessName,
              sectionStatus,
              subscriptionStatus,
              isHappyPathEnrollment
            )).addBusinessNameToSession(businessPartnerDetails.get.businessName).removeJouneyStartLocationFromSession.addSectionStatusToSession(sectionStatus)
          }
      }
  }

  def showLastLocation = asyncRestrictedAccess {
    implicit user => implicit request =>
      Future.successful(Redirect(sessionUtil(request).getPreviousLocation.fold("/alcohol-wholesale-scheme/index")(x => x)))
  }

  def unauthorised = AuthenticatedBy(AwrsRegistrationGovernmentGateway, pageVisibility = GGConfidence).async {
    implicit user => implicit request => Future.successful(Unauthorized(views.html.unauthorised()))
  }
}
