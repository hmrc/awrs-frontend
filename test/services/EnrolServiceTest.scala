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

package services

import _root_.models._
import connectors.TaxEnrolmentsConnector
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import utils.AwrsUnitTestTraits
import utils.TestConstants._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class EnrolServiceTest extends AwrsUnitTestTraits {
  val userId = ""
  val saUtr: String = testUtr
  val ctUtr: String = testCTUtr
  val enrolRequestSAUTR =  EnrolRequest(portalId = "Default", serviceName = "HMRC-AWRS-ORG",
    friendlyName = "AWRS Enrolment", knownFacts = Seq("XAAW000000123456","",saUtr,"postcode"))
  val enrolRequestCTUTR =  EnrolRequest(portalId = "Default", serviceName = "HMRC-AWRS-ORG",
    friendlyName = "AWRS Enrolment", knownFacts = Seq("XAAW000000123456",ctUtr,"","postcode"))
  val enrolRequestNoSACT =  EnrolRequest(portalId = "Default", serviceName = "HMRC-AWRS-ORG",
    friendlyName = "AWRS Enrolment", knownFacts = Seq("XAAW000000123456","","","postcode"))
  val successfulEnrolResponse = Some(EnrolResponse(serviceName = "AWRS", state = "Not-activated",
    identifiers = List(Identifier("AWRS","Awrs-ref-no"))))
  val sourceId: String = "AWRS"
  val testBusinessCustomerDetails = BusinessCustomerDetails("ACME", Some("SOP"),
    BCAddress("line1", "line2", Option("line3"), Option("line4"), Option("post code"), Option("country")),
    "sap123", "safe123", false, Some("agent123"))
  val businessType = "LTD"
  val businessTypeSOP = "SOP"

  val successfulSubscriptionResponse = SuccessfulSubscriptionResponse("","XAAW000000123456","")
  val mockAuthConnector: DefaultAuthConnector = mock[DefaultAuthConnector]
  val mockTaxEnrolmentsConnector: TaxEnrolmentsConnector = mock[TaxEnrolmentsConnector]

  val enrolServiceTest: EnrolService = new EnrolService(mockTaxEnrolmentsConnector, mockAuthConnector)

  val enrolServiceEMACTest: EnrolService = new EnrolService(mockTaxEnrolmentsConnector, mockAuthConnector)

  override def beforeEach(): Unit = {
    reset(mockTaxEnrolmentsConnector)
  }

  "Enrol Service" must {
    behave like enrolService(enrolServiceTest)
  }

  "Enrol Service with EMAC switched on" must {
    behave like enrolService(enrolServiceEMACTest)
  }

  def mockAuthorise[T](predicate: Predicate, retrieval: Retrieval[T])(result: T): Unit =
    when(mockAuthConnector.authorise[T](
      ArgumentMatchers.eq(predicate),
      ArgumentMatchers.any[Retrieval[T]]
    )(ArgumentMatchers.any[HeaderCarrier], ArgumentMatchers.any[ExecutionContext]))
      .thenReturn(Future.successful(result))

  val testCredId = ""
  val testGroupId = ""

  def enrolService(enrolService: EnrolService): Unit = {
    "fetch data if found in save4later" in {
      mockAuthorise(EmptyPredicate, Retrievals.credentials and Retrievals.groupIdentifier)(new ~(Some(Credentials(testCredId, enrolService.GGProviderId)), Some(testGroupId)))
      when(mockTaxEnrolmentsConnector.enrol(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(successfulEnrolResponse))
      val result = enrolService.enrolAWRS(successfulSubscriptionResponse.awrsRegistrationNumber,
        testBusinessCustomerDetails, businessType, Some(testUtr))
      await(result) mustBe successfulEnrolResponse
    }

    "create correct EnrolRequest when business type is SOP and UTR present " in {
      val result = enrolService.createEnrolment(successfulSubscriptionResponse.awrsRegistrationNumber, testBusinessCustomerDetails, businessTypeSOP, saUtr)
      result mustBe enrolRequestSAUTR
    }

    "create correct EnrolRequest when business type is other than SOP and UTR present " in {
      val result = enrolService.createEnrolment(successfulSubscriptionResponse.awrsRegistrationNumber, testBusinessCustomerDetails, "LTD", ctUtr)
      result mustBe enrolRequestCTUTR
    }
    "create correct EnrolRequest when business type and UTR NOT present " in {
      val result = enrolService.createEnrolment(successfulSubscriptionResponse.awrsRegistrationNumber, testBusinessCustomerDetails,"" , None)
      result mustBe enrolRequestNoSACT
    }
  }

}
