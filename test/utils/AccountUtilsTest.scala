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
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

class AccountUtilsTest extends PlaySpec with MockitoSugar{

  val accountUtils = new AccountUtils(mock[Auditable])

  "getUtr" must {
    "Return the user SA utr " in {
      val utr = accountUtils.getUtr(TestUtil.authRetrievalSAUTR)
      utr mustBe "0123456"
    }

    "Return the user CT utr for org" in {
      val utr = accountUtils.getUtr(TestUtil.defaultAuthRetrieval)
      utr mustBe "6543210"
    }

    "Return the fake cred id for org account" in {
      val utr = accountUtils.getUtr(TestUtil.authRetrievalEmptySetEnrolments)
      utr mustBe "fakeCredID"
    }

    "throw exception if no details are found" in {
      val thrown = the[RuntimeException] thrownBy accountUtils.getUtr(TestUtil.emptyAuthRetrieval)
      thrown.getMessage must include("[getUtr] No UTR found")
    }
  }

  "authLink" must {
    "Return the user SA link " in {
      val utr = accountUtils.authLink(TestUtil.authRetrievalSAUTR)
      utr mustBe "sa/0123456"
    }

    "Return the user CT link" in {
      val utr = accountUtils.authLink(TestUtil.defaultAuthRetrieval)
      utr mustBe "org/UNUSED"
    }

    "throw exception if no details are found for authlink" in {
      val thrown = the[RuntimeException] thrownBy accountUtils.authLink(TestUtil.emptyAuthRetrieval)
      thrown.getMessage must include("User does not have the correct authorisation")
    }
  }

  "isSaAccount" must {
    "return true if it is an SA account" in {
      accountUtils.isSaAccount(TestUtil.authRetrievalSAUTR.enrolments) mustBe Some(true)
    }

    "return None if it is not SA" in {
      accountUtils.isSaAccount(TestUtil.defaultAuthRetrieval.enrolments) mustBe None
    }
  }

  "isOrgAccount" must {
    "return true if it is a CT account" in {
      accountUtils.isOrgAccount(TestUtil.defaultAuthRetrieval) mustBe Some(true)
    }

    "return true if it is also an organisation account" in {
      accountUtils.isOrgAccount(TestUtil.authRetrievalSAUTR) mustBe Some(true)
    }
  }
}
