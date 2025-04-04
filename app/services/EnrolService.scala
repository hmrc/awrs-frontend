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
                registeredPostcode: String,
                utr: Option[String],
                businessType: String,
                auditMap: Map[String, String])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[EnrolResponse]] = {
    val postCode = registeredPostcode.replaceAll("\\s+", "")
    val verifiers = createVerifiers(utr, businessType, postCode)
    authConnector.authorise(EmptyPredicate, credentials and groupIdentifier) flatMap {
      case Some(Credentials(ggCred, _)) ~ Some(groupId) =>
        val requestPayload = RequestPayload(ggCred, friendly, enrolmentType, verifiers)
        taxEnrolmentsConnector.enrol(requestPayload, groupId, awrsRef, auditMap)
      case _ ~ None =>
        Future.failed(new InternalServerException("Failed to enrol - user did not have a group identifier (not a valid GG user)"))
      case Some(Credentials(_, _)) ~ _ =>
        Future.failed(new InternalServerException("Failed to enrol - user had a different auth provider ID (not a valid GG user)"))
      case _ =>
        Future.failed(new InternalServerException("Failed to enrol - user had unknown credentials"))
    }
  }

  private[services] def createVerifiers(utr: Option[String], businessType: String, postcode: String) = {

    val postCodeVerifier =
      List(
        Verifier("Postcode", postcode)
    )

      utr match {
        case Some(utr) => businessType match {
          case "SOP" => postCodeVerifier :+ Verifier("SAUTR", utr)
          case _ => postCodeVerifier :+ Verifier("CTUTR", utr)
        }
        case _ => postCodeVerifier
      }
  }
}
