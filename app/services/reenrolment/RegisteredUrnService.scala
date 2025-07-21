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

package services.reenrolment

import config.ApplicationConfig
import controllers.reenrolment.routes
import models.AwrsEnrolmentUrn
import models.reenrolment.AwrsKnownFacts
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc._
import services.{EnrolmentStoreProxyService, KeyStoreService}
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.credentials
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegisteredUrnService @Inject() (keyStoreService: KeyStoreService,
                                      val authConnector: DefaultAuthConnector,
                                      val enrolmentStoreService: EnrolmentStoreProxyService,
                                      implicit val applicationConfig: ApplicationConfig)
    extends Logging {

  def handleEnrolmentConfirmationFlow(awrsUrn: AwrsEnrolmentUrn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    keyStoreService.saveAwrsEnrolmentUrn(awrsUrn) flatMap { _ =>
      enrolmentStoreService.lookupKnownFacts(AwrsKnownFacts(awrsUrn.awrsUrn)).flatMap {
        case Some(knownFactsResponse) =>
          keyStoreService.saveKnownFacts(knownFactsResponse).flatMap { _ =>
            authConnector.authorise(EmptyPredicate, credentials).flatMap {
              case Some(Credentials(userId, _)) =>
                checkEnrolmentExistsAndConfirmDeEnrollment(userId, awrsUrn)
              case _ =>
                logger.error("missing userId required enrollment check")
                Future.successful(Redirect(routes.KickoutController.showURNKickOutPage))
            }
          }
        case None =>
          logger.warn("no known facts found for awrs urn")
          Future.successful(Redirect(routes.KickoutController.showURNKickOutPage))
      } recover { case ex: Exception =>
        logger.error("Exception occurred handling de-enrolment confirmation", ex)
        Redirect(routes.KickoutController.showURNKickOutPage)
      }
    }
  }

  private def checkEnrolmentExistsAndConfirmDeEnrollment(userId: String, awrsUrn: AwrsEnrolmentUrn)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext) = {
    enrolmentStoreService.isUserAssignedToAWRSEnrolment(userId, awrsUrn.awrsUrn).map {
      case true => Redirect(routes.DeEnrollmentConfirmationPageController.showDeEnrollmentConfirmationPage)
      case false =>
        logger.warn("no enrolments exist for user id")
        Redirect(routes.KickoutController.showURNKickOutPage)

    }
  }

}
