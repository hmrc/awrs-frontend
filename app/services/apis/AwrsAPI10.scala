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

package services.apis

import connectors.AWRSConnector
import models.{DeRegistration, DeRegistrationType}
import play.api.mvc.{AnyContent, Request}
import services.KeyStoreService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.AccountUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


trait AwrsAPI10 {
  val awrsConnector: AWRSConnector
  val dataCacheService: KeyStoreService

  def deRegistration()(implicit request: Request[AnyContent], hc: HeaderCarrier, user: AuthContext): Future[Option[DeRegistrationType]] =
    for {
      someDate <- dataCacheService.fetchDeRegistrationDate
      someReason <- dataCacheService.fetchDeRegistrationReason
      response <- deRegistrationEtmp(DeRegistration.toDeRegistration(someDate, someReason))
    } yield {
      response
    }


  private def getAwrsRefNo(implicit user: AuthContext, hc: HeaderCarrier) = AccountUtils.getAwrsRefNo.toString()

  private def deRegistrationEtmp(someDeRegistration: Option[DeRegistration])(implicit request: Request[AnyContent], user: AuthContext, hc: HeaderCarrier): Future[Option[DeRegistrationType]] =
    someDeRegistration match {
      case Some(deRegistration) =>
        AccountUtils.hasAwrs match {
          case false => Future.successful(None)
          case true => awrsConnector.deRegistration(getAwrsRefNo, deRegistration) flatMap {
            case successData: DeRegistrationType =>
              Future.successful(Some(successData))
          }
        }
      case _ => Future.successful(None)
    }
}

object AwrsAPI10 extends AwrsAPI10 {
  override val awrsConnector = AWRSConnector
  override val dataCacheService = KeyStoreService
}
