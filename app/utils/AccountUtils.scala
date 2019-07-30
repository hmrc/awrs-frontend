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

package utils

import audit.Auditable
import controllers.auth.StandardAuthRetrievals
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment}

@Singleton
class AccountUtils @Inject()(val auditable: Auditable) extends LoggingUtils {

  def getUtr(authRetrievals: StandardAuthRetrievals): String = {
    val firstUtr = (authRetrievals.enrolments flatMap { enrolment =>
      enrolment.identifiers.filter(_.key.toLowerCase == "utr")
    }).headOption

    (firstUtr, authRetrievals.affinityGroup) match {
      case (Some(utr), _) => utr.value.toString
      case (_, Some(org)) if org equals AffinityGroup.Organisation => authRetrievals.credId
      case (_, affinityGroup) => throw new RuntimeException(s"[getUtr] No UTR found and affinity group was ${affinityGroup.getOrElse("None")}")
    }
  }

  def getAuthType(legalEntityType: String, authRetrievals: StandardAuthRetrievals): String = {

    legalEntityType match {
      case "SOP" => authRetrievals.enrolments.find(_.key == "IR-SA") match {
        case Some(enrolment) => s"sa/${enrolment.identifiers.find(_.key == "UTR").get.value}"
        case _ =>
          warn("[getAuthType] No SA enrolment")
          throw new RuntimeException("[getAuthType] No SA enrolment")
      }
      case _ =>
        if (authRetrievals.affinityGroup.get == AffinityGroup.Organisation) {
          "org/UNUSED"
        } else {
          warn("[getAuthType] Not an organisation account")
          throw new RuntimeException("[getAuthType] Not an organisation account")
        }
    }
  }

  def authLink(authRetrievals: StandardAuthRetrievals): String = {
    (authRetrievals.affinityGroup, authRetrievals.enrolments.find(_.key == "IR-SA")) match {
      case (Some(AffinityGroup.Organisation), _) => "org/UNUSED"
      case (_,            Some(enrolment)) => s"sa/${enrolment.identifiers.find(_.key == "UTR").get.value}"
      case _ => throw new RuntimeException("User does not have the correct authorisation")
    }
  }

  def isSaAccount(enrolments: Set[Enrolment]): Option[Boolean] = {
    Some(enrolments.exists(_.key == "IR-SA")).filter(identity)
  }

  def isOrgAccount(authRetrievals: StandardAuthRetrievals): Option[Boolean] = {
    Some(authRetrievals.enrolments.exists(_.key == "IR-CT") || authRetrievals.affinityGroup.contains(AffinityGroup.Organisation))
      .filter(identity)
  }

  def hasAwrs(enrolments: Set[Enrolment]): Boolean = {
    enrolments.exists(_.key == "HMRC-AWRS-ORG")
  }

  def getAwrsRefNo(enrolments: Set[Enrolment]): String = {
    val refno: Option[String] = enrolments.collectFirst {
      case enrolment if enrolment.key == "HMRC-AWRS-ORG" => enrolment.identifiers.find(_.key == "AWRSRefNumber").get.value
    }

    refno.get
  }
}
