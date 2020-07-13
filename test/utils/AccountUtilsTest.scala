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
import controllers.auth.StandardAuthRetrievals
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.play.test.UnitSpec

class AccountUtilsTest extends UnitSpec with MockitoSugar{

  val accountUtils = new AccountUtils(mock[Auditable])

  "getUtr" should {
    "Return the user SA utr " in {
      val utr = accountUtils.getUtr(TestUtil.authRetrievalSAUTR)
      utr shouldBe "0123456"
    }

    "Return the user CT utr for org" in {
      val utr = accountUtils.getUtr(TestUtil.defaultAuthRetrieval)
      utr shouldBe "6543210"
    }

    "Return the fake cred id for org account" in {
      val utr = accountUtils.getUtr(TestUtil.authRetrievalEmptySetEnrolments)
      utr shouldBe "fakeCredID"
    }

    "throw exception if no details are found" in {
      val thrown = the[RuntimeException] thrownBy accountUtils.getUtr(TestUtil.emptyAuthRetrieval)
      thrown.getMessage should include("[getUtr] No UTR found")
    }
  }

  "authLink" should {
    "Return the user SA link " in {
      val utr = accountUtils.authLink(TestUtil.authRetrievalSAUTR)
      utr shouldBe "sa/0123456"
    }

    "Return the user CT link" in {
      val utr = accountUtils.authLink(TestUtil.defaultAuthRetrieval)
      utr shouldBe "org/UNUSED"
    }

    "throw exception if no details are found for authlink" in {
      val thrown = the[RuntimeException] thrownBy accountUtils.authLink(TestUtil.emptyAuthRetrieval)
      thrown.getMessage should include("User does not have the correct authorisation")
    }
  }

  "isSaAccount" should {
    "return true if it is an SA account" in {
      accountUtils.isSaAccount(TestUtil.authRetrievalSAUTR.enrolments) shouldBe Some(true)
    }

    "return None if it is not SA" in {
      accountUtils.isSaAccount(TestUtil.defaultAuthRetrieval.enrolments) shouldBe None
    }
  }

  "isOrgAccount" should {
    "return true if it is a CT account" in {
      accountUtils.isOrgAccount(TestUtil.defaultAuthRetrieval) shouldBe Some(true)
    }

    "return true if it is also an organisation account" in {
      accountUtils.isOrgAccount(TestUtil.authRetrievalSAUTR) shouldBe Some(true)
    }
  }
}
