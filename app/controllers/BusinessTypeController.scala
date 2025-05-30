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
import forms.BusinessTypeForm._
import models.{BusinessType, NewApplicationType}
import play.api.data.Form
import play.api.mvc._
import services.apis.AwrsAPI5
import services.{CheckEtmpService, DeEnrolService, Save4LaterService}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AccountUtils

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BusinessTypeController @Inject()(mcc: MessagesControllerComponents,
                                       api5: AwrsAPI5,
                                       val save4LaterService: Save4LaterService,
                                       val deEnrolService: DeEnrolService,
                                       val authConnector: DefaultAuthConnector,
                                       val auditable: Auditable,
                                       val accountUtils: AccountUtils,
                                       val checkEtmpService: CheckEtmpService,
                                       implicit val applicationConfig: ApplicationConfig,
                                       template: views.html.awrs_business_type
                                      ) extends FrontendController(mcc) with AwrsController {

  implicit val ec: ExecutionContext = mcc.executionContext
  val signInUrl: String = applicationConfig.signIn
  private def standardApi5Journey(authRetrievals: StandardAuthRetrievals)(implicit request: Request[AnyContent]): Future[Result] =
    for {
      _ <- api5.retrieveApplication(authRetrievals)
      Some(businessType) <- save4LaterService.mainStore.fetchBusinessType(authRetrievals)
      businessCustomerDetails <- save4LaterService.mainStore.fetchBusinessCustomerDetails(authRetrievals)
    } yield {
      businessCustomerDetails match {
        // the business customer details populated by the API 5 call must have been persisted by the HomeController by this point.
        case Some(details) =>
          debug("Business Details found: " + details)
          Redirect(controllers.routes.ApplicationStatusController.showStatus(mustShow = false)) addBusinessTypeToSession businessType addBusinessNameToSession details.businessName
        case None => throw new InternalServerException("API5 journey, no businessCustomerDetails found")
      }
    }

  private def api4Journey(authRetrievals: StandardAuthRetrievals)(implicit request: Request[AnyContent]): Future[Result] =
    authorisedAction { ar =>
      for {
        Some(bcd) <- save4LaterService.mainStore.fetchBusinessCustomerDetails(authRetrievals)
        _ <- save4LaterService.mainStore.saveNewApplicationType(NewApplicationType(Some(true)), authRetrievals)
        businessType <- save4LaterService.mainStore.fetchBusinessType(authRetrievals)
      } yield {
        val display = (form: Form[BusinessType]) => Ok(template(form, bcd.businessName, bcd.isAGroup, accountUtils.isSaAccount(ar.enrolments), accountUtils.isOrgAccount(authRetrievals))) addBusinessNameToSession bcd.businessName

        businessType match {
          case Some(data) => display(businessTypeForm.fill(data)) addBusinessTypeToSession data
          case _ => display(businessTypeForm)
        }
      }
    }

  // showBusinessType is added to enable users who had submitted the wrong legal entities to correct them post submission.
  // they will have to manually enter the amendment url in order to access this feature
  def showBusinessType(showBusinessType: Boolean = false): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { implicit ar =>
      restrictedAccessCheck {
        if (accountUtils.hasAwrs(ar.enrolments)) {
          standardApi5Journey(ar)
        } else {
          api4Journey(ar)
        }
      }
    }
  }

  // this methods api4 or change legal entity journeys.
  def saveAndContinue(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedAction { ar =>
      save4LaterService.mainStore.fetchBusinessCustomerDetails(ar) flatMap {
        case Some(businessDetails) =>
          validateBusinessType(businessTypeForm.bindFromRequest()).fold(
            formWithErrors => Future.successful(BadRequest(template(formWithErrors, businessDetails.businessType.fold("")(x => x), businessDetails.isAGroup, accountUtils.isSaAccount(ar.enrolments), accountUtils.isOrgAccount(ar))) addBusinessNameToSession businessDetails.businessName),
            businessTypeData =>
              save4LaterService.mainStore.saveBusinessType(businessTypeData, ar) flatMap { _ =>
                val legalEntity = businessTypeData.legalEntity.getOrElse("SOP")
                checkEtmpService.validateBusinessDetails(businessDetails, legalEntity) flatMap { result =>
                  val nextPage = if (result) {
                    logger.info("[BusinessTypeController][saveAndContinue] Upserted details and enrolments to EACD")
                    authorisedAction { updatedRetrievals =>
                      standardApi5Journey(updatedRetrievals)
                    }
                  } else {
                    {
                      if (businessDetails.isAGroup) {
                        Future.successful(Redirect(controllers.routes.GroupDeclarationController.showGroupDeclaration))
                      } else {
                        Future.successful(Redirect(controllers.routes.IndexController.showIndex))
                      }
                    } map { result =>
                      result addBusinessTypeToSession legalEntity addBusinessNameToSession businessDetails.businessName
                    }
                  }

                  nextPage
                }
              }
          )
        case _ => throw new InternalServerException("no businessCustomerDetails found")
      }
    }
  }
}
