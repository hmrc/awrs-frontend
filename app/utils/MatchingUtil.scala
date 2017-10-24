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

package utils

import forms.AWRSEnums
import models.{BusinessCustomerDetails, BusinessType, Organisation}
import services.{BusinessMatchingService, Save4LaterService}
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

object MatchingUtil extends MatchingUtil {
  override val businessMatchingService = BusinessMatchingService
  override val dataCacheService = Save4LaterService
}

trait MatchingUtil extends LoggingUtils {

  val dataCacheService: Save4LaterService
  val businessMatchingService: BusinessMatchingService

  def isValidMatchedGroupUtr(utr: String)(implicit user: AuthContext, hc: HeaderCarrier): Future[Boolean] = {
    for {
      bcd <- dataCacheService.mainStore.fetchBusinessCustomerDetails
      businessType <- dataCacheService.mainStore.fetchBusinessType
      isMatchFound <- businessMatchingService.matchBusinessWithUTR(utr, getOrganisation(bcd, businessType))
    } yield {
      isMatchFound
    }
  }

  private def getOrganisation(businessCustomerDetails: Option[BusinessCustomerDetails], businessType: Option[BusinessType]) = {
    (businessCustomerDetails, businessType) match {
      case (Some(bcd), Some(bt)) if bcd.isAGroup => Some(Organisation(bcd.businessName, getGroupOrgType(bt)))
      case _ => throw new Exception("Missing organisation details for match.")
    }
  }

  private def getGroupOrgType(bt: BusinessType) = {
    bt.legalEntity match {
      case Some("LTD_GRP") => AWRSEnums.CorporateBodyString
      case Some("LLP_GRP") => AWRSEnums.LlpString
      case orgType@_ => throw new Exception("Invalid group organisation type: " + orgType)
    }
  }
}
