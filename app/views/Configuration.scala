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

package views

import models.FormBundleStatus.{Approved, ApprovedWithConditions, Revoked, RevokedUnderReviewOrAppeal}
import play.api.mvc.{AnyContent, Request}
import utils.SessionUtil._
import scala.language.postfixOps


object Configuration {


  sealed trait NewBusinessStartDateConfiguration

  case object NewApplicationMode extends NewBusinessStartDateConfiguration

  case object ReturnedApplicationMode extends NewBusinessStartDateConfiguration

  case object ReturnedApplicationEditMode extends NewBusinessStartDateConfiguration

  def showHint1(implicit request: Request[AnyContent]): Boolean =
    request getSessionStatus match {
      case None => throw new RuntimeException("Missing status in session data")
      case Some(Approved) | Some(ApprovedWithConditions) | Some(Revoked) | Some(RevokedUnderReviewOrAppeal) => false
      case _ => true
    }

}
