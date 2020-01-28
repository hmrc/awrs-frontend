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

package utils

import audit.Auditable
import controllers.auth.StandardAuthRetrievals
import forms.AWRSEnums
import javax.inject.Inject
import models.{BusinessCustomerDetails, BusinessType, Organisation}
import services.{BusinessMatchingService, Save4LaterService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class MatchingUtil @Inject()(dataCacheService: Save4LaterService,
                             businessMatchingService: BusinessMatchingService,
                             val auditable: Auditable) extends LoggingUtils {

  def isValidMatchedGroupUtr(utr: String, authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    for {
      bcd <- dataCacheService.mainStore.fetchBusinessCustomerDetails(authRetrievals)
      businessType <- dataCacheService.mainStore.fetchBusinessType(authRetrievals)
      isMatchFound <- businessMatchingService.matchBusinessWithUTR(utr, getOrganisation(bcd, businessType), authRetrievals)
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
