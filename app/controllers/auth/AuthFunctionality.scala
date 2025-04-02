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

package controllers.auth

import config.ApplicationConfig
import org.apache.commons.codec.binary.Base64._
import org.apache.commons.codec.digest.DigestUtils
import play.api.Logging
import play.api.i18n.Messages
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

case class StandardAuthRetrievals(
                                   enrolments: Set[Enrolment],
                                   affinityGroup: Option[AffinityGroup],
                                   credId: String,
                                   plainTextCredId: String,
                                   role: Option[CredentialRole]
                                 )

trait AuthFunctionality extends AuthorisedFunctions with Logging {
  val origin: String = "awrs-frontend"
  val signInUrl: String
  implicit val applicationConfig: ApplicationConfig

  def loginParams: Map[String, Seq[String]] = Map(
    "continue_url" -> Seq(signInUrl),
    "origin" -> Seq(origin)
  )

  private def recoverAuthorisedCalls(implicit request: Request[AnyContent], messages: Messages): PartialFunction[Throwable, Result] = {
    case _: NoActiveSession         => Redirect(signInUrl, loginParams)
    case er: AuthorisationException =>
      logger.warn(s"[recoverAuthorisedCalls] Auth exception: $er")
      Unauthorized(applicationConfig.templateUnauthorised())
  }

  def enrolmentEligibleAuthorisedAction(body: StandardAuthRetrievals => Future[Result])
                                       (implicit req: Request[AnyContent], ec: ExecutionContext, hc: HeaderCarrier, messages: Messages): Future[Result] = {
    authorised(Enrolment("IR-CT") or Enrolment("IR-SA") or AffinityGroup.Organisation or AffinityGroup.Individual)
      .retrieve(authorisedEnrolments and affinityGroup and credentials and credentialRole) {
        case Enrolments(enrolments) ~ affGroup ~ Some(Credentials(providerId, _)) ~ role =>
          body(StandardAuthRetrievals(enrolments, affGroup, UrlSafe.hash(providerId), providerId, role))
        case _ =>
          throw new RuntimeException("[authorisedAction] Unknown retrieval model")
      } recover recoverAuthorisedCalls
  }


  def authorisedAction(body: StandardAuthRetrievals => Future[Result])
                      (implicit req: Request[AnyContent], ec: ExecutionContext, hc: HeaderCarrier, messages: Messages): Future[Result] = {
    authorised(Enrolment("IR-CT") or Enrolment("IR-SA") or Enrolment("HMRC-AWRS-ORG") or AffinityGroup.Organisation)
      .retrieve(authorisedEnrolments and affinityGroup and credentials and credentialRole) {
        case Enrolments(enrolments) ~ affGroup ~ Some(Credentials(providerId, _)) ~ role =>
          body(StandardAuthRetrievals(enrolments, affGroup, UrlSafe.hash(providerId), providerId, role))
        case _ =>
          throw new RuntimeException("[authorisedAction] Unknown retrieval model")
      } recover recoverAuthorisedCalls
  }
}

object UrlSafe {

  def hash(value: String): String = {
    val sha1: Array[Byte] = DigestUtils.sha1(value)
    val encoded = encodeBase64String(sha1)

    urlSafe(encoded)
  }

  private def urlSafe(encoded: String): String = {
    encoded.replace("=", "")
      .replace("/", "_")
      .replace("+", "-")
  }
}
