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
import forms.AwrsEnrolmentUtrForm.awrsEnrolmentUtrForm
import models.{AwrsRegisteredPostcode, BusinessCustomerDetails}
import play.api.mvc._
import services.{BusinessMatchingService, DeEnrolService, EnrolService, KeyStoreService, LookupService}
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.authorisedEnrolments
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{AWRSFeatureSwitches, AccountUtils}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AwrsUtrController @Inject()(mcc: MessagesControllerComponents,
                                  val keyStoreService: KeyStoreService,
                                  val deEnrolService: DeEnrolService,
                                  val authConnector: DefaultAuthConnector,
                                  val auditable: Auditable,
                                  val accountUtils: AccountUtils,
                                  val businessMatchingService: BusinessMatchingService,
                                  val enrolService: EnrolService,
                                  val awrsFeatureSwitches: AWRSFeatureSwitches,
                                  implicit val applicationConfig: ApplicationConfig,
                                  template: views.html.awrs_utr
                                      ) extends FrontendController(mcc) with AwrsController {

  implicit val ec: ExecutionContext = mcc.executionContext
  val signInUrl: String = applicationConfig.signIn

  def showArwsUtrPage(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    btaAuthorisedAction { implicit ar =>
      restrictedAccessCheck {
        if (awrsFeatureSwitches.enrolmentJourney().enabled) {
          val isSA = accountUtils.isSaAccount(ar.enrolments).getOrElse(false)
          keyStoreService.fetchAwrsEnrolmentUtr flatMap {
            case Some(utr) => Future.successful(Ok(template(awrsEnrolmentUtrForm.form.fill(utr), isSA)))
            case _ => Future.successful(Ok(template(awrsEnrolmentUtrForm.form, isSA)))
          }
        } else Future.successful(NotFound)
      }
    }
  }

  def saveAndContinue(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    btaAuthorisedAction { implicit ar =>
      restrictedAccessCheck {
        if (awrsFeatureSwitches.enrolmentJourney().enabled) {
          val isSA = accountUtils.isSaAccount(ar.enrolments).getOrElse(false)
          awrsEnrolmentUtrForm.bindFromRequest.fold(
            formWithErrors => Future.successful(BadRequest(template(formWithErrors))),
            utr => for {
              sr <- keyStoreService.fetchAwrsUrnSearchResult
              pc <- keyStoreService.fetchAwrsRegisteredPostcode
            } yield {
              (sr, pc) match {
                case (Some(result), Some(registeredPostcode)) =>
                  businessMatchingService.isValidUTRandPostCode(utr.utr, registeredPostcode, ar, isSA).flatMap { matched =>
                  if (matched) {
                    //Deenrollment??
                    val businessType = if(isSA) "SOP" else "CT"
                    enrolService.enrolAWRS(result.results.head.awrsRef, registeredPostcode, utr, businessType) flatMap { re

                    }

                    //                        enrolService.enrolAWRS()
                    Future.successful(Ok(template(awrsEnrolmentUtrForm.form, isSA)))
                  } else Future.successful(Redirect(routes.AwrsUrnKickoutController.showURNKickOutPage))

                }

                case _ => Future.successful(Redirect(routes.AwrsUrnKickoutController.showURNKickOutPage))
              }

            })
        } else Future.successful(NotFound)
      }
    }
  }

}