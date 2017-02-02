/*
 * Copyright 2017 HM Revenue & Customs
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

import connectors.AWRSNotificationConnector
import models.ApiTypes.ApiType
import models.FormBundleStatus.{Approved, ApprovedWithConditions, Pending}
import models.{ApiTypes, ConfirmationEmailRequest}
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.{HeaderCarrier, InternalServerException}
import utils.AccountUtils
import utils.SessionUtil.sessionUtilForRequest

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait EmailService {
  val awrsNotificationConnector: AWRSNotificationConnector

  def sendConfirmationEmail(email: String, reference: String, isNewBusiness: Boolean)(implicit user: AuthContext, request: Request[AnyContent], hc: HeaderCarrier): Future[Boolean] = {
    implicit def conv(v: ApiType): Future[ApiType] = Future.successful(v)
    val apiTypePromise: Future[ApiType] = AccountUtils.hasAwrs match {
      case true =>
        request.getSessionStatus match {
          case Some(Pending) => ApiTypes.API6Pending
          case Some(Approved | ApprovedWithConditions) => ApiTypes.API6Approved
          case Some(status) => Future.failed(new InternalServerException(s"Unexpected status found: $status"))
          case None => Future.failed(new InternalServerException("Status is missing from session"))
        }
      case false => ApiTypes.API4
    }
    apiTypePromise flatMap { apiType =>
      val emailRequest = ConfirmationEmailRequest(apiType, request.getBusinessName.fold("")(x => x), reference, email, isNewBusiness = isNewBusiness)
      awrsNotificationConnector.sendConfirmationEmail(emailRequest = emailRequest)
    }
  }

}

object EmailService extends EmailService {
  override val awrsNotificationConnector = AWRSNotificationConnector
}
