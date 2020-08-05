/*
 * Copyright 2020 HM Revenue & Customs
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
import controllers.auth.StandardAuthRetrievals
import controllers.util.{JourneyPage, RedirectParam, SaveAndRoutable}
import forms.BusinessContactsForm._
import javax.inject.Inject
import play.api.mvc._
import services.DataCacheKeys._
import services.{DeEnrolService, EmailVerificationService, Save4LaterService}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AccountUtils
import views.view_application.helpers.{EditSectionOnlyMode, LinearViewMode, ViewApplicationType}

import scala.concurrent.{ExecutionContext, Future}

class BusinessContactsController @Inject()(val mcc: MessagesControllerComponents,
                                           emailVerificationService: EmailVerificationService,
                                           val save4LaterService: Save4LaterService,
                                           val deEnrolService: DeEnrolService,
                                           val authConnector: DefaultAuthConnector,
                                           val auditable: Auditable,
                                           val accountUtils: AccountUtils,
                                           implicit val applicationConfig: ApplicationConfig,
                                           template: views.html.awrs_business_contacts
                                          ) extends FrontendController(mcc) with JourneyPage with SaveAndRoutable {

  override implicit val ec: ExecutionContext = mcc.executionContext
  override val section: String = businessContactsName
  val signInUrl: String = applicationConfig.signIn

  def showBusinessContacts(isLinearMode: Boolean): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit ar =>
      restrictedAccessCheck {
        implicit val viewApplicationType: ViewApplicationType = if (isLinearMode) {
          LinearViewMode
        } else {
          EditSectionOnlyMode
        }

        val businessType = request.getBusinessType
        save4LaterService.mainStore.fetchBusinessCustomerDetails(ar).flatMap {
          case Some(businessCustomerDetails) =>
            save4LaterService.mainStore.fetchBusinessContacts(ar).flatMap {
              case Some(data) => Future.successful(Ok(template(accountUtils.hasAwrs(ar.enrolments),
                businessType,
                businessCustomerDetails.businessAddress,
                businessContactsForm.form.fill(data))))
              case _ =>
                val firstTimeForm = businessContactsForm.form
                Future.successful(Ok(template(accountUtils.hasAwrs(ar.enrolments),
                  businessType,
                  businessCustomerDetails.businessAddress,
                  firstTimeForm)))
            }
          case None => showErrorPage
        }
      }
    }
  }

  override def save(id: Int, redirectRoute: (Option[RedirectParam], Boolean) => Future[Result], viewApplicationType: ViewApplicationType, isNewRecord: Boolean, authRetrievals: StandardAuthRetrievals)
          (implicit request: Request[AnyContent]): Future[Result] = {
    implicit val viewMode: ViewApplicationType = viewApplicationType
    businessContactsForm.bindFromRequest.fold(
      formWithErrors =>
        save4LaterService.mainStore.fetchBusinessCustomerDetails(authRetrievals).flatMap {
          case Some(businessCustomerDetails) =>
            Future.successful(BadRequest(template(accountUtils.hasAwrs(authRetrievals.enrolments), request.getBusinessType, businessCustomerDetails.businessAddress, formWithErrors)))
          case _ => showErrorPage
        }
      ,
      addressesAndContactDetailsData =>
        save4LaterService.mainStore.saveBusinessContacts(authRetrievals, addressesAndContactDetailsData) flatMap {
          _ =>
            if (applicationConfig.emailVerificationEnabled) {
              emailVerificationService.sendVerificationEmail(addressesAndContactDetailsData.email.get)
            }
            redirectRoute(Some(RedirectParam("No", id)), isNewRecord)
        }
    )
  }

}