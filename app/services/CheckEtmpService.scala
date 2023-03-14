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

package services

import connectors.AWRSConnector
import controllers.auth.StandardAuthRetrievals

import javax.inject.Inject
import models.{AwrsUsers, BusinessCustomerDetails}
import play.api.Logging
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.{ExecutionContext, Future}

class CheckEtmpService @Inject()(awrsConnector: AWRSConnector,
                                 save4LaterService: Save4LaterService,
                                 enrolService: EnrolService) extends Logging {

  def validateBusinessDetails(busCusDetails: BusinessCustomerDetails, legalEntity: String)
                             (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    logger.info("[CheckEtmpService][validateBusinessDetails] Validating business details for self-heal")

    awrsConnector.checkEtmp(busCusDetails, legalEntity) flatMap {
      case Some(successResponse) =>
        enrolService.enrolAWRS(
          successResponse.regimeRefNumber,
          busCusDetails,
          legalEntity
        ) map {
          case Some(_) =>
            logger.info("[CheckEtmpService][validateBusinessDetails] ES8 success")
            true
          case _ =>
            logger.info("[CheckEtmpService][validateBusinessDetails] ES8 failure")
            false
        }
      case None =>
        logger.info("[CheckEtmpService][validateBusinessDetails] Could not perform ES6")
        Future.successful(false)
    }
  }

  def checkUsersEnrolments(authRetrievals: StandardAuthRetrievals, details: BusinessCustomerDetails)
                          (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Boolean]] = {
    logger.info("[CheckEtmpService][checkUsersEnrolments] Checking for existing awrs enrolment")
    awrsConnector.checkUsersEnrolments(details.safeId, authRetrievals.credId) map { users =>
      users match {
        case Some(users) =>
          Some(isCredIdPresent(authRetrievals.credId, users))
        case _ =>
          throw new InternalServerException(s"""[awrs-frontend][checkUsersEnrolments] - error when calling awrsConnector checkUsersEnrolments """)
      }
    }
  }

  def isCredIdPresent(credId: String, awrsUsers: AwrsUsers): Boolean =
    if (awrsUsers.delegatedUserIds.contains(credId) || awrsUsers.principalUserIds.contains(credId)) true else false

}
