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

package utils

import audit.Auditable
import play.api.Logger
import uk.gov.hmrc.play.http.HeaderCarrier

// This logging utility should be used to replace any manual logging or Splunk auditing
// This means that any Splunk audit calls will automatically be logged as DEBUG to aid local debugging but not appear in
// the production logs. All trace and debug calls will only appear locally so should only be used for local debugging
// and not for anything that you would want to see logged in production.
trait LoggingUtils extends Auditable {

  final val auditAPI4TxName: String = "API4"
  final val auditAPI5TxName: String = "API5"
  final val auditAPI6TxName: String = "API6"
  final val auditAPI8TxName: String = "API8"
  final val auditAPI9TxName: String = "API9"
  final val auditAPI10TxName: String = "API10"
  final val auditAPI11TxName: String = "API11"
  final val auditAPI12TxName: String = "API12"
  final val auditAPI12ViewStatusName: String = "API12-View-Status"
  final val auditAPI12DeleteTxName: String = "API12-Delete"
  final val auditConfirmationEmailTxName: String = "API-Confirmation"
  final val auditSubscribeTxName: String = "AWRS ETMP Subscribe"
  final val auditGGTxName: String = "AWRS GG Enrol"

  final val eventTypeSuccess: String = "AwrsSuccess"
  final val eventTypeFailure: String = "AwrsFailure"
  final val eventTypeBadRequest: String = "BadRequest"
  final val eventTypeNotFound: String = "NotFound"
  final val eventTypeInternalServerError: String = "InternalServerError"
  final val eventTypeGeneric: String = "UnexpectedError"

  final val postcodeAddressSubmitted: String = "postcodeAddressSubmitted"
  final val postcodeAddressModifiedSubmitted: String = "postcodeAddressModifiedSubmitted"
  final val manualAddressSubmitted: String = "manualAddressSubmitted"
  final val internationalAddressSubmitted: String = "internationalAddressSubmitted"

  final val splunkString = "SPLUNK AUDIT:\n"

  private def splunkToLogger(transactionName: String, detail: Map[String, String], eventType: String): String =
    s"${if (eventType.nonEmpty) eventType + "\n"}$transactionName\n$detail"

  private def splunkFunction(transactionName: String, detail: Map[String, String], eventType: String)(implicit hc: HeaderCarrier) = {
    Logger.debug(splunkString + splunkToLogger(transactionName, detail, eventType))
    sendDataEvent(
      transactionName = transactionName,
      detail = detail,
      eventType = eventType
    )
  }

  def audit(transactionName: String, detail: Map[String, String], eventType: String)(implicit hc: HeaderCarrier) = splunkFunction(transactionName, detail, eventType)

  @inline def trace(msg: String) = Logger.trace(msg)

  @inline def trace(transactionName: String, detail: Map[String, String], eventType: String = ""): Unit = trace(splunkToLogger(transactionName, detail, eventType))

  @inline def debug(msg: String) = Logger.debug(msg)

  @inline def debug(transactionName: String, detail: Map[String, String], eventType: String = ""): Unit = debug(splunkToLogger(transactionName, detail, eventType))

  @inline def info(msg: String) = Logger.info(msg)

  @inline def info(transactionName: String, detail: Map[String, String], eventType: String = ""): Unit = info(splunkToLogger(transactionName, detail, eventType))

  @inline def warn(msg: String) = Logger.warn(msg)

  @inline def warn(transactionName: String, detail: Map[String, String], eventType: String = ""): Unit = warn(splunkToLogger(transactionName, detail, eventType))

  @inline def err(msg: String) = Logger.error(msg)

  @inline def err(transactionName: String, detail: Map[String, String], eventType: String = ""): Unit = err(splunkToLogger(transactionName, detail, eventType))

}
