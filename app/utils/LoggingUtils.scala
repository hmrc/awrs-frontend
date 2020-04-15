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

package utils

import audit.Auditable
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

// This logging utility should be used to replace any manual logging or Splunk auditing
// This means that any Splunk audit calls will automatically be logged as DEBUG to aid local debugging but not appear in
// the production logs. All trace and debug calls will only appear locally so should only be used for local debugging
// and not for anything that you would want to see logged in production.
trait LoggingUtils {

  val auditable: Auditable

  final val auditAPI4TxName: String = "API4"
  final val auditAPI5TxName: String = "API5"
  final val auditAPI6TxName: String = "API6"
  final val auditAPI3TxName: String = "API3 - Update Group Business Partner"
  final val auditAPI8TxName: String = "API8 - Withdraw Application"
  final val auditAPI9TxName: String = "API9 - View Application Status"
  final val auditAPI10TxName: String = "API10 - Deregister From AWRS"
  final val auditAPI11TxName: String = "API11 - View Decision Text"
  final val auditConfirmationEmailTxName: String = "API-Confirmation"
  final val auditCancellationEmailTxName: String = "API-Cancellation"
  final val auditWithdrawnEmailTxtName: String = "API-Withdrawn"
  final val auditSubscribeTxName: String = "AWRS ETMP Subscribe"
  final val auditGGTxName: String = "AWRS GG Enrol"
  final val auditEMACTxName: String = "AWRS EMAC Enrol"
  final val auditEmailVerification: String = "AWRS Send Email Verification"
  final val auditVerifyEmail: String = "AWRS Verify Email Address"

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
    auditable.sendDataEvent(
      transactionName = transactionName,
      detail = detail,
      eventType = eventType
    )
  }

  def audit(transactionName: String, detail: Map[String, String], eventType: String)(implicit hc: HeaderCarrier) = splunkFunction(transactionName, detail, eventType)

  @inline def trace(msg: String): Unit = Logger.trace(msg)
  @inline def debug(msg: String): Unit = Logger.debug(msg)
  @inline def info(msg: String): Unit = Logger.info(msg)
  @inline def warn(msg: String): Unit = Logger.warn(msg)
  @inline def err(msg: String): Unit = Logger.error(msg)
}
