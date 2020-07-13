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

package services

import connectors.AWRSConnector
import javax.inject.Inject
import models.BusinessCustomerDetails
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class CheckEtmpService @Inject()(awrsConnector: AWRSConnector,
                                 enrolService: EnrolService) {

  def validateBusinessDetails(busCusDetails: BusinessCustomerDetails, legalEntity: String)
                             (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    Logger.info("[CheckEtmpService][validateBusinessDetails] Validating business details for self-heal")

    awrsConnector.checkEtmp(busCusDetails, legalEntity) flatMap {
      case Some(successResponse) =>
        enrolService.enrolAWRS(
          successResponse.regimeRefNumber,
          busCusDetails,
          legalEntity,
          busCusDetails.utr
        ) map {
          case Some(_) =>
            Logger.info("[CheckEtmpService][validateBusinessDetails] ES8 success")
            true
          case _       =>
            Logger.info("[CheckEtmpService][validateBusinessDetails] ES8 failure")
            false
        }
      case None =>
        Logger.info("[CheckEtmpService][validateBusinessDetails] Could not perform ES6")
        Future.successful(false)
    }
  }
}
