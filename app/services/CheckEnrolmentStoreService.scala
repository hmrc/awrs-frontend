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

import akka.actor.typed.delivery.internal.ProducerControllerImpl.Request
import connectors.AWRSConnector
import controllers.auth.StandardAuthRetrievals
import models.BusinessCustomerDetails
import play.api.Logging
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckEnrolmentStoreService @Inject()(awrsConnector: AWRSConnector) extends Logging {
  def getAwrsRefNumber(safeId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] = {
    awrsConnector.getAwrsRefNumber(safeId) match {
      case _ => Future("AWRS123")
    }
  }

  def checkEnrolmentStoreProxy(utr: Option[String], authRetrievals: StandardAuthRetrievals)
                             (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {

    awrsConnector.checkEnrolmentStore(utr, authRetrievals) match {
      case _ => Future(true)
    }
  }
}