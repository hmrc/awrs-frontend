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

package services

import _root_.models.FormBundleStatus.{Approved, ApprovedWithConditions, Pending}
import models._
import play.api.mvc.{AnyContent, Request}
import services.apis.{AwrsAPI11, AwrsAPI12Cache, AwrsAPI9}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.LoggingUtils
import utils.SessionUtil._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{ BadRequestException, HeaderCarrier }

case class StatusReturnType(wasViewed: Boolean,
                            status: Option[SubscriptionStatusType],
                            notification: Option[StatusNotification],
                            info: Option[StatusInfoType])

trait StatusManagementService extends LoggingUtils {

  val api9: AwrsAPI9
  val api11: AwrsAPI11
  val api12: AwrsAPI12Cache
  val save4LaterService: Save4LaterService
  val keyStoreService: KeyStoreService


  // This method will call api 9 first and depending on the status decides whether to call api 12 or not before calling api 11.
  // when changing the routing conditions also review the impl in api 11, as it will have safe guards of its own within its methods.
  private def deepFetch(implicit user: AuthContext, hc: HeaderCarrier, request: Request[AnyContent]): Future[StatusReturnType] = {
    lazy val onComplete =
      (status: Option[SubscriptionStatusType], notification: Option[StatusNotification], info: Option[StatusInfoType]) =>
        api12.getNotificationViewedStatus flatMap {
          case Some(viewedStatusResponse) =>
            val wasViewed = viewedStatusResponse.viewed
            keyStoreService.saveViewedStatus(wasViewed).flatMap { _ =>
              api12.markNotificationViewedStatusAsViewed map {
                _ => StatusReturnType(wasViewed = wasViewed, status = status, notification = notification, info = info)
              }
            }
          case _ =>
            val errorStr = "API12 viewed status call failure, nothing was returned"
            err(errorStr)
            throw new BadRequestException(errorStr)
        }

    lazy val callAPI11 =
      (status: SubscriptionStatusType, contactNumber: Option[String], notification: Option[StatusNotification]) =>
        api11.getStatusInfo(status.formBundleStatus, contactNumber, notification.fold[Option[StatusContactType]](None)(x => x.contactType)) flatMap {
          info => onComplete(Some(status), notification, info)
        }

    lazy val callAPI12 =
      (status: SubscriptionStatusType, api9ContactNumber: Option[String]) => api12.getNotificationCache(status.formBundleStatus) flatMap {
        // N.B. we also get a status code back from API 12, this should be the same as the status from API9
        // (except pending). We are not policing this check here because the likelihood of these two status being
        // different is negligible
        case notification@Some(StatusNotification(_, contactNumber, _, _, _)) => callAPI11(status, contactNumber, notification)
        // if there there's nothing stored in the API 12 cache then
        case None => status.formBundleStatus match {
          // in case of pending and approved do no call api 11, since they should not by default
          case Pending | Approved => onComplete(Some(status), None, None)
          // in case of all other cases continue to call API 11 using the contact number returned from API 9
          case _ => callAPI11(status, api9ContactNumber, None)
        }
      }

    api9.getSubscriptionStatus flatMap {
      case Some(subscriptionStatus) =>
        subscriptionStatus.formBundleStatus match {
          case Pending | Approved | ApprovedWithConditions => callAPI12(subscriptionStatus, subscriptionStatus.businessContactNumber)
          case _ => callAPI11(subscriptionStatus, subscriptionStatus.businessContactNumber, None)
        }
      case None =>
        val errorStr = "API9 call failure, nothing was returned"
        err(errorStr)
        Future.failed(new BadRequestException(errorStr))
    }
  }

  private def localFetch(implicit user: AuthContext, hc: HeaderCarrier, request: Request[AnyContent]): Future[StatusReturnType] =
    for {
      subscriptionStatus <- api9.getSubscriptionStatusFromCache
      alertStatus <- api12.getAlertFromCache
      statusInfo <- api11.getStatusInfoFromCache
      Some(wasViewed) <- keyStoreService.fetchViewedStatus
    } yield StatusReturnType(wasViewed = wasViewed, subscriptionStatus, alertStatus, statusInfo)


  // if all the api calls were successful then the status should be in the session, and the session variable can only exist if all the status related apis were called successfully
  def retrieveStatus(implicit user: AuthContext, hc: HeaderCarrier, request: Request[AnyContent]): Future[StatusReturnType] =
  request getSessionStatus match {
    case Some(status) => localFetch
    case _ => deepFetch
  }

}

object StatusManagementService extends StatusManagementService {
  val api9 = AwrsAPI9
  val api11 = AwrsAPI11
  val api12 = AwrsAPI12Cache
  val save4LaterService = Save4LaterService
  val keyStoreService = KeyStoreService
}
