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

import _root_.models._
import connectors.EmailVerificationConnector
import javax.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class EmailVerificationService @Inject()(emailVerificationConnector: EmailVerificationConnector) {

  def sendVerificationEmail(emailAddress: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    emailVerificationConnector.sendVerificationEmail(emailAddress)
  }

  def isEmailVerified(businessContacts: Option[BusinessContacts])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    businessContacts match {
      case Some(data) => isEmailAddressVerified(data.email)
      case _ => Future.successful(false)
    }
  }

  def isEmailAddressVerified(email: Option[String])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    emailVerificationConnector.isEmailAddressVerified(email)
  }

}
