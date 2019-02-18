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

import uk.gov.hmrc.domain.AwrsUtr
import uk.gov.hmrc.play.frontend.auth.AuthContext

object AccountUtils extends AccountUtils

trait AccountUtils extends LoggingUtils {

  def getUtrOrName()(implicit user: AuthContext) = {
    (user.principal.accounts.sa, user.principal.accounts.ct, user.principal.accounts.org) match {
      case (Some(sa), _, _) => sa.utr.utr
      case (None, Some(ct), _) => ct.utr.utr
      case (None, None, Some(org)) => org.org.org
      case _ => throw new RuntimeException("No data found")
    }
  }

  def getAuthType(legalEntityType: String)(implicit user: AuthContext) = {

    legalEntityType match {
      case "SOP" => user.principal.accounts.sa.getOrElse {
        warn( "AWRS User Enrollment" + user.principal.accounts.toMap.toString())
        throw new RuntimeException("sa link not found")
      }.link.replace("/individual","")
      case _ => user.principal.accounts.org.getOrElse {
        warn( "AWRS User Enrollment" + user.principal.accounts.toMap.toString())
        throw new RuntimeException("Org link not found")
      }.link
    }
  }

  def authLink(implicit user: AuthContext): String = {
    (user.principal.accounts.org, user.principal.accounts.sa, user.principal.accounts.agent) match {
      case (Some(orgAccount), _, _) => orgAccount.link
      case (None, Some(saAccount), _) => saAccount.link.replaceAllLiterally("/individual", "")
      case _ => throw new RuntimeException("User does not have the correct authorisation")
    }
  }

  def isSaAccount()(implicit user: AuthContext) = {
    user.principal.accounts.sa.isDefined match {
      case true => Some(true)
      case _ => None
    }
  }

  def isOrgAccount()(implicit user: AuthContext) = {
    user.principal.accounts.ct.isDefined match {
      case true => Some(true)
      case _ => {
        user.principal.accounts.org.isDefined match {
          case true => Some(true)
          case _ => None
        }
      }
    }
  }

  def hasAwrs(implicit user: AuthContext): Boolean = {
    user.principal.accounts.awrs.isDefined
  }

  def getAwrsRefNo(implicit user: AuthContext): AwrsUtr = {
    user.principal.accounts.awrs.get.utr
  }
}
