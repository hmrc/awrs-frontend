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

package connectors

import config.{AwrsFrontendAuditConnector, WSHttp}
import metrics.AwrsMetrics
import models._
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.{HeaderCarrier, InternalServerException, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.http.Status._
import utils.{AccountUtils, LoggingUtils}

trait AWRSNotificationConnector extends ServicesConfig with RawResponseReads with LoggingUtils {

  lazy val serviceURL = baseUrl("awrs-notification")

  val baseURI = "/awrs-notification"
  val cacheURI = s"$baseURI/cache"
  val markAsViewedURI = "/viewed"
  val confirmationEmailURI = s"$baseURI/email/confirmation"
  val cancellationEmailURI = s"$baseURI/email/cancellation"

  val httpGet: HttpGet
  val httpDelete: HttpDelete
  val httpPut: HttpPut
  val httpPost: HttpPost
  val metrics: AwrsMetrics

  def fetchNotificationCache(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[StatusNotification]] = {
    val awrsRefNo = AccountUtils.getAwrsRefNo.toString
    val getURL = s"""$serviceURL$cacheURI/$awrsRefNo"""
    mapResult(auditAPI12TxName, awrsRefNo, httpGet.GET(getURL)).map {
      case Some(response: HttpResponse) => Some(response.json.as[StatusNotification])
      case None => None
    }
  }

  def getNotificationViewedStatus(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[ViewedStatusResponse]] = {
    val awrsRefNo = AccountUtils.getAwrsRefNo.toString
    val getURL = s"""$serviceURL$cacheURI$markAsViewedURI/$awrsRefNo"""
    mapResult(auditAPI12ViewStatusName, awrsRefNo, httpGet.GET(getURL)).map {
      case Some(response: HttpResponse) => Some(response.json.as[ViewedStatusResponse])
      case None => None
    }
  }

  def markNotificationViewedStatusAsViewed(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[Boolean]] = {
    val awrsRefNo = AccountUtils.getAwrsRefNo.toString
    val getURL = s"""$serviceURL$cacheURI$markAsViewedURI/$awrsRefNo"""
    mapResult(s"$auditAPI12ViewStatusName-Viewed", awrsRefNo, httpPut.PUT(getURL, "")).map {
      case Some(response: HttpResponse) => Some(true)
      case None => None
    }
  }

  // Currently the delete call will return a response with OK even if the entity does not exists, which means this method
  // will return true in the cases where the entity does not exist.
  // However, this will only ever be called call if a valid status has been found that qualifies for deletion
  def deleteFromNotificationCache(implicit user: AuthContext, hc: HeaderCarrier): Future[Boolean] = {
    val awrsRefNo = AccountUtils.getAwrsRefNo.toString
    val deleteURL = s"""$serviceURL$cacheURI/$awrsRefNo"""
    mapResult(auditAPI12DeleteTxName, awrsRefNo, httpDelete.DELETE(deleteURL)).map {
      case Some(_) => true
      case _ => false
    }.recover {
      case e: InternalServerException => false
    }
  }

  def sendConfirmationEmail(emailRequest: ConfirmationEmailRequest)(implicit user: AuthContext, hc: HeaderCarrier): Future[Boolean] = {
    doEmailCall(emailRequest,auditConfirmationEmailTxName,confirmationEmailURI)
  }

  def sendCancellationEmail(emailRequest: ConfirmationEmailRequest)(implicit user: AuthContext, hc: HeaderCarrier): Future[Boolean] ={
    doEmailCall(emailRequest,auditCancellationEmailTxName,cancellationEmailURI)
  }

  private def doEmailCall(emailRequest: ConfirmationEmailRequest, auditTxt: String, uri: String)(implicit hc:HeaderCarrier, user: AuthContext) = {
    mapResult(auditTxt, emailRequest.reference, httpPost.POST(s"$serviceURL${uri}", emailRequest)).map {
      case Some(_) => true
      case _ => false
    }
  }

  private def mapResult(auditTxName: String, awrsRefNo: String, result: Future[HttpResponse])(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[HttpResponse]] =
    result map {
      response =>
        response.status match {
          case OK =>
            warn(f"[$auditTxName] - Successful return of data")
            audit(transactionName = auditTxName, detail = Map("awrsRegistrationNumber" -> awrsRefNo), eventType = eventTypeSuccess)
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

object AWRSNotificationConnector extends AWRSNotificationConnector {
  override val appName = "awrs-frontend"
 override val metrics = AwrsMetrics
  override val audit: Audit = new Audit(AppName.appName, AwrsFrontendAuditConnector)

  override val httpGet: HttpGet = WSHttp
  override val httpDelete: HttpDelete = WSHttp
  override val httpPut: HttpPut = WSHttp
  override val httpPost: HttpPost = WSHttp
}
