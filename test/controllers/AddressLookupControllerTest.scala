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

package controllers

import builders.SessionBuilder
import connectors.mock.MockAuthConnector
import uk.gov.hmrc.address.client.v1.RecordSet
import org.mockito.ArgumentMatchers.{any, eq => mockEq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{AddressLookupErrorResponse, AddressLookupService, AddressLookupSuccessResponse}
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestConstants._

import scala.concurrent.Future
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}

class AddressLookupControllerTest extends UnitSpec with MockitoSugar with BeforeAndAfterEach with MockAuthConnector {

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val postCode: String = testPostcode
  val invalidPostcode: String = "/*/*/"
  val mockAddressLookupService: AddressLookupService = mock[AddressLookupService]
  val testAddressLookupController: AddressLookupController = new AddressLookupController(mockMCC, mockAuthConnector, mockAddressLookupService, mockAuditable, mockAppConfig)

  "Postcode lookup controller" should {
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

    when(testAddressLookupController.addressLookupService.lookup(mockEq(postCode))(any[HeaderCarrier], any())) thenReturn Future.successful(AddressLookupSuccessResponse(RecordSet(List())))
    setAuthMocks()
    val result = testAddressLookupController.addressLookup(postCode).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def addressLookupInvalidPostcode(test: Future[Result] => Any) {
    implicit val hc = mock[HeaderCarrier]

    when(testAddressLookupController.addressLookupService.lookup(mockEq(invalidPostcode))(any[HeaderCarrier], any())) thenReturn Future.successful(AddressLookupErrorResponse(new BadRequestException("")))
    setAuthMocks()
    val result = testAddressLookupController.addressLookup(invalidPostcode).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def addressLookupInternalServerError(test: Future[Result] => Any) {
    implicit val hc = mock[HeaderCarrier]

    when(testAddressLookupController.addressLookupService.lookup(mockEq(postCode))(any[HeaderCarrier], any())) thenReturn Future.successful(AddressLookupErrorResponse(new Exception("")))
    setAuthMocks()
    val result = testAddressLookupController.addressLookup(postCode).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }
}
