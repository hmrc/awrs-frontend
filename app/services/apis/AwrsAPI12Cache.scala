/*
 * Copyright 2022 HM Revenue & Customs
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

import connectors.AWRSNotificationConnector
import controllers.auth.StandardAuthRetrievals
import javax.inject.Inject
import models.FormBundleStatus.{Pending, Revoked}
import models.StatusContactType.{MindedToReject, MindedToRevoke}
import models.{FormBundleStatus, StatusContactType, StatusNotification, ViewedStatusResponse}
import services.KeyStoreService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}


class AwrsAPI12Cache @Inject()(val awrsNotificationConnector: AWRSNotificationConnector,
                               val keyStoreService: KeyStoreService
                              ){

  def getNotificationCache(status: FormBundleStatus, authRetrievals: StandardAuthRetrievals)
                          (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[StatusNotification]] =
    keyStoreService.fetchStatusNotification flatMap {
      case None => awrsNotificationConnector.fetchNotificationCache(authRetrievals) flatMap {
        case notification@Some(notificationStatus) =>
          lazy val returnResult = Future.successful(notification)
          // if they were previously MindedToReject & Pending but have now changed to Approved, Approved with Conditions or Rejected
          // we need to delete the MindedToReject notification from the cache
          notificationStatus.contactType match {
            case Some(MindedToReject) if status != Pending =>
              deleteNotificationFromCache(authRetrievals)
              returnResult // we are deliberately not waiting for the delete to finish
            case Some(MindedToRevoke) if status == Revoked =>
              deleteNotificationFromCache(authRetrievals)
              returnResult // we are deliberately not waiting for the delete to finish
            case _ =>
              keyStoreService.saveStatusNotification(notificationStatus) flatMap { _ =>
                // If the stored notification is a 'No longer...' notification, we need to delete it at the API 12 cache so it is not shown again after this session ends
                notificationStatus.contactType match {
                  case Some(StatusContactType.NoLongerMindedToRevoke) =>
                    deleteNotificationFromCache(authRetrievals)
                    returnResult // we are deliberately not waiting for the delete to finish
                  case _ => returnResult
                }
              }
          }
        case None => Future.successful(None)
      }
      case keystoreCache@Some(_) => Future.successful(keystoreCache)
    }


  @inline def getAlertFromCache(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[StatusNotification]] =
    keyStoreService.fetchStatusNotification

  @inline def deleteNotificationFromCache(authRetrievals: StandardAuthRetrievals)
                                         (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    awrsNotificationConnector.deleteFromNotificationCache(authRetrievals)

  @inline def getNotificationViewedStatus(authRetrievals: StandardAuthRetrievals)
                                         (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ViewedStatusResponse]] =
    awrsNotificationConnector.getNotificationViewedStatus(authRetrievals)

  @inline def markNotificationViewedStatusAsViewed(authRetrievals: StandardAuthRetrievals)
                                                  (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Boolean]] =
    awrsNotificationConnector.markNotificationViewedStatusAsViewed(authRetrievals)

}
