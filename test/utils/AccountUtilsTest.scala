/*
 * Copyright 2023 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

class AccountUtilsTest extends PlaySpec with MockitoSugar{

  val mockAccountUtils = mock[AccountUtils]

  "getUtr" must {
    "Return the user SA utr " in {
      when(mockAccountUtils.getUtr(any())).thenReturn("0123456")
      val utr = mockAccountUtils.getUtr(TestUtil.authRetrievalSAUTR)
      utr mustBe "0123456"
    }

    "Return the user CT utr for org" in {
      when(mockAccountUtils.getUtr(any())).thenReturn("6543210")
      val utr = mockAccountUtils.getUtr(TestUtil.defaultAuthRetrieval)
      utr mustBe "6543210"
    }

    "Return the fake cred id for org account" in {
      when(mockAccountUtils.getUtr(any())).thenReturn("fakeCredID")
      val utr = mockAccountUtils.getUtr(TestUtil.authRetrievalEmptySetEnrolments)
      utr mustBe "fakeCredID"
    }

    "throw exception if no details are found" in {
      when(mockAccountUtils.getUtr(any())).thenThrow(new RuntimeException("[getUtr] No UTR found"))
      val thrown = the[RuntimeException] thrownBy mockAccountUtils.getUtr(TestUtil.emptyAuthRetrieval)
      thrown.getMessage must include("[getUtr] No UTR found")
    }
  }

  "authLink" must {
    "Return the user SA link " in {
      when(mockAccountUtils.authLink(any())).thenReturn("sa/0123456")
      val utr = mockAccountUtils.authLink(TestUtil.authRetrievalSAUTR)
      utr mustBe "sa/0123456"
    }

    "Return the user CT link" in {
      when(mockAccountUtils.authLink(any())).thenReturn("org/UNUSED")
      val utr = mockAccountUtils.authLink(TestUtil.defaultAuthRetrieval)
      utr mustBe "org/UNUSED"
    }

    "throw exception if no details are found for authlink" in {
      when(mockAccountUtils.authLink(any())).thenThrow(new RuntimeException("User does not have the correct authorisation"))
      val thrown = the[RuntimeException] thrownBy mockAccountUtils.authLink(TestUtil.emptyAuthRetrieval)
      thrown.getMessage must include("User does not have the correct authorisation")
    }
  }

  "isSaAccount" must {
    "return true if it is an SA account" in {
      when(mockAccountUtils.isSaAccount(any())).thenReturn(Some(true))
      mockAccountUtils.isSaAccount(TestUtil.authRetrievalSAUTR.enrolments) mustBe Some(true)
    }

    "return None if it is not SA" in {
      when(mockAccountUtils.isSaAccount(any())).thenReturn(None)
      mockAccountUtils.isSaAccount(TestUtil.defaultAuthRetrieval.enrolments) mustBe None
    }
  }

  "isOrgAccount" must {
    "return true if it is a CT account" in {
      when(mockAccountUtils.isOrgAccount(any())).thenReturn(Some(true))
      mockAccountUtils.isOrgAccount(TestUtil.defaultAuthRetrieval) mustBe Some(true)
    }

    "return true if it is also an organisation account" in {
      when(mockAccountUtils.isOrgAccount(any())).thenReturn(Some(true))
      mockAccountUtils.isOrgAccount(TestUtil.authRetrievalSAUTR) mustBe Some(true)
    }
  }
}
