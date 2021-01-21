/*
 * Copyright 2021 HM Revenue & Customs
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
import controllers.auth.StandardAuthRetrievals
import javax.inject.Inject
import models.FormBundleStatus.{Approved, ApprovedWithConditions, Pending}
import models.{ApiTypes, DeRegistrationDate, EmailRequest}
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utils.AccountUtils
import utils.SessionUtil.sessionUtilForRequest

import scala.concurrent.{ExecutionContext, Future}

class EmailService @Inject()(
                              awrsNotificationConnector: AWRSNotificationConnector,
                              accountUtils: AccountUtils
                            ) {

  def sendConfirmationEmail(email: String, reference: String, isNewBusiness: Boolean, authRetrievals: StandardAuthRetrievals)
                           (implicit request: Request[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    val apiType: Future[ApiTypes.Value] = if (accountUtils.hasAwrs(authRetrievals.enrolments)) {
      request.getSessionStatus match {
        case Some(Pending) => Future.successful(ApiTypes.API6Pending)
        case Some(Approved | ApprovedWithConditions) => Future.successful(ApiTypes.API6Approved)
        case Some(status) => Future.failed(new InternalServerException(s"Unexpected status found: $status"))
        case None => Future.failed(new InternalServerException("Status is missing from session"))
      }
    } else {
      Future.successful(ApiTypes.API4)
    }

    apiType flatMap {
      sendEmail(email, awrsNotificationConnector.sendConfirmationEmail, _, None, Some(reference), Some(isNewBusiness))
    }
  }

  def sendWithdrawnEmail(email: String)
                        (implicit request: Request[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    sendEmail(email, awrsNotificationConnector.sendWithdrawnEmail, ApiTypes.API8)
  }

  def sendCancellationEmail(email: String, deRegistrationDate : Option[DeRegistrationDate])
                           (implicit request: Request[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    sendEmail(email, awrsNotificationConnector.sendCancellationEmail, ApiTypes.API10,deRegistrationDate)
  }

  private def sendEmail(email: String,
                        doEmailCall: EmailRequest => Future[Boolean],
                        apiTypePromise: ApiTypes.ApiType,
                        deRegistrationDate : Option[DeRegistrationDate] = None,
                        reference: Option[String] = None,
                        isNewBusiness: Option[Boolean] = None)
                       (implicit request: Request[AnyContent]): Future[Boolean] = {
        val deRegistrationDateStr = deRegistrationDate match {
          case Some(deRegDate) => Some(deRegDate.proposedEndDate.toString("dd MMMM yyyy"))
          case _ => None
        }
      val emailRequest = EmailRequest(apiTypePromise, request.getBusinessName.fold("")(x => x), email, reference, isNewBusiness, deRegistrationDateStr)
      doEmailCall(emailRequest)
    }
}