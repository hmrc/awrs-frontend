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

package controllers

import java.util.UUID

import audit.TestAudit
import builders.{AuthBuilder, SessionBuilder}
import config.FrontendAuthConnector
import connectors.mock.MockAuthConnector
import controllers.auth.Utr._
import uk.gov.hmrc.address.client.v1.RecordSet
import org.mockito.Matchers.{eq => mockEq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{AddressLookupErrorResponse, AddressLookupService, AddressLookupSuccessResponse}
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestConstants._
import scala.concurrent.Future
import uk.gov.hmrc.http.{ BadRequestException, HeaderCarrier }

class AddressLookupControllerTest extends UnitSpec with MockitoSugar with OneServerPerSuite with BeforeAndAfterEach with MockAuthConnector {

  val request = FakeRequest()
  val postCode = testPostcode
  val invalidPostcode = "/*/*/"

  object TestAddressLookupController extends AddressLookupController {
    override val authConnector = mockAuthConnector
    override val addressLookupService = mock[AddressLookupService]

    override def appName: String = "test"

    override def audit: Audit = new TestAudit
  }

  "Postcode lookup controller" should {

    "use the correct AuthConnector" in {
      AddressLookupController.authConnector shouldBe FrontendAuthConnector
    }

    "return Ok response for a valid postcode" in {
      addressLookupValidPostcode {
        result =>
          status(result) should be(OK)
      }
    }

    "return bad request for invalid postcode" in {
      addressLookupInvalidPostcode {
        result =>
          status(result) should be(BAD_REQUEST)
      }
    }

    "return internal server error" in {
      addressLookupInternalServerError {
        result =>
          status(result) should be(INTERNAL_SERVER_ERROR)
      }
    }
  }

  def addressLookupValidPostcode(test: Future[Result] => Any) {
    implicit val hc = mock[HeaderCarrier]

    when(TestAddressLookupController.addressLookupService.lookup(mockEq(postCode))(any[HeaderCarrier])) thenReturn Future.successful(AddressLookupSuccessResponse(RecordSet(List())))
    val result = TestAddressLookupController.addressLookup(postCode).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def addressLookupInvalidPostcode(test: Future[Result] => Any) {
    implicit val hc = mock[HeaderCarrier]

    when(TestAddressLookupController.addressLookupService.lookup(mockEq(invalidPostcode))(any[HeaderCarrier])) thenReturn Future.successful(AddressLookupErrorResponse(new BadRequestException("")))
    val result = TestAddressLookupController.addressLookup(invalidPostcode).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def addressLookupInternalServerError(test: Future[Result] => Any) {
    implicit val hc = mock[HeaderCarrier]

    when(TestAddressLookupController.addressLookupService.lookup(mockEq(postCode))(any[HeaderCarrier])) thenReturn Future.successful(AddressLookupErrorResponse(new Exception("")))
    val result = TestAddressLookupController.addressLookup(postCode).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }
}
