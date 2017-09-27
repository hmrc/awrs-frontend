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

import connectors.{BusinessCustomerDataCacheConnector, KeyStoreConnector}
import play.api.libs.json

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

trait BusinessCustomerService {

  def businessCustomerConnector: KeyStoreConnector

  val bcSourceId: String = "BC_Business_Details"

  // N.B. this keystore is populated when we call the business customer front end, we do not populate this database
  // in our application
  def getReviewBusinessDetails[T](implicit hc: HeaderCarrier, formats: json.Format[T]): Future[Option[T]] =
  businessCustomerConnector.fetchDataFromKeystore[T](bcSourceId)

}

object BusinessCustomerService extends BusinessCustomerService {
  override val businessCustomerConnector: KeyStoreConnector = BusinessCustomerDataCacheConnector
}
