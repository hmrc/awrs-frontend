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
import controllers.auth.AwrsController
import controllers.util.KnownFactsVerifier
import forms.reenrolment.RegisteredUtrForm.awrsEnrolmentUtrForm
import models.AwrsEnrolmentUtr
import play.api.mvc._
import services.{DeEnrolService, EnrolService, EnrolmentStoreProxyService, KeyStoreService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{AWRSFeatureSwitches, AccountUtils}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegisteredUtrController @Inject()(mcc: MessagesControllerComponents,
                                        keyStoreService: KeyStoreService,
                                        val deEnrolService: DeEnrolService,
                                        val authConnector: DefaultAuthConnector,
                                        val auditable: Auditable,
                                        val accountUtils: AccountUtils,
                                        enrolService: EnrolService,
                                        enrolmentStoreProxyService: EnrolmentStoreProxyService,
                                        awrsFeatureSwitches: AWRSFeatureSwitches,
                                        implicit val applicationConfig: ApplicationConfig,
                                        template: views.html.reenrolment.awrs_registered_utr
                                 ) extends FrontendController(mcc) with AwrsController {

  implicit val ec: ExecutionContext = mcc.executionContext
  val signInUrl: String = applicationConfig.signIn

  def showArwsUtrPage(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    enrolmentEligibleAuthorisedAction { implicit ar =>
      restrictedAccessCheck {
        val isSA = accountUtils.isSaAccount(ar.enrolments).getOrElse(false)
        keyStoreService.fetchAwrsEnrolmentUtr flatMap {
          case Some(utr) => Future.successful(Ok(template(awrsEnrolmentUtrForm.form.fill(utr), isSA)))
          case _ => Future.successful(Ok(template(awrsEnrolmentUtrForm.form, isSA)))
        }
      }
    }
  }


  private def getOrThrow[T](x: Option[T]): T = x.fold(throw new RuntimeException(s"No value found for ${x.getClass.getName} in keystore - exiting enrolment journey"))(identity)

  def saveAndContinue(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    enrolmentEligibleAuthorisedAction { implicit ar =>
      restrictedAccessCheck {
        if (awrsFeatureSwitches.enrolmentJourney().enabled) {
          val isSA = accountUtils.isSaAccount(ar.enrolments).getOrElse(false)
          awrsEnrolmentUtrForm.bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(template(formWithErrors, isSA))),
            utr => processEnrolment(utr, isSA)
          )
        } else {
          Future.successful(NotFound)
        }
      }
    }
  }

  private def processEnrolment(utr: AwrsEnrolmentUtr, isSA: Boolean)(implicit hc: HeaderCarrier): Future[Result] = {
    keyStoreService.saveAwrsEnrolmentUtr(utr)
    for {
      // 1. Fetch required data
      maybeAwrsUrn <- keyStoreService.fetchAwrsEnrolmentUrn
      maybePostcode <- keyStoreService.fetchAwrsRegisteredPostcode
      maybeKnownFacts <- keyStoreService.fetchKnownFacts

      // 2. Extract and validate data
      awrsRef = getOrThrow(maybeAwrsUrn).awrsUrn
      postcode = getOrThrow(maybePostcode).registeredPostcode

      // 3. Verify known facts
      isVerified = KnownFactsVerifier.knownFactsVerified(maybeKnownFacts, awrsRef, isSA, utr.utr, postcode)
      _          = logger.info(s"known facts verification returned $isVerified for $awrsRef")

      // 4. Process de-enrolment if needed
      deEnrolmentSuccessful <- if (isVerified) {
        enrolmentStoreProxyService.queryForPrincipalGroupIdOfAWRSEnrolment(awrsRef) flatMap  {
          case Some(groupId) => deEnrolService.deEnrolAwrs(awrsRef, groupId)
          case None => Future.successful(true)
        }
      } else Future.successful(false)
      _          = logger.info(s"De enrolment process returns $deEnrolmentSuccessful")

      // 5. Process enrolment
      utrType = if (isSA) "SOP" else "CT"
      enrolmentResult <- if (isVerified && deEnrolmentSuccessful) enrolService.enrolAWRS(
        awrsRef,
        postcode,
        Some(utr.utr),
        utrType,
        Map.empty
      ) else Future.successful(None)
    } yield {
      enrolmentResult match {
        case Some(_) =>
          logger.info(s"enrolment succeeded for AWRS ref $awrsRef")
          Redirect(routes.SuccessfulEnrolmentController.showSuccessfulEnrolmentPage)
        case None    =>
          logger.info(s"enrolment failed for AWRS ref $awrsRef")
          Redirect(routes.KickoutController.showURNKickOutPage)
      }
    }
  }.recover {
    case ex: Exception =>
      logger.error("Exception occurred during re-enrolment journey", ex)
      Redirect(routes.KickoutController.showURNKickOutPage)
  }

}