/*
 * Copyright 2023 HM Revenue & Customs
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

import controllers.auth.StandardAuthRetrievals
import javax.inject.Inject
import models.{BusinessDetailsSupport, NewApplicationType}
import uk.gov.hmrc.http.HeaderCarrier
import views.Configuration.{NewApplicationMode, NewBusinessStartDateConfiguration, ReturnedApplicationEditMode, ReturnedApplicationMode}

import scala.concurrent.{ExecutionContext, Future}

class BusinessDetailsService @Inject()(
                                        save4LaterService: Save4LaterService
                                      ) {
  def businessDetailsPageRenderMode(authRetrievals: StandardAuthRetrievals)
                                   (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[NewBusinessStartDateConfiguration] = {
    save4LaterService.mainStore.fetchNewApplicationType(authRetrievals) flatMap { newApplicationType =>
      val isNewApplication = newApplicationType.getOrElse(NewApplicationType(Some(false))).isNewApplication.get
      val etmpBug =
        if (isNewApplication) {
          Future.successful(false)
        } else {
          save4LaterService.api.fetchBusinessDetailsSupport(authRetrievals) flatMap {
            case Some(BusinessDetailsSupport(missingProposedStartDate)) => Future.successful(missingProposedStartDate)
            case None => throw new RuntimeException("Unable to find API 5 data")
          }
        }
      etmpBug flatMap { x =>
        (isNewApplication, x) match {
          case (true, _) => Future.successful(NewApplicationMode)
          case (false, false) => Future.successful(ReturnedApplicationMode)
          case (false, true) => Future.successful(ReturnedApplicationEditMode)
        }
      }
    }
  }
}
