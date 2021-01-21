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

package services.apis

import connectors.AWRSConnector
import controllers.auth.StandardAuthRetrievals
import javax.inject.Inject
import models.{DeRegistration, DeRegistrationType}
import play.api.mvc.{AnyContent, Request}
import services.KeyStoreService
import uk.gov.hmrc.http.HeaderCarrier
import utils.AccountUtils

import scala.concurrent.{ExecutionContext, Future}

class AwrsAPI10 @Inject() (val accountUtils: AccountUtils,
                           val awrsConnector: AWRSConnector,
                           val dataCacheService: KeyStoreService
                          ){

  def deRegistration(authRetrievals: StandardAuthRetrievals)
                    (implicit request: Request[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[DeRegistrationType]] =
    for {
      someDate <- dataCacheService.fetchDeRegistrationDate
      someReason <- dataCacheService.fetchDeRegistrationReason
      response <- deRegistrationEtmp(authRetrievals)(DeRegistration.toDeRegistration(someDate, someReason))
    } yield {
      response
    }

  private def deRegistrationEtmp(authRetrievals: StandardAuthRetrievals)
                                (someDeRegistration: Option[DeRegistration])
                                (implicit request: Request[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[DeRegistrationType]] =
    someDeRegistration match {
      case Some(deRegistration) =>
        if (accountUtils.hasAwrs(authRetrievals.enrolments)) {
          awrsConnector.deRegistration(deRegistration, authRetrievals) flatMap { successData =>
            Future.successful(Some(successData))
          }
        } else {
          Future.successful(None)
        }
      case _ => Future.successful(None)
    }
}
