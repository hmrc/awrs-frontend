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

import connectors.BusinessCustomerDataCacheConnector
import javax.inject.Inject
import play.api.libs.json
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class BusinessCustomerService @Inject()(businessCustomerConnector: BusinessCustomerDataCacheConnector) {

  val bcSourceId: String = "BC_Business_Details"

  // N.B. this keystore is populated when we call the business customer front end, we do not populate this database
  // in our application
  def getReviewBusinessDetails[T](implicit hc: HeaderCarrier, formats: json.Format[T], ec: ExecutionContext): Future[Option[T]] =
    businessCustomerConnector.fetchDataFromKeystore[T](bcSourceId)
}
