/*
 * Copyright 2018 HM Revenue & Customs
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

package builders

import org.mockito.Matchers
import org.mockito.Mockito._
import uk.gov.hmrc.domain._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import utils.TestConstants._
import scala.concurrent.Future

object AuthBuilder {

  def createUserAuthContext(userId: String, userName: String): AuthContext = {
    val orgAuthority = Authority(userId, Accounts(org = Some(OrgAccount("org/1234", Org("1234")))), None, None, CredentialStrength.Strong, ConfidenceLevel.L50, None, None, None, "")
    AuthContext(authority = orgAuthority, nameFromSession = Some(userName))
  }

  def createUserAuthContextIndCt(userId: String, userName: String, ctutr: String): AuthContext = {
    val ctAuthority = Authority(userId, Accounts(ct = Some(CtAccount(userId, CtUtr(ctutr)))), None, None, CredentialStrength.Strong, ConfidenceLevel.L50, None, None, None, "")
    AuthContext(authority = ctAuthority, nameFromSession = Some(userName))
  }

  def createUserAuthContextIndSa(userId: String, userName: String, sautr: String): AuthContext = {
    val saAuthority = Authority(userId, Accounts(sa = Some(SaAccount(s"/sa/individual/$sautr", SaUtr(sautr)))), None, None, CredentialStrength.Strong, ConfidenceLevel.L50, None, None, None, "")
    AuthContext(authority = saAuthority, nameFromSession = Some(userName))
  }

  def createUserAuthContextIndSaWithAWRS(userId: String, userName: String, sautr: String): AuthContext = {
    val saAuthority = Authority(userId, Accounts(sa = Some(SaAccount(userId, SaUtr(sautr))),
      awrs = Some(AwrsAccount(userId, AwrsUtr(testAWRSUtr)))), None, None, CredentialStrength.Strong, ConfidenceLevel.L50, None, None, None, "")
    AuthContext(authority = saAuthority, nameFromSession = Some(userName))
  }

  def createUserAuthContextOrg(userId: String, userName: String, org: String): AuthContext = {
    val ctAuthority = Authority(userId, Accounts(org = Some(OrgAccount(s"/org/$org", Org(org)))), None, None, CredentialStrength.Strong, ConfidenceLevel.L50, None, None, None, "")
    AuthContext(authority = ctAuthority, nameFromSession = Some(userName))
  }

  def createUserAuthContextOrgWithAWRS(userId: String, userName: String, org: String): AuthContext = {
    val ctAuthority = Authority(userId, Accounts(org = Some(OrgAccount(s"/org/$org", Org(org))),
      awrs = Some(AwrsAccount(userId, AwrsUtr(testAWRSUtr)))), None, None, CredentialStrength.Strong, ConfidenceLevel.L50, None, None, None, "")
    AuthContext(authority = ctAuthority, nameFromSession = Some(userName))
  }

  def createAuthContextWithOrWithoutAWWRS(userId: String, userName: String, org: String, hasAwrs: Boolean): AuthContext = {
    val mockAuthority = Authority(userId, Accounts(awrs = hasAwrs match {
        case true => Some(AwrsAccount(userId, AwrsUtr(testAWRSUtr)))
        case _ => None
      }, paye = Some(PayeAccount(userId, Nino("AA026813B")))), None, None, CredentialStrength.Strong, ConfidenceLevel.L50, None, None, None, "")
    AuthContext(authority = mockAuthority, nameFromSession = Some(userName))
  }



  def createUserAuthFailure(userId: String, userName: String): AuthContext = {
    val ctAuthority = Authority(userId, Accounts(epaye = Some(EpayeAccount("emp", EmpRef("745", "TZ00055")))), None, None, CredentialStrength.Strong, ConfidenceLevel.L50, None, None, None, "")
    AuthContext(authority = ctAuthority, nameFromSession = Some(userName))
  }

  def mockAuthorisedUser(userId: String, mockAuthConnector: AuthConnector, utr: String) {
    when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())) thenReturn {
      utr match {
        case "ct" =>
          val ctAuthority = Authority(userId, Accounts(ct = Some(CtAccount(userId, CtUtr(testCTUtr)))), None, None, CredentialStrength.Strong, ConfidenceLevel.L50, None, None, None, "")
          Future.successful(Some(ctAuthority))
        case "sa" =>
          val saAuthority = Authority(userId, Accounts(sa = Some(SaAccount(userId, SaUtr(testUtr)))), None, None, CredentialStrength.Strong, ConfidenceLevel.L50, None, None, None, "")
          Future.successful(Some(saAuthority))
        case "sa_ct" =>
          val saCtAuthority = Authority(userId, Accounts(sa = Some(SaAccount(userId, SaUtr(testUtr))), ct = Some(CtAccount(userId, CtUtr(testCTUtr)))), None, None, CredentialStrength.Strong, ConfidenceLevel.L50, None, None, None, "")
          Future.successful(Some(saCtAuthority))
        case "awrs" =>
          val awrsAuthority = Authority(userId, Accounts(awrs = Some(AwrsAccount(userId, AwrsUtr(testAWRSUtr))), sa = Some(SaAccount(userId, SaUtr(testUtr)))), None, None, CredentialStrength.Strong, ConfidenceLevel.L50, None, None, None, "")
          Future.successful(Some(awrsAuthority))
      }
    }
  }

  def mockUnAuthorisedUser(userId: String, mockAuthConnector: AuthConnector) {
    when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())) thenReturn {
      val payeAuthority = Authority(userId, Accounts(paye = Some(PayeAccount(userId, Nino(testNino)))), None, None,CredentialStrength.Strong, ConfidenceLevel.L50, None, None, None, "")
      Future.successful(Some(payeAuthority))
    }
  }

  def mockUnAuthorisedAgentUser(userId: String, mockAuthConnector: AuthConnector) {
    when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())) thenReturn {
      val agentAuthority = Authority(userId, Accounts(agent = Some(AgentAccount(link = "agency", agentCode = AgentCode("ABC123"), agentUserId = AgentUserId("12345"), agentUserRole = AgentAdmin, payeReference = None))), None, None,CredentialStrength.Strong, ConfidenceLevel.L50, None, None, None, "")
      Future.successful(Some(agentAuthority))
    }
  }
}
