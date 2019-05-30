/*
 * Copyright 2019 HM Revenue & Customs
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

package services.apis

import connectors.AWRSConnector
import controllers.auth.StandardAuthRetrievals
import models.SubscriptionStatusType
import services.{KeyStoreService, Save4LaterService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.AccountUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// Unit tests for this class are covered in EtmpLookupService for retrieveApplication

trait AwrsAPI9 {
  val awrsConnector: AWRSConnector
  val keyStoreService: KeyStoreService
  val save4LaterService: Save4LaterService

  def getSubscriptionStatus(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier): Future[Option[SubscriptionStatusType]] =
    getSubscriptionStatusFromCache flatMap {
      case None => getSubscriptionStatusFromEtmp(authRetrievals)
      case successData: Option[SubscriptionStatusType] => Future.successful(successData)
    }

  @inline def getSubscriptionStatusFromCache(implicit hc: HeaderCarrier): Future[Option[SubscriptionStatusType]] =
    keyStoreService.fetchSubscriptionStatus

  private def getSubscriptionStatusFromEtmp(authRetrievals: StandardAuthRetrievals)
                                           (implicit hc: HeaderCarrier): Future[Option[SubscriptionStatusType]] =

    save4LaterService.mainStore.fetchBusinessCustomerDetails(authRetrievals) flatMap {
      businessDetails =>
        if (AccountUtils.hasAwrs(authRetrievals.enrolments)) {
          awrsConnector.checkStatus(authRetrievals, businessDetails.get.businessName) flatMap {
            successData: SubscriptionStatusType =>
              keyStoreService.saveSubscriptionStatus(successData) flatMap {
                _ => Future.successful(Some(successData))
              }
          }
        } else {
          Future.successful(None)
        }
    }

}

object AwrsAPI9 extends AwrsAPI9 {
  override val awrsConnector = AWRSConnector
  override val keyStoreService = KeyStoreService
  override val save4LaterService = Save4LaterService
}
