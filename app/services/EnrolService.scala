/*
 * Copyright 2018 HM Revenue & Customs
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

import _root_.models.{BusinessCustomerDetails, EnrolRequest, SuccessfulSubscriptionResponse}
import connectors.GovernmentGatewayConnector

import scala.concurrent.ExecutionContext

import GGConstants._
import uk.gov.hmrc.http.HeaderCarrier

trait EnrolService {
  val ggConnector: GovernmentGatewayConnector

  def enrolAWRS(success: SuccessfulSubscriptionResponse, businessPartnerDetails: BusinessCustomerDetails, businessType: String, utr: Option[String])(implicit hc: HeaderCarrier, ec: ExecutionContext) =
    ggConnector.enrol(createEnrolment(success, businessPartnerDetails, businessType, utr), businessPartnerDetails, businessType)

  def createEnrolment(success: SuccessfulSubscriptionResponse, businessPartnerDetails: BusinessCustomerDetails, businessType: String, utr: Option[String])(implicit ec: ExecutionContext) = {

    val awrsRef = success.awrsRegistrationNumber
    val postcode: String = businessPartnerDetails.businessAddress.postcode.fold("")(x=>x).replaceAll("\\s+", "")

    val knownFacts = (utr, businessType) match {
      case (Some(saUtr), "SOP") => Seq(awrsRef,"", saUtr, postcode)
      case (Some(ctUtr), _) => Seq(awrsRef, ctUtr,"", postcode)
      case (_, _) => Seq(awrsRef,"","", postcode)
    }

    EnrolRequest(portalId = mdtp,
      serviceName = service,
      friendlyName = friendly,
      knownFacts = knownFacts)
  }

}

object EnrolService extends EnrolService {
  val ggConnector = GovernmentGatewayConnector
}
