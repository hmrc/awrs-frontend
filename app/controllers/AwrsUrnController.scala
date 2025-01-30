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
import forms.AwrsEnrollmentUrnForm.awrsEnrolmentUrnForm
import play.api.mvc._
import services.{DeEnrolService, KeyStoreService, LookupService}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{AWRSFeatureSwitches, AccountUtils}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AwrsUrnController @Inject()(mcc: MessagesControllerComponents,
                                  val keyStoreService: KeyStoreService,
                                  val deEnrolService: DeEnrolService,
                                  val authConnector: DefaultAuthConnector,
                                  val auditable: Auditable,
                                  val accountUtils: AccountUtils,
                                  lookupService: LookupService,
                                  implicit val applicationConfig: ApplicationConfig,
                                  template: views.html.awrs_urn
                                      ) extends FrontendController(mcc) with AwrsController {

  implicit val ec: ExecutionContext = mcc.executionContext
  val signInUrl: String = applicationConfig.signIn

  def showArwsUrnPage(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    btaAuthorisedAction { implicit ar =>
      println("********nhere " + AWRSFeatureSwitches.enrollmentJourney().enabled)
//      if (AWRSFeatureSwitches.enrollmentJourney().enabled) {
        keyStoreService.fetchAwrsEnrolmentUrn flatMap {
          case Some(awrsUrn) => Future.successful(Ok(template(awrsEnrolmentUrnForm.form.fill(awrsUrn))))
          case _ => Future.successful(Ok(template(awrsEnrolmentUrnForm.form)))
        }
//      } else Future.successful(NotFound)
    }
  }

  def saveAndContinue(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    btaAuthorisedAction {
      implicit ar =>
        awrsEnrolmentUrnForm.bindFromRequest.fold(
          formWithErrors => Future.successful(BadRequest(template(formWithErrors))),
          awrsUrn => {
            keyStoreService.saveAwrsEnrolmentUrn(awrsUrn) flatMap  {_=>
              lookupService.lookup(awrsUrn.awrsUrn).flatMap { result =>
                result match {
                  case Some(searchResult) => Future.successful(Ok(template(awrsEnrolmentUrnForm.form)))
                  case None => Future.successful(Ok(template(awrsEnrolmentUrnForm.form)))
                }
              }
            }
          }
        )
    }
  }

}