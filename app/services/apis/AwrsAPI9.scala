/*
 * Copyright 2018 HM Revenue & Customs
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
import models.SubscriptionStatusType
import services.{KeyStoreService, Save4LaterService}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.AccountUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

// Unit tests for this class are covered in EtmpLookupService for retrieveApplication

trait AwrsAPI9 {
  val awrsConnector: AWRSConnector
  val keyStoreService: KeyStoreService
  val save4LaterService: Save4LaterService

  def getSubscriptionStatus(implicit hc: HeaderCarrier, user: AuthContext): Future[Option[SubscriptionStatusType]] =
    getSubscriptionStatusFromCache flatMap {
      case None => getSubscriptionStatusFromEtmp
      case successData: Option[SubscriptionStatusType] => Future.successful(successData)
    }

  @inline def getSubscriptionStatusFromCache(implicit user: AuthContext, hc: HeaderCarrier) =
    keyStoreService.fetchSubscriptionStatus

  private def getAwrsRefNo(implicit user: AuthContext, hc: HeaderCarrier) = AccountUtils.getAwrsRefNo.toString()

  private def getSubscriptionStatusFromEtmp(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[SubscriptionStatusType]] =

      save4LaterService.mainStore.fetchBusinessCustomerDetails flatMap {
        businessDetails =>
          AccountUtils.hasAwrs match {
            case false => Future.successful(None)
            case true => awrsConnector.checkStatus(getAwrsRefNo,businessDetails.get.businessName) flatMap {
              case successData: SubscriptionStatusType =>
                keyStoreService.saveSubscriptionStatus(successData) flatMap {
                  _ => Future.successful(Some(successData))
                }
            }
          }
      }

}

object AwrsAPI9 extends AwrsAPI9 {
  override val awrsConnector = AWRSConnector
  override val keyStoreService = KeyStoreService
  override val save4LaterService = Save4LaterService
}
