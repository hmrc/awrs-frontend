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

import _root_.models.FormBundleStatus.{Approved, ApprovedWithConditions, Pending}
import audit.Auditable
import controllers.auth.StandardAuthRetrievals
import javax.inject.Inject
import models._
import play.api.mvc.{AnyContent, Request}
import services.apis.{AwrsAPI11, AwrsAPI12Cache, AwrsAPI9}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import utils.LoggingUtils
import utils.SessionUtil._

import scala.concurrent.{ExecutionContext, Future}

case class StatusReturnType(wasViewed: Boolean,
                            status: Option[SubscriptionStatusType],
                            notification: Option[StatusNotification],
                            info: Option[StatusInfoType])

class StatusManagementService @Inject()(api9: AwrsAPI9,
                                        api11: AwrsAPI11,
                                        api12: AwrsAPI12Cache,
                                        keyStoreService: KeyStoreService,
                                        val auditable: Auditable) extends LoggingUtils {

  // This method will call api 9 first and depending on the status decides whether to call api 12 or not before calling api 11.
  // when changing the routing conditions also review the impl in api 11, as it will have safe guards of its own within its methods.
  private def deepFetch(authRetrievals: StandardAuthRetrievals)
                       (implicit hc: HeaderCarrier, request: Request[AnyContent], ec: ExecutionContext): Future[StatusReturnType] = {
    lazy val onComplete =
      (status: Option[SubscriptionStatusType], notification: Option[StatusNotification], info: Option[StatusInfoType]) =>
        api12.getNotificationViewedStatus(authRetrievals) flatMap {
          case Some(viewedStatusResponse) =>
            val wasViewed = viewedStatusResponse.viewed
            keyStoreService.saveViewedStatus(wasViewed).flatMap { _ =>
              api12.markNotificationViewedStatusAsViewed(authRetrievals) map {
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
        api11.getStatusInfo(status.formBundleStatus, contactNumber, notification.fold[Option[StatusContactType]](None)(x => x.contactType), authRetrievals) flatMap {
          info => onComplete(Some(status), notification, info)
        }

    lazy val callAPI12 =
      (status: SubscriptionStatusType, api9ContactNumber: Option[String]) => api12.getNotificationCache(status.formBundleStatus, authRetrievals) flatMap {
        // N.B. we also get a status code back from API 12, this must be the same as the status from API9
        // (except pending). We are not policing this check here because the likelihood of these two status being
        // different is negligible
        case notification@Some(StatusNotification(_, contactNumber, _, _, _)) => callAPI11(status, contactNumber, notification)
        // if there there's nothing stored in the API 12 cache then
        case None => status.formBundleStatus match {
          // in case of pending and approved do no call api 11, since they must not by default
          case Pending | Approved => onComplete(Some(status), None, None)
          // in case of all other cases continue to call API 11 using the contact number returned from API 9
          case _ => callAPI11(status, api9ContactNumber, None)
        }
      }

    api9.getSubscriptionStatus(authRetrievals) flatMap {
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

  private def localFetch(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[StatusReturnType] =
    for {
      subscriptionStatus <- api9.getSubscriptionStatusFromCache
      alertStatus <- api12.getAlertFromCache
      statusInfo <- api11.getStatusInfoFromCache
      Some(wasViewed) <- keyStoreService.fetchViewedStatus
    } yield StatusReturnType(wasViewed = wasViewed, subscriptionStatus, alertStatus, statusInfo)


  // if all the api calls were successful then the status must be in the session, and the session variable can only exist if all the status related apis were called successfully
  def retrieveStatus(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier, request: Request[AnyContent], ec: ExecutionContext): Future[StatusReturnType] =
    request.getSessionStatus match {
      case Some(_) => localFetch
      case _       => deepFetch(authRetrievals)
    }

}