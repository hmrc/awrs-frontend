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

package services

import connectors.TaxEnrolmentsConnector
import javax.inject.Inject
import models._
import services.GGConstants._
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector

import scala.concurrent.{ExecutionContext, Future}

class EnrolService @Inject()(taxEnrolmentsConnector: TaxEnrolmentsConnector,
                             val authConnector: DefaultAuthConnector) extends AuthorisedFunctions {

  val enrolmentType = "principal"
  val GGProviderId = "GovernmentGateway"

  def enrolAWRS(awrsRef: String,
                businessPartnerDetails: BusinessCustomerDetails,
                businessType: String,
                utr: Option[String])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[EnrolResponse]] = {
    val enrolment = createEnrolment(awrsRef, businessPartnerDetails, businessType, utr)
      val postCode = businessPartnerDetails.businessAddress.postcode.fold("")(x => x).replaceAll("\\s+", "")
      val verifiers = createVerifiers(businessPartnerDetails.utr, businessType, postCode)
      authConnector.authorise(EmptyPredicate, credentials and groupIdentifier) flatMap {
        case Some(Credentials(ggCred, _)) ~ Some(groupId) =>
          val grpId = groupId
          val requestPayload = RequestPayload(ggCred, enrolment.friendlyName, enrolmentType, verifiers)
          taxEnrolmentsConnector.enrol(requestPayload, grpId, awrsRef, businessPartnerDetails, businessType)
        case _ ~ None =>
          Future.failed(new InternalServerException("Failed to enrol - user did not have a group identifier (not a valid GG user)"))
        case Some(Credentials(_, _)) ~ _ =>
          Future.failed(new InternalServerException("Failed to enrol - user had a different auth provider ID (not a valid GG user)"))
        case _ =>
          Future.failed(new InternalServerException("Failed to enrol - user had unknown credentials"))
      }
    }


  private def createVerifiers(utr: Option[String], businessType: String, postcode: String) = {
    val utrVerifier = businessType match {
      case "SOP" => Verifier("SAUTR", utr.getOrElse(""))
      case _ => Verifier("CTUTR", utr.getOrElse(""))
    }
    List(
      Verifier("Postcode", postcode)
    ) :+ utrVerifier
  }


  def createEnrolment(awrsRef: String,
                      businessPartnerDetails: BusinessCustomerDetails,
                      businessType: String,
                      utr: Option[String]): EnrolRequest = {

    val postcode: String = businessPartnerDetails.businessAddress.postcode
      .fold("")(x => x).replaceAll("\\s+", "")

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
