/*
 * Copyright 2017 HM Revenue & Customs
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
import connectors.GovernmentGatewayConnector
import org.mockito.Matchers
import org.mockito.Mockito._
import utils.AwrsUnitTestTraits
import utils.TestConstants._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future



class EnrolServiceTest extends AwrsUnitTestTraits {
  val mockGovernmentGatewayConnector: GovernmentGatewayConnector = mock[GovernmentGatewayConnector]

  val saUtr: String = testUtr
  val ctUtr: String = testCTUtr
  val enrolRequestSAUTR =  EnrolRequest(portalId = "Default", serviceName = "HMRC-AWRS-ORG", friendlyName = "AWRS Enrolment", knownFacts = Seq("XAAW000000123456",saUtr,"postcode"))
  val enrolRequestCTUTR =  EnrolRequest(portalId = "Default", serviceName = "HMRC-AWRS-ORG", friendlyName = "AWRS Enrolment", knownFacts = Seq("XAAW000000123456",ctUtr,"postcode"))
  val enrolRequestNoSACT =  EnrolRequest(portalId = "Default", serviceName = "HMRC-AWRS-ORG", friendlyName = "AWRS Enrolment", knownFacts = Seq("XAAW000000123456","postcode"))
  val successfulEnrolResponse = Some(EnrolResponse(serviceName = "AWRS", state = "Not-activated", identifiers = List(Identifier("AWRS","Awrs-ref-no"))))
  val sourceId: String = "AWRS"
  val testBusinessCustomerDetails = BusinessCustomerDetails("ACME", Some("SOP"), BCAddress("line1", "line2", Option("line3"), Option("line4"), Option("post code"), Option("country")),"sap123", "safe123", false, Some("agent123"))
  val businessType = "LTD"
  val businessTypeSOP = "SOP"

  val successfulSubscriptionResponse = SuccessfulSubscriptionResponse("","XAAW000000123456","")

  object EnrolServiceTest extends EnrolService {
    override val ggConnector = mockGovernmentGatewayConnector
  }

  override def beforeEach(): Unit = {
    reset(mockGovernmentGatewayConnector)
  }
  "Enrol Service" should {
    "use the correct DataCacheconnector" in {
      EnrolService.ggConnector shouldBe GovernmentGatewayConnector
    }

    "fetch data if found in save4later" in {
      when(mockGovernmentGatewayConnector.enrol(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(successfulEnrolResponse))
      val result = EnrolServiceTest.enrolAWRS(successfulSubscriptionResponse, testBusinessCustomerDetails, businessType, Some(testUtr))
      await(result) shouldBe successfulEnrolResponse
    }

    "create correct EnrolRequest when business type is SOP and UTR present " in {
      val result = EnrolServiceTest.createEnrolment(successfulSubscriptionResponse, testBusinessCustomerDetails, businessTypeSOP, saUtr)
      result shouldBe enrolRequestSAUTR
    }

    "create correct EnrolRequest when business type is other than SOP and UTR present " in {
      val result = EnrolServiceTest.createEnrolment(successfulSubscriptionResponse, testBusinessCustomerDetails, "LTD", ctUtr)
      result shouldBe enrolRequestCTUTR
    }
    "create correct EnrolRequest when business type and UTR NOT present " in {
      val result = EnrolServiceTest.createEnrolment(successfulSubscriptionResponse, testBusinessCustomerDetails,"" , None)
      result shouldBe enrolRequestNoSACT
    }

  }
}
