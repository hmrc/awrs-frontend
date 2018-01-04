/*
 * Copyright 2018 HM Revenue & Customs
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
import controllers.auth.AwrsController
import forms.BusinessTypeForm._
import models.{BusinessType, NewApplicationType}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.Save4LaterService
import services.apis.AwrsAPI5
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.AccountUtils
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.concurrent.Future
import uk.gov.hmrc.http.InternalServerException

trait BusinessTypeController extends AwrsController with AccountUtils {

  val save4LaterService: Save4LaterService
  val api5: AwrsAPI5

  private def standardApi5Journey(implicit user: AuthContext, request: Request[AnyContent]): Future[Result] =
    for {
      subscriptionType <- api5.retrieveApplication
      Some(businessType) <- save4LaterService.mainStore.fetchBusinessType
      businessCustomerDetails <- save4LaterService.mainStore.fetchBusinessCustomerDetails
    } yield {
      businessCustomerDetails match {
        // the business customer details populated by the API 5 call should have been persisted by the HomeController by this point.
        case Some(details) =>
          debug("Business Details found: " + details)
          Redirect(controllers.routes.ApplicationStatusController.showStatus(mustShow = false)) addBusinessTypeToSession businessType addBusinessNameToSession details.businessName
        case None => throw new InternalServerException("API5 journey, no businessCustomerDetails found")
      }
    }

  private def api4Journey(implicit user: AuthContext, request: Request[AnyContent]): Future[Result] =
    for {
      Some(bcd) <- save4LaterService.mainStore.fetchBusinessCustomerDetails
      _ <- save4LaterService.mainStore.saveNewApplicationType(NewApplicationType(Some(true)))
      businessType <- save4LaterService.mainStore.fetchBusinessType
    } yield {
      val display = (form: Form[BusinessType]) => Ok(views.html.awrs_business_type(form, bcd.businessName, bcd.isAGroup, AccountUtils.isSaAccount(), AccountUtils.isOrgAccount())) addBusinessNameToSession bcd.businessName
      businessType match {
        case Some(data) => display(businessTypeForm.fill(data)) addBusinessTypeToSession data
        case _ => display(businessTypeForm)
      }
    }

  // showBusinessType is added to enable users who had submitted the wrong legal entities to correct them post submission.
  // they will have to manually enter the amendment url in order to access this feature
  def showBusinessType(showBusinessType: Boolean = false): Action[AnyContent] = asyncRestrictedAccess {
    implicit user => implicit request =>
      hasAwrs match {
        case true if !showBusinessType => standardApi5Journey
        // if showBusinessType is true then the users are directed into the api4 journey where they can change the legal entity
        case _ => api4Journey
      }
  }

  // this methods api4 or change legal entity journeys.
  def saveAndContinue: Action[AnyContent] = async {
    implicit user => implicit request =>
      save4LaterService.mainStore.fetchBusinessCustomerDetails flatMap {
        case Some(businessDetails) =>
          businessTypeForm.bindFromRequest.fold(
            formWithErrors => Future.successful(BadRequest(views.html.awrs_business_type(formWithErrors, businessDetails.businessType.fold("")(x => x), businessDetails.isAGroup, AccountUtils.isSaAccount(), AccountUtils.isOrgAccount())) addBusinessNameToSession businessDetails.businessName),
            businessTypeData =>
              save4LaterService.mainStore.saveBusinessType(businessTypeData) map { _ =>
                val legalEntity = businessTypeData.legalEntity.getOrElse("SOP")
                val nextPage = businessDetails.isAGroup match {
                  case true => Redirect(controllers.routes.GroupDeclarationController.showGroupDeclaration())
                  case _ => Redirect(controllers.routes.IndexController.showIndex())
                }
                nextPage addBusinessTypeToSession legalEntity addBusinessNameToSession businessDetails.businessName
              }
          )
      }
  }

}

object BusinessTypeController extends BusinessTypeController {
  override val authConnector = FrontendAuthConnector
  override val save4LaterService = Save4LaterService
  override val api5 = AwrsAPI5
}
