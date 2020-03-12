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
import controllers.auth.StandardAuthRetrievals
import javax.inject.Inject
import models.{BusinessCustomerDetails, BusinessRegistrationDetails}
import play.api.Logger
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import utils.AwrsSessionKeys

import scala.concurrent.{ExecutionContext, Future}

class CheckEtmpService @Inject()(awrsConnector: AWRSConnector,
                                 enrolService: EnrolService,
                                 save4LaterService: Save4LaterService) {

  def getRegistrationDetails(authRetrievals: StandardAuthRetrievals)
                            (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[BusinessRegistrationDetails]] = {
    save4LaterService.mainStore.fetchBusinessRegistrationDetails(authRetrievals)
  }

  def validateBusinessDetails(authRetrievals: StandardAuthRetrievals, busCusDetails: BusinessCustomerDetails)
                             (implicit hc: HeaderCarrier, ec: ExecutionContext, request: Request[_]): Future[Boolean] = {
    Logger.info("[CheckEtmpService][validateBusinessDetails] Retrieving business customer and registration details")

    getRegistrationDetails(authRetrievals).flatMap {
      case Some(businessRegistrationDetails) =>
        awrsConnector.checkEtmp(busCusDetails, businessRegistrationDetails) flatMap {
          case Some(successResponse) =>
            val businessType = request.session.get(AwrsSessionKeys.sessionBusinessType).getOrElse("")

            enrolService.enrolAWRS(
              successResponse.regimeRefNumber,
              busCusDetails,
              businessType,
              businessRegistrationDetails.utr
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
      case _ =>
        Logger.info("[CheckEtmpService][validateBusinessDetails] No business registration details")
        Future.successful(false)
    }
  }
}
