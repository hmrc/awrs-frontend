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


import config.{AuthClientConnector, FrontendAuthConnector}
import connectors.{GovernmentGatewayConnector, TaxEnrolmentsConnector}
import models._
import services.GGConstants._
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthProviders, AuthorisedFunctions}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.RunMode

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

trait EnrolService extends RunMode with AuthorisedFunctions {
  val ggConnector: GovernmentGatewayConnector
  val taxEnrolmentsConnector: TaxEnrolmentsConnector

  val isEmacFeatureToggle: Boolean

  val enrolmentType = "principal"

  private def formatGroupId(str: String) = str.substring(str.indexOf("-") + 1, str.length)

  def getGroupIdentifier(implicit hc: HeaderCarrier): Future[String] = {
    authorised(AuthProviders(GovernmentGateway) and AffinityGroup.Organisation).retrieve(groupIdentifier) {
      case Some(groupId) => Future.successful(formatGroupId(groupId))
      case _ => throw new RuntimeException("No group identifier found for the agent!")
    }
  }

  private def createVerifiers(safeId: String, utr: Option[String], businessType: String, postcode: String) = {
    val utrVerifier = businessType match {
      case "SOP" => Verifier("SAUTR", utr.getOrElse(""))
      case _ => Verifier("CTUTR", utr.getOrElse(""))
    }
    List(
      Verifier("Postcode", postcode),
      Verifier("SAFEID", safeId)
    ) :+ utrVerifier
  }

  def enrolAWRS(success: SuccessfulSubscriptionResponse,
                businessPartnerDetails: BusinessCustomerDetails,
                businessType: String,
                utr: Option[String],
                userId: String
               )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[EnrolResponse]] = {
      val enrolment = createEnrolment(success, businessPartnerDetails, businessType, utr)
      if (isEmacFeatureToggle) {
        val emacUserId = userId.replace("/auth/session/", "")
        println( "\n\n\nHERE 1")
        val postCode = businessPartnerDetails.businessAddress.postcode.fold("")(x => x).replaceAll("\\s+", "")
        println( "HERE 2" + postCode)
        val verifiers = createVerifiers(businessPartnerDetails.safeId, businessPartnerDetails.utr, businessType, postCode)
        println( "HERE 3" + businessPartnerDetails.safeId)
        println( "HERE 4" + verifiers)
        val requestPayload = RequestPayload(emacUserId, enrolment.friendlyName, enrolmentType, verifiers)
        println( "HERE 5" + requestPayload)
        getGroupIdentifier.flatMap(groupId => {
          println("HERE 6" + groupId)
          taxEnrolmentsConnector.enrol(requestPayload, groupId, success.awrsRegistrationNumber, businessPartnerDetails, businessType)
        }
        )
      } else {
        ggConnector.enrol(enrolment, businessPartnerDetails, businessType)
      }

  }

  def createEnrolment(success: SuccessfulSubscriptionResponse, businessPartnerDetails: BusinessCustomerDetails, businessType: String, utr: Option[String])(implicit ec: ExecutionContext) = {

    val awrsRef = success.awrsRegistrationNumber
    val postcode: String = businessPartnerDetails.businessAddress.postcode.fold("")(x => x).replaceAll("\\s+", "")

    val knownFacts = (utr, businessType) match {
      case (Some(saUtr), "SOP") => Seq(awrsRef, "", saUtr, postcode)
      case (Some(ctUtr), _) => Seq(awrsRef, ctUtr, "", postcode)
      case (_, _) => Seq(awrsRef, "", "", postcode)
    }

    EnrolRequest(portalId = mdtp,
      serviceName = service,
      friendlyName = friendly,
      knownFacts = knownFacts)
  }

}

object EnrolService extends EnrolService {
  val ggConnector = GovernmentGatewayConnector
  val taxEnrolmentsConnector: TaxEnrolmentsConnector = TaxEnrolmentsConnector
  val isEmacFeatureToggle = runModeConfiguration.getBoolean("emacsFeatureToggle").getOrElse(false)
  override val authConnector = AuthClientConnector
}
