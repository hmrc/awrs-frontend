/*
 * Copyright 2025 HM Revenue & Customs
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

package services

import models.AwrsEnrolmentUtr
import models.reenrolment._
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import utils.AwrsUnitTestTraits

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EnrolmentStoreProxyServiceTest extends AwrsUnitTestTraits with BeforeAndAfterEach with MockitoSugar {

  implicit val req: Request[AnyContent] = FakeRequest()

  def createKnownFacts(urn: String): KnownFacts = KnownFacts("HMRC-AWRS-ORG", Seq(KnownFact("AWRSRefNumber", urn)))
  val testUrn = "XKAW00000200130"
  val testUtr = AwrsEnrolmentUtr("123456789")
  val testPostcode = AwrsRegisteredPostcode("AB1 2CD")

  override def beforeEach(): Unit = {
    reset(mockEnrolmentStoreProxyConnector)
    super.beforeEach()
  }

  val testService: EnrolmentStoreProxyService = new EnrolmentStoreProxyService(mockEnrolmentStoreProxyConnector)

  "verifyKnownFacts" must {
    "return false when ES20 api return None" in {
      when(mockEnrolmentStoreProxyConnector.lookupEnrolments(createKnownFacts(testUrn)))
        .thenReturn(Future.successful(None))

      val result = testService.verifyKnownFacts(testUrn, false, testUtr, testPostcode)

      await(result) mustBe false
    }

    "return false when ES20 api return empty Enrolments for service HMRC-AWRS-ORG" in {
      when(mockEnrolmentStoreProxyConnector.lookupEnrolments(createKnownFacts(testUrn)))
        .thenReturn(Future.successful(Some(EnrolmentSuccessResponse("HMRC-AWRS-ORG", Seq.empty))))

      val result = testService.verifyKnownFacts(testUrn, false, testUtr, testPostcode)

      await(result) mustBe false
    }

    "return false when ES20 api return Enrolments where service is not HMRC-AWRS-ORG" in {
      when(mockEnrolmentStoreProxyConnector.lookupEnrolments(createKnownFacts(testUrn)))
        .thenReturn(Future.successful(Some(EnrolmentSuccessResponse("IR-SA", Seq.empty))))

      val result = testService.verifyKnownFacts(testUrn, false, testUtr, testPostcode)

      await(result) mustBe false
    }

    "return false when ES20 api return Enrolments for service HMRC-AWRS-ORG with CTUTR but no postcode match" in {
      when(mockEnrolmentStoreProxyConnector.lookupEnrolments(createKnownFacts(testUrn)))
        .thenReturn(Future.successful(Some(EnrolmentSuccessResponse("HMRC-AWRS-ORG", Seq(
          Enrolment(
            identifiers = Seq(Identifier(key = "AWRSRefNumber", value = testUrn)),
            verifiers = Seq(Verifier(key = "CTUTR", value = testUtr.utr))
          )
        )))))

      val result = testService.verifyKnownFacts(testUrn, false, testUtr, testPostcode)

      await(result) mustBe false
    }

    "return false when ES20 api return Enrolments for service HMRC-AWRS-ORG with PostCode match but no CTUTR match" in {
      when(mockEnrolmentStoreProxyConnector.lookupEnrolments(createKnownFacts(testUrn)))
        .thenReturn(Future.successful(Some(EnrolmentSuccessResponse("HMRC-AWRS-ORG", Seq(
          Enrolment(
            identifiers = Seq(Identifier(key = "AWRSRefNumber", value = testUrn)),
            verifiers = Seq(Verifier(key = "Postcode", value = testPostcode.registeredPostcode))
          )
        )))))

      val result = testService.verifyKnownFacts(testUrn, false, testUtr, testPostcode)

      await(result) mustBe false
    }

    "return false when ES20 api return Enrolments for service HMRC-AWRS-ORG but no AWRSRefNumber identifier" in {
      when(mockEnrolmentStoreProxyConnector.lookupEnrolments(createKnownFacts(testUrn)))
        .thenReturn(Future.successful(Some(EnrolmentSuccessResponse("HMRC-AWRS-ORG", Seq(
          Enrolment(
            identifiers = Seq(Identifier(key = "SomeOther", value = testUrn)),
            verifiers = Seq(Verifier(key = "CTUTR", value = testUtr.utr))
          )
        )))))

      val result = testService.verifyKnownFacts(testUrn, false, testUtr, testPostcode)

      await(result) mustBe false
    }

    "return false when ES20 api return Enrolments for service HMRC-AWRS-ORG but no match for AWRSRefNumber identifier" in {
      when(mockEnrolmentStoreProxyConnector.lookupEnrolments(createKnownFacts(testUrn)))
        .thenReturn(Future.successful(Some(EnrolmentSuccessResponse("HMRC-AWRS-ORG", Seq(
          Enrolment(
            identifiers = Seq(Identifier(key = "AWRSRefNumber", value = "NotTheSame")),
            verifiers = Seq(Verifier(key = "CTUTR", value = testUtr.utr))
          )
        )))))

      val result = testService.verifyKnownFacts(testUrn, false, testUtr, testPostcode)

      await(result) mustBe false
    }


    "return true when ES20 api return Enrolments for service HMRC-AWRS-ORG and finds AWRSRefNumber identifier and CTUTR and Postcode " in {
      when(mockEnrolmentStoreProxyConnector.lookupEnrolments(createKnownFacts(testUrn)))
        .thenReturn(Future.successful(Some(EnrolmentSuccessResponse("HMRC-AWRS-ORG", Seq(
          Enrolment(
            identifiers = Seq(Identifier(key = "AWRSRefNumber", value = testUrn)),
            verifiers = Seq(Verifier(key = "CTUTR", value = testUtr.utr), Verifier(key = "Postcode", value = testPostcode.registeredPostcode))
          )
        )))))

      val result = testService.verifyKnownFacts(testUrn, false, testUtr, testPostcode)

      await(result) mustBe true
    }

    "return true when ES20 api return Enrolments for service HMRC-AWRS-ORG and finds AWRSRefNumber identifier and SAUTR and Postcode " in {
      when(mockEnrolmentStoreProxyConnector.lookupEnrolments(createKnownFacts(testUrn)))
        .thenReturn(Future.successful(Some(EnrolmentSuccessResponse("HMRC-AWRS-ORG", Seq(
          Enrolment(
            identifiers = Seq(Identifier(key = "AWRSRefNumber", value = testUrn)),
            verifiers = Seq(Verifier(key = "SAUTR", value = testUtr.utr), Verifier(key = "Postcode", value = testPostcode.registeredPostcode))
          )
        )))))

      val result = testService.verifyKnownFacts(testUrn, true, testUtr, testPostcode)

      await(result) mustBe true
    }

  }
}
