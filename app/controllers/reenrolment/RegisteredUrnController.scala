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

package controllers.reenrolment

import audit.Auditable
import config.ApplicationConfig
import connectors.EnrolmentsConnector
import controllers.auth.AwrsController
import forms.reenrolment.RegisteredUrnForm.awrsEnrolmentUrnForm
import models.KnownFacts
import models.enrolment.EnrolmentResponse
import play.api.mvc._
import services.{DeEnrolService, KeyStoreService, LookupService}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{AWRSFeatureSwitches, AccountUtils}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegisteredUrnController @Inject() (mcc: MessagesControllerComponents,
                                         keyStoreService: KeyStoreService,
                                         val deEnrolService: DeEnrolService,
                                         val authConnector: DefaultAuthConnector,
                                         val auditable: Auditable,
                                         val accountUtils: AccountUtils,
                                         enrolmentsConnector: EnrolmentsConnector,
                                         lookupService: LookupService,
                                         awrsFeatureSwitches: AWRSFeatureSwitches,
                                         implicit val applicationConfig: ApplicationConfig,
                                         template: views.html.reenrolment.awrs_registered_urn)
    extends FrontendController(mcc)
    with AwrsController {

  implicit val ec: ExecutionContext = mcc.executionContext
  val signInUrl: String             = applicationConfig.signIn

  val validIds: List[String] = List(
    "XLAW00000200190",
    "XYAW00000200191",
    "XRAW00000200192",
    "XZAW00000200193",
    "XXAW00000200188",
    "XXAW00000200295",
    "XFAW00000200294",
    "XYAW00000200297",
    "XLAW00000200296",
    "XPAW00000200123",
    "XFAW00000200111",
    "XXAW00000200112",
    "XYAW00000200114",
    "XRAW00000200115",
    "XZAW00000200116",
    "XAAW00000200117",
    "XCAW00000200135",
    "XDAW00000200118",
    "XGAW00000200090",
    "XJAW00000200091",
    "XYAW00000200084",
    "XBAW00000200101",
    "XEAW00000200102",
    "XQAW00000200106",
    "XTAW00000200107",
    "XHAW00000200128",
    "XCAW00000200110",
    "XVAW00000200125",
    "XEAW00000200127",
    "XWAW00000200134",
    "XPAW00000200093",
    "XSAW00000200094",
    "XVAW00000200095",
    "XKAW00000200129",
    "XKAW00000200130",
    "XQAW00000200132",
    "XGAW00000200119",
    "XEAW00000200097",
    "XHAW00000200098",
    "XKAW00000200099"
  )

  def showArwsUrnPage(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    enrolmentEligibleAuthorisedAction { implicit ar =>
      restrictedAccessCheck {
        if (awrsFeatureSwitches.enrolmentJourney().enabled) {
          keyStoreService.fetchAwrsEnrolmentUrn flatMap {
            case Some(awrsUrn) => Future.successful(Ok(template(awrsEnrolmentUrnForm.form.fill(awrsUrn))))
            case _             => Future.successful(Ok(template(awrsEnrolmentUrnForm.form)))
          }
        } else Future.successful(NotFound)
      }
    }
  }

  def saveAndContinue(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    enrolmentEligibleAuthorisedAction { implicit ar =>
      restrictedAccessCheck {
        if (awrsFeatureSwitches.enrolmentJourney().enabled) {
          awrsEnrolmentUrnForm
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(template(formWithErrors))),
              awrsUrn => {
                validIds.map { ids =>
                  val es20Resp = enrolmentsConnector.lookupEnrolments(KnownFacts(urn = ids))
                  es20Resp.map(resp => logger.info(s"es20 response for $ids - $resp"))
                }
                Future.successful(Ok("Done"))
              }
            )
        } else {
          Future.successful(NotFound)
        }
      }
    }
  }

}
