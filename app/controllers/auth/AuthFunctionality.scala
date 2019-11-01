/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.Logger
import play.api.i18n.Messages
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.{GGCredId, ~}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

case class StandardAuthRetrievals(
                                   enrolments: Set[Enrolment],
                                   affinityGroup: Option[AffinityGroup],
                                   credId: String
                                 )

trait AuthFunctionality extends AuthorisedFunctions {
  val origin: String = "awrs-frontend"
  val signInUrl: String
  implicit val applicationConfig: ApplicationConfig

  def loginParams(implicit request: Request[AnyContent]): Map[String, Seq[String]] = Map(
    "continue" -> Seq(signInUrl),
    "origin" -> Seq(origin)
  )

  private def recoverAuthorisedCalls(implicit request: Request[AnyContent], messages: Messages): PartialFunction[Throwable, Result] = {
    case _: NoActiveSession         => Redirect(signInUrl, loginParams)
    case er: AuthorisationException =>
      Logger.warn(s"[recoverAuthorisedCalls] Auth exception: $er")
      Unauthorized(views.html.unauthorised())
    case er                         =>
      Logger.error(s"[recoverAuthorisedCalls] Unhandled error occurred - $er - ${er.getMessage} - ${er.getStackTrace.mkString("\n")}")
      AwrsController.showErrorPageRaw
  }

  def authorisedAction(body: StandardAuthRetrievals => Future[Result])
                      (implicit req: Request[AnyContent], ec: ExecutionContext, hc: HeaderCarrier, messages: Messages): Future[Result] = {
    authorised(Enrolment("IR-CT") or Enrolment("IR-SA") or Enrolment("HMRC-AWRS-ORG") or AffinityGroup.Organisation)
      .retrieve(authorisedEnrolments and affinityGroup and authProviderId) {
        case Enrolments(enrolments) ~ affGroup ~ (credentials: GGCredId) => body(StandardAuthRetrievals(enrolments, affGroup, UrlSafe.hash(credentials.credId)))
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