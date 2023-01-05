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

import audit.Auditable
import connectors.BusinessMatchingConnector
import controllers.auth.StandardAuthRetrievals
import forms.AWRSEnums
import javax.inject.Inject
import models._
import play.api.libs.json.{JsSuccess, JsValue}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{LoggingUtils, SessionUtil}

import scala.concurrent.{ExecutionContext, Future}

class BusinessMatchingService @Inject()(keyStoreService: KeyStoreService,
                                        businessMatchingConnector: BusinessMatchingConnector,
                                        save4LaterService: Save4LaterService,
                                        val auditable: Auditable) extends LoggingUtils {

  def isValidMatchedGroupUtr(utr: String, authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    for {
      bcd <- save4LaterService.mainStore.fetchBusinessCustomerDetails(authRetrievals)
      businessType <- save4LaterService.mainStore.fetchBusinessType(authRetrievals)
      isMatchFound <- matchBusinessWithUTR(utr, getOrganisation(bcd, businessType), authRetrievals)
    } yield {
      isMatchFound
    }
  }

  private def getOrganisation(businessCustomerDetails: Option[BusinessCustomerDetails], businessType: Option[BusinessType]): Option[Organisation] = {
    (businessCustomerDetails, businessType) match {
      case (Some(bcd), Some(bt)) if bcd.isAGroup => Some(Organisation(bcd.businessName, getGroupOrgType(bt)))
      case _ => throw new Exception("Missing organisation details for match.")
    }
  }

  private def getGroupOrgType(bt: BusinessType): String = {
    bt.legalEntity match {
      case Some("LTD_GRP") => AWRSEnums.CorporateBodyString
      case Some("LLP_GRP") => AWRSEnums.LlpString
      case orgType@_ => throw new Exception("Invalid group organisation type: " + orgType)
    }
  }

  def matchBusinessWithUTR(utr: String, organisation: Option[Organisation], authRetrievals: StandardAuthRetrievals)
                          (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    val searchData = MatchBusinessData(acknowledgementReference = SessionUtil.getUniqueAckNo,
      utr = utr, requiresNameMatch = true, individual = None, organisation = organisation)
    // set the user type to ORG as long as this is only ever used for groups as individuals cannot be group members
    businessMatchingConnector.lookup(searchData, "org", authRetrievals) flatMap { dataReturned =>
      storeBCAddressApi3(dataReturned)
      isSuccessfulMatch(dataReturned = dataReturned)
    }
  }

  private def storeBCAddressApi3(dataReturned: JsValue)(implicit hc: HeaderCarrier, ec: ExecutionContext): Unit = {
    val address = (dataReturned \ "address").validate[BCAddressApi3]
    address match {
      case s: JsSuccess[BCAddressApi3] => keyStoreService.saveBusinessCustomerAddress(s.get)
      case _ => {}
    }
  }

  private def isSuccessfulMatch(dataReturned: JsValue): Future[Boolean] = {
    val isSuccessResponse = dataReturned.validate[MatchSuccessResponse].isSuccess
    debug(s"[BusinessMatchingService][matchBusinessWithUTR]dataReturned = $dataReturned, isSuccessResponse = $isSuccessResponse")
    Future.successful(isSuccessResponse)
  }

}
