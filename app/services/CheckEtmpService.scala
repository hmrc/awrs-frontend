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
import models.BusinessCustomerDetails
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckEtmpService @Inject()(awrsConnector: AWRSConnector,
                                 enrolService: EnrolService) extends Logging {

  def validateBusinessDetails(busCusDetails: BusinessCustomerDetails, legalEntity: String)
                             (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    logger.info("[CheckEtmpService][validateBusinessDetails] Validating business details for self-heal")

    awrsConnector.checkEtmp(busCusDetails, legalEntity) flatMap {
      case Some(successResponse) =>
        enrolService.enrolAWRS(
          successResponse.regimeRefNumber,
          busCusDetails.businessAddress.postcode.fold("")(x => x).replaceAll("\\s+", ""),
          busCusDetails.utr,
          legalEntity,
          Map(
            "safeId" -> busCusDetails.safeId,
            "UserDetail" -> busCusDetails.businessName,
            "legal-entity" -> legalEntity)
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

  def checkUsersEnrolments(details: BusinessCustomerDetails, credId: String)
                          (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Boolean]] = {
    logger.info("[CheckEtmpService][checkUsersEnrolments] Checking for existing AWRS users")
    awrsConnector.checkUsersEnrolments(details.safeId) map { users =>
      users match {
        case Some(users) if users.principalUserIds == Nil && users.delegatedUserIds == Nil =>
          logger.info(s"""[CheckEtmpService][checkUsersEnrolments] No AWRS users found for ${details.businessName} user $credId""")
          Some(false)
        case Some(users) if users.principalUserIds.contains(credId) || users.delegatedUserIds.contains(credId) =>
          logger.info(s"""[CheckEtmpService][checkUsersEnrolments] Users credId found in list of AWRS users""")
          Some(false)
        case Some(users) =>
          logger.info(s"""[CheckEtmpService][checkUsersEnrolments] $users Existing users found for ${details.businessName}""")
          Some(true)
        case _ =>
          logger.info(s"""[CheckEtmpService][checkUsersEnrolments] - No regime number found for ${details.businessName}""")
          Some(false)
      }
    }
  }
}
