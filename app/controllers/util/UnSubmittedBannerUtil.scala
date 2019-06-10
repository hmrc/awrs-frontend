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

package controllers.util

import controllers.auth.StandardAuthRetrievals
import models.BusinessDetails
import play.api.mvc.{AnyContent, Request}
import services.DataCacheKeys._
import services.{ApplicationService, IndexService, KeyStoreService, Save4LaterService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{AccountUtils, AwrsSessionKeys}

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier

case class UnSubmittedChangesBannerParam(allSectionComplete: Boolean)

object UnSubmittedChangesBannerParam {

  def apply(hasAwrs: Boolean, hasApplicationChanged: Boolean, allSectionComplete: Boolean): Option[UnSubmittedChangesBannerParam] =
    if (hasAwrs && hasApplicationChanged) {
      Some(UnSubmittedChangesBannerParam(allSectionComplete = allSectionComplete))
    } else {
      None
    }

}

trait UnSubmittedBannerUtil {

  val applicationService: ApplicationService
  val save4LaterService: Save4LaterService
  val keyStoreService: KeyStoreService
  val indexService: IndexService

  def unSubmittedChangesBanner(awrsDataMap: Option[CacheMap], authRetrievals: StandardAuthRetrievals)
                              (implicit request: Request[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[UnSubmittedChangesBannerParam]] = {
    if (AccountUtils.hasAwrs(authRetrievals.enrolments)) {
      val businessType = request.session.get(AwrsSessionKeys.sessionBusinessType).fold("")(x => x)
      applicationService.hasAPI5ApplicationChanged(AccountUtils.getUtr(authRetrievals), authRetrievals).flatMap {
        case false => Future.successful(None)
        case true =>
          for {
            sectionStatus <- indexService.getStatus(awrsDataMap, businessType, authRetrievals)
          } yield {
            Some(UnSubmittedChangesBannerParam(allSectionComplete = indexService.showContinueButton(sectionStatus)))
          }
      }
    } else {
      Future.successful(None)
    }
  }

}
