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

package services

import connectors.BusinessMatchingConnector
import models._
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{LoggingUtils, SessionUtil}
import services.KeyStoreService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object BusinessMatchingService extends BusinessMatchingService {
  val businessMatchingConnector: BusinessMatchingConnector = BusinessMatchingConnector
  override val keyStoreService = KeyStoreService
}

trait BusinessMatchingService extends LoggingUtils {

  val keyStoreService: KeyStoreService

  def businessMatchingConnector: BusinessMatchingConnector

  def matchBusinessWithUTR(utr: String, organisation: Option[Organisation])
                          (implicit user: AuthContext, hc: HeaderCarrier): Future[Boolean] = {
    val searchData = MatchBusinessData(acknowledgementReference = SessionUtil.getUniqueAckNo,
      utr = utr, requiresNameMatch = true, isAnAgent = false, individual = None, organisation = organisation)
    // set the user type to ORG as long as this is only ever used for groups as individuals cannot be group members
    businessMatchingConnector.lookup(searchData, "org") flatMap { dataReturned =>
      storeBCAddressApi3(dataReturned)
      isSuccessfulMatch(dataReturned = dataReturned)
    }
  }

  private def storeBCAddressApi3(dataReturned: JsValue)(implicit user: AuthContext, hc: HeaderCarrier): Unit = {
    val address = (dataReturned \ "address").validate[BCAddressApi3]
    address match {
      case s: JsSuccess[BCAddressApi3] => keyStoreService.saveBusinessCustomerAddress(s.get)
      case _ => {}
    }
  }

  private def isSuccessfulMatch(dataReturned: JsValue)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val isSuccessResponse = dataReturned.validate[MatchSuccessResponse].isSuccess
    debug(s"[BusinessMatchingService][matchBusinessWithUTR]dataReturned = $dataReturned, isSuccessResponse = $isSuccessResponse")
    Future.successful(isSuccessResponse)
  }

}
