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

package controllers

import config.{AwrsFrontendAuditConnector, FrontendAuthConnector}
import controllers.auth.{AuthFunctionality, ExternalUrls}
import models.{Address, AddressAudit, AddressAudits}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import services._
import services.helper.AddressComparator
import uk.gov.hmrc.address.client.v1.RecordSet
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.LoggingUtils

import scala.concurrent.Future

trait AddressLookupController extends FrontendController with HasAddressLookupService with LoggingUtils with AuthFunctionality {

  def addressLookup(postcode: String): Action[AnyContent] = Action.async {
    implicit request => authorisedAction {_ =>
      implicit val writes = Json.format[RecordSet]
      val validPostcodeCharacters = "^[A-z0-9 ]*$"
      if (postcode.matches(validPostcodeCharacters)) {
        addressLookupService.lookup(postcode) map {
          case AddressLookupErrorResponse(e: BadRequestException) => BadRequest(e.message)
          case AddressLookupErrorResponse(e) => InternalServerError
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
            case Some(addressCountry) => Map(
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
    implicit request => authorisedAction { _ =>

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
              } else {

              }
            case _ =>
          }
      }
      Future.successful(Ok)
    }
  }

  @inline private def areAddressesDifferent(toAddress: Option[Address], fromAddress: Option[Address]) = AddressComparator.isDifferent(toAddress, fromAddress)
}

object AddressLookupController extends AddressLookupController {
  override val addressLookupService = AddressLookupService
  val authConnector = FrontendAuthConnector
  val signInUrl = ExternalUrls.signIn
  override def appName: String = "awrs-frontend"

  override def audit: Audit = new Audit(appName, AwrsFrontendAuditConnector)
}
