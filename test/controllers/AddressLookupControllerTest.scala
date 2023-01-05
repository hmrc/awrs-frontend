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

package controllers

import builders.SessionBuilder
import connectors.mock.MockAuthConnector
import connectors.{AddressLookupConnector, AddressLookupErrorResponse, AddressLookupSuccessResponse}
import models.{Address, AddressAudit, AddressAudits}
import org.mockito.ArgumentMatchers.{any, eq => mockEq}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.address.client.v1.RecordSet
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import org.scalatestplus.play.PlaySpec
import utils.TestConstants._

import scala.concurrent.Future

class AddressLookupControllerTest extends PlaySpec with MockitoSugar with BeforeAndAfterEach with MockAuthConnector {

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val postCode: String = testPostcode
  val invalidPostcode: String = "/*/*/"
  val mockAddressLookupConnector: AddressLookupConnector = mock[AddressLookupConnector]
  val testAddressLookupController: AddressLookupController = new AddressLookupController(mockMCC, mockAuthConnector, mockAddressLookupConnector, mockAuditable, mockAppConfig)

  "Postcode lookup controller" must {
    "return Ok response for a valid postcode" in {
      addressLookupValidPostcode {
        result =>
          status(result) must be(OK)
      }
    }

    "return bad request for invalid postcode" in {
      addressLookupInvalidPostcode {
        result =>
          status(result) must be(BAD_REQUEST)
      }
    }

    "return internal server error" in {
      addressLookupInternalServerError {
        result =>
          status(result) must be(INTERNAL_SERVER_ERROR)
      }
    }

    "return ok on an address audit POST route" in {
      val address1One = Address("address Line 1", "address Line 2", None, None, Some("ZZ1 1ZZ"))
      val addressAudit = AddressAudit(Some("id"), Some("postcodeAddressSubmitted"), Some("uprn"), Some(address1One), Some(address1One))
      val addressAudits = AddressAudits(List(addressAudit))

      addressAuditCall(addressAudits) {
        result =>
          status(result) must be(OK)
      }
    }

    "return ok on an address audit POST route for manualAddressSubmitted" in {
      val address1One = Address("address Line 1", "address Line 2", None, None, Some("ZZ1 1ZZ"))
      val addressAudit = AddressAudit(Some("id"), Some("manualAddressSubmitted"), Some("uprn"), Some(address1One), Some(address1One))
      val addressAudits = AddressAudits(List(addressAudit))

      addressAuditCall(addressAudits) {
        result =>
          status(result) must be(OK)
      }
    }

    "return ok on an address audit POST route with country code and no duplicate address " in {
      val address1One = Address("address Line 1", "address Line 2", None, None, None, Some("Country"))
      val addressAudit = AddressAudit(Some("id"), Some("postcodeAddressSubmitted"), Some("uprn"), Some(address1One), None)
      val addressAudits = AddressAudits(List(addressAudit))

      addressAuditCall(addressAudits) {
        result =>
          status(result) must be(OK)
      }
    }

    "return ok on an address audit POST route with an empty list" in {
      val addressAudits = AddressAudits(List())

      addressAuditCall(addressAudits) {
        result =>
          status(result) must be(OK)
      }
    }
  }

  def addressLookupValidPostcode(test: Future[Result] => Any) {
    mock[HeaderCarrier]

    when(testAddressLookupController.addressLookupConnector.lookup(mockEq(postCode))(any[HeaderCarrier], any())) thenReturn Future.successful(AddressLookupSuccessResponse(RecordSet(List())))
    setAuthMocks()
    val result = testAddressLookupController.addressLookup(postCode).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def addressLookupInvalidPostcode(test: Future[Result] => Any) {
    mock[HeaderCarrier]

    when(testAddressLookupController.addressLookupConnector.lookup(mockEq(invalidPostcode))(any[HeaderCarrier], any())) thenReturn Future.successful(AddressLookupErrorResponse(new BadRequestException("")))
    setAuthMocks()
    val result = testAddressLookupController.addressLookup(invalidPostcode).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def addressLookupInternalServerError(test: Future[Result] => Any) {
    mock[HeaderCarrier]

    when(testAddressLookupController.addressLookupConnector.lookup(mockEq(postCode))(any[HeaderCarrier], any())) thenReturn Future.successful(AddressLookupErrorResponse(new Exception("")))
    setAuthMocks()
    val result = testAddressLookupController.addressLookup(postCode).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def addressAuditCall(addressAudits: AddressAudits)(test: Future[Result] => Any): Unit = {
    mock[HeaderCarrier]

    when(testAddressLookupController.addressLookupConnector.lookup(mockEq(postCode))(any[HeaderCarrier], any())) thenReturn Future.successful(AddressLookupSuccessResponse(RecordSet(List())))
    setAuthMocks()

    val fakeRequest: FakeRequest[AnyContent] = if (addressAudits.addressAudits.isEmpty) {
      SessionBuilder.buildRequestWithSession(userId)
    } else {
      SessionBuilder.buildRequestWithSession(userId).withJsonBody(Json.toJson(addressAudits))
    }

    val result = testAddressLookupController.auditAddress().apply(fakeRequest)
    test(result)
  }
}
