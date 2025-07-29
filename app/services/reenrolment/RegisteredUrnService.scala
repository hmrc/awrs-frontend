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
import connectors.EnrolmentStoreProxyConnector
import models.AwrsEnrolmentUrn
import models.reenrolment._
import play.api.Logging
import services.KeyStoreService
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegisteredUrnService @Inject() (keyStoreService: KeyStoreService,
                                      enrolmentStoreConnector: EnrolmentStoreProxyConnector,
                                      implicit val applicationConfig: ApplicationConfig)
    extends Logging {

  def handleAWRSRefChecks(providerId: String, awrsUrn: AwrsEnrolmentUrn)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[AWRSRefCheckResponse] = {
    keyStoreService.saveAwrsEnrolmentUrn(awrsUrn) flatMap { _ =>
      enrolmentStoreConnector.lookupEnrolments(AwrsKnownFacts(awrsUrn.awrsUrn)).flatMap {
        case Some(knownFactsResponse) =>
          keyStoreService.saveKnownFacts(knownFactsResponse).flatMap { _ =>
            isProviderIdEnrolled(providerId, awrsUrn)
              .map(isUserEnrolled => if (isUserEnrolled) UserIsEnrolled else UserIsNotEnrolled)
          }
        case None =>
          logger.warn("no known facts found for awrs urn")
          Future.successful(NoKnownFactsExist)
      } recover { case ex: Exception =>
        logger.error("Exception occurred handling de-enrolment confirmation", ex)
        ErrorRetrievingEnrolments
      }
    }
  }

  private def isProviderIdEnrolled(userId: String, urn: AwrsEnrolmentUrn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    enrolmentStoreConnector
      .queryForAssignedPrincipalUsersOfAWRSEnrolment(urn.awrsUrn)
      .map(_.exists(_.principalUserIds.contains(userId)))

}
