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

package controllers

import audit.Auditable
import config.ApplicationConfig
import connectors.{AddressLookupConnector, AddressLookupErrorResponse, AddressLookupSuccessResponse, HasAddressLookupConnector}
import controllers.auth.AuthFunctionality
import javax.inject.Inject
import models.{Address, AddressAudit, AddressAudits}
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.helper.AddressComparator
import uk.gov.hmrc.address.client.v1.RecordSet
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.LoggingUtils

import scala.concurrent.{ExecutionContext, Future}

class AddressLookupController @Inject()(mcc: MessagesControllerComponents,
                                        val authConnector: DefaultAuthConnector,
                                        val addressLookupConnector: AddressLookupConnector,
                                        val auditable: Auditable,
                                        implicit val applicationConfig: ApplicationConfig) extends FrontendController(mcc) with HasAddressLookupConnector with LoggingUtils with AuthFunctionality {

  implicit val ec: ExecutionContext = mcc.executionContext
  val signInUrl: String = applicationConfig.signIn

  def addressLookup(postcode: String): Action[AnyContent] = Action.async {
    implicit request =>
      authorisedAction { _ =>
        implicit val writes: OFormat[RecordSet] = Json.format[RecordSet]
        val validPostcodeCharacters = "^[A-z0-9 ]*$"
        if (postcode.matches(validPostcodeCharacters)) {
          addressLookupConnector.lookup(postcode) map {
            case AddressLookupErrorResponse(e: BadRequestException) => BadRequest(e.message)
            case AddressLookupErrorResponse(_) => InternalServerError
            case AddressLookupSuccessResponse(recordSet) => Ok(writes.writes(recordSet))
          }
        } else {
          Future.successful(BadRequest("missing or badly-formed postcode parameter"))
        }
      }
  }

  def addressToAuditMap(key: String, someAddress: Option[Address]): Map[String, String] =
    someAddress match {
      case Some(address) =>
        Map(
          (key + ".line1", address.addressLine1),
          (key + ".line2", address.addressLine2),
          (key + ".line3", address.addressLine3.fold("")(x => x)),
          (key + ".line4", address.addressLine4.fold("")(x => x))
        ) ++ {
          // if there isn't a post code then don't include it
          address.postcode match {
            case Some(postcode) => Map((key + ".postcode", postcode))
            case _ => Map()
          }
        } ++ {
          // if the address is not foreign don't include these
          address.addressCountry match {
            case Some(_) => Map(
              (key + ".addressCountry", address.addressCountry.fold("")(x => x))
            )
            case _ => Map()
          }
        }
      case _ => Map((key, "")) // this is an erroneous state
    }


  def auditAddressMap(addressAudit: AddressAudit, fromAddress: Boolean = true, toAddress: Boolean = true): Map[String, String] = {
    if (fromAddress) {
      addressToAuditMap("fromAddress", addressAudit.fromAddress)
    } else {
      Map[String, String]()
    }
  } ++ {
    if (toAddress) {
      addressToAuditMap("toAddress", addressAudit.toAddress)
    } else {
      Map()
    }
  }


  def auditAddress(): Action[AnyContent] = Action.async {
    implicit request =>
      authorisedAction { _ =>

        val addressToAudit = request.body.asJson match {
          case Some(js) => js.as[AddressAudits]
          case None => AddressAudits(addressAudits = List())
        }

        addressToAudit.addressAudits.foreach {
          toAudit: AddressAudit =>
            lazy val uprn = Map(("uprn", toAudit.uprn.fold("")(x => x)))

            toAudit.eventType match {
              case Some(`postcodeAddressSubmitted`) =>
                if (areAddressesDifferent(toAudit.toAddress, toAudit.fromAddress)) {
                  audit(transactionName = toAudit.auditPointId.get, detail = auditAddressMap(toAudit) ++ uprn, eventType = postcodeAddressModifiedSubmitted)
                } else {
                  audit(transactionName = toAudit.auditPointId.get, detail = auditAddressMap(toAudit, fromAddress = false) ++ uprn, eventType = toAudit.eventType.get)
                }
              case Some(`manualAddressSubmitted` | `internationalAddressSubmitted`) =>
                if (areAddressesDifferent(toAudit.toAddress, toAudit.fromAddress)) {
                  audit(transactionName = toAudit.auditPointId.get, detail = auditAddressMap(toAudit), eventType = toAudit.eventType.get)
                }
              case _ =>
            }
        }
        Future.successful(Ok)
      }
  }

  @inline private def areAddressesDifferent(toAddress: Option[Address], fromAddress: Option[Address]) = AddressComparator.isDifferent(toAddress, fromAddress)
}
