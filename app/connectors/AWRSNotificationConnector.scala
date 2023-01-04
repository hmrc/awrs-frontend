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

package connectors

import audit.Auditable
import config.ApplicationConfig
import controllers.auth.StandardAuthRetrievals
import javax.inject.Inject
import models._
import play.api.http.Status._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import utils.{AccountUtils, LoggingUtils}

import scala.concurrent.{ExecutionContext, Future}

class AWRSNotificationConnector @Inject()(http: DefaultHttpClient,
                                          applicationConfig: ApplicationConfig,
                                          val auditable: Auditable,
                                          val accountUtils: AccountUtils
                                         ) extends RawResponseReads with LoggingUtils {

  lazy val serviceURL: String = applicationConfig.servicesConfig.baseUrl("awrs-notification")

  val baseURI = "/awrs-notification"
  val cacheURI = s"$baseURI/cache"
  val markAsViewedURI = "/viewed"
  val confirmationEmailURI = s"$baseURI/email/confirmation"
  val cancellationEmailURI = s"$baseURI/email/cancellation"
  val withdrawnEmailURI = s"$baseURI/email/withdrawn"

  def fetchNotificationCache(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[StatusNotification]] = {
    val awrsRefNo = accountUtils.getAwrsRefNo(authRetrievals.enrolments)
    val getURL = s"""$serviceURL$cacheURI/$awrsRefNo"""
    mapResult("", awrsRefNo, http.GET(getURL, Seq.empty, Seq.empty)).map {
      case Some(response: HttpResponse) => Some(response.json.as[StatusNotification])
      case None => None
    }
  }

  def getNotificationViewedStatus(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ViewedStatusResponse]] = {
    val awrsRefNo = accountUtils.getAwrsRefNo(authRetrievals.enrolments)
    val getURL = s"""$serviceURL$cacheURI$markAsViewedURI/$awrsRefNo"""
    mapResult("", awrsRefNo, http.GET(getURL, Seq.empty, Seq.empty)).map {
      case Some(response: HttpResponse) => Some(response.json.as[ViewedStatusResponse])
      case None => None
    }
  }

  def markNotificationViewedStatusAsViewed(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Boolean]] = {
    val awrsRefNo = accountUtils.getAwrsRefNo(authRetrievals.enrolments)
    val getURL = s"""$serviceURL$cacheURI$markAsViewedURI/$awrsRefNo"""
    mapResult("", awrsRefNo, http.PUT(getURL, "", Seq.empty)).map {
      case Some(_: HttpResponse) => Some(true)
      case None => None
    }
  }

  // Currently the delete call will return a response with OK even if the entity does not exists, which means this method
  // will return true in the cases where the entity does not exist.
  // However, this will only ever be called call if a valid status has been found that qualifies for deletion
  def deleteFromNotificationCache(authRetrievals: StandardAuthRetrievals)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    val awrsRefNo = accountUtils.getAwrsRefNo(authRetrievals.enrolments)
    val deleteURL = s"""$serviceURL$cacheURI/$awrsRefNo"""
    mapResult("", awrsRefNo, http.DELETE(deleteURL, Seq.empty)).map {
      case Some(_) => true
      case _ => false
    }.recover {
      case _: InternalServerException =>
        logger.info("[deleteFromNotificationCache] Internal server error occurred when deleting from notification cache")
        false
    }
  }

  def sendConfirmationEmail(emailRequest: EmailRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    doEmailCall(emailRequest, auditConfirmationEmailTxName, confirmationEmailURI)
  }

  def sendCancellationEmail(emailRequest: EmailRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    doEmailCall(emailRequest, auditCancellationEmailTxName, cancellationEmailURI)
  }

  def sendWithdrawnEmail(emailRequest: EmailRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    doEmailCall(emailRequest, auditWithdrawnEmailTxtName, withdrawnEmailURI)
  }

  private def doEmailCall(emailRequest: EmailRequest, auditTxt: String, uri: String)(implicit hc: HeaderCarrier, ec: ExecutionContext) = {
    mapResult(auditTxt, emailRequest.reference.fold("")(x => x), http.POST(s"$serviceURL$uri", emailRequest, Seq.empty)).map {
      case Some(_) => true
      case _ => false
    }
  }

  private def mapResult(auditTxName: String,
                        awrsRefNo: String,
                        result: Future[HttpResponse])
                       (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[HttpResponse]] =
    result map {
      response =>
        response.status match {
          case OK | NO_CONTENT =>
            warn(f"[$auditTxName] - Successful return of data")
            if (auditTxName.nonEmpty) {
              audit(transactionName = auditTxName, detail = Map("awrsRegistrationNumber" -> awrsRefNo), eventType = eventTypeSuccess)
            }
            Some(response)
          case NOT_FOUND =>
            warn(f"[$auditTxName - $awrsRefNo ] - No data found in awrs-notification cache")
            None
          case SERVICE_UNAVAILABLE =>
            warn(f"[$auditTxName - $awrsRefNo ] - Dependant systems are currently not responding")
            throw new ServiceUnavailableException("Dependant systems are currently not responding")
          case BAD_REQUEST =>
            warn(f"[$auditTxName - $awrsRefNo ] - The request has not passed validation")
            throw new BadRequestException("The request has not passed validation")
          case INTERNAL_SERVER_ERROR =>
            warn(f"[$auditTxName - $awrsRefNo ] - WSO2 is currently experiencing problems that require live service intervention")
            throw new InternalServerException("WSO2 is currently experiencing problems that require live service intervention")
          case status@_ =>
            warn(f"[$auditTxName - $awrsRefNo ] - Unsuccessful return of data. Status code: $status")
            throw new InternalServerException(f"Unsuccessful return of data. Status code: $status")
        }
    }
}
